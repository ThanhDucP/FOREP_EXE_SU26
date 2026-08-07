package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.domain.Enums.RegistrationStatus;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.WorkspaceRegistrationEntity;
import com.forep.exe.persistence.WorkspaceRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Removes abandoned/failed workspace registration flows that never produced a workspace.
 *
 * Successful/activated registrations are intentionally preserved for subscription/payment audit.
 * A registration is eligible for deletion only when it has no workspace and either:
 *  - its registration status is terminal (REJECTED, CANCELLED, EXPIRED), or
 *  - it has payment attempts and every payment attempt is terminal-failed.
 */
@Service
public class WorkspaceRegistrationCleanupService {
    private static final Logger log = LoggerFactory.getLogger(WorkspaceRegistrationCleanupService.class);

    private static final Set<RegistrationStatus> TERMINAL_REGISTRATION_STATUSES = EnumSet.of(
            RegistrationStatus.REJECTED,
            RegistrationStatus.CANCELLED,
            RegistrationStatus.EXPIRED
    );

    private static final Set<PaymentTransactionStatus> TERMINAL_FAILED_PAYMENT_STATUSES = EnumSet.of(
            PaymentTransactionStatus.FAILED,
            PaymentTransactionStatus.EXPIRED,
            PaymentTransactionStatus.CANCELLED
    );

    private final WorkspaceRegistrationRepository workspaceRegistrations;
    private final PaymentTransactionRepository paymentTransactions;
    private final long retentionMinutes;

    public WorkspaceRegistrationCleanupService(
            WorkspaceRegistrationRepository workspaceRegistrations,
            PaymentTransactionRepository paymentTransactions,
            @Value("${forep.workspace-registration.failed-retention-minutes:10}") long retentionMinutes) {
        this.workspaceRegistrations = workspaceRegistrations;
        this.paymentTransactions = paymentTransactions;
        this.retentionMinutes = Math.max(0L, retentionMinutes);
    }

    @Scheduled(
            fixedDelayString = "${forep.workspace-registration.cleanup-delay-ms:60000}",
            initialDelayString = "${forep.workspace-registration.cleanup-initial-delay-ms:10000}"
    )
    @Transactional
    public void purgeFailedRegistrations() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(retentionMinutes);
        List<WorkspaceRegistrationEntity> candidates = workspaceRegistrations.findAllByOrderByCreatedAtDesc().stream()
                .filter(registration -> registration.getWorkspaceId() == null)
                .filter(registration -> registration.getUpdatedAt() != null && !registration.getUpdatedAt().isAfter(cutoff))
                .filter(this::isFailedOrAbandoned)
                .toList();

        for (WorkspaceRegistrationEntity registration : candidates) {
            List<PaymentTransactionEntity> payments = paymentTransactions
                    .findByWorkspaceRegistrationIdOrderByCreatedAtDesc(registration.getId());

            // Defensive guard: never delete a registration that has a successful payment.
            boolean hasSuccessfulPayment = payments.stream().anyMatch(payment ->
                    payment.getStatus() == PaymentTransactionStatus.SUCCESS
                            || payment.getStatus() == PaymentTransactionStatus.PAID);
            if (hasSuccessfulPayment) {
                log.warn("Skipped cleanup for registration={} because a successful payment exists.", registration.getId());
                continue;
            }

            if (!payments.isEmpty()) {
                paymentTransactions.deleteAll(payments);
            }
            workspaceRegistrations.delete(registration);
            log.info("Deleted failed workspace registration={} status={} paymentAttempts={}",
                    registration.getId(), registration.getRegistrationStatus(), payments.size());
        }
    }

    private boolean isFailedOrAbandoned(WorkspaceRegistrationEntity registration) {
        if (TERMINAL_REGISTRATION_STATUSES.contains(registration.getRegistrationStatus())) {
            return true;
        }

        List<PaymentTransactionEntity> payments = paymentTransactions
                .findByWorkspaceRegistrationIdOrderByCreatedAtDesc(registration.getId());
        if (payments.isEmpty()) {
            return false;
        }

        return payments.stream().allMatch(payment ->
                TERMINAL_FAILED_PAYMENT_STATUSES.contains(payment.getStatus()));
    }
}
