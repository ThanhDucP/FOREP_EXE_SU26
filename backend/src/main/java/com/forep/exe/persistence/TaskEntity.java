package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.AssignmentType;
import com.forep.exe.domain.Enums.TaskPriority;
import com.forep.exe.domain.Enums.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class TaskEntity extends BaseEntity {
    @Column(nullable = false)
    private UUID workspaceId;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String requirements;
    @Column(columnDefinition = "text")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentType assignmentType;
    @Column(nullable = false)
    private UUID assigneeId;
    @Column(nullable = false)
    private UUID creatorId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;
    @Column(nullable = false)
    private OffsetDateTime deadline;
    private OffsetDateTime startDate;
    private BigDecimal estimatedHours;
    private Integer difficulty;
    @Column(columnDefinition = "text")
    private String requiredSkills;
    private UUID requiredJobPositionId;
    private String taskDomain;
    private UUID projectId;
    private UUID departmentId;
    @Column(nullable = false)
    private int progressPercent;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
    private OffsetDateTime completedAt;

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AssignmentType getAssignmentType() { return assignmentType; }
    public void setAssignmentType(AssignmentType assignmentType) { this.assignmentType = assignmentType; }
    public UUID getAssigneeId() { return assigneeId; }
    public void setAssigneeId(UUID assigneeId) { this.assigneeId = assigneeId; }
    public UUID getCreatorId() { return creatorId; }
    public void setCreatorId(UUID creatorId) { this.creatorId = creatorId; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public OffsetDateTime getDeadline() { return deadline; }
    public void setDeadline(OffsetDateTime deadline) { this.deadline = deadline; }
    public OffsetDateTime getStartDate() { return startDate; }
    public void setStartDate(OffsetDateTime startDate) { this.startDate = startDate; }
    public BigDecimal getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(BigDecimal estimatedHours) { this.estimatedHours = estimatedHours; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
    public UUID getRequiredJobPositionId() { return requiredJobPositionId; }
    public void setRequiredJobPositionId(UUID requiredJobPositionId) { this.requiredJobPositionId = requiredJobPositionId; }
    public String getTaskDomain() { return taskDomain; }
    public void setTaskDomain(String taskDomain) { this.taskDomain = taskDomain; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}
