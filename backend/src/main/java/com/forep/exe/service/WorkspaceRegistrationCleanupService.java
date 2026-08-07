package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentStatus;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.domain.Enums.RegistrationStatus;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.UserEntity;
import com.forep.exe.persistence.UserRepository;
import com.forep.exe.persistence.WorkspaceEntity;
import com.forep.exe.persistence.WorkspaceRegistrationEntity;
import com.forep.exe.persistence.WorkspaceRegistrationRepository;
import com.forep.exe.persistence.WorkspaceRepository;
import com.forep.exe.persistence.WorkspaceSubscriptionEntity;
import com.forep.exe.persistence.WorkspaceSubscriptionRepository;
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
import java.util.UUID;

/**
 * Cleans invalid workspace-registration data produced by failed/abandoned payment flows.
 *
 * There are two repair paths:
 *  1) If an older bug created a workspace/owner/subscription before payment was actually
 *     confirmed, the generated workspace artifacts are removed and the registration is
 *     returned to PENDING_PAYMENT. This releases the owner email for a legitimate retry.
 *  2) Failed/abandoned registrations that never created a workspace are deleted after the
 *     configured retention period.
 *
 * A registration is NEVER repaired or deleted when any payment transaction is PAID or SUCCESS.
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
    private final WorkspaceRepository workspaces;
    private final WorkspaceSubscriptionRepository workspaceSubscriptions;
    private final UserRepository users;
    private final long retentionMinutes;

    public WorkspaceRegistrationCleanupService(
            WorkspaceRegistrationRepository workspaceRegistrations,
            PaymentTransactionRepository paymentTransactions,
            WorkspaceRepository workspaces,
            WorkspaceSubscriptionRepository workspaceSubscriptions,
            UserRepository users,
            @Value("${forep.workspace-registration.failed-retention-minutes:10}") long retentionMinutes) {
        this.workspaceRegistrations = workspaceRegistrations;
        this.paymentTransactions = paymentTransactions;
        this.workspaces = workspaces;
        this.workspaceSubscriptions = workspaceSubscriptions;
        this.users = users;
        this.retentionMinutes = Math.max(0L, retentionMinutes);
    }

    @Scheduled(
            fixedDelayString = "${forep.workspace-registration.cleanup-delay-ms:60000}",
            initialDelayString = "${forep.workspace-registration.cleanup-initial-delay-ms:10000}"
    )
    @Transactional
    public void purgeFailedRegistrations() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(retentionMinutes);

        // Repair rows produced by the old payment bug first. A workspace must never exist
        // solely because the local flow advanced before PayOS had a PAID/SUCCESS transaction.
        repairUnpaidCreatedWorkspaces(cutoff);

        List<WorkspaceRegistrationEntity> candidates = workspaceRegistrations.findAllByOrderByCreatedAtDesc().stream()
                .filter(registration -> registration.getWorkspaceId() == null)
                .filter(registration -> registration.getUpdatedAt() != null && !registration.getUpdatedAt().isAfter(cutoff))
                .filter(this::isFailedOrAbandoned)
                .toList();

        for (WorkspaceRegistrationEntity registration : candidates) {
            List<PaymentTransactionEntity> payments = paymentsFor(registration);

            // Defensive guard: never delete a registration that has a successful payment.
            if (hasSuccessfulPayment(payments)) {
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

    private void repairUnpaidCreatedWorkspaces(OffsetDateTime cutoff) {
        List<WorkspaceRegistrationEntity> inconsistent = workspaceRegistrations.findAllByOrderByCreatedAtDesc().stream()
                .filter(registration -> registration.getWorkspaceId() != null)
                .filter(registration -> registration.getUpdatedAt() != null && !registration.getUpdatedAt().isAfter(cutoff))
                .toList();

        for (WorkspaceRegistrationEntity registration : inconsistent) {
            List<PaymentTransactionEntity> payments = paymentsFor(registration);
            if (hasSuccessfulPayment(payments)) {
                continue;
            }

            UUID workspaceId = registration.getWorkspaceId();
            WorkspaceEntity workspace = workspaces.findById(workspaceId).orElse(null);

            // Break possible FK/reference chains before deleting generated artifacts.
            registration.setWorkspaceId(null);
            registration.setActivationDate(null);
            registration.setExpirationDate(null);
            registration.setPaymentStatus(PaymentStatus.PENDING);
            registration.setRegistrationStatus(RegistrationStatus.PENDING_PAYMENT);
            registration.setUpdatedAt(OffsetDateTime.now());
            workspaceRegistrations.saveAndFlush(registration);

            if (workspace != null) {
                workspace.setOwnerId(null);
                workspace.setOwnerAccountCount(0);
                workspace.setOwnerAccountProvisionedAt(null);
                workspaces.saveAndFlush(workspace);
            }

            List<WorkspaceSubscriptionEntity> subscriptions = workspaceSubscriptions
                    .findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
            if (!subscriptions.isEmpty()) {
                workspaceSubscriptions.deleteAll(subscriptions);
                workspaceSubscriptions.flush();
            }

            List<UserEntity> generatedUsers = users.findByWorkspaceId(workspaceId);
            if (!generatedUsers.isEmpty()) {
                users.deleteAll(generatedUsers);
                users.flush();
            }

            if (workspace != null) {
                workspaces.delete(workspace);
                workspaces.flush();
            }

            log.warn(
                    "Removed unpaid workspace created by inconsistent payment flow. registration={} workspace={} ownerEmail={} paymentAttempts={}",
                    registration.getId(), workspaceId, registration.getOwnerEmail(), payments.size());
        }
    }

    private boolean isFailedOrAbandoned(WorkspaceRegistrationEntity registration) {
        if (TERMINAL_REGISTRATION_STATUSES.contains(registration.getRegistrationStatus())) {
            return true;
        }

        List<PaymentTransactionEntity> payments = paymentsFor(registration);
        if (payments.isEmpty()) {
            return false;
        }

        return payments.stream().allMatch(payment ->
                TERMINAL_FAILED_PAYMENT_STATUSES.contains(payment.getStatus()));
    }

    private List<PaymentTransactionEntity> paymentsFor(WorkspaceRegistrationEntity registration) {
        return paymentTransactions.findByWorkspaceRegistrationIdOrderByCreatedAtDesc(registration.getId());
    }

    private boolean hasSuccessfulPayment(List<PaymentTransactionEntity> payments) {
        return payments.stream().anyMatch(payment ->
                payment.getStatus() == PaymentTransactionStatus.SUCCESS
                        || payment.getStatus() == PaymentTransactionStatus.PAID);
    }
}
