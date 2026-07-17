package com.forep.exe.dto;

import com.forep.exe.domain.Enums.AssignmentType;
import com.forep.exe.domain.Enums.AttachmentType;
import com.forep.exe.domain.Enums.EmployeeLevel;
import com.forep.exe.domain.Enums.EmploymentType;
import com.forep.exe.domain.Enums.DepartmentStatus;
import com.forep.exe.domain.Enums.JobPositionStatus;
import com.forep.exe.domain.Enums.PermissionGroup;
import com.forep.exe.domain.Enums.Role;
import com.forep.exe.domain.Enums.PaymentMethod;
import com.forep.exe.domain.Enums.SeniorityLevel;
import com.forep.exe.domain.Enums.SubscriptionPlanStatus;
import com.forep.exe.domain.Enums.TaskPriority;
import com.forep.exe.domain.Enums.TaskStatus;
import com.forep.exe.domain.Enums.UpdateType;
import com.forep.exe.domain.Enums.UserStatus;
import com.forep.exe.domain.Enums.WorkingStatus;
import com.forep.exe.domain.Enums.WorkspaceStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Requests {
    private Requests() {
    }

    public record LoginRequest(@Email String email, String username, @NotBlank String password) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {
    }

    public record RegisterWorkspaceRequest(
            @NotBlank String workspaceName,
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{2}$") String shortCode,
            String address,
            @NotBlank String ownerFullName,
            @Email String ownerEmail,
            String ownerPhone,
            @NotBlank String ownerPassword
    ) {
    }

    public record CreateEmployeeRequest(
            @NotBlank String fullName,
            @Email String email,
            String phone,
            String jobTitle,
            SeniorityLevel seniorityLevel,
            @Min(1) @Max(5) Integer skillRating,
            @Min(0) Integer yearsOfExperience,
            String skills,
            UUID departmentId,
            UUID jobPositionId,
            LocalDate dateOfBirth,
            String gender,
            String address,
            String personalSummary,
            EmploymentType employmentType,
            WorkingStatus workingStatus,
            EmployeeLevel employeeLevel,
            @Min(1) Integer monthlyWorkingCapacityHours,
            String mainExpertise,
            String secondaryExpertise
    ) {
        public Role role() {
            return Role.EMPLOYEE;
        }
    }

    public record UpdateEmployeeStatusRequest(@NotNull String status) {
    }

    public record UpdateWorkspaceRequest(String name, @Pattern(regexp = "^[A-Za-z0-9]{2}$") String shortCode, String logo, String address) {
    }

    public record UpdateEmployeeRequest(
            @NotBlank String fullName,
            @Email String email,
            String phone,
            UserStatus status,
            String jobTitle,
            SeniorityLevel seniorityLevel,
            @Min(1) @Max(5) Integer skillRating,
            @Min(0) Integer yearsOfExperience,
            String skills,
            UUID departmentId,
            UUID jobPositionId,
            LocalDate dateOfBirth,
            String gender,
            String address,
            String personalSummary,
            EmploymentType employmentType,
            WorkingStatus workingStatus,
            EmployeeLevel employeeLevel,
            @Min(1) Integer monthlyWorkingCapacityHours,
            String mainExpertise,
            String secondaryExpertise
    ) {
    }

    public record CreateTaskRequest(
            @NotBlank String title,
            @NotBlank String requirements,
            String description,
            String customerPhone,
            @Email String customerEmail,
            String customerDescription,
            UUID assigneeId,
            AssignmentType assignmentType,
            UUID teamLeaderId,
            List<UUID> teamMemberIds,
            TaskPriority priority,
            @NotNull OffsetDateTime deadline,
            OffsetDateTime startDate,
            @NotNull @Min(1) BigDecimal estimatedHours,
            @Min(1) @Max(5) Integer difficulty,
            String requiredSkills,
            UUID requiredJobPositionId,
            String taskDomain,
            UUID projectId,
            UUID departmentId,
            List<TaskAttachmentRequest> attachments
    ) {
    }

    public record UpdateTaskRequest(
            @NotBlank String title,
            @NotBlank String requirements,
            String description,
            String customerPhone,
            @Email String customerEmail,
            String customerDescription,
            UUID assigneeId,
            AssignmentType assignmentType,
            UUID teamLeaderId,
            List<UUID> teamMemberIds,
            TaskPriority priority,
            @NotNull OffsetDateTime deadline,
            OffsetDateTime startDate,
            @NotNull @Min(1) BigDecimal estimatedHours,
            @Min(1) @Max(5) Integer difficulty,
            String requiredSkills,
            UUID requiredJobPositionId,
            String taskDomain,
            UUID projectId,
            UUID departmentId,
            List<TaskAttachmentRequest> attachments
    ) {
    }

    public record AssignTaskRequest(@NotNull UUID assigneeId) {
    }

    public record AssignIndividualRequest(@NotNull UUID employeeId) {
    }

    public record AssignTeamRequest(@NotNull UUID teamLeaderId, List<UUID> teamMemberIds) {
    }

    public record UpdateTaskCustomerInfoRequest(
            String customerPhone,
            @Email String customerEmail,
            String customerDescription
    ) {
    }

    public record TaskAttachmentRequest(
            @NotBlank String fileName,
            @NotBlank String fileUrl,
            String contentType,
            Long fileSize,
            AttachmentType attachmentType
    ) {
    }

    public record JobPositionRequest(
            @NotBlank String title,
            @NotNull PermissionGroup permissionGroup,
            @NotNull UUID departmentId,
            String departmentName,
            String description,
            String requiredSkills,
            JobPositionStatus status
    ) {
    }

    public record BusinessPositionRequest(
            @NotBlank String name,
            String code,
            @NotNull PermissionGroup permissionGroup,
            @NotNull UUID departmentId,
            String description,
            JobPositionStatus status
    ) {
    }

    public record DepartmentRequest(
            @NotBlank String name,
            String code,
            String description,
            DepartmentStatus status
    ) {
    }

    public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {
    }

    public record UpdateProgressRequest(
            @Min(0) @Max(100) int progressPercent,
            @NotBlank String content,
            @NotNull UpdateType updateType,
            String attachment
    ) {
    }

    public record SubmitTaskCompletionRequest(
            @NotBlank String content,
            String attachment
    ) {
    }

    public record ReturnTaskRequest(
            @NotBlank String reason,
            String attachment
    ) {
    }

    public record DailyReportRequest(
            @NotNull LocalDate reportDate,
            @NotBlank String todayCompleted,
            @NotBlank String currentWork,
            String blockers,
            String tomorrowPlan
    ) {
    }

    public record RecommendAssigneeRequest(
            @NotBlank String title,
            @NotBlank String requirements,
            @NotNull OffsetDateTime deadline,
            BigDecimal estimatedHours,
            String taskDomain,
            UUID departmentId,
            UUID requiredJobPositionId,
            String requiredSkills
    ) {
    }

    public record TaskDomainAnalysisRequest(
            @NotBlank String taskTitle,
            @NotBlank String taskDescription,
            String projectDescription,
            String departmentName,
            OffsetDateTime startDate,
            OffsetDateTime deadline
    ) {
    }

    public record ExtractTasksRequest(
            @NotBlank String text,
            OffsetDateTime defaultDeadline
    ) {
    }

    public record EstimateHoursRequest(
            @NotBlank String taskTitle,
            String taskDescription,
            String difficulty,
            String taskType,
            OffsetDateTime startDate,
            OffsetDateTime deadline,
            @Min(1) Integer backendWorkingDays,
            @Min(1) BigDecimal backendDefaultHours
    ) {
    }

    public record RecommendationExplanationRequest(
            @NotBlank String recommendationType,
            @NotNull Map<String, Object> task,
            @NotNull List<Map<String, Object>> candidates
    ) {
    }

    public record RecommendationResultExplanationRequest(
            @NotNull Map<String, Object> task,
            @NotNull Map<String, Object> selectedAssigneeOrTeam,
            List<Map<String, Object>> rankingData,
            List<Map<String, Object>> comparisonWithOtherCandidates,
            Map<String, Object> workloadData,
            Map<String, Object> performanceData
    ) {
    }

    public record WorkloadRiskExplanationRequest(
            @NotBlank String employeeName,
            @NotNull @Min(1) BigDecimal monthlyCapacityHours,
            @NotNull List<Map<String, Object>> monthlyWorkloadEvaluation,
            String backendOverallRisk
    ) {
    }

    public record EmployeeReportAiRequest(
            @NotNull Map<String, Object> employee,
            @NotNull Map<String, Object> period,
            @NotNull Map<String, Object> metrics,
            List<Map<String, Object>> notableTasks,
            List<String> risks
    ) {
    }

    public record CreateSubscriptionPlanRequest(
            @NotBlank String name,
            String description,
            @NotNull BigDecimal price,
            @Min(1) int durationDays,
            @Min(1) Integer durationInMonths,
            @Min(1) int maxUsers,
            @Min(1) Integer maxOwnerAccounts,
            @Min(1) Integer maxEmployeeAccounts,
            Boolean hasFullFeatures,
            Integer maxWorkspaces,
            Integer aiUsageLimit,
            String features,
            SubscriptionPlanStatus status
    ) {
    }

    public record UpdateSubscriptionPlanRequest(
            String name,
            String description,
            BigDecimal price,
            Integer durationDays,
            Integer durationInMonths,
            Integer maxUsers,
            Integer maxOwnerAccounts,
            Integer maxEmployeeAccounts,
            Boolean hasFullFeatures,
            Integer maxWorkspaces,
            Integer aiUsageLimit,
            String features,
            SubscriptionPlanStatus status
    ) {
    }

    public record AdminCreateWorkspaceRequest(
            @NotBlank String businessName,
            @NotBlank String workspaceName,
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{2}$") String workspaceIdentifier,
            @Email @NotBlank String contactEmail,
            @NotBlank String contactPhone,
            String businessAddress,
            @NotNull UUID subscriptionPlanId,
            @Min(1) int maxUsers,
            OffsetDateTime activationDate,
            OffsetDateTime expirationDate,
            WorkspaceStatus status
    ) {
    }

    public record AdminUpdateWorkspaceRequest(
            String businessName,
            String workspaceName,
            @Email String contactEmail,
            String contactPhone,
            String businessAddress,
            UUID subscriptionPlanId,
            Integer maxUsers,
            OffsetDateTime activationDate,
            OffsetDateTime expirationDate,
            WorkspaceStatus status
    ) {
    }

    public record CreateBusinessOwnerRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            String username,
            String temporaryPassword,
            String phone,
            UserStatus status
    ) {
    }

    public record WorkspaceRegistrationRequest(
            @NotBlank String businessName,
            @NotBlank String workspaceName,
            @Pattern(regexp = "^[A-Za-z0-9]{2}$") String workspaceIdentifier,
            @Email @NotBlank String contactEmail,
            String contactPhone,
            String businessAddress,
            UUID subscriptionPlanId,
            @Min(1) Integer maxUsers,
            String ownerFullName,
            @Email String ownerEmail,
            String ownerPhone,
            String ownerPassword,
            @NotBlank String representativeFullName,
            @Email @NotBlank String representativeEmail,
            String representativePhone,
            String paymentProofUrl,
            String paymentNote
    ) {
    }

    public record SelectSubscriptionPlanRequest(@NotNull UUID subscriptionPlanId) {
    }

    public record CreatePaymentRequest(@NotNull PaymentMethod paymentMethod) {
    }

    public record PaymentCallbackRequest(
            String orderCode,
            String requestId,
            String providerTransactionId,
            String resultCode,
            String message,
            BigDecimal amount,
            String signature,
            String rawPayload
    ) {
    }

    public record ReviewRegistrationRequest(String note) {
    }

    public record SubmitPaymentRequest(
            @NotBlank String paymentProofUrl,
            String paymentNote
    ) {
    }

    public record BusinessFeedbackRequest(
            @Min(1) @Max(5) int rating,
            @NotBlank String content
    ) {
    }

    public record ReviewBusinessFeedbackRequest(String supportNote) {
    }
}
