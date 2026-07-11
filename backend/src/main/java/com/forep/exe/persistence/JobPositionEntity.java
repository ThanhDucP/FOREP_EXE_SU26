package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.JobPositionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_positions")
public class JobPositionEntity extends BaseEntity {
    @Column(nullable = false)
    private UUID workspaceId;
    @Column(nullable = false)
    private String title;
    private String departmentName;
    @Column(columnDefinition = "text")
    private String description;
    @Column(columnDefinition = "text")
    private String requiredSkills;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobPositionStatus status;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
    public JobPositionStatus getStatus() { return status; }
    public void setStatus(JobPositionStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
