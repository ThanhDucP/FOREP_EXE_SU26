package com.forep.exe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.persistence.PaymentTransactionEntity;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MomoPaymentService {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public MomoPaymentService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProviderPaymentResult createPayment(PaymentTransactionEntity payment, MomoProviderConfig config) {
        if (!isRealProviderConfigured(config)) {
            throw new IllegalArgumentException("MoMo chưa được cấu hình. Vui lòng đợi quản trị viên cập nhật phương thức thanh toán.");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("partnerCode", config.partnerCode());
        request.put("accessKey", config.accessKey());
        request.put("requestId", payment.getRequestId());
        request.put("amount", momoAmount(payment));
        request.put("orderId", payment.getOrderCode());
        request.put("orderInfo", "FOREP workspace registration " + payment.getOrderCode());
        request.put("redirectUrl", config.returnUrl());
        request.put("ipnUrl", config.notifyUrl());
        request.put("extraData", "");
        request.put("requestType", "captureWallet");
        request.put("lang", "vi");
        request.put("signature", signCreatePaymentRequest(request, config.secretKey()));
        return createRealProviderPayment(request, config.endpoint());
    }

    private ProviderPaymentResult createRealProviderPayment(Map<String, Object> request, String endpoint) {
        String rawRequest = toJson(request);
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(rawRequest, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Map<?, ?> response = objectMapper.readValue(httpResponse.body(), Map.class);
            Object resultCode = response.get("resultCode");
            if (httpResponse.statusCode() >= 400 || (resultCode != null && !"0".equals(String.valueOf(resultCode)))) {
                throw new IllegalStateException("MoMo payment creation failed: " + stringValue(response.get("message")));
            }
            return new ProviderPaymentResult(
                    stringValue(response.get("payUrl")),
                    stringValue(response.get("deeplink")),
                    stringValue(response.get("qrCodeUrl")),
                    rawRequest,
                    httpResponse.body()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create MoMo provider payment.", exception);
        }
    }

    public boolean verifyCallbackSignature(Map<String, ?> payload, String signature, MomoProviderConfig config) {
        if (!isRealProviderConfigured(config)) {
            return false;
        }
        String expected = signCanonical(payload, config.secretKey());
        return hasText(signature) && expected.equalsIgnoreCase(signature);
    }

    private String signCanonical(Map<String, ?> values, String secretKey) {
        StringBuilder canonical = new StringBuilder();
        values.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> !"signature".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (!canonical.isEmpty()) canonical.append('&');
                    canonical.append(entry.getKey()).append('=').append(entry.getValue());
                });
        return hmacSha256(canonical.toString(), secretKey);
    }

    private String signCreatePaymentRequest(Map<String, ?> request, String secretKey) {
        String raw = "accessKey=" + request.get("accessKey")
                + "&amount=" + request.get("amount")
                + "&extraData=" + request.get("extraData")
                + "&ipnUrl=" + request.get("ipnUrl")
                + "&orderId=" + request.get("orderId")
                + "&orderInfo=" + request.get("orderInfo")
                + "&partnerCode=" + request.get("partnerCode")
                + "&redirectUrl=" + request.get("redirectUrl")
                + "&requestId=" + request.get("requestId")
                + "&requestType=" + request.get("requestType");
        return hmacSha256(raw, secretKey);
    }

    private String hmacSha256(String raw, String secretKey) {
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
                && hasText(config.endpoint())
                && hasText(config.partnerCode())
                && hasText(config.accessKey())
                && hasText(config.secretKey())
                && hasText(config.returnUrl())
                && hasText(config.notifyUrl());
    }

    private String momoAmount(PaymentTransactionEntity payment) {
        return payment.getAmount().setScale(0, java.math.RoundingMode.UNNECESSARY).toPlainString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize payment payload.", exception);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record MomoProviderConfig(
            String endpoint,
            String partnerCode,
            String accessKey,
            String secretKey,
            String returnUrl,
            String notifyUrl
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
}
