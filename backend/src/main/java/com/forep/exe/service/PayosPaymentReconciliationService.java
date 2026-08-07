package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentMethod;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.dto.Requests.PayosWebhookRequest;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.PayosConfigEntity;
import com.forep.exe.persistence.PayosConfigRepository;
import com.forep.exe.service.PayosPaymentService.PayosProviderConfig;
import com.forep.exe.service.PayosPaymentService.ProviderPaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reconciles a local pending PayOS transaction against PayOS server-to-server status.
 *
 * This is deliberately invoked from payment-status reads after the browser returns from PayOS,
 * so a successful PayOS payment immediately activates the workspace even when the asynchronous
 * webhook is delayed or missed. Browser callback/query parameters are never trusted.
 *
 * The existing webhook remains an idempotent secondary delivery mechanism.
 */
@Service
public class PayosPaymentReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(PayosPaymentReconciliationService.class);
    private static final UUID PAYOS_CONFIG_ID = UUID.fromString("39000000-0000-0000-0000-000000000002");

    private final PaymentTransactionRepository paymentTransactions;
    private final PayosConfigRepository payosConfigs;
    private final PayosPaymentService payosPaymentService;
    private final PayosCredentialCipher credentialCipher;
    private final ForepService forepService;

    public PayosPaymentReconciliationService(
            PaymentTransactionRepository paymentTransactions,
            PayosConfigRepository payosConfigs,
            PayosPaymentService payosPaymentService,
            PayosCredentialCipher credentialCipher,
            ForepService forepService) {
        this.paymentTransactions = paymentTransactions;
        this.payosConfigs = payosConfigs;
        this.payosPaymentService = payosPaymentService;
        this.credentialCipher = credentialCipher;
        this.forepService = forepService;
    }

    public void reconcileByPaymentCode(String paymentCode) {
        paymentTransactions.findByPaymentCode(paymentCode).ifPresent(this::reconcile);
    }

    public void reconcileByOrderCode(String orderCode) {
        paymentTransactions.findByOrderCode(orderCode).ifPresent(this::reconcile);
    }

    private void reconcile(PaymentTransactionEntity payment) {
        if (payment.getPaymentMethod() != PaymentMethod.PAYOS) {
            return;
        }
        if (payment.getStatus() == PaymentTransactionStatus.SUCCESS
                || payment.getStatus() == PaymentTransactionStatus.PAID) {
            return;
        }
        if (payment.getStatus() == PaymentTransactionStatus.FAILED
                || payment.getStatus() == PaymentTransactionStatus.CANCELLED
                || payment.getStatus() == PaymentTransactionStatus.REFUNDED) {
            return;
        }

        PayosConfigEntity setting = payosConfigs.findById(PAYOS_CONFIG_ID).orElse(null);
        if (setting == null || !setting.isActive()) {
            log.warn("Skipped PayOS reconciliation for orderCode={} because PayOS is not configured/enabled.", payment.getOrderCode());
            return;
        }

        try {
            PayosProviderConfig config = new PayosProviderConfig(
                    setting.getApiEndpoint(),
                    setting.getClientId(),
                    credentialCipher.decrypt(setting.getApiKeyEncrypted()),
                    credentialCipher.decrypt(setting.getChecksumKeyEncrypted()),
                    setting.getReturnUrl(),
                    setting.getCancelUrl()
            );
            ProviderPaymentStatus providerStatus = payosPaymentService.getPaymentStatus(payment.getOrderCode(), config);
            if (!"PAID".equalsIgnoreCase(providerStatus.status())) {
                return;
            }

            // Protect against activating a workspace for a different amount/order.
            long expectedAmount = payment.getAmount().longValueExact();
            if (providerStatus.amount() > 0 && providerStatus.amount() != expectedAmount) {
                log.error("PayOS reconciliation amount mismatch orderCode={} expected={} actual={}",
                        payment.getOrderCode(), expectedAmount, providerStatus.amount());
                return;
            }
            if (providerStatus.amountPaid() > 0 && providerStatus.amountPaid() < expectedAmount) {
                log.warn("PayOS reconciliation ignored underpaid orderCode={} expected={} paid={}",
                        payment.getOrderCode(), expectedAmount, providerStatus.amountPaid());
                return;
            }

            String paymentLinkId = providerStatus.paymentLinkId();
            if (paymentLinkId == null || paymentLinkId.isBlank()) {
                paymentLinkId = payment.getPaymentLinkId();
            }
            if (paymentLinkId == null || paymentLinkId.isBlank()) {
                log.error("PayOS PAID status has no paymentLinkId for orderCode={}", payment.getOrderCode());
                return;
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderCode", Long.parseLong(payment.getOrderCode()));
            data.put("amount", expectedAmount);
            data.put("paymentLinkId", paymentLinkId);
            data.put("code", "00");

            String checksumKey = credentialCipher.decrypt(setting.getChecksumKeyEncrypted());
            String signature = payosPaymentService.hmacSha256(
                    payosPaymentService.webhookRawSignature(data),
                    checksumKey
            );

            // Reuse the exact same idempotent payment-confirmation + workspace-activation pipeline
            // as the real webhook instead of duplicating activation logic.
            forepService.handlePayosWebhook(new PayosWebhookRequest(
                    "00",
                    "Reconciled directly from PayOS payment status",
                    true,
                    data,
                    signature
            ));

            log.info("Reconciled PayOS PAID orderCode={} and triggered workspace activation.", payment.getOrderCode());
        } catch (Exception exception) {
            // Status reads must still return the last persisted state if PayOS is temporarily unavailable.
            log.warn("Could not reconcile PayOS orderCode={}: {}", payment.getOrderCode(), exception.getMessage());
        }
    }
}
