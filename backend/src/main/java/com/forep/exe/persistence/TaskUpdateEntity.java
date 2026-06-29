package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.UpdateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_updates")
public class TaskUpdateEntity extends BaseEntity {
    @Column(nullable = false)
    private UUID taskId;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private int progressPercent;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    private String attachment;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UpdateType updateType;
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAttachment() { return attachment; }
    public void setAttachment(String attachment) { this.attachment = attachment; }
    public UpdateType getUpdateType() { return updateType; }
    public void setUpdateType(UpdateType updateType) { this.updateType = updateType; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
