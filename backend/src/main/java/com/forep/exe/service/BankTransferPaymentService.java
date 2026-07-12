package com.forep.exe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.service.MomoPaymentService.ProviderPaymentResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class BankTransferPaymentService {
    private final ObjectMapper objectMapper;
    private final String bankCode;
    private final String bankName;
    private final String accountNumber;
    private final String accountName;
    private final String vietQrTemplate;
    private final String webhookSecret;

    public BankTransferPaymentService(ObjectMapper objectMapper,
                                      @Value("${forep.payments.bank.bank-code:}") String bankCode,
                                      @Value("${forep.payments.bank.bank-name:}") String bankName,
                                      @Value("${forep.payments.bank.account-number:}") String accountNumber,
                                      @Value("${forep.payments.bank.account-name:}") String accountName,
                                      @Value("${forep.payments.bank.vietqr-template:https://img.vietqr.io/image/{bankCode}-{accountNumber}-compact2.png?amount={amount}&addInfo={content}&accountName={accountName}}") String vietQrTemplate,
                                      @Value("${forep.payments.bank.webhook-secret:}") String webhookSecret) {
        this.objectMapper = objectMapper;
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.vietQrTemplate = vietQrTemplate;
        this.webhookSecret = webhookSecret;
    }

    public ProviderPaymentResult createPayment(PaymentTransactionEntity payment) {
        String configuredBankCode = hasText(bankCode) ? bankCode : "BANK";
        String configuredAccountNumber = hasText(accountNumber) ? accountNumber : "0000000000";
        String configuredAccountName = hasText(accountName) ? accountName : "FOREP PLATFORM";
        String qrCodeUrl = vietQrTemplate
                .replace("{bankCode}", url(configuredBankCode))
                .replace("{accountNumber}", url(configuredAccountNumber))
                .replace("{amount}", url(payment.getAmount().toPlainString()))
                .replace("{content}", url(payment.getTransferContent()))
                .replace("{accountName}", url(configuredAccountName));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("bankCode", configuredBankCode);
        request.put("accountNumber", configuredAccountNumber);
        request.put("accountName", configuredAccountName);
        request.put("amount", payment.getAmount());
        request.put("transferContent", payment.getTransferContent());

        Map<String, Object> response = new LinkedHashMap<>(request);
        response.put("provider", "VIETQR");
        response.put("bankName", hasText(bankName) ? bankName : configuredBankCode);
        response.put("qrCodeUrl", qrCodeUrl);

        return new ProviderPaymentResult(
                null,
                null,
                qrCodeUrl,
                configuredBankCode,
                hasText(bankName) ? bankName : configuredBankCode,
                configuredAccountNumber,
                configuredAccountName,
                toJson(request),
                toJson(response)
        );
    }

    public boolean verifyCallbackSignature(Map<String, ?> payload, String signature) {
        if (!hasText(webhookSecret)) {
            return false;
        }
        String expected = signCanonical(payload);
        return hasText(signature) && expected.equalsIgnoreCase(signature);
    }

    private String signCanonical(Map<String, ?> values) {
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
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not verify bank transfer callback signature.", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize bank transfer payload.", exception);
        }
    }

    private String url(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
