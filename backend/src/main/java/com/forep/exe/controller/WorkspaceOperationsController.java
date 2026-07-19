package com.forep.exe.controller;

import com.forep.exe.ai.AiProviderException;
import com.forep.exe.ai.AiRateLimitException;
import com.forep.exe.domain.Enums.JobPositionStatus;
import com.forep.exe.domain.Enums.AiHistoryStatus;
import com.forep.exe.domain.Enums.DepartmentStatus;
import com.forep.exe.domain.Enums.PermissionGroup;
import com.forep.exe.dto.ApiResponse;
import com.forep.exe.dto.Requests.AssignIndividualRequest;
import com.forep.exe.dto.Requests.AssignTeamRequest;
import com.forep.exe.dto.Requests.BusinessPositionRequest;
import com.forep.exe.dto.Requests.CreateTaskRequest;
import com.forep.exe.dto.Requests.CreateEmployeeRequest;
import com.forep.exe.dto.Requests.CreateHrAccountRequest;
import com.forep.exe.dto.Requests.DepartmentRequest;
import com.forep.exe.dto.Requests.EmployeeReportAiRequest;
import com.forep.exe.dto.Requests.EstimateHoursRequest;
import com.forep.exe.dto.Requests.JobPositionRequest;
import com.forep.exe.dto.Requests.RecommendAssigneeRequest;
import com.forep.exe.dto.Requests.RecommendationExplanationRequest;
import com.forep.exe.dto.Requests.RecommendationResultExplanationRequest;
import com.forep.exe.dto.Requests.ReturnTaskRequest;
import com.forep.exe.dto.Requests.SubmitTaskCompletionRequest;
import com.forep.exe.dto.Requests.TaskAttachmentRequest;
import com.forep.exe.dto.Requests.TaskDomainAnalysisRequest;
import com.forep.exe.dto.Requests.UpdateTaskCustomerInfoRequest;
import com.forep.exe.dto.Requests.UpdateTaskRequest;
import com.forep.exe.dto.Requests.UpdateEmployeeRequest;
import com.forep.exe.dto.Requests.UpdateEmployeeStatusRequest;
import com.forep.exe.domain.Enums.UserStatus;
import com.forep.exe.dto.Requests.WorkloadRiskExplanationRequest;
import com.forep.exe.service.ForepService;
import com.forep.exe.service.EmployeeImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceOperationsController {
    private final ForepService service;
    private final EmployeeImportService employeeImportService;

    public WorkspaceOperationsController(ForepService service, EmployeeImportService employeeImportService) {
        this.service = service;
        this.employeeImportService = employeeImportService;
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

    @GetMapping("/hr/employees")
    ApiResponse<?> employees() {
        return ApiResponse.ok(service.employees());
    }

    @GetMapping("/hr/employees/import-template")
    ResponseEntity<byte[]> employeeImportTemplate() {
        return fileResponse(employeeImportService.template());
    }

    @PostMapping(value = "/hr/employees/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<?> validateEmployeeImport(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(employeeImportService.validate(file));
    }

    @GetMapping("/hr/employees/imports")
    ApiResponse<?> employeeImportHistory() {
        return ApiResponse.ok(employeeImportService.history());
    }

    @GetMapping("/hr/employees/imports/{batchId}")
    ApiResponse<?> employeeImportBatch(@PathVariable UUID batchId) {
        return ApiResponse.ok(employeeImportService.batch(batchId));
    }

    @PostMapping("/hr/employees/imports/{batchId}/confirm")
    ApiResponse<?> confirmEmployeeImport(@PathVariable UUID batchId) {
        return ApiResponse.ok(employeeImportService.confirm(batchId));
    }

    @GetMapping("/hr/employees/imports/{batchId}/errors")
    ResponseEntity<byte[]> employeeImportErrors(@PathVariable UUID batchId) {
        return fileResponse(employeeImportService.errorReport(batchId));
    }

    @DeleteMapping("/hr/employees/imports/{batchId}")
    ApiResponse<?> cancelEmployeeImport(@PathVariable UUID batchId) {
        return ApiResponse.ok(employeeImportService.cancel(batchId));
    }

    private ResponseEntity<byte[]> fileResponse(EmployeeImportService.FilePayload file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.content().length)
                .body(file.content());
    }

    @GetMapping("/hr/employees/{id}")
    ApiResponse<?> employee(@PathVariable UUID id) {
        return ApiResponse.ok(service.employee(id));
    }

    @PostMapping("/hr/employees")
    ApiResponse<?> createEmployee(@RequestBody @Valid CreateEmployeeRequest request) {
        return ApiResponse.ok(service.createEmployee(request));
    }

    @PutMapping("/hr/employees/{id}")
    ApiResponse<?> updateEmployee(@PathVariable UUID id, @RequestBody @Valid UpdateEmployeeRequest request) {
        return ApiResponse.ok(service.updateEmployee(id, request));
    }

    @PatchMapping("/hr/employees/{id}/status")
    ApiResponse<?> updateEmployeeStatus(@PathVariable UUID id, @RequestBody @Valid UpdateEmployeeStatusRequest request) {
        return ApiResponse.ok(service.updateEmployeeStatus(id, UserStatus.valueOf(request.status().toUpperCase())));
    }

    @GetMapping("/business-owner/hr-accounts")
    ApiResponse<?> hrAccounts() {
        return ApiResponse.ok(service.hrAccounts());
    }

    @PostMapping("/business-owner/hr-accounts")
    ApiResponse<?> createInitialHrAccount(@RequestBody @Valid CreateHrAccountRequest request) {
        return ApiResponse.ok(service.createInitialHrAccount(request));
    }

    @PatchMapping("/business-owner/hr-accounts/{id}/status")
    ApiResponse<?> updateHrAccountStatus(@PathVariable UUID id, @RequestBody @Valid UpdateEmployeeStatusRequest request) {
        return ApiResponse.ok(service.updateHrAccountStatus(id, UserStatus.valueOf(request.status().toUpperCase())));
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

    @GetMapping("/hr/business-positions")
    ApiResponse<?> businessPositions(@RequestParam(required = false) String search,
                                     @RequestParam(required = false) UUID departmentId,
                                     @RequestParam(required = false) PermissionGroup permissionGroup,
                                     @RequestParam(required = false) JobPositionStatus status) {
        return ApiResponse.ok(service.businessPositions(search, departmentId, permissionGroup, status));
    }

    @PostMapping("/hr/business-positions")
    ApiResponse<?> createBusinessPosition(@RequestBody @Valid BusinessPositionRequest request) {
        return ApiResponse.ok(service.createBusinessPosition(request));
    }

    @GetMapping("/hr/business-positions/{id}")
    ApiResponse<?> businessPosition(@PathVariable UUID id) {
        return ApiResponse.ok(service.businessPosition(id));
    }

    @PutMapping("/hr/business-positions/{id}")
    ApiResponse<?> updateBusinessPosition(@PathVariable UUID id, @RequestBody @Valid BusinessPositionRequest request) {
        return ApiResponse.ok(service.updateBusinessPosition(id, request));
    }

    @PatchMapping("/hr/business-positions/{id}/activate")
    ApiResponse<?> activateBusinessPosition(@PathVariable UUID id) {
        return ApiResponse.ok(service.updateBusinessPositionStatus(id, JobPositionStatus.ACTIVE));
    }

    @PatchMapping("/hr/business-positions/{id}/deactivate")
    ApiResponse<?> deactivateBusinessPosition(@PathVariable UUID id) {
        return ApiResponse.ok(service.updateBusinessPositionStatus(id, JobPositionStatus.INACTIVE));
    }

    @PutMapping("/hr/job-positions/{id}")
    ApiResponse<?> updateJobPosition(@PathVariable UUID id, @RequestBody @Valid JobPositionRequest request) {
        return ApiResponse.ok(service.updateJobPosition(id, request));
    }

    @PatchMapping("/hr/job-positions/{id}/status")
    ApiResponse<?> updateJobPositionStatus(@PathVariable UUID id, @RequestParam JobPositionStatus status) {
        return ApiResponse.ok(service.updateJobPositionStatus(id, status));
    }

    @PostMapping("/ai/recommendations/individual")
    ApiResponse<?> recommendIndividual(@RequestBody @Valid RecommendAssigneeRequest request) {
        return ApiResponse.ok(service.recommendAssignee(request));
    }

    @PostMapping("/ai/recommendations/team-leaders")
    ApiResponse<?> recommendTeamLeaders(@RequestBody @Valid RecommendAssigneeRequest request) {
        return ApiResponse.ok(service.recommendTeamLeaders(request));
    }

    @PostMapping("/ai/recommendations/team-members")
    ApiResponse<?> recommendTeamMembers(@RequestBody @Valid RecommendAssigneeRequest request) {
        return ApiResponse.ok(service.recommendTeamMembers(request));
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

    @GetMapping("/ai-history")
    ApiResponse<?> aiHistory(@RequestParam(required = false) String function,
                             @RequestParam(required = false) AiHistoryStatus status,
                             @RequestParam(required = false) OffsetDateTime from,
                             @RequestParam(required = false) OffsetDateTime to,
                             @RequestParam(required = false) String caller,
                             @RequestParam(required = false) Integer limit,
                             @RequestParam(required = false) Integer offset) {
        return ApiResponse.ok(service.aiHistory(function, status, from, to, caller, limit, offset));
    }

    @GetMapping("/workload/monthly")
    ApiResponse<?> monthlyWorkload(@RequestParam int year, @RequestParam int month) {
        return ApiResponse.ok(service.monthlyWorkload(year, month));
    }

    @GetMapping("/business-owner/dashboard")
    ApiResponse<?> ownerDashboard() {
        return ApiResponse.ok(service.ownerDashboard());
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
