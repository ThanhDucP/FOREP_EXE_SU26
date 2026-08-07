package com.forep.exe.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Internal bridge that sends a provider-confirmed PayOS payment directly into
 * FOREP's existing payment confirmation/workspace activation pipeline.
 *
 * This intentionally avoids manufacturing a fake webhook. The real webhook,
 * proactive PayOS status reconciliation and FE status polling may all confirm
 * the same payment, while the existing confirmation pipeline remains the
 * single idempotent source of workspace/subscription/account activation.
 *
 * NOTE: confirmPayment currently lives as a private method in the legacy
 * ForepService monolith. This bridge is deliberately isolated so the payment
 * flow can be corrected without duplicating the large activation routine.
 * When ForepService is split into modules, replace this bridge with a normal
 * public internal application service.
 */
@Service
public class PayosWorkspaceActivationService {
    private final ForepService forepService;
    private final Method confirmPaymentMethod;

    public PayosWorkspaceActivationService(ForepService forepService) {
        this.forepService = forepService;
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
        try {
            ReflectionUtils.invokeMethod(
                    confirmPaymentMethod,
                    forepService,
                    paymentId,
                    false,
                    providerEvidence
            );
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not activate provider-confirmed PayOS payment.", exception);
        }
    }
}
