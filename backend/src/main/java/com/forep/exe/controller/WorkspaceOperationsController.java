package com.forep.exe.controller;

import com.forep.exe.domain.Enums.JobPositionStatus;
import com.forep.exe.domain.Enums.AiHistoryStatus;
import com.forep.exe.domain.Enums.DepartmentStatus;
import com.forep.exe.domain.Enums.PermissionGroup;
import com.forep.exe.dto.ApiResponse;
import com.forep.exe.dto.Requests.AssignIndividualRequest;
import com.forep.exe.dto.Requests.AssignTeamRequest;
import com.forep.exe.dto.Requests.BusinessPositionRequest;
import com.forep.exe.dto.Requests.CreateTaskRequest;
import com.forep.exe.dto.Requests.DepartmentRequest;
import com.forep.exe.dto.Requests.JobPositionRequest;
import com.forep.exe.dto.Requests.RecommendAssigneeRequest;
import com.forep.exe.dto.Requests.TaskAttachmentRequest;
import com.forep.exe.dto.Requests.TaskDomainAnalysisRequest;
import com.forep.exe.dto.Requests.UpdateTaskCustomerInfoRequest;
import com.forep.exe.dto.Requests.UpdateTaskRequest;
import com.forep.exe.service.ForepService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceOperationsController {
    private final ForepService service;

    public WorkspaceOperationsController(ForepService service) {
        this.service = service;
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

    @GetMapping("/tasks/{id}/attachments")
    ApiResponse<?> taskAttachments(@PathVariable UUID id) {
        return ApiResponse.ok(service.taskAttachments(id));
    }

    @PostMapping("/tasks/{id}/attachments")
    ApiResponse<?> addTaskAttachment(@PathVariable UUID id, @RequestBody @Valid TaskAttachmentRequest request) {
        return ApiResponse.ok(service.addTaskAttachment(id, request));
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
}
