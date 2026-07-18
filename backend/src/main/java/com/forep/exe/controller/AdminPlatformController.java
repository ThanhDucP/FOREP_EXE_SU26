package com.forep.exe.controller;

import com.forep.exe.ai.AiProviderException;
import com.forep.exe.ai.AiRateLimitException;
import com.forep.exe.domain.Enums.WorkspaceStatus;
import com.forep.exe.dto.ApiResponse;
import com.forep.exe.dto.Requests.CreateSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.ReviewBusinessFeedbackRequest;
import com.forep.exe.dto.Requests.ReviewRegistrationRequest;
import com.forep.exe.dto.Requests.UpdateSubscriptionPlanRequest;
import com.forep.exe.service.ForepService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/payments")
    ApiResponse<?> payments() {
        return ApiResponse.ok(service.adminPayments());
    }

    @GetMapping("/payments/{paymentId}")
    ApiResponse<?> payment(@PathVariable UUID paymentId) {
        return ApiResponse.ok(service.adminPayment(paymentId));
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

    @PostMapping("/workspaces/{id}/provision-owner-accounts")
    ApiResponse<?> provisionOwnerAccounts(@PathVariable UUID id) {
        return ApiResponse.ok(service.provisionOwnerAccounts(id));
    }

    @GetMapping("/business-feedback")
    ApiResponse<?> businessFeedback() {
        return ApiResponse.ok(service.adminBusinessFeedback());
    }

    @PatchMapping("/business-feedback/{id}/mark-reviewed")
    ApiResponse<?> markBusinessFeedbackReviewed(@PathVariable UUID id, @RequestBody(required = false) ReviewBusinessFeedbackRequest request) {
        return ApiResponse.ok(service.reviewBusinessFeedback(id, request));
    }

    @GetMapping("/audit-logs")
    ApiResponse<?> auditLogs() {
        return ApiResponse.ok(service.adminAuditLogs());
    }

    @GetMapping("/dashboard/overview")
    ApiResponse<?> dashboardOverview() {
        return ApiResponse.ok(service.adminDashboardOverview());
    }

    @GetMapping("/dashboard/revenue/monthly")
    ApiResponse<?> dashboardRevenueMonthly() {
        return ApiResponse.ok(service.adminDashboardRevenueMonthly());
    }

    @GetMapping("/dashboard/revenue/quarterly")
    ApiResponse<?> dashboardRevenueQuarterly() {
        return ApiResponse.ok(service.adminDashboardRevenueQuarterly());
    }

    @GetMapping("/dashboard/revenue/yearly")
    ApiResponse<?> dashboardRevenueYearly() {
        return ApiResponse.ok(service.adminDashboardRevenueYearly());
    }

    @GetMapping("/dashboard/revenue/by-plan")
    ApiResponse<?> dashboardRevenueByPlan() {
        return ApiResponse.ok(service.adminDashboardRevenueByPlan());
    }

    @GetMapping("/dashboard/workspaces/by-status")
    ApiResponse<?> dashboardWorkspacesByStatus() {
        return ApiResponse.ok(service.adminDashboardWorkspacesByStatus());
    }

    @GetMapping("/dashboard/workspaces/by-plan")
    ApiResponse<?> dashboardWorkspacesByPlan() {
        return ApiResponse.ok(service.adminDashboardWorkspacesByPlan());
    }

    @GetMapping("/dashboard/payments/summary")
    ApiResponse<?> dashboardPaymentsSummary() {
        return ApiResponse.ok(service.adminDashboardPaymentsSummary());
    }

    @GetMapping("/dashboard/feedback/summary")
    ApiResponse<?> dashboardFeedbackSummary() {
        return ApiResponse.ok(service.adminDashboardFeedbackSummary());
    }

    @GetMapping("/ai/platform-summary")
    ApiResponse<?> platformSummary() {
        return ApiResponse.ok(service.platformAdminSystemSummary());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ApiResponse<?> handleBadRequest(IllegalArgumentException exception) {
        return ApiResponse.error("BUSINESS_RULE_ERROR", exception.getMessage(), null);
    }

    @ExceptionHandler(AiRateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    ApiResponse<?> handleAiRateLimitError(AiRateLimitException exception) {
        return ApiResponse.error("AI_RATE_LIMITED", "AI đang xử lý quá nhiều yêu cầu. Vui lòng thử lại sau " + exception.retryAfterSeconds() + " giây.", null);
    }

    @ExceptionHandler(AiProviderException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ApiResponse<?> handleAiProviderError(AiProviderException exception) {
        return ApiResponse.error("AI_PROVIDER_ERROR", "Không thể tạo phân tích AI ở thời điểm này. Vui lòng thử lại sau.", null);
    }
}
