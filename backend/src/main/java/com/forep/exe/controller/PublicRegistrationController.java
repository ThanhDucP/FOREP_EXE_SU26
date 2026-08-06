package com.forep.exe.controller;

import com.forep.exe.dto.ApiResponse;
import com.forep.exe.dto.Requests.CreatePaymentRequest;
import com.forep.exe.dto.Requests.PayosWebhookRequest;
import com.forep.exe.dto.Requests.SelectSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.WorkspaceRegistrationRequest;
import com.forep.exe.service.ForepService;
import com.forep.exe.service.PayosPaymentService.PayosProviderException;
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

    public PublicRegistrationController(ForepService service) {
        this.service = service;
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
        return ApiResponse.ok(service.publicPaymentStatus(paymentCode, token));
    }

    @GetMapping("/payments/{orderCode}/status")
    ApiResponse<?> payosPaymentStatus(@PathVariable String orderCode) {
        return ApiResponse.ok(service.payosPaymentStatus(orderCode));
    }

    @PostMapping("/payments/payos/webhook")
    ApiResponse<?> payosWebhook(@RequestBody PayosWebhookRequest request) {
        try {
            ApiResponse<?> response = ApiResponse.ok(service.handlePayosWebhook(request));
            log.info("Accepted PayOS webhook orderCode={} code={}", webhookValue(request, "orderCode"), request.code());
            return response;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            log.warn("Rejected PayOS webhook orderCode={} reason={}", webhookValue(request, "orderCode"), exception.getMessage());
            return ApiResponse.error("PAYOS_WEBHOOK_REJECTED", exception.getMessage(), null);
        }
    }

    private Object webhookValue(PayosWebhookRequest request, String key) {
        return request == null || request.data() == null ? null : request.data().get(key);
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
