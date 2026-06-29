package com.forep.exe.controller;

import com.forep.exe.dto.ApiResponse;
import com.forep.exe.ai.AiProviderException;
import com.forep.exe.dto.Requests.AssignTaskRequest;
import com.forep.exe.dto.Requests.CreateEmployeeRequest;
import com.forep.exe.dto.Requests.CreateTaskRequest;
import com.forep.exe.dto.Requests.DailyReportRequest;
import com.forep.exe.dto.Requests.ExtractTasksRequest;
import com.forep.exe.dto.Requests.LoginRequest;
import com.forep.exe.dto.Requests.RecommendAssigneeRequest;
import com.forep.exe.dto.Requests.RegisterWorkspaceRequest;
import com.forep.exe.dto.Requests.UpdateEmployeeRequest;
import com.forep.exe.dto.Requests.UpdateProgressRequest;
import com.forep.exe.dto.Requests.UpdateTaskStatusRequest;
import com.forep.exe.dto.Requests.UpdateTaskRequest;
import com.forep.exe.dto.Requests.UpdateWorkspaceRequest;
import com.forep.exe.domain.Enums.AiSuggestionStatus;
import com.forep.exe.domain.Enums.UserStatus;
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

    @PostMapping("/workspaces/register")
    ApiResponse<?> registerWorkspace(@RequestBody @Valid RegisterWorkspaceRequest request) {
        return ApiResponse.ok(service.registerWorkspace(request));
    }

    @GetMapping("/workspaces/current")
    ApiResponse<?> currentWorkspace() {
        return ApiResponse.ok(service.currentWorkspace());
    }

    @PutMapping("/workspaces/current")
    ApiResponse<?> updateWorkspace(@RequestBody UpdateWorkspaceRequest request) {
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

    @PatchMapping("/tasks/{id}/assign")
    ApiResponse<?> assignTask(@PathVariable UUID id, @RequestBody @Valid AssignTaskRequest request) {
        return ApiResponse.ok(service.assignTask(id, request));
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

    @GetMapping("/analytics/employees/{id}/workload")
    ApiResponse<?> employeeWorkload(@PathVariable UUID id) {
        return ApiResponse.ok(service.employeeWorkload(id));
    }

    @PostMapping("/ai/recommend-assignee")
    ApiResponse<?> recommendAssignee(@RequestBody @Valid RecommendAssigneeRequest request) {
        return ApiResponse.ok(service.recommendAssignee(request));
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

    @ExceptionHandler(AiProviderException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ApiResponse<?> handleAiProviderError(AiProviderException exception) {
        return ApiResponse.error("AI_PROVIDER_ERROR", "Không thể tạo phân tích AI ở thời điểm này. Vui lòng thử lại sau.", null);
    }
}
