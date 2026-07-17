package com.forep.exe.controller;

import com.forep.exe.dto.ApiResponse;
import com.forep.exe.ai.AiProviderException;
import com.forep.exe.ai.AiRateLimitException;
import com.forep.exe.dto.Requests.AssignIndividualRequest;
import com.forep.exe.dto.Requests.AssignTaskRequest;
import com.forep.exe.dto.Requests.AssignTeamRequest;
import com.forep.exe.dto.Requests.AdminCreateWorkspaceRequest;
import com.forep.exe.dto.Requests.AdminUpdateWorkspaceRequest;
import com.forep.exe.dto.Requests.BusinessFeedbackRequest;
import com.forep.exe.dto.Requests.ChangePasswordRequest;
import com.forep.exe.dto.Requests.CreateBusinessOwnerRequest;
import com.forep.exe.dto.Requests.CreateEmployeeRequest;
import com.forep.exe.dto.Requests.CreatePaymentRequest;
import com.forep.exe.dto.Requests.CreateSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.CreateTaskRequest;
import com.forep.exe.dto.Requests.DailyReportRequest;
import com.forep.exe.dto.Requests.DepartmentRequest;
import com.forep.exe.dto.Requests.EmployeeReportAiRequest;
import com.forep.exe.dto.Requests.EstimateHoursRequest;
import com.forep.exe.dto.Requests.ExtractTasksRequest;
import com.forep.exe.dto.Requests.JobPositionRequest;
import com.forep.exe.dto.Requests.LoginRequest;
import com.forep.exe.dto.Requests.PaymentCallbackRequest;
import com.forep.exe.dto.Requests.RecommendAssigneeRequest;
import com.forep.exe.dto.Requests.RecommendationExplanationRequest;
import com.forep.exe.dto.Requests.RecommendationResultExplanationRequest;
import com.forep.exe.dto.Requests.RegisterWorkspaceRequest;
import com.forep.exe.dto.Requests.ReturnTaskRequest;
import com.forep.exe.dto.Requests.ReviewBusinessFeedbackRequest;
import com.forep.exe.dto.Requests.ReviewRegistrationRequest;
import com.forep.exe.dto.Requests.SelectSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.SubmitTaskCompletionRequest;
import com.forep.exe.dto.Requests.SubmitPaymentRequest;
import com.forep.exe.dto.Requests.TaskAttachmentRequest;
import com.forep.exe.dto.Requests.TaskDomainAnalysisRequest;
import com.forep.exe.dto.Requests.UpdateSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.UpdateEmployeeRequest;
import com.forep.exe.dto.Requests.UpdateProgressRequest;
import com.forep.exe.dto.Requests.UpdateTaskCustomerInfoRequest;
import com.forep.exe.dto.Requests.UpdateTaskStatusRequest;
import com.forep.exe.dto.Requests.UpdateTaskRequest;
import com.forep.exe.dto.Requests.UpdateWorkspaceRequest;
import com.forep.exe.dto.Requests.WorkloadRiskExplanationRequest;
import com.forep.exe.dto.Requests.WorkspaceRegistrationRequest;
import com.forep.exe.domain.Enums.AiSuggestionStatus;
import com.forep.exe.domain.Enums.DepartmentStatus;
import com.forep.exe.domain.Enums.JobPositionStatus;
import com.forep.exe.domain.Enums.UserStatus;
import com.forep.exe.domain.Enums.WorkspaceStatus;
import com.forep.exe.service.ForepService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ForepController {
    private final ForepService service;

    public ForepController(ForepService service) {
        this.service = service;
    }

    @PostMapping("/auth/login")
    ApiResponse<?> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.ok(service.login(request));
    }

    @PostMapping("/auth/logout")
    ApiResponse<?> logout() {
        return ApiResponse.ok(Map.of("message", "Đã đăng xuất."));
    }

    @GetMapping("/auth/me")
    ApiResponse<?> me() {
        return ApiResponse.ok(service.me());
    }

    @PatchMapping("/auth/change-password")
    ApiResponse<?> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        return ApiResponse.ok(service.changePassword(request));
    }

    @PostMapping("/workspaces/register")
    ApiResponse<?> registerWorkspace(@RequestBody @Valid RegisterWorkspaceRequest request) {
        return ApiResponse.ok(service.registerWorkspace(request));
    }

    @PostMapping("/workspace-registrations")
    ApiResponse<?> submitWorkspaceRegistration(@RequestBody @Valid WorkspaceRegistrationRequest request) {
        return ApiResponse.ok(service.submitWorkspaceRegistration(request));
    }

    @GetMapping("/workspace-registrations/{id}")
    ApiResponse<?> workspaceRegistration(@PathVariable UUID id) {
        return ApiResponse.ok(service.workspaceRegistration(id));
    }

    @GetMapping("/subscription-plans")
    ApiResponse<?> publicSubscriptionPlans() {
        return ApiResponse.ok(service.publicSubscriptionPlans());
    }

    @GetMapping("/subscription-plans/active")
    ApiResponse<?> activeSubscriptionPlans() {
        return ApiResponse.ok(service.publicSubscriptionPlans());
    }

    @PatchMapping("/workspace-registrations/{id}/select-plan")
    ApiResponse<?> selectSubscriptionPlan(@PathVariable UUID id, @RequestBody @Valid SelectSubscriptionPlanRequest request) {
        return ApiResponse.ok(service.selectSubscriptionPlan(id, request));
    }

    @PatchMapping("/workspace-registrations/{id}/payment")
    ApiResponse<?> submitRegistrationPayment(@PathVariable UUID id, @RequestBody @Valid SubmitPaymentRequest request) {
        return ApiResponse.ok(service.submitRegistrationPayment(id, request));
    }

    @PostMapping("/workspace-registrations/{id}/payments")
    ApiResponse<?> createPayment(@PathVariable UUID id, @RequestBody @Valid CreatePaymentRequest request) {
        return ApiResponse.ok(service.createPayment(id, request));
    }

    @GetMapping("/payments/{paymentId}")
    ApiResponse<?> payment(@PathVariable UUID paymentId) {
        return ApiResponse.ok(service.payment(paymentId));
    }

    @PostMapping("/payments/momo/callback")
    ApiResponse<?> momoCallback(@RequestBody PaymentCallbackRequest request) {
        return ApiResponse.ok(service.handleMomoCallback(request));
    }

    @PostMapping("/payments/bank-transfer/callback")
    ApiResponse<?> bankTransferCallback(@RequestBody PaymentCallbackRequest request) {
        return ApiResponse.ok(service.handleBankTransferCallback(request));
    }

    @GetMapping("/admin/workspaces")
    ApiResponse<?> adminWorkspaces() {
        return ApiResponse.ok(service.adminWorkspaces());
    }

    @PostMapping("/admin/workspaces")
    ApiResponse<?> adminCreateWorkspace(@RequestBody @Valid AdminCreateWorkspaceRequest request) {
        return ApiResponse.ok(service.adminCreateWorkspace(request));
    }

    @GetMapping("/admin/workspaces/{id}")
    ApiResponse<?> adminWorkspace(@PathVariable UUID id) {
        return ApiResponse.ok(service.adminWorkspace(id));
    }

    @PutMapping("/admin/workspaces/{id}")
    ApiResponse<?> adminUpdateWorkspace(@PathVariable UUID id, @RequestBody @Valid AdminUpdateWorkspaceRequest request) {
        return ApiResponse.ok(service.adminUpdateWorkspace(id, request));
    }

    @PatchMapping("/admin/workspaces/{id}/status")
    ApiResponse<?> adminUpdateWorkspaceStatus(@PathVariable UUID id, @RequestParam WorkspaceStatus status) {
        return ApiResponse.ok(service.adminUpdateWorkspaceStatus(id, status));
    }

    @GetMapping("/admin/workspaces/{id}/business-owners")
    ApiResponse<?> adminBusinessOwners(@PathVariable UUID id) {
        return ApiResponse.ok(service.adminBusinessOwners(id));
    }

    @PostMapping("/admin/workspaces/{id}/business-owners")
    ApiResponse<?> adminCreateBusinessOwner(@PathVariable UUID id, @RequestBody @Valid CreateBusinessOwnerRequest request) {
        return ApiResponse.ok(service.adminCreateBusinessOwner(id, request));
    }

    @PatchMapping("/admin/business-owners/{id}/reset-password")
    ApiResponse<?> adminResetOwnerPassword(@PathVariable UUID id) {
        return ApiResponse.ok(service.adminResetOwnerPassword(id));
    }

    @PatchMapping("/admin/business-owners/{id}/status")
    ApiResponse<?> adminUpdateOwnerStatus(@PathVariable UUID id, @RequestParam UserStatus status) {
        return ApiResponse.ok(service.adminUpdateOwnerStatus(id, status));
    }

    @GetMapping("/admin/subscription-plans")
    ApiResponse<?> subscriptionPlans() {
        return ApiResponse.ok(service.subscriptionPlans());
    }

    @PostMapping("/admin/subscription-plans")
    ApiResponse<?> createSubscriptionPlan(@RequestBody @Valid CreateSubscriptionPlanRequest request) {
        return ApiResponse.ok(service.createSubscriptionPlan(request));
    }

    @PutMapping("/admin/subscription-plans/{id}")
    ApiResponse<?> updateSubscriptionPlan(@PathVariable UUID id, @RequestBody @Valid UpdateSubscriptionPlanRequest request) {
        return ApiResponse.ok(service.updateSubscriptionPlan(id, request));
    }

    @PatchMapping("/admin/subscription-plans/{id}/activate")
    ApiResponse<?> activateSubscriptionPlan(@PathVariable UUID id) {
        return ApiResponse.ok(service.activateSubscriptionPlan(id));
    }

    @PatchMapping("/admin/subscription-plans/{id}/deactivate")
    ApiResponse<?> deactivateSubscriptionPlan(@PathVariable UUID id) {
        return ApiResponse.ok(service.deactivateSubscriptionPlan(id));
    }

    @GetMapping("/admin/workspace-registrations")
    ApiResponse<?> adminWorkspaceRegistrations() {
        return ApiResponse.ok(service.adminWorkspaceRegistrations());
    }

    @PatchMapping("/admin/workspace-registrations/{id}/approve")
    ApiResponse<?> approveWorkspaceRegistration(@PathVariable UUID id, @RequestBody(required = false) ReviewRegistrationRequest request) {
        return ApiResponse.ok(service.approveWorkspaceRegistration(id, request));
    }

    @PatchMapping("/admin/workspace-registrations/{id}/confirm-payment")
    ApiResponse<?> confirmRegistrationPayment(@PathVariable UUID id, @RequestBody(required = false) ReviewRegistrationRequest request) {
        return ApiResponse.ok(service.confirmRegistrationPayment(id, request));
    }

    @PostMapping("/admin/workspace-registrations/{id}/activate")
    ApiResponse<?> activateWorkspaceRegistration(@PathVariable UUID id, @RequestBody(required = false) ReviewRegistrationRequest request) {
        return ApiResponse.ok(service.approveWorkspaceRegistration(id, request));
    }

    @PatchMapping("/admin/payments/{paymentId}/confirm")
    ApiResponse<?> adminConfirmPayment(@PathVariable UUID paymentId, @RequestBody(required = false) ReviewRegistrationRequest request) {
        return ApiResponse.ok(service.adminConfirmPayment(paymentId, request));
    }

    @PatchMapping("/admin/payments/{paymentId}/reject")
    ApiResponse<?> adminRejectPayment(@PathVariable UUID paymentId, @RequestBody(required = false) ReviewRegistrationRequest request) {
        return ApiResponse.ok(service.adminRejectPayment(paymentId, request));
    }

    @PatchMapping("/admin/workspace-registrations/{id}/request-payment-correction")
    ApiResponse<?> requestRegistrationPaymentCorrection(@PathVariable UUID id, @RequestBody(required = false) ReviewRegistrationRequest request) {
        return ApiResponse.ok(service.requestRegistrationPaymentCorrection(id, request));
    }

    @PatchMapping("/admin/workspace-registrations/{id}/reject")
    ApiResponse<?> rejectWorkspaceRegistration(@PathVariable UUID id, @RequestBody(required = false) ReviewRegistrationRequest request) {
        return ApiResponse.ok(service.rejectWorkspaceRegistration(id, request));
    }

    @GetMapping("/admin/monitoring")
    ApiResponse<?> adminMonitoring() {
        return ApiResponse.ok(service.adminMonitoring());
    }

    @GetMapping("/admin/business-feedback")
    ApiResponse<?> adminBusinessFeedback() {
        return ApiResponse.ok(service.adminBusinessFeedback());
    }

    @PatchMapping("/admin/business-feedback/{id}/review")
    ApiResponse<?> reviewBusinessFeedback(@PathVariable UUID id, @RequestBody(required = false) ReviewBusinessFeedbackRequest request) {
        return ApiResponse.ok(service.reviewBusinessFeedback(id, request));
    }

    @PostMapping("/business-feedback")
    ApiResponse<?> submitBusinessFeedback(@RequestBody @Valid BusinessFeedbackRequest request) {
        return ApiResponse.ok(service.submitBusinessFeedback(request));
    }

    @GetMapping("/workspaces/current")
    ApiResponse<?> currentWorkspace() {
        return ApiResponse.ok(service.currentWorkspace());
    }

    @PutMapping("/workspaces/current")
    ApiResponse<?> updateWorkspace(@RequestBody @Valid UpdateWorkspaceRequest request) {
        return ApiResponse.ok(service.updateWorkspace(request));
    }

    @GetMapping("/employees")
    ApiResponse<?> employees() {
        return ApiResponse.ok(service.employees());
    }

    @PostMapping("/employees")
    ApiResponse<?> createEmployee(@RequestBody @Valid CreateEmployeeRequest request) {
        return ApiResponse.ok(service.createEmployee(request));
    }

    @GetMapping("/employees/{id}")
    ApiResponse<?> employee(@PathVariable UUID id) {
        return ApiResponse.ok(service.employee(id));
    }

    @PutMapping("/employees/{id}")
    ApiResponse<?> updateEmployee(@PathVariable UUID id, @RequestBody @Valid UpdateEmployeeRequest request) {
        return ApiResponse.ok(service.updateEmployee(id, request));
    }

    @PatchMapping("/employees/{id}/status")
    ApiResponse<?> updateEmployeeStatus(@PathVariable UUID id, @RequestParam UserStatus status) {
        return ApiResponse.ok(service.updateEmployeeStatus(id, status));
    }

    @PatchMapping("/employees/{id}/reset-password")
    ApiResponse<?> resetEmployeePassword(@PathVariable UUID id) {
        return ApiResponse.ok(service.resetEmployeePassword(id));
    }

    @GetMapping("/tasks")
    ApiResponse<?> tasks() {
        return ApiResponse.ok(service.tasks());
    }

    @PostMapping("/tasks")
    ApiResponse<?> createTask(@RequestBody @Valid CreateTaskRequest request) {
        return ApiResponse.ok(service.createTask(request));
    }

    @GetMapping("/tasks/{id}")
    ApiResponse<?> task(@PathVariable UUID id) {
        return ApiResponse.ok(service.task(id));
    }

    @PutMapping("/tasks/{id}")
    ApiResponse<?> updateTask(@PathVariable UUID id, @RequestBody @Valid UpdateTaskRequest request) {
        return ApiResponse.ok(service.updateTask(id, request));
    }

    @PatchMapping("/tasks/{id}/customer-info")
    ApiResponse<?> updateTaskCustomerInfo(@PathVariable UUID id, @RequestBody @Valid UpdateTaskCustomerInfoRequest request) {
        return ApiResponse.ok(service.updateTaskCustomerInfo(id, request));
    }

    @PatchMapping("/tasks/{id}/assign")
    ApiResponse<?> assignTask(@PathVariable UUID id, @RequestBody @Valid AssignTaskRequest request) {
        return ApiResponse.ok(service.assignTask(id, request));
    }

    @PatchMapping("/tasks/{id}/assign-individual")
    ApiResponse<?> assignIndividual(@PathVariable UUID id, @RequestBody @Valid AssignIndividualRequest request) {
        return ApiResponse.ok(service.assignIndividual(id, request));
    }

    @PatchMapping("/tasks/{id}/assign-team")
    ApiResponse<?> assignTeam(@PathVariable UUID id, @RequestBody @Valid AssignTeamRequest request) {
        return ApiResponse.ok(service.assignTeam(id, request));
    }

    @PatchMapping("/tasks/{id}/accept")
    ApiResponse<?> acceptTask(@PathVariable UUID id) {
        return ApiResponse.ok(service.acceptTask(id));
    }

    @PatchMapping("/tasks/{id}/submit-completion")
    ApiResponse<?> submitTaskCompletion(@PathVariable UUID id, @RequestBody @Valid SubmitTaskCompletionRequest request) {
        return ApiResponse.ok(service.submitTaskCompletion(id, request));
    }

    @PatchMapping("/tasks/{id}/approve-completion")
    ApiResponse<?> approveTaskCompletion(@PathVariable UUID id) {
        return ApiResponse.ok(service.approveTaskCompletion(id));
    }

    @PatchMapping("/tasks/{id}/return")
    ApiResponse<?> returnTask(@PathVariable UUID id, @RequestBody @Valid ReturnTaskRequest request) {
        return ApiResponse.ok(service.returnTask(id, request));
    }

    @GetMapping("/tasks/{id}/attachments")
    ApiResponse<?> taskAttachments(@PathVariable UUID id) {
        return ApiResponse.ok(service.taskAttachments(id));
    }

    @PostMapping("/tasks/{id}/attachments")
    ApiResponse<?> addTaskAttachment(@PathVariable UUID id, @RequestBody @Valid TaskAttachmentRequest request) {
        return ApiResponse.ok(service.addTaskAttachment(id, request));
    }

    @PatchMapping("/tasks/{id}/status")
    ApiResponse<?> updateStatus(@PathVariable UUID id, @RequestBody @Valid UpdateTaskStatusRequest request) {
        return ApiResponse.ok(service.updateStatus(id, request));
    }

    @PatchMapping("/tasks/{id}/progress")
    ApiResponse<?> updateProgress(@PathVariable UUID id, @RequestBody @Valid UpdateProgressRequest request) {
        return ApiResponse.ok(service.updateProgress(id, request));
    }

    @GetMapping("/tasks/{id}/updates")
    ApiResponse<?> updates(@PathVariable UUID id) {
        return ApiResponse.ok(service.taskUpdates(id));
    }

    @PostMapping("/tasks/{id}/updates")
    ApiResponse<?> addUpdate(@PathVariable UUID id, @RequestBody @Valid UpdateProgressRequest request) {
        return ApiResponse.ok(service.updateProgress(id, request));
    }

    @PatchMapping("/tasks/{id}/cancel")
    ApiResponse<?> cancelTask(@PathVariable UUID id) {
        return ApiResponse.ok(service.cancelTask(id));
    }

    @GetMapping("/analytics/owner-dashboard")
    ApiResponse<?> ownerDashboard() {
        return ApiResponse.ok(service.ownerDashboard());
    }

    @GetMapping("/analytics/workload")
    ApiResponse<?> workload() {
        return ApiResponse.ok(service.workload());
    }

    @GetMapping("/analytics/workload/monthly")
    ApiResponse<?> monthlyWorkload(@RequestParam int year, @RequestParam int month) {
        return ApiResponse.ok(service.monthlyWorkload(year, month));
    }

    @GetMapping("/analytics/employees/{id}/workload")
    ApiResponse<?> employeeWorkload(@PathVariable UUID id) {
        return ApiResponse.ok(service.employeeWorkload(id));
    }

    @PostMapping("/ai/recommend-assignee")
    ApiResponse<?> recommendAssignee(@RequestBody @Valid RecommendAssigneeRequest request) {
        return ApiResponse.ok(service.recommendAssignee(request));
    }

    @PostMapping("/ai/recommend-team-leaders")
    ApiResponse<?> recommendTeamLeaders(@RequestBody @Valid RecommendAssigneeRequest request) {
        return ApiResponse.ok(service.recommendTeamLeaders(request));
    }

    @PostMapping("/ai/recommend-team-members")
    ApiResponse<?> recommendTeamMembers(@RequestBody @Valid RecommendAssigneeRequest request) {
        return ApiResponse.ok(service.recommendTeamMembers(request));
    }

    @GetMapping("/ai/workload-summary")
    ApiResponse<?> workloadSummary() {
        return ApiResponse.ok(service.workloadSummary());
    }

    @GetMapping("/ai/delay-risks")
    ApiResponse<?> delayRisks() {
        return ApiResponse.ok(service.delayRisks());
    }

    @GetMapping("/ai/daily-reports/insights")
    ApiResponse<?> dailyReportInsights() {
        return ApiResponse.ok(service.dailyReportInsights());
    }

    @GetMapping("/ai/daily-reports/missing")
    ApiResponse<?> missingReports() {
        return ApiResponse.ok(service.missingReports());
    }

    @PostMapping("/ai/tasks/extract")
    ApiResponse<?> extractTasks(@RequestBody @Valid ExtractTasksRequest request) {
        return ApiResponse.ok(service.extractTasks(request));
    }

    @PostMapping("/ai/tasks/analyze")
    ApiResponse<?> analyzeTaskDomain(@RequestBody @Valid TaskDomainAnalysisRequest request) {
        return ApiResponse.ok(service.analyzeTaskDomain(request));
    }

    @PostMapping("/ai/tasks/estimate-hours")
    ApiResponse<?> estimateTaskHours(@RequestBody @Valid EstimateHoursRequest request) {
        return ApiResponse.ok(service.estimateTaskHours(request));
    }

    @PostMapping("/ai/recommendations/explain")
    ApiResponse<?> explainRecommendation(@RequestBody @Valid RecommendationExplanationRequest request) {
        return ApiResponse.ok(service.explainRecommendation(request));
    }

    @PostMapping("/ai/recommendations/result/explain")
    ApiResponse<?> explainRecommendationResult(@RequestBody @Valid RecommendationResultExplanationRequest request) {
        return ApiResponse.ok(service.explainRecommendationResult(request));
    }

    @PostMapping("/ai/workload/risk")
    ApiResponse<?> explainWorkloadRisk(@RequestBody @Valid WorkloadRiskExplanationRequest request) {
        return ApiResponse.ok(service.explainWorkloadRisk(request));
    }

    @PostMapping("/ai/employee-report")
    ApiResponse<?> generateEmployeeReport(@RequestBody @Valid EmployeeReportAiRequest request) {
        return ApiResponse.ok(service.generateEmployeeReport(request));
    }

    @GetMapping("/ai/business-owner/operational-summary")
    ApiResponse<?> businessOwnerOperationalSummary() {
        return ApiResponse.ok(service.businessOwnerOperationalSummary());
    }

    @PostMapping("/ai/tasks/{id}/split")
    ApiResponse<?> splitTask(@PathVariable UUID id) {
        return ApiResponse.ok(service.splitTask(id));
    }

    @PostMapping("/ai/tasks/{id}/adjust")
    ApiResponse<?> taskAdjustment(@PathVariable UUID id) {
        return ApiResponse.ok(service.taskAdjustment(id));
    }

    @GetMapping("/ai/action-suggestions")
    ApiResponse<?> actionSuggestions() {
        return ApiResponse.ok(service.actionSuggestions());
    }

    @GetMapping("/ai/suggestions")
    ApiResponse<?> aiSuggestions() {
        return ApiResponse.ok(service.aiSuggestions());
    }

    @PatchMapping("/ai/suggestions/{id}/status")
    ApiResponse<?> updateAiSuggestionStatus(@PathVariable UUID id, @RequestParam AiSuggestionStatus status) {
        return ApiResponse.ok(service.updateAiSuggestionStatus(id, status));
    }

    @GetMapping("/ai/business-summary/daily")
    ApiResponse<?> dailyBusinessSummary() {
        return ApiResponse.ok(service.dailyAiSummary());
    }

    @GetMapping("/ai/business-summary/weekly")
    ApiResponse<?> weeklyBusinessSummary() {
        return ApiResponse.ok(service.businessSummary("weekly"));
    }

    @GetMapping("/ai/business-summary/monthly")
    ApiResponse<?> monthlyBusinessSummary() {
        return ApiResponse.ok(service.businessSummary("monthly"));
    }

    @GetMapping("/daily-reports")
    ApiResponse<?> reports() {
        return ApiResponse.ok(service.reports());
    }

    @GetMapping("/hr/departments")
    ApiResponse<?> departments() {
        return ApiResponse.ok(service.departments());
    }

    @GetMapping("/hr/departments/{id}")
    ApiResponse<?> department(@PathVariable UUID id) {
        return ApiResponse.ok(service.department(id));
    }

    @PostMapping("/hr/departments")
    ApiResponse<?> createDepartment(@RequestBody @Valid DepartmentRequest request) {
        return ApiResponse.ok(service.createDepartment(request));
    }

    @PutMapping("/hr/departments/{id}")
    ApiResponse<?> updateDepartment(@PathVariable UUID id, @RequestBody @Valid DepartmentRequest request) {
        return ApiResponse.ok(service.updateDepartment(id, request));
    }

    @PatchMapping("/hr/departments/{id}/activate")
    ApiResponse<?> activateDepartment(@PathVariable UUID id) {
        return ApiResponse.ok(service.updateDepartmentStatus(id, DepartmentStatus.ACTIVE));
    }

    @PatchMapping("/hr/departments/{id}/deactivate")
    ApiResponse<?> deactivateDepartment(@PathVariable UUID id) {
        return ApiResponse.ok(service.updateDepartmentStatus(id, DepartmentStatus.INACTIVE));
    }

    @GetMapping("/hr/job-positions")
    ApiResponse<?> jobPositions() {
        return ApiResponse.ok(service.jobPositions());
    }

    @PostMapping("/hr/job-positions")
    ApiResponse<?> createJobPosition(@RequestBody @Valid JobPositionRequest request) {
        return ApiResponse.ok(service.createJobPosition(request));
    }

    @PutMapping("/hr/job-positions/{id}")
    ApiResponse<?> updateJobPosition(@PathVariable UUID id, @RequestBody @Valid JobPositionRequest request) {
        return ApiResponse.ok(service.updateJobPosition(id, request));
    }

    @PatchMapping("/hr/job-positions/{id}/status")
    ApiResponse<?> updateJobPositionStatus(@PathVariable UUID id, @RequestParam JobPositionStatus status) {
        return ApiResponse.ok(service.updateJobPositionStatus(id, status));
    }

    @PostMapping("/daily-reports")
    ApiResponse<?> createReport(@RequestBody @Valid DailyReportRequest request) {
        return ApiResponse.ok(service.createReport(request));
    }

    @GetMapping("/daily-reports/{id}")
    ApiResponse<?> report(@PathVariable UUID id) {
        return ApiResponse.ok(service.report(id));
    }

    @PatchMapping("/daily-reports/{id}/review")
    ApiResponse<?> reviewReport(@PathVariable UUID id) {
        return ApiResponse.ok(service.reviewReport(id));
    }

    @GetMapping("/notifications")
    ApiResponse<?> notifications() {
        return ApiResponse.ok(service.notifications());
    }

    @PatchMapping("/notifications/{id}/read")
    ApiResponse<?> readNotification(@PathVariable UUID id) {
        return ApiResponse.ok(service.readNotification(id));
    }

    @PatchMapping("/notifications/read-all")
    ApiResponse<?> readAllNotifications() {
        return ApiResponse.ok(service.readAllNotifications());
    }

    @GetMapping("/health")
    ApiResponse<?> health() {
        return ApiResponse.ok(Map.of("status", "ok"));
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
