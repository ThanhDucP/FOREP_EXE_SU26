package com.forep.exe.controller;

import com.forep.exe.dto.ApiResponse;
import com.forep.exe.dto.Requests.CreatePaymentRequest;
import com.forep.exe.dto.Requests.SelectSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.WorkspaceRegistrationRequest;
import com.forep.exe.service.ForepService;
import com.forep.exe.service.PayosPaymentReconciliationService;
import com.forep.exe.service.PayosPaymentService.PayosProviderException;
import com.forep.exe.service.PayosPaymentStatusQueryService;
import com.forep.exe.service.WorkspaceValidationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PublicRegistrationController {
    private final ForepService service;
    private final PayosPaymentReconciliationService payosReconciliation;
    private final PayosPaymentStatusQueryService payosStatusQuery;

    public PublicRegistrationController(ForepService service,
                                        PayosPaymentReconciliationService payosReconciliation,
                                        PayosPaymentStatusQueryService payosStatusQuery) {
        this.service = service;
        this.payosReconciliation = payosReconciliation;
        this.payosStatusQuery = payosStatusQuery;
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
        // PayOS server-to-server status is the single source of truth.
        // If the provider reports PAID, reconciliation immediately confirms payment,
        // activates the workspace, creates owner account(s), and triggers credential email.
        payosReconciliation.reconcileByPaymentCode(paymentCode);
        return ApiResponse.ok(service.publicPaymentStatus(paymentCode, token));
    }

    @GetMapping("/payments/{orderCode}/status")
    ApiResponse<?> payosPaymentStatus(@PathVariable String orderCode) {
        // Never trust browser query parameters as proof of payment.
        // Query PayOS server-to-server first, then return the refreshed local state.
        payosReconciliation.reconcileByOrderCode(orderCode);
        return ApiResponse.ok(payosStatusQuery.statusByOrderCode(orderCode));
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
