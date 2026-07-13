package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.JobPositionStatus;
import com.forep.exe.domain.Enums.PermissionGroup;
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
    @Column(name = "title", nullable = false)
    private String name;
    private String code;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionGroup permissionGroup;
    @Column(nullable = false)
    private UUID departmentId;
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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTitle() { return name; }
    public void setTitle(String title) { this.name = title; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public PermissionGroup getPermissionGroup() { return permissionGroup; }
    public void setPermissionGroup(PermissionGroup permissionGroup) { this.permissionGroup = permissionGroup; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
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
