package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.WorkspaceRegistrationEntity;
import com.forep.exe.persistence.WorkspaceRegistrationRepository;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Sends a PayOS provider-confirmed payment into FOREP's existing confirmation /
 * workspace activation pipeline.
 *
 * Runtime payment confirmation does not depend on webhook delivery. This bridge
 * also repairs older/partial rows where the local payment was already marked PAID
 * but workspace activation did not finish.
 */
@Service
public class PayosWorkspaceActivationService {
    private final ForepService forepServiceTarget;
    private final PaymentTransactionRepository paymentTransactions;
    private final WorkspaceRegistrationRepository workspaceRegistrations;
    private final Method confirmPaymentMethod;

    public PayosWorkspaceActivationService(
            ForepService forepService,
            PaymentTransactionRepository paymentTransactions,
            WorkspaceRegistrationRepository workspaceRegistrations) {
        this.forepServiceTarget = unwrapForepServiceTarget(forepService);
        this.paymentTransactions = paymentTransactions;
        this.workspaceRegistrations = workspaceRegistrations;

        Method method = ReflectionUtils.findMethod(
                ForepService.class,
                "confirmPayment",
                UUID.class,
                boolean.class,
                String.class
        );
        if (method == null) {
            throw new IllegalStateException("ForepService.confirmPayment(UUID, boolean, String) was not found.");
        }
        ReflectionUtils.makeAccessible(method);
        this.confirmPaymentMethod = method;
    }

    @Transactional
    public void activateProviderConfirmedPayment(UUID paymentId, String providerEvidence) {
        PaymentTransactionEntity payment = paymentTransactions.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found."));
        WorkspaceRegistrationEntity registration = workspaceRegistrations
                .findByIdForUpdate(payment.getWorkspaceRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace registration not found."));

        boolean locallyConfirmed = payment.getStatus() == PaymentTransactionStatus.PAID
                || payment.getStatus() == PaymentTransactionStatus.SUCCESS;

        // Idempotent success: nothing else to do when activation is already complete.
        if (locallyConfirmed && registration.getWorkspaceId() != null) {
            return;
        }

        // Legacy confirmPayment returns immediately for PAID/SUCCESS. If an older or
        // interrupted flow left such a status without a workspace, temporarily move
        // it back to PROCESSING so the real activation routine can finish safely.
        if (locallyConfirmed && registration.getWorkspaceId() == null) {
            payment.setStatus(PaymentTransactionStatus.PROCESSING);
            payment.setUpdatedAt(OffsetDateTime.now());
            paymentTransactions.save(payment);
        }

        // ForepService is @Transactional and therefore normally injected as a Spring
        // CGLIB proxy. Reflectively invoking its private method on that proxy bypasses
        // the interceptor and executes against the proxy instance itself; constructor-
        // injected superclass fields can then be null. Invoke the private method on the
        // real initialized singleton target instead.
        ReflectionUtils.invokeMethod(
                confirmPaymentMethod,
                forepServiceTarget,
                paymentId,
                false,
                providerEvidence
        );

        PaymentTransactionEntity updatedPayment = paymentTransactions.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment disappeared after activation."));
        WorkspaceRegistrationEntity updatedRegistration = workspaceRegistrations
                .findById(updatedPayment.getWorkspaceRegistrationId())
                .orElseThrow(() -> new IllegalStateException("Workspace registration disappeared after activation."));

        if (updatedRegistration.getWorkspaceId() == null) {
            throw new IllegalStateException(
                    "PayOS payment was confirmed but workspace activation did not create a workspace. "
                            + "paymentStatus=" + updatedPayment.getStatus()
                            + ", registrationStatus=" + updatedRegistration.getRegistrationStatus()
                            + ", registrationPaymentStatus=" + updatedRegistration.getPaymentStatus());
        }

        if (updatedPayment.getStatus() != PaymentTransactionStatus.SUCCESS
                && updatedPayment.getStatus() != PaymentTransactionStatus.PAID) {
            throw new IllegalStateException(
                    "Workspace was created but payment did not finish in a successful state. status="
                            + updatedPayment.getStatus());
        }
    }

    private ForepService unwrapForepServiceTarget(ForepService forepService) {
        Object singletonTarget = AopProxyUtils.getSingletonTarget(forepService);
        if (singletonTarget == null) {
            return forepService;
        }
        if (!(singletonTarget instanceof ForepService target)) {
            throw new IllegalStateException(
                    "Unexpected ForepService proxy target type: " + singletonTarget.getClass().getName());
        }
        return target;
    }
}
