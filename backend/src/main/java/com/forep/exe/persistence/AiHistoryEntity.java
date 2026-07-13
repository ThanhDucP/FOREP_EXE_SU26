package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.AiHistoryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_history")
public class AiHistoryEntity extends BaseEntity {
    @Column(nullable = false)
    private UUID workspaceId;
    @Column(nullable = false)
    private UUID callerId;
    @Column(nullable = false)
    private String callerName;
    @Column(nullable = false)
    private String callerRole;
    @Column(nullable = false)
    private String functionName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiHistoryStatus status;
    @Column(nullable = false)
    private OffsetDateTime calledAt;
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public UUID getCallerId() { return callerId; }
    public void setCallerId(UUID callerId) { this.callerId = callerId; }
    public String getCallerName() { return callerName; }
    public void setCallerName(String callerName) { this.callerName = callerName; }
    public String getCallerRole() { return callerRole; }
    public void setCallerRole(String callerRole) { this.callerRole = callerRole; }
    public String getFunctionName() { return functionName; }
    public void setFunctionName(String functionName) { this.functionName = functionName; }
    public AiHistoryStatus getStatus() { return status; }
    public void setStatus(AiHistoryStatus status) { this.status = status; }
    public OffsetDateTime getCalledAt() { return calledAt; }
    public void setCalledAt(OffsetDateTime calledAt) { this.calledAt = calledAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}