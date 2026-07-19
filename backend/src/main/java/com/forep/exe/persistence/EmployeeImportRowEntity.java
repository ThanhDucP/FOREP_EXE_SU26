package com.forep.exe.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "employee_import_rows")
public class EmployeeImportRowEntity extends BaseEntity {
    @Column(nullable = false) private UUID batchId;
    @Column(nullable = false) private UUID workspaceId;
    @Column(nullable = false) private int rowNumber;
    @Column(nullable = false, columnDefinition = "text") private String rawData;
    @Column(nullable = false) private boolean valid;
    @Column(columnDefinition = "text") private String errors;
    @Column(nullable = false) private boolean imported;
    private UUID importedUserId;
    @Column(nullable = false) private OffsetDateTime createdAt;

    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getErrors() { return errors; }
    public void setErrors(String errors) { this.errors = errors; }
    public boolean isImported() { return imported; }
    public void setImported(boolean imported) { this.imported = imported; }
    public UUID getImportedUserId() { return importedUserId; }
    public void setImportedUserId(UUID importedUserId) { this.importedUserId = importedUserId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
