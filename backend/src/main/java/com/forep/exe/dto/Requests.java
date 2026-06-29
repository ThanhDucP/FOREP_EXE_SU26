package com.forep.exe.dto;

import com.forep.exe.domain.Enums.Role;
import com.forep.exe.domain.Enums.TaskPriority;
import com.forep.exe.domain.Enums.TaskStatus;
import com.forep.exe.domain.Enums.UpdateType;
import com.forep.exe.domain.Enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class Requests {
    private Requests() {
    }

    public record LoginRequest(@Email String email, @NotBlank String password) {
    }

    public record RegisterWorkspaceRequest(
            @NotBlank String workspaceName,
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
            String phone
    ) {
        public Role role() {
            return Role.EMPLOYEE;
        }
    }

    public record UpdateEmployeeStatusRequest(@NotNull String status) {
    }

    public record UpdateWorkspaceRequest(String name, String logo, String address) {
    }

    public record UpdateEmployeeRequest(
            @NotBlank String fullName,
            @Email String email,
            String phone,
            UserStatus status
    ) {
    }

    public record CreateTaskRequest(
            @NotBlank String title,
            @NotBlank String requirements,
            String description,
            @NotNull UUID assigneeId,
            TaskPriority priority,
            @NotNull OffsetDateTime deadline,
            BigDecimal estimatedHours
    ) {
    }

    public record UpdateTaskRequest(
            @NotBlank String title,
            @NotBlank String requirements,
            String description,
            @NotNull UUID assigneeId,
            TaskPriority priority,
            @NotNull OffsetDateTime deadline,
            BigDecimal estimatedHours
    ) {
    }

    public record AssignTaskRequest(@NotNull UUID assigneeId) {
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
            BigDecimal estimatedHours
    ) {
    }

    public record ExtractTasksRequest(
            @NotBlank String text,
            OffsetDateTime defaultDeadline
    ) {
    }
}
