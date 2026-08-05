package com.forep.exe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.persistence.PaymentTransactionEntity;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

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
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MomoPaymentService {
    static final String CREATE_PATH = "/v2/gateway/api/create";
    static final String QUERY_PATH = "/v2/gateway/api/query";
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    @Autowired
    public MomoPaymentService(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), DEFAULT_REQUEST_TIMEOUT);
    }

    MomoPaymentService(ObjectMapper objectMapper, HttpClient httpClient) {
        this(objectMapper, httpClient, DEFAULT_REQUEST_TIMEOUT);
    }

    MomoPaymentService(ObjectMapper objectMapper, HttpClient httpClient, Duration requestTimeout) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
    }

    public ProviderPaymentResult createPayment(PaymentTransactionEntity payment, MomoProviderConfig config) {
        if (!isRealProviderConfigured(config)) {
            throw new IllegalArgumentException("MoMo payment provider is not fully configured.");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("partnerCode", config.partnerCode());
        request.put("requestId", payment.getRequestId());
        request.put("amount", momoAmount(payment));
        request.put("orderId", payment.getOrderCode());
        request.put("orderInfo", "FOREP workspace registration " + payment.getOrderCode());
        request.put("redirectUrl", config.returnUrl());
        request.put("ipnUrl", config.ipnUrl());
        request.put("extraData", "");
        request.put("requestType", "captureWallet");
        request.put("lang", "vi");
        request.put("signature", signCreatePaymentRequest(request, config.accessKey(), config.secretKey()));
        return createRealProviderPayment(request, createEndpoint(config.baseUrl()));
    }

    private ProviderPaymentResult createRealProviderPayment(Map<String, Object> request, String endpoint) {
        String providerRequest = toJson(request);
        Map<String, Object> persistedRequest = new LinkedHashMap<>(request);
        persistedRequest.remove("signature");
        String sanitizedRequest = toJson(persistedRequest);
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(providerRequest, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Map<?, ?> response = objectMapper.readValue(httpResponse.body(), Map.class);
            String resultCode = stringValue(response.get("resultCode"));
            String payUrl = stringValue(response.get("payUrl"));
            if (httpResponse.statusCode() >= 400 || !"0".equals(resultCode) || !hasText(payUrl)) {
                throw new MomoProviderException("MoMo rejected payment creation: " + safeProviderMessage(response.get("message")));
            }
            return new ProviderPaymentResult(
                    payUrl,
                    stringValue(response.get("deeplink")),
                    stringValue(response.get("qrCodeUrl")),
                    sanitizedRequest,
                    httpResponse.body()
            );
        } catch (HttpTimeoutException exception) {
            throw new MomoProviderException("MoMo payment creation timed out.", exception);
        } catch (MomoProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MomoProviderException("Could not create MoMo provider payment.", exception);
        }
    }

    public boolean verifyIpnSignature(Map<String, ?> payload, String signature, MomoProviderConfig config) {
        if (!isRealProviderConfigured(config) || !hasText(signature)) {
            return false;
        }
        String expected = signIpnPayload(payload, config.accessKey(), config.secretKey());
        return MessageDigest.isEqual(
                expected.toLowerCase().getBytes(StandardCharsets.US_ASCII),
                signature.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII)
        );
    }

    String signCreatePaymentRequest(Map<String, ?> request, String accessKey, String secretKey) {
        return hmacSha256(createRawSignature(request, accessKey), secretKey);
    }

    String createRawSignature(Map<String, ?> request, String accessKey) {
        return "accessKey=" + accessKey
                + "&amount=" + value(request, "amount")
                + "&extraData=" + value(request, "extraData")
                + "&ipnUrl=" + value(request, "ipnUrl")
                + "&orderId=" + value(request, "orderId")
                + "&orderInfo=" + value(request, "orderInfo")
                + "&partnerCode=" + value(request, "partnerCode")
                + "&redirectUrl=" + value(request, "redirectUrl")
                + "&requestId=" + value(request, "requestId")
                + "&requestType=" + value(request, "requestType");
    }

    String signIpnPayload(Map<String, ?> payload, String accessKey, String secretKey) {
        return hmacSha256(ipnRawSignature(payload, accessKey), secretKey);
    }

    String ipnRawSignature(Map<String, ?> payload, String accessKey) {
        return "accessKey=" + accessKey
                + "&amount=" + value(payload, "amount")
                + "&extraData=" + value(payload, "extraData")
                + "&message=" + value(payload, "message")
                + "&orderId=" + value(payload, "orderId")
                + "&orderInfo=" + value(payload, "orderInfo")
                + "&orderType=" + value(payload, "orderType")
                + "&partnerCode=" + value(payload, "partnerCode")
                + "&payType=" + value(payload, "payType")
                + "&requestId=" + value(payload, "requestId")
                + "&responseTime=" + value(payload, "responseTime")
                + "&resultCode=" + value(payload, "resultCode")
                + "&transId=" + value(payload, "transId");
    }

    String hmacSha256(String raw, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign MoMo payment payload.", exception);
        }
    }

    public boolean isRealProviderConfigured(MomoProviderConfig config) {
        return config != null
                && hasText(config.baseUrl())
                && hasText(config.partnerCode())
                && hasText(config.accessKey())
                && hasText(config.secretKey())
                && hasText(config.returnUrl())
                && hasText(config.ipnUrl());
    }

    public String normalizeBaseUrl(String value) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith(CREATE_PATH)) {
            normalized = normalized.substring(0, normalized.length() - CREATE_PATH.length());
        } else if (normalized.endsWith(QUERY_PATH)) {
            normalized = normalized.substring(0, normalized.length() - QUERY_PATH.length());
        }
        return normalized;
    }

    public String createEndpoint(String baseUrl) {
        return normalizeBaseUrl(baseUrl) + CREATE_PATH;
    }

    public String queryEndpoint(String baseUrl) {
        return normalizeBaseUrl(baseUrl) + QUERY_PATH;
    }

    private String momoAmount(PaymentTransactionEntity payment) {
        if (payment.getAmount() == null || payment.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("MoMo amount must be a positive integer.");
        }
        try {
            return payment.getAmount().setScale(0, java.math.RoundingMode.UNNECESSARY).toPlainString();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("MoMo amount must be a positive integer.", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize payment payload.", exception);
        }
    }

    private String safeProviderMessage(Object value) {
        String message = stringValue(value);
        return hasText(message) ? message : "unknown provider error";
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String value(Map<String, ?> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : value.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record MomoProviderConfig(
            String baseUrl,
            String partnerCode,
            String accessKey,
            String secretKey,
            String returnUrl,
            String ipnUrl
    ) {
    }

    public record ProviderPaymentResult(
            String paymentUrl,
            String deeplink,
            String qrCodeUrl,
            String rawRequest,
            String rawResponse
    ) {
    }

    public static class MomoProviderException extends IllegalStateException {
        public MomoProviderException(String message) {
            super(message);
        }

        public MomoProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
