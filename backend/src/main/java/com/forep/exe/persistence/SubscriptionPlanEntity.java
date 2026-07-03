package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.SubscriptionPlanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlanEntity extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false)
    private BigDecimal price;
    @Column(nullable = false)
    private int durationDays;
    @Column(nullable = false)
    private int durationInMonths = 1;
    @Column(nullable = false)
    private int maxUsers;
    @Column(nullable = false)
    private int maxOwnerAccounts = 1;
    @Column(nullable = false)
    private int maxEmployeeAccounts = 1;
    @Column(nullable = false)
    private boolean hasFullFeatures = true;
    private Integer maxWorkspaces;
    private Integer aiUsageLimit;
    @Column(columnDefinition = "text")
    private String features;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlanStatus status;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
    public int getDurationInMonths() { return durationInMonths; }
    public void setDurationInMonths(int durationInMonths) { this.durationInMonths = durationInMonths; }
    public int getMaxUsers() { return maxUsers; }
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }
    public int getMaxOwnerAccounts() { return maxOwnerAccounts; }
    public void setMaxOwnerAccounts(int maxOwnerAccounts) { this.maxOwnerAccounts = maxOwnerAccounts; }
    public int getMaxEmployeeAccounts() { return maxEmployeeAccounts; }
    public void setMaxEmployeeAccounts(int maxEmployeeAccounts) { this.maxEmployeeAccounts = maxEmployeeAccounts; }
    public boolean isHasFullFeatures() { return hasFullFeatures; }
    public void setHasFullFeatures(boolean hasFullFeatures) { this.hasFullFeatures = hasFullFeatures; }
    public Integer getMaxWorkspaces() { return maxWorkspaces; }
    public void setMaxWorkspaces(Integer maxWorkspaces) { this.maxWorkspaces = maxWorkspaces; }
    public Integer getAiUsageLimit() { return aiUsageLimit; }
    public void setAiUsageLimit(Integer aiUsageLimit) { this.aiUsageLimit = aiUsageLimit; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
    public SubscriptionPlanStatus getStatus() { return status; }
    public void setStatus(SubscriptionPlanStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
