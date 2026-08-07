package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentMethod;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Proactively monitors local PayOS payments that still need reconciliation or activation.
 *
 * Runtime payment confirmation does not depend on webhook delivery. As soon as PayOS
 * reports PAID through its server API, reconciliation confirms the payment, activates
 * the workspace, creates subscription/owner accounts and sends credentials.
 *
 * PAID is intentionally monitored as a recovery state for older/interrupted flows where
 * PayOS had already been confirmed but workspace activation did not finish.
 */
@Service
public class PayosPendingPaymentMonitorService {
    private static final Logger log = LoggerFactory.getLogger(PayosPendingPaymentMonitorService.class);

    private static final Set<PaymentTransactionStatus> MONITORED_STATUSES = EnumSet.of(
            PaymentTransactionStatus.PENDING,
            PaymentTransactionStatus.PROCESSING,
            PaymentTransactionStatus.PAID
    );

    private final PaymentTransactionRepository paymentTransactions;
    private final PayosPaymentReconciliationService reconciliationService;
    private final int batchSize;
    private final long lookbackHours;

    public PayosPendingPaymentMonitorService(
            PaymentTransactionRepository paymentTransactions,
            PayosPaymentReconciliationService reconciliationService,
            @Value("${forep.payos.pending-monitor-batch-size:20}") int batchSize,
            @Value("${forep.payos.pending-monitor-lookback-hours:24}") long lookbackHours) {
        this.paymentTransactions = paymentTransactions;
        this.reconciliationService = reconciliationService;
        this.batchSize = Math.max(1, batchSize);
        this.lookbackHours = Math.max(1L, lookbackHours);
    }

    @Scheduled(
            fixedDelayString = "${forep.payos.pending-monitor-delay-ms:2000}",
            initialDelayString = "${forep.payos.pending-monitor-initial-delay-ms:2000}"
    )
    public void reconcilePendingPayosPayments() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(lookbackHours);

        List<PaymentTransactionEntity> pending = paymentTransactions
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(payment -> payment.getPaymentMethod() == PaymentMethod.PAYOS)
                .filter(payment -> MONITORED_STATUSES.contains(payment.getStatus()))
                .filter(payment -> payment.getCreatedAt() != null && payment.getCreatedAt().isAfter(cutoff))
                .filter(payment -> payment.getOrderCode() != null && !payment.getOrderCode().isBlank())
                .limit(batchSize)
                .toList();

        for (PaymentTransactionEntity payment : pending) {
            try {
                reconciliationService.reconcileByOrderCode(payment.getOrderCode());
            } catch (Exception exception) {
                // One provider/network/activation failure must not stop reconciliation for others.
                log.warn("Scheduled PayOS reconciliation failed orderCode={}: {}",
                        payment.getOrderCode(), exception.getMessage());
            }
        }
    }
}
