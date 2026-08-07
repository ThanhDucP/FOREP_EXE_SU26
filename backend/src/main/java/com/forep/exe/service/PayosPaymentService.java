package com.forep.exe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PayosPaymentService {
    static final String CREATE_PATH = "/v2/payment-requests";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    @Autowired
    public PayosPaymentService(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), DEFAULT_TIMEOUT);
    }

    PayosPaymentService(ObjectMapper objectMapper, HttpClient httpClient, Duration requestTimeout) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
    }

    public ProviderPaymentResult createPayment(long orderCode, long amount, String description,
                                               List<Map<String, Object>> items, PayosProviderConfig config) {
        if (!isConfigured(config)) throw new IllegalArgumentException("PayOS provider is not fully configured.");
        if (orderCode <= 0 || amount <= 0) throw new IllegalArgumentException("PayOS orderCode and amount must be positive integers.");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("orderCode", orderCode);
        request.put("amount", amount);
        request.put("description", description);
        request.put("returnUrl", config.returnUrl());
        request.put("cancelUrl", config.cancelUrl());
        request.put("items", items == null ? List.of() : items);
        request.put("signature", signCreatePayment(orderCode, amount, description, config.returnUrl(), config.cancelUrl(), config.checksumKey()));
        String rawBody = json(request);
        Map<String, Object> sanitized = new LinkedHashMap<>(request);
        sanitized.remove("signature");
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(createEndpoint(config.apiEndpoint())))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .header("x-client-id", config.clientId())
                    .header("x-api-key", config.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(rawBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Map<?, ?> envelope = objectMapper.readValue(response.body(), Map.class);
            Map<?, ?> data = envelope.get("data") instanceof Map<?, ?> map ? map : Map.of();
            String code = string(envelope.get("code"));
            String checkoutUrl = string(data.get("checkoutUrl"));
            String paymentLinkId = string(data.get("paymentLinkId"));
            if (response.statusCode() >= 400 || !"00".equals(code) || blank(checkoutUrl) || blank(paymentLinkId)) {
                throw new PayosProviderException("PayOS rejected payment creation: " + safeMessage(envelope.get("desc")));
            }
            return new ProviderPaymentResult(checkoutUrl, paymentLinkId, json(sanitized), response.body(), code);
        } catch (HttpTimeoutException exception) {
            throw new PayosProviderException("PayOS payment creation timed out.", exception);
        } catch (PayosProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PayosProviderException("Could not create PayOS payment link.", exception);
        }
    }

    /**
     * Server-to-server status lookup used as the authoritative payment confirmation mechanism.
     * Browser callback parameters are never trusted as proof of payment. When PayOS reports PAID,
     * the reconciliation pipeline activates the workspace immediately.
     */
    public ProviderPaymentStatus getPaymentStatus(String orderCode, PayosProviderConfig config) {
        if (!isConfigured(config)) throw new IllegalArgumentException("PayOS provider is not fully configured.");
        if (blank(orderCode)) throw new IllegalArgumentException("PayOS orderCode is required.");
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(paymentStatusEndpoint(config.apiEndpoint(), orderCode)))
                    .timeout(requestTimeout)
                    .header("Accept", "application/json")
                    .header("x-client-id", config.clientId())
                    .header("x-api-key", config.apiKey())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Map<?, ?> envelope = objectMapper.readValue(response.body(), Map.class);
            Map<?, ?> data = envelope.get("data") instanceof Map<?, ?> map ? map : Map.of();
            String code = string(envelope.get("code"));
            if (response.statusCode() >= 400 || !"00".equals(code)) {
                throw new PayosProviderException("PayOS rejected payment status lookup: " + safeMessage(envelope.get("desc")));
            }
            String status = string(data.get("status"));
            String paymentLinkId = string(data.get("id"));
            long amount = longValue(data.get("amount"));
            long amountPaid = longValue(data.get("amountPaid"));
            if (blank(status)) {
                throw new PayosProviderException("PayOS payment status response did not contain a status.");
            }
            return new ProviderPaymentStatus(status, amount, amountPaid, paymentLinkId, response.body());
        } catch (HttpTimeoutException exception) {
            throw new PayosProviderException("PayOS payment status lookup timed out.", exception);
        } catch (PayosProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PayosProviderException("Could not query PayOS payment status.", exception);
        }
    }

    public String createRawSignature(long orderCode, long amount, String description, String returnUrl, String cancelUrl) {
        return "amount=" + amount + "&cancelUrl=" + cancelUrl + "&description=" + description
                + "&orderCode=" + orderCode + "&returnUrl=" + returnUrl;
    }

    public String signCreatePayment(long orderCode, long amount, String description, String returnUrl,
                                    String cancelUrl, String checksumKey) {
        return hmacSha256(createRawSignature(orderCode, amount, description, returnUrl, cancelUrl), checksumKey);
    }

    /**
     * Legacy compatibility only. Runtime PayOS activation no longer depends on webhook delivery.
     * The legacy webhook endpoint is blocked by Spring Security and can be removed once the old
     * /api/v1 controller surface is retired.
     */
    @Deprecated
    public boolean verifyWebhook(Map<String, Object> data, String signature, String checksumKey) {
        if (data == null || blank(signature) || blank(checksumKey)) return false;
        String expected = hmacSha256(webhookRawSignature(data), checksumKey);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                signature.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    // Package-private only for legacy tests that are still compiled by Maven even with -DskipTests.
    // Runtime payment activation does not call this helper or any webhook endpoint.
    String webhookRawSignature(Map<String, Object> data) {
        return data.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + signatureValue(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    String hmacSha256(String raw, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign PayOS payload.", exception);
        }
    }

    public String normalizeBaseUrl(String value) {
        if (blank(value)) return null;
        String normalized = value.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        if (normalized.endsWith(CREATE_PATH)) normalized = normalized.substring(0, normalized.length() - CREATE_PATH.length());
        return normalized;
    }

    public String createEndpoint(String baseUrl) { return normalizeBaseUrl(baseUrl) + CREATE_PATH; }

    public String paymentStatusEndpoint(String baseUrl, String orderCode) {
        return normalizeBaseUrl(baseUrl) + CREATE_PATH + "/" + orderCode;
    }

    public boolean isConfigured(PayosProviderConfig config) {
        return config != null && !blank(config.apiEndpoint()) && !blank(config.clientId())
                && !blank(config.apiKey()) && !blank(config.checksumKey())
                && !blank(config.returnUrl()) && !blank(config.cancelUrl());
    }

    private long longValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(value.toString()); }
        catch (NumberFormatException exception) { return 0L; }
    }

    private String signatureValue(Object value) {
        if (value == null) return "";
        if (value instanceof List<?> list) {
            List<Object> sorted = new ArrayList<>();
            for (Object item : list) sorted.add(item instanceof Map<?, ?> map ? sortedMap(map) : item);
            return json(sorted);
        }
        if (value instanceof Map<?, ?> map) return json(sortedMap(map));
        return value.toString();
    }

    private Map<String, Object> sortedMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> result.put(entry.getKey().toString(), entry.getValue()));
        return result;
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Could not serialize PayOS payload.", exception); }
    }
    private String string(Object value) { return value == null ? null : value.toString(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String safeMessage(Object value) { String text = string(value); return blank(text) ? "unknown provider error" : text; }

    public record PayosProviderConfig(String apiEndpoint, String clientId, String apiKey, String checksumKey,
                                      String returnUrl, String cancelUrl) {}
    public record ProviderPaymentResult(String checkoutUrl, String paymentLinkId, String rawRequest,
                                        String rawResponse, String responseCode) {}
    public record ProviderPaymentStatus(String status, long amount, long amountPaid, String paymentLinkId,
                                        String rawResponse) {}
    public static class PayosProviderException extends IllegalStateException {
        public PayosProviderException(String message) { super(message); }
        public PayosProviderException(String message, Throwable cause) { super(message, cause); }
    }
}
