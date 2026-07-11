package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.TaskParticipantRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_assignees")
public class TaskAssigneeEntity extends BaseEntity {
    @Column(nullable = false)
    private UUID workspaceId;
    @Column(nullable = false)
    private UUID taskId;
    @Column(nullable = false)
    private UUID employeeId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskParticipantRole participantRole;
    @Column(nullable = false)
    private boolean leader;
    private BigDecimal allocatedHours;
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public TaskParticipantRole getParticipantRole() { return participantRole; }
    public void setParticipantRole(TaskParticipantRole participantRole) { this.participantRole = participantRole; }
    public boolean isLeader() { return leader; }
    public void setLeader(boolean leader) { this.leader = leader; }
    public BigDecimal getAllocatedHours() { return allocatedHours; }
    public void setAllocatedHours(BigDecimal allocatedHours) { this.allocatedHours = allocatedHours; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
