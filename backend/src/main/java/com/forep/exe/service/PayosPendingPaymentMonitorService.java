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
 * Proactively monitors local PayOS payments that are still pending.
 *
 * This makes workspace activation independent from both the browser return URL
 * and webhook delivery. As soon as PayOS reports PAID through its server API,
 * the existing reconciliation pipeline confirms the payment, activates the
 * workspace, creates the subscription/owner accounts and sends credentials.
 *
 * Browser callback parameters are intentionally NOT trusted as proof of
 * payment. PayOS remains the source of truth.
 */
@Service
public class PayosPendingPaymentMonitorService {
    private static final Logger log = LoggerFactory.getLogger(PayosPendingPaymentMonitorService.class);

    private static final Set<PaymentTransactionStatus> MONITORED_STATUSES = EnumSet.of(
            PaymentTransactionStatus.PENDING,
            PaymentTransactionStatus.PROCESSING
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
            fixedDelayString = "${forep.payos.pending-monitor-delay-ms:5000}",
            initialDelayString = "${forep.payos.pending-monitor-initial-delay-ms:5000}"
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
                // One provider/network failure must not stop reconciliation for the remaining payments.
                log.warn("Scheduled PayOS reconciliation failed orderCode={}: {}",
                        payment.getOrderCode(), exception.getMessage());
            }
        }
    }
}
