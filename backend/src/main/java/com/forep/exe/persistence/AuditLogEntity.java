package com.forep.exe.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity extends BaseEntity {
    private UUID workspaceId;
    private UUID actorId;
    private String actorNameSnapshot;
    private String actorRoleSnapshot;
    @Column(nullable = false)
    private String action;
    @Column(nullable = false)
    private String entityType;
    @Column(nullable = false)
    private UUID entityId;
    @Column(nullable = false)
    private String result;
    private String ipAddress;
    @Column(columnDefinition = "text")
    private String userAgent;
    private String requestId;
    @Column(columnDefinition = "text")
    private String metadata;
    @Column(columnDefinition = "text")
    private String oldValue;
    @Column(columnDefinition = "text")
    private String newValue;
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getActorNameSnapshot() { return actorNameSnapshot; }
    public void setActorNameSnapshot(String actorNameSnapshot) { this.actorNameSnapshot = actorNameSnapshot; }
    public String getActorRoleSnapshot() { return actorRoleSnapshot; }
    public void setActorRoleSnapshot(String actorRoleSnapshot) { this.actorRoleSnapshot = actorRoleSnapshot; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
