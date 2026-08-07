package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentMethod;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Proactively monitors PayOS payments that still require provider reconciliation
 * or workspace activation recovery.
 *
 * Normal pending/processing rows are limited to a recent lookback window. Provider-
 * confirmed PAID/SUCCESS rows with no workspace are recovered regardless of age so an
 * interrupted legacy activation cannot remain stuck forever.
 */
@Service
public class PayosPendingPaymentMonitorService {
    private static final Logger log = LoggerFactory.getLogger(PayosPendingPaymentMonitorService.class);

    private static final Set<PaymentTransactionStatus> RECONCILIATION_STATUSES = EnumSet.of(
            PaymentTransactionStatus.PENDING,
            PaymentTransactionStatus.PROCESSING
    );

    private static final Set<PaymentTransactionStatus> ACTIVATION_RECOVERY_STATUSES = EnumSet.of(
            PaymentTransactionStatus.PAID,
            PaymentTransactionStatus.SUCCESS
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
        PageRequest page = PageRequest.of(0, batchSize);

        // Prioritize provider-confirmed rows whose workspace activation never finished.
        List<PaymentTransactionEntity> recovery = paymentTransactions.findIncompleteActivations(
                PaymentMethod.PAYOS,
                ACTIVATION_RECOVERY_STATUSES,
                page
        );

        // Query only recent pending/processing PayOS rows in the database instead of
        // loading the complete payment table into memory every scheduling interval.
        List<PaymentTransactionEntity> pending = paymentTransactions
                .findByPaymentMethodAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
                        PaymentMethod.PAYOS,
                        RECONCILIATION_STATUSES,
                        cutoff,
                        page
                );

        Map<UUID, PaymentTransactionEntity> uniqueCandidates = new LinkedHashMap<>();
        recovery.forEach(payment -> uniqueCandidates.put(payment.getId(), payment));
        pending.forEach(payment -> uniqueCandidates.putIfAbsent(payment.getId(), payment));

        List<PaymentTransactionEntity> candidates = uniqueCandidates.values().stream()
                .filter(payment -> payment.getOrderCode() != null && !payment.getOrderCode().isBlank())
                .limit(batchSize)
                .toList();

        for (PaymentTransactionEntity payment : candidates) {
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
