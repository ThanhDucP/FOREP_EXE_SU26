package com.forep.exe.controller;

import com.forep.exe.dto.ApiResponse;
import com.forep.exe.dto.Requests.CreatePaymentRequest;
import com.forep.exe.dto.Requests.PaymentCallbackRequest;
import com.forep.exe.dto.Requests.SelectSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.WorkspaceRegistrationRequest;
import com.forep.exe.service.ForepService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PublicRegistrationController {
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

    @PostMapping("/payment-callbacks/momo")
    ApiResponse<?> momoCallback(@RequestBody PaymentCallbackRequest request) {
        return ApiResponse.ok(service.handleMomoCallback(request));
    }

    @PostMapping("/payment-callbacks/bank")
    ApiResponse<?> bankCallback(@RequestBody PaymentCallbackRequest request) {
        return ApiResponse.ok(service.handleBankTransferCallback(request));
    }
}
