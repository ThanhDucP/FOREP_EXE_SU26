package com.forep.exe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.persistence.PaymentTransactionEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
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

    public MomoPaymentService(ObjectMapper objectMapper,
                              @Value("${forep.payments.momo.endpoint:}") String endpoint,
                              @Value("${forep.payments.momo.partner-code:}") String partnerCode,
                              @Value("${forep.payments.momo.access-key:}") String accessKey,
                              @Value("${forep.payments.momo.secret-key:}") String secretKey,
                              @Value("${forep.payments.momo.return-url:}") String returnUrl,
                              @Value("${forep.payments.momo.notify-url:}") String notifyUrl) {
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.partnerCode = partnerCode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.returnUrl = returnUrl;
        this.notifyUrl = notifyUrl;
    }

    public ProviderPaymentResult createPayment(PaymentTransactionEntity payment) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("partnerCode", partnerCode);
        request.put("accessKey", accessKey);
        request.put("requestId", payment.getRequestId());
        request.put("amount", payment.getAmount());
        request.put("orderId", payment.getOrderCode());
        request.put("orderInfo", "FOREP workspace registration " + payment.getOrderCode());
        request.put("redirectUrl", returnUrl);
        request.put("ipnUrl", notifyUrl);
        request.put("requestType", "captureWallet");
        request.put("signature", signCanonical(request));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", "MOMO");
        response.put("mode", hasText(endpoint) ? "CONFIGURED" : "SANDBOX_INSTRUCTIONS");
        response.put("orderId", payment.getOrderCode());
        response.put("requestId", payment.getRequestId());
        response.put("amount", payment.getAmount());
        response.put("payUrl", hasText(endpoint) ? endpoint + "?orderId=" + payment.getOrderCode() : "momo://pay?orderId=" + payment.getOrderCode());
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
            return true;
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
