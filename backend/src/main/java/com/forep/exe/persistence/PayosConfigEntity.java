package com.forep.exe.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payos_config")
public class PayosConfigEntity extends BaseEntity {
    @Column(nullable = false, columnDefinition = "text")
    private String apiEndpoint;
    @Column(nullable = false)
    private String clientId;
    @Column(nullable = false, columnDefinition = "text")
    private String apiKeyEncrypted;
    @Column(nullable = false, columnDefinition = "text")
    private String checksumKeyEncrypted;
    @Column(nullable = false, columnDefinition = "text")
    private String returnUrl;
    @Column(nullable = false, columnDefinition = "text")
    private String cancelUrl;
    @Column(columnDefinition = "text")
    private String transferPrefix;
    @Column(nullable = false)
    private boolean active;
    private UUID updatedBy;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public String getApiEndpoint() { return apiEndpoint; }
    public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getApiKeyEncrypted() { return apiKeyEncrypted; }
    public void setApiKeyEncrypted(String apiKeyEncrypted) { this.apiKeyEncrypted = apiKeyEncrypted; }
    public String getChecksumKeyEncrypted() { return checksumKeyEncrypted; }
    public void setChecksumKeyEncrypted(String checksumKeyEncrypted) { this.checksumKeyEncrypted = checksumKeyEncrypted; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
    public String getTransferPrefix() { return transferPrefix; }
    public void setTransferPrefix(String transferPrefix) { this.transferPrefix = transferPrefix; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
