package com.forep.exe.controller;

import com.forep.exe.domain.Enums.WorkspaceStatus;
import com.forep.exe.dto.ApiResponse;
import com.forep.exe.dto.Requests.CreateSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.ReviewBusinessFeedbackRequest;
import com.forep.exe.dto.Requests.ReviewRegistrationRequest;
import com.forep.exe.dto.Requests.UpdateSubscriptionPlanRequest;
import com.forep.exe.service.ForepService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminPlatformController {
    private final ForepService service;

    public AdminPlatformController(ForepService service) {
        this.service = service;
    }

    @GetMapping("/subscription-plans")
    ApiResponse<?> subscriptionPlans() {
        return ApiResponse.ok(service.subscriptionPlans());
    }

    @PostMapping("/subscription-plans")
    ApiResponse<?> createSubscriptionPlan(@RequestBody @Valid CreateSubscriptionPlanRequest request) {
        return ApiResponse.ok(service.createSubscriptionPlan(request));
    }

    @PutMapping("/subscription-plans/{id}")
    ApiResponse<?> updateSubscriptionPlan(@PathVariable UUID id, @RequestBody @Valid UpdateSubscriptionPlanRequest request) {
        return ApiResponse.ok(service.updateSubscriptionPlan(id, request));
    }

    @PatchMapping("/subscription-plans/{id}/activate")
    ApiResponse<?> activateSubscriptionPlan(@PathVariable UUID id) {
        return ApiResponse.ok(service.activateSubscriptionPlan(id));
    }

    @PatchMapping("/subscription-plans/{id}/deactivate")
    ApiResponse<?> deactivateSubscriptionPlan(@PathVariable UUID id) {
        return ApiResponse.ok(service.deactivateSubscriptionPlan(id));
    }

    @GetMapping("/workspace-registrations")
    ApiResponse<?> workspaceRegistrations() {
        return ApiResponse.ok(service.adminWorkspaceRegistrations());
    }

    @GetMapping("/workspace-registrations/{id}")
    ApiResponse<?> workspaceRegistration(@PathVariable UUID id) {
        return ApiResponse.ok(service.workspaceRegistration(id));
    }

    @PatchMapping("/workspace-registrations/{id}/approve")
    ApiResponse<?> approveWorkspaceRegistration(@PathVariable UUID id, @RequestBody(required = false) ReviewRegistrationRequest request) {
        return ApiResponse.ok(service.approveWorkspaceRegistration(id, request));
    }

    @PatchMapping("/workspace-registrations/{id}/reject")
    ApiResponse<?> rejectWorkspaceRegistration(@PathVariable UUID id, @RequestBody(required = false) ReviewRegistrationRequest request) {
        return ApiResponse.ok(service.rejectWorkspaceRegistration(id, request));
    }

    @PatchMapping("/payments/{paymentId}/confirm")
    ApiResponse<?> confirmPayment(@PathVariable UUID paymentId, @RequestBody(required = false) ReviewRegistrationRequest request) {
        return ApiResponse.ok(service.adminConfirmPayment(paymentId, request));
    }

    @PatchMapping("/payments/{paymentId}/reject")
    ApiResponse<?> rejectPayment(@PathVariable UUID paymentId, @RequestBody(required = false) ReviewRegistrationRequest request) {
        return ApiResponse.ok(service.adminRejectPayment(paymentId, request));
    }

    @GetMapping("/workspaces")
    ApiResponse<?> workspaces() {
        return ApiResponse.ok(service.adminWorkspaces());
    }

    @GetMapping("/workspaces/{id}")
    ApiResponse<?> workspace(@PathVariable UUID id) {
        return ApiResponse.ok(service.adminWorkspace(id));
    }

    @PatchMapping("/workspaces/{id}/suspend")
    ApiResponse<?> suspendWorkspace(@PathVariable UUID id) {
        return ApiResponse.ok(service.adminUpdateWorkspaceStatus(id, WorkspaceStatus.SUSPENDED));
    }

    @PatchMapping("/workspaces/{id}/restore")
    ApiResponse<?> restoreWorkspace(@PathVariable UUID id) {
        return ApiResponse.ok(service.adminUpdateWorkspaceStatus(id, WorkspaceStatus.ACTIVE));
    }

    @GetMapping("/business-feedback")
    ApiResponse<?> businessFeedback() {
        return ApiResponse.ok(service.adminBusinessFeedback());
    }

    @PatchMapping("/business-feedback/{id}/mark-reviewed")
    ApiResponse<?> markBusinessFeedbackReviewed(@PathVariable UUID id, @RequestBody(required = false) ReviewBusinessFeedbackRequest request) {
        return ApiResponse.ok(service.reviewBusinessFeedback(id, request));
    }
}
