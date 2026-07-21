package com.forep.exe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.persistence.PaymentTransactionEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.Map;

@Service
public class MomoPaymentService {
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String partnerCode;
    private final String accessKey;
    private final String secretKey;
    private final String returnUrl;
    private final String notifyUrl;
    private final boolean sandboxMode;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public MomoPaymentService(ObjectMapper objectMapper,
                              @Value("${forep.payments.momo.endpoint:}") String endpoint,
                              @Value("${forep.payments.momo.partner-code:}") String partnerCode,
                              @Value("${forep.payments.momo.access-key:}") String accessKey,
                              @Value("${forep.payments.momo.secret-key:}") String secretKey,
                              @Value("${forep.payments.momo.return-url:}") String returnUrl,
                              @Value("${forep.payments.momo.notify-url:}") String notifyUrl,
                              @Value("${forep.payments.momo.sandbox-mode:false}") boolean sandboxMode) {
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.partnerCode = partnerCode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.returnUrl = returnUrl;
        this.notifyUrl = notifyUrl;
        this.sandboxMode = sandboxMode;
    }

    public ProviderPaymentResult createPayment(PaymentTransactionEntity payment) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("partnerCode", partnerCode);
        request.put("accessKey", accessKey);
        request.put("requestId", payment.getRequestId());
        request.put("amount", momoAmount(payment));
        request.put("orderId", payment.getOrderCode());
        request.put("orderInfo", "FOREP workspace registration " + payment.getOrderCode());
        request.put("redirectUrl", returnUrl);
        request.put("ipnUrl", notifyUrl);
        request.put("extraData", "");
        request.put("requestType", "captureWallet");
        request.put("lang", "vi");
        request.put("signature", signCreatePaymentRequest(request));

        if (!isRealProviderConfigured()) {
            throw new IllegalArgumentException("MoMo chưa được cấu hình. Vui lòng đợi quản trị viên cập nhật phương thức thanh toán.");
        }
        return createRealProviderPayment(request);
    }

    private ProviderPaymentResult createRealProviderPayment(Map<String, Object> request) {
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
                    null,
                    null,
                    null,
                    null,
                    rawRequest,
                    httpResponse.body()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create MoMo provider payment.", exception);
        }
    }

    private ProviderPaymentResult createSandboxPayment(PaymentTransactionEntity payment, Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", "MOMO");
        response.put("mode", sandboxMode ? "SANDBOX_MODE" : "SANDBOX_INSTRUCTIONS_MISSING_PROVIDER_CONFIG");
        response.put("orderId", payment.getOrderCode());
        response.put("requestId", payment.getRequestId());
        response.put("amount", payment.getAmount());
        response.put("payUrl", "momo://pay?orderId=" + payment.getOrderCode());
        response.put("deeplink", "momo://payment?action=pay&orderId=" + payment.getOrderCode());
        response.put("qrCodeUrl", "https://api.qrserver.com/v1/create-qr-code/?size=260x260&data=MOMO-" + payment.getOrderCode());

        return new ProviderPaymentResult(
                stringValue(response.get("payUrl")),
                stringValue(response.get("deeplink")),
                stringValue(response.get("qrCodeUrl")),
                null,
                null,
                null,
                null,
                toJson(request),
                toJson(response)
        );
    }

    public boolean verifyCallbackSignature(Map<String, ?> payload, String signature) {
        if (!hasText(secretKey)) {
            return sandboxMode;
        }
        String expected = signCanonical(payload);
        return hasText(signature) && expected.equalsIgnoreCase(signature);
    }

    private String signCanonical(Map<String, ?> values) {
        if (!hasText(secretKey)) {
            return "";
        }
        StringBuilder canonical = new StringBuilder();
        values.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> !"signature".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (!canonical.isEmpty()) canonical.append('&');
                    canonical.append(entry.getKey()).append('=').append(entry.getValue());
                });
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign MoMo payment payload.", exception);
        }
    }

    private String signCreatePaymentRequest(Map<String, ?> request) {
        if (!hasText(secretKey)) {
            return "";
        }
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
        return hmacSha256(raw);
    }

    private String hmacSha256(String raw) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign MoMo payment payload.", exception);
        }
    }

    public boolean isRealProviderConfigured() {
        return !sandboxMode
                && hasText(endpoint)
                && hasText(partnerCode)
                && hasText(accessKey)
                && hasText(secretKey)
                && hasText(returnUrl)
                && hasText(notifyUrl);
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

    public record ProviderPaymentResult(
            String paymentUrl,
            String deeplink,
            String qrCodeUrl,
            String bankCode,
            String bankName,
            String bankAccountNumber,
            String bankAccountName,
            String rawRequest,
            String rawResponse
    ) {
    }
}
