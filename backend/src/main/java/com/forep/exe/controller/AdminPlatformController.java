package com.forep.exe.controller;

import com.forep.exe.ai.AiProviderException;
import com.forep.exe.ai.AiRateLimitException;
import com.forep.exe.domain.Enums.WorkspaceStatus;
import com.forep.exe.domain.Enums.PaymentMethod;
import com.forep.exe.domain.Enums.UserStatus;
import com.forep.exe.dto.ApiResponse;
import com.forep.exe.dto.Requests.CreateBusinessOwnerRequest;
import com.forep.exe.dto.Requests.CreateSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.ReviewBusinessFeedbackRequest;
import com.forep.exe.dto.Requests.ReviewRegistrationRequest;
import com.forep.exe.dto.Requests.UpdateSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.UpdatePaymentQrSettingRequest;
import com.forep.exe.service.ForepService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
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

    @GetMapping("/payment-qr-settings")
    ApiResponse<?> paymentQrSettings() {
        return ApiResponse.ok(service.adminPaymentQrSettings());
    }

    @PutMapping("/payment-qr-settings/{paymentMethod}")
    ApiResponse<?> updatePaymentQrSetting(@PathVariable PaymentMethod paymentMethod,
                                          @RequestBody @Valid UpdatePaymentQrSettingRequest request) {
        return ApiResponse.ok(service.updatePaymentQrSetting(paymentMethod, request));
    }

    @PostMapping(value = "/payment-qr-settings/{paymentMethod}/qr-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<?> uploadPaymentQrImage(@PathVariable PaymentMethod paymentMethod,
                                        @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(service.uploadPaymentQrImage(paymentMethod, file));
    }

    @DeleteMapping("/payment-qr-settings/{paymentMethod}/qr-image")
    ApiResponse<?> removePaymentQrImage(@PathVariable PaymentMethod paymentMethod) {
        return ApiResponse.ok(service.removePaymentQrImage(paymentMethod));
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

    @GetMapping("/workspaces/{id}/business-owners")
    ApiResponse<?> businessOwners(@PathVariable UUID id) {
        return ApiResponse.ok(service.adminBusinessOwners(id));
    }

    @PostMapping("/workspaces/{id}/business-owners")
    ApiResponse<?> createBusinessOwner(@PathVariable UUID id, @RequestBody @Valid CreateBusinessOwnerRequest request) {
        return ApiResponse.ok(service.adminCreateBusinessOwner(id, request));
    }

    @PatchMapping("/business-owners/{id}/reset-password")
    ApiResponse<?> resetOwnerPassword(@PathVariable UUID id) {
        return ApiResponse.ok(service.adminResetOwnerPassword(id));
    }

    @PatchMapping("/business-owners/{id}/status")
    ApiResponse<?> updateOwnerStatus(@PathVariable UUID id, @RequestParam UserStatus status) {
        return ApiResponse.ok(service.adminUpdateOwnerStatus(id, status));
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
    ApiResponse<?> auditLogs(@RequestParam(required = false) UUID workspaceId,
                             @RequestParam(required = false) UUID actorId,
                             @RequestParam(required = false) String action,
                             @RequestParam(required = false) String entityType,
                             @RequestParam(required = false) String result,
                             @RequestParam(required = false) OffsetDateTime from,
                             @RequestParam(required = false) OffsetDateTime to,
                             @RequestParam(required = false) String search,
                             @RequestParam(required = false) Integer page,
                             @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(service.adminAuditLogs(workspaceId, actorId, action, entityType, result, from, to, search, page, size));
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
