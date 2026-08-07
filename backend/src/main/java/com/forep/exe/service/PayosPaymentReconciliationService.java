package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentMethod;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.PayosConfigEntity;
import com.forep.exe.persistence.PayosConfigRepository;
import com.forep.exe.service.PayosPaymentService.PayosProviderConfig;
import com.forep.exe.service.PayosPaymentService.ProviderPaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Reconciles a local pending PayOS transaction against PayOS server-to-server status.
 *
 * This service is invoked from payment-status reads and by the proactive pending-payment monitor.
 * When PayOS reports PAID, the payment is sent directly into FOREP's existing confirmation +
 * workspace activation pipeline. No fake webhook is created and browser callback parameters are
 * never trusted as proof of payment.
 */
@Service
public class PayosPaymentReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(PayosPaymentReconciliationService.class);
    private static final UUID PAYOS_CONFIG_ID = UUID.fromString("39000000-0000-0000-0000-000000000002");

    private final PaymentTransactionRepository paymentTransactions;
    private final PayosConfigRepository payosConfigs;
    private final PayosPaymentService payosPaymentService;
    private final PayosCredentialCipher credentialCipher;
    private final PayosWorkspaceActivationService activationService;

    public PayosPaymentReconciliationService(
            PaymentTransactionRepository paymentTransactions,
            PayosConfigRepository payosConfigs,
            PayosPaymentService payosPaymentService,
            PayosCredentialCipher credentialCipher,
            PayosWorkspaceActivationService activationService) {
        this.paymentTransactions = paymentTransactions;
        this.payosConfigs = payosConfigs;
        this.payosPaymentService = payosPaymentService;
        this.credentialCipher = credentialCipher;
        this.activationService = activationService;
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
                || payment.getStatus() == PaymentTransactionStatus.REFUNDED
                || payment.getStatus() == PaymentTransactionStatus.EXPIRED) {
            return;
        }

        PayosConfigEntity setting = payosConfigs.findById(PAYOS_CONFIG_ID).orElse(null);
        if (setting == null || !setting.isActive()) {
            log.warn("Skipped PayOS reconciliation orderCode={} because PayOS is not configured/enabled.",
                    payment.getOrderCode());
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
            log.info("PayOS status lookup orderCode={} localStatus={} providerStatus={} amount={} amountPaid={}",
                    payment.getOrderCode(), payment.getStatus(), providerStatus.status(),
                    providerStatus.amount(), providerStatus.amountPaid());

            if (!"PAID".equalsIgnoreCase(providerStatus.status())) {
                return;
            }

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
            if (paymentLinkId != null && !paymentLinkId.isBlank()) {
                if (payment.getPaymentLinkId() != null
                        && !payment.getPaymentLinkId().isBlank()
                        && !paymentLinkId.equals(payment.getPaymentLinkId())) {
                    log.error("PayOS reconciliation paymentLinkId mismatch orderCode={} expected={} actual={}",
                            payment.getOrderCode(), payment.getPaymentLinkId(), paymentLinkId);
                    return;
                }
                if (payment.getPaymentLinkId() == null || payment.getPaymentLinkId().isBlank()) {
                    payment.setPaymentLinkId(paymentLinkId);
                }
                if (payment.getProviderTransactionId() == null || payment.getProviderTransactionId().isBlank()) {
                    payment.setProviderTransactionId(paymentLinkId);
                }
            }

            payment.setResponseCode("00");
            payment.setRawProviderResponse(providerStatus.rawResponse());
            payment.setUpdatedAt(java.time.OffsetDateTime.now());
            paymentTransactions.save(payment);

            // Directly reuse the existing idempotent payment confirmation/workspace activation pipeline.
            // This creates the workspace, subscription, Business Owner account(s) and sends email credentials.
            activationService.activateProviderConfirmedPayment(
                    payment.getId(),
                    providerStatus.rawResponse()
            );

            log.info("PayOS PAID orderCode={} activated directly from provider status.", payment.getOrderCode());
        } catch (Exception exception) {
            // Keep status reads available, but log the exact root cause so production no longer fails silently.
            log.error("PayOS reconciliation failed orderCode={} reason={}",
                    payment.getOrderCode(), exception.getMessage(), exception);
        }
    }
}
