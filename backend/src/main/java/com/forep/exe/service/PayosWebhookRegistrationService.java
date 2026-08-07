package com.forep.exe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.persistence.PayosConfigEntity;
import com.forep.exe.persistence.PayosConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps the PayOS payment-channel webhook pointed at this backend.
 *
 * payOS requires POST /confirm-webhook with x-client-id/x-api-key before it will deliver
 * payment notifications to the merchant webhook. The system used to configure only returnUrl
 * and cancelUrl, which meant a paid transaction could remain PENDING forever when no webhook
 * had been registered in the PayOS channel.
 *
 * This service registers the webhook after application startup and periodically re-confirms it.
 * Payment activation is still idempotent and also has provider-status reconciliation as fallback.
 */
@Service
public class PayosWebhookRegistrationService {
    private static final Logger log = LoggerFactory.getLogger(PayosWebhookRegistrationService.class);
    private static final UUID PAYOS_CONFIG_ID = UUID.fromString("39000000-0000-0000-0000-000000000002");
    private static final String CONFIRM_WEBHOOK_PATH = "/confirm-webhook";

    private final PayosConfigRepository payosConfigs;
    private final PayosCredentialCipher credentialCipher;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String webhookUrl;

    private volatile String lastConfirmedFingerprint;

    public PayosWebhookRegistrationService(
            PayosConfigRepository payosConfigs,
            PayosCredentialCipher credentialCipher,
            ObjectMapper objectMapper,
            @Value("${forep.payos.webhook-url:https://forep-exe-backend.onrender.com/api/payments/payos/webhook}") String webhookUrl) {
        this.payosConfigs = payosConfigs;
        this.credentialCipher = credentialCipher;
        this.objectMapper = objectMapper;
        this.webhookUrl = normalizeWebhookUrl(webhookUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerOnStartup() {
        confirmConfiguredWebhook(false);
    }

    /**
     * Re-check frequently enough that changing PayOS credentials/config in admin does not require
     * a server restart. The fingerprint prevents unnecessary PayOS calls while configuration is
     * unchanged. Set forep.payos.webhook-refresh-ms to tune the interval.
     */
    @Scheduled(
            fixedDelayString = "${forep.payos.webhook-refresh-ms:60000}",
            initialDelayString = "${forep.payos.webhook-refresh-initial-ms:15000}"
    )
    public void refreshWebhookRegistration() {
        confirmConfiguredWebhook(false);
    }

    public WebhookRegistrationResult forceConfirm() {
        return confirmConfiguredWebhook(true);
    }

    private synchronized WebhookRegistrationResult confirmConfiguredWebhook(boolean force) {
        PayosConfigEntity config = payosConfigs.findById(PAYOS_CONFIG_ID).orElse(null);
        if (config == null) {
            return WebhookRegistrationResult.skipped("PayOS configuration does not exist yet.");
        }
        if (!config.isActive()) {
            return WebhookRegistrationResult.skipped("PayOS configuration is disabled.");
        }
        if (isBlank(config.getApiEndpoint()) || isBlank(config.getClientId())
                || isBlank(config.getApiKeyEncrypted())) {
            return WebhookRegistrationResult.skipped("PayOS configuration is incomplete.");
        }
        if (isBlank(webhookUrl)) {
            return WebhookRegistrationResult.skipped("PayOS webhook URL is empty.");
        }

        String fingerprint = config.getUpdatedAt() + "|" + config.getApiEndpoint() + "|"
                + config.getClientId() + "|" + webhookUrl;
        if (!force && fingerprint.equals(lastConfirmedFingerprint)) {
            return WebhookRegistrationResult.skipped("Webhook is already confirmed for the current configuration.");
        }

        try {
            String apiKey = credentialCipher.decrypt(config.getApiKeyEncrypted());
            if (isBlank(apiKey)) {
                return WebhookRegistrationResult.failed("PayOS API key could not be decrypted.", null);
            }

            String endpoint = normalizeBaseUrl(config.getApiEndpoint()) + CONFIRM_WEBHOOK_PATH;
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("webhookUrl", webhookUrl);
            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("x-client-id", config.getClientId())
                    .header("x-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            Map<?, ?> envelope = objectMapper.readValue(response.body(), Map.class);
            String code = envelope.get("code") == null ? null : envelope.get("code").toString();
            String desc = envelope.get("desc") == null ? null : envelope.get("desc").toString();

            if (response.statusCode() >= 200 && response.statusCode() < 300 && "00".equals(code)) {
                lastConfirmedFingerprint = fingerprint;
                log.info("PayOS webhook confirmed successfully. webhookUrl={} endpoint={}", webhookUrl, endpoint);
                return WebhookRegistrationResult.success(webhookUrl, response.body());
            }

            log.error("PayOS webhook confirmation failed. httpStatus={} code={} desc={} webhookUrl={}",
                    response.statusCode(), code, desc, webhookUrl);
            return WebhookRegistrationResult.failed(
                    "PayOS rejected webhook registration. HTTP " + response.statusCode()
                            + (desc == null ? "" : ": " + desc),
                    response.body()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("PayOS webhook confirmation interrupted. webhookUrl={}", webhookUrl);
            return WebhookRegistrationResult.failed("PayOS webhook confirmation was interrupted.", null);
        } catch (Exception exception) {
            log.error("Could not confirm PayOS webhook URL {}: {}", webhookUrl, exception.getMessage(), exception);
            return WebhookRegistrationResult.failed(
                    "Could not confirm PayOS webhook: " + exception.getMessage(),
                    null
            );
        }
    }

    private String normalizeBaseUrl(String value) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("PayOS API endpoint is required.");
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v2/payment-requests")) {
            normalized = normalized.substring(0, normalized.length() - "/v2/payment-requests".length());
        }
        return normalized;
    }

    private String normalizeWebhookUrl(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record WebhookRegistrationResult(
            boolean success,
            boolean skipped,
            String webhookUrl,
            String message,
            String providerResponse
    ) {
        static WebhookRegistrationResult success(String webhookUrl, String providerResponse) {
            return new WebhookRegistrationResult(true, false, webhookUrl,
                    "PayOS webhook confirmed successfully.", providerResponse);
        }

        static WebhookRegistrationResult skipped(String message) {
            return new WebhookRegistrationResult(false, true, null, message, null);
        }

        static WebhookRegistrationResult failed(String message, String providerResponse) {
            return new WebhookRegistrationResult(false, false, null, message, providerResponse);
        }
    }
}
