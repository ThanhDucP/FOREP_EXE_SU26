package com.forep.exe.controller;

import com.forep.exe.dto.ApiResponse;
import com.forep.exe.dto.Requests.CreatePaymentRequest;
import com.forep.exe.dto.Requests.PayosWebhookRequest;
import com.forep.exe.dto.Requests.SelectSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.WorkspaceRegistrationRequest;
import com.forep.exe.service.ForepService;
import com.forep.exe.service.PayosPaymentReconciliationService;
import com.forep.exe.service.PayosPaymentService.PayosProviderException;
import com.forep.exe.service.WorkspaceValidationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PublicRegistrationController {
    private static final Logger log = LoggerFactory.getLogger(PublicRegistrationController.class);
    private final ForepService service;
    private final PayosPaymentReconciliationService payosReconciliation;

    public PublicRegistrationController(ForepService service,
                                        PayosPaymentReconciliationService payosReconciliation) {
        this.service = service;
        this.payosReconciliation = payosReconciliation;
    }

    @GetMapping("/public/subscription-plans")
    ApiResponse<?> publicSubscriptionPlans() {
        return ApiResponse.ok(service.publicSubscriptionPlans());
    }

    @GetMapping("/public/subscription-plans/{id}")
    ApiResponse<?> publicSubscriptionPlan(@PathVariable UUID id) {
        return ApiResponse.ok(service.publicSubscriptionPlan(id));
    }

    @PostMapping("/public/workspace-registrations")
    ApiResponse<?> submitWorkspaceRegistration(@RequestBody @Valid WorkspaceRegistrationRequest request) {
        return ApiResponse.ok(service.submitWorkspaceRegistration(request));
    }

    @GetMapping("/public/workspace-registrations/{id}")
    ApiResponse<?> workspaceRegistration(@PathVariable UUID id, @RequestParam String token) {
        return ApiResponse.ok(service.publicWorkspaceRegistration(id, token));
    }

    @PatchMapping("/public/workspace-registrations/{id}/select-plan")
    ApiResponse<?> selectSubscriptionPlan(@PathVariable UUID id,
                                          @RequestParam String token,
                                          @RequestBody @Valid SelectSubscriptionPlanRequest request) {
        return ApiResponse.ok(service.publicSelectSubscriptionPlan(id, token, request));
    }

    @PatchMapping("/public/workspace-registrations/{id}/cancel")
    ApiResponse<?> cancelWorkspaceRegistration(@PathVariable UUID id, @RequestParam String token) {
        return ApiResponse.ok(service.publicCancelWorkspaceRegistration(id, token));
    }

    @PostMapping("/public/workspace-registrations/{id}/payments")
    ApiResponse<?> createPayment(@PathVariable UUID id,
                                 @RequestParam String token,
                                 @RequestBody @Valid CreatePaymentRequest request) {
        return ApiResponse.ok(service.publicCreatePayment(id, token, request));
    }

    @GetMapping("/public/payments/{paymentCode}/status")
    ApiResponse<?> paymentStatus(@PathVariable String paymentCode, @RequestParam String token) {
        // If PayOS already says PAID, activate the workspace immediately before returning status.
        // The browser does not need to wait for a webhook or perform any manual confirmation.
        payosReconciliation.reconcileByPaymentCode(paymentCode);
        return ApiResponse.ok(service.publicPaymentStatus(paymentCode, token));
    }

    @GetMapping("/payments/{orderCode}/status")
    ApiResponse<?> payosPaymentStatus(@PathVariable String orderCode) {
        // Return/callback page can poll this endpoint. A direct server-to-server PayOS status
        // check promotes PAID orders through the existing activation pipeline immediately.
        payosReconciliation.reconcileByOrderCode(orderCode);
        return ApiResponse.ok(service.payosPaymentStatus(orderCode));
    }

    @PostMapping("/payments/payos/webhook")
    ApiResponse<?> payosWebhook(@RequestBody PayosWebhookRequest request) {
        try {
            ForepService.WorkspaceActivationResponse result = service.handlePayosWebhook(request);
            log.info("Accepted PayOS webhook orderCode={} code={} workspaceCreated={} workspaceId={}",
                    webhookValue(request, "orderCode"), request.code(), result.workspaceCreated(), result.workspaceId());
            return ApiResponse.ok(result);
        } catch (WorkspaceValidationException exception) {
            log.error("PayOS webhook activation failed for orderCode={} errorCode={} reason={}",
                    webhookValue(request, "orderCode"), exception.getErrorCode(), exception.getMessage());
            return ApiResponse.error(exception.getErrorCode(), exception.getMessage(), null);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            log.warn("Rejected PayOS webhook orderCode={} reason={}", webhookValue(request, "orderCode"), exception.getMessage());
            return ApiResponse.error("PAYOS_WEBHOOK_REJECTED", exception.getMessage(), null);
        }
    }

    private Object webhookValue(PayosWebhookRequest request, String key) {
        return request == null || request.data() == null ? null : request.data().get(key);
    }

    @ExceptionHandler(WorkspaceValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ApiResponse<?> handleWorkspaceValidationError(WorkspaceValidationException exception) {
        return ApiResponse.error(exception.getErrorCode(), exception.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResponse<?> handleBadRequest(IllegalArgumentException exception) {
        return ApiResponse.error("BUSINESS_RULE_ERROR", exception.getMessage(), null);
    }

    @ExceptionHandler(PayosProviderException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ApiResponse<?> handlePayosProviderError(PayosProviderException exception) {
        return ApiResponse.error("PAYOS_PROVIDER_ERROR", exception.getMessage(), null);
    }
}
