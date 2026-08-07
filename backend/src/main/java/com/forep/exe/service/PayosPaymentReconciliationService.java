package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentMethod;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.PayosConfigEntity;
import com.forep.exe.persistence.PayosConfigRepository;
import com.forep.exe.service.PayosPaymentService.PayosProviderConfig;
import com.forep.exe.service.PayosPaymentService.PayosProviderException;
import com.forep.exe.service.PayosPaymentService.ProviderPaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Reconciles local PayOS transactions against PayOS server-to-server status.
 *
 * Webhook delivery is not part of this flow. Status reads and the scheduled monitor
 * both query PayOS directly. A PAID provider state is sent immediately into the
 * existing idempotent workspace activation pipeline.
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

        // A legacy/interrupted activation may have persisted PAID or SUCCESS before
        // workspace creation completed. Always send both successful local states through
        // the idempotent activation bridge; it returns immediately when the workspace is
        // already present and repairs the row when it is not.
        if (payment.getStatus() == PaymentTransactionStatus.PAID
                || payment.getStatus() == PaymentTransactionStatus.SUCCESS) {
            activationService.activateProviderConfirmedPayment(
                    payment.getId(),
                    payment.getRawProviderResponse() == null
                            ? "PayOS provider status already confirmed successful."
                            : payment.getRawProviderResponse()
            );
            return;
        }

        if (payment.getStatus() == PaymentTransactionStatus.FAILED
                || payment.getStatus() == PaymentTransactionStatus.CANCELLED
                || payment.getStatus() == PaymentTransactionStatus.REFUNDED
                || payment.getStatus() == PaymentTransactionStatus.EXPIRED) {
            return;
        }

        PayosConfigEntity setting = payosConfigs.findById(PAYOS_CONFIG_ID)
                .orElseThrow(() -> new PayosProviderException("PayOS is not configured."));
        if (!setting.isActive()) {
            throw new PayosProviderException("PayOS configuration is disabled.");
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
            String providerState = providerStatus.status().trim().toUpperCase(Locale.ROOT);
            log.info("PayOS status lookup orderCode={} localStatus={} providerStatus={} amount={} amountPaid={}",
                    payment.getOrderCode(), payment.getStatus(), providerState,
                    providerStatus.amount(), providerStatus.amountPaid());

            payment.setRawProviderResponse(providerStatus.rawResponse());
            payment.setResponseCode(providerState);
            payment.setUpdatedAt(OffsetDateTime.now());

            if (!"PAID".equals(providerState)) {
                syncNonPaidProviderState(payment, providerState);
                return;
            }

            validatePaidProviderState(payment, providerStatus);
            backfillProviderTransactionId(payment, providerStatus.paymentLinkId());
            paymentTransactions.save(payment);

            // Do not set local status to PAID before this call: the legacy confirmPayment
            // routine treats PAID as already processed. The activation bridge validates
            // the final state and retries recoverable PAID/SUCCESS rows separately.
            activationService.activateProviderConfirmedPayment(payment.getId(), providerStatus.rawResponse());

            PaymentTransactionEntity activated = paymentTransactions.findById(payment.getId())
                    .orElseThrow(() -> new IllegalStateException("Payment disappeared after PayOS activation."));
            log.info("PayOS PAID orderCode={} activation finished localStatus={}",
                    payment.getOrderCode(), activated.getStatus());
        } catch (PayosProviderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            String reason = rootMessage(exception);
            log.error("PayOS reconciliation failed orderCode={} reason={}", payment.getOrderCode(), reason, exception);
            throw new PayosProviderException(
                    "PayOS đã xác nhận/đang được đối soát nhưng FOREP không thể hoàn tất xử lý: " + reason,
                    exception
            );
        }
    }

    private void validatePaidProviderState(PaymentTransactionEntity payment, ProviderPaymentStatus providerStatus) {
        long expectedAmount = payment.getAmount().longValueExact();
        if (providerStatus.amount() > 0 && providerStatus.amount() != expectedAmount) {
            throw new IllegalStateException(
                    "PayOS amount mismatch. expected=" + expectedAmount + ", actual=" + providerStatus.amount());
        }
        if (providerStatus.amountPaid() > 0 && providerStatus.amountPaid() < expectedAmount) {
            throw new IllegalStateException(
                    "PayOS transaction is underpaid. expected=" + expectedAmount + ", paid=" + providerStatus.amountPaid());
        }

        String providerPaymentLinkId = providerStatus.paymentLinkId();
        if (providerPaymentLinkId != null && !providerPaymentLinkId.isBlank()
                && payment.getPaymentLinkId() != null && !payment.getPaymentLinkId().isBlank()
                && !providerPaymentLinkId.equals(payment.getPaymentLinkId())) {
            throw new IllegalStateException(
                    "PayOS paymentLinkId mismatch for orderCode=" + payment.getOrderCode());
        }
    }

    private void backfillProviderTransactionId(PaymentTransactionEntity payment, String providerPaymentLinkId) {
        if (providerPaymentLinkId == null || providerPaymentLinkId.isBlank()) {
            return;
        }
        if (payment.getPaymentLinkId() == null || payment.getPaymentLinkId().isBlank()) {
            payment.setPaymentLinkId(providerPaymentLinkId);
        }
        if (payment.getProviderTransactionId() == null || payment.getProviderTransactionId().isBlank()) {
            payment.setProviderTransactionId(providerPaymentLinkId);
        }
    }

    private void syncNonPaidProviderState(PaymentTransactionEntity payment, String providerState) {
        PaymentTransactionStatus next = switch (providerState) {
            case "PROCESSING" -> PaymentTransactionStatus.PROCESSING;
            case "FAILED" -> PaymentTransactionStatus.FAILED;
            case "CANCELLED", "CANCELED" -> PaymentTransactionStatus.CANCELLED;
            case "EXPIRED" -> PaymentTransactionStatus.EXPIRED;
            case "REFUNDED" -> PaymentTransactionStatus.REFUNDED;
            case "PENDING" -> payment.getStatus() == PaymentTransactionStatus.PROCESSING
                    ? PaymentTransactionStatus.PROCESSING
                    : PaymentTransactionStatus.PENDING;
            default -> payment.getStatus();
        };
        payment.setStatus(next);
        paymentTransactions.save(payment);
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return message == null || message.isBlank() ? cursor.getClass().getSimpleName() : message;
    }
}
