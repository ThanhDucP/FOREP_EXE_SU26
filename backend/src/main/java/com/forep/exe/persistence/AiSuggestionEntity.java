package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.AiSuggestionStatus;
import com.forep.exe.domain.Enums.AiSuggestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_suggestions")
public class AiSuggestionEntity extends BaseEntity {
    @Column(nullable = false)
    private UUID workspaceId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiSuggestionType type;
    @Column(nullable = false, columnDefinition = "text")
    private String inputData;
    @Column(nullable = false, columnDefinition = "text")
    private String outputData;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiSuggestionStatus status;
    @Column(nullable = false)
    private UUID createdBy;
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public AiSuggestionType getType() { return type; }
    public void setType(AiSuggestionType type) { this.type = type; }
    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }
    public String getOutputData() { return outputData; }
    public void setOutputData(String outputData) { this.outputData = outputData; }
    public AiSuggestionStatus getStatus() { return status; }
    public void setStatus(AiSuggestionStatus status) { this.status = status; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
