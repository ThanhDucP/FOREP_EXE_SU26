package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.WorkspaceSubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspace_subscriptions")
public class WorkspaceSubscriptionEntity extends BaseEntity {
    @Column(nullable = false)
    private UUID workspaceId;
    @Column(nullable = false)
    private UUID subscriptionPlanId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceSubscriptionStatus status;
    @Column(nullable = false)
    private OffsetDateTime startDate;
    @Column(nullable = false)
    private OffsetDateTime endDate;
    private OffsetDateTime renewalDate;
    @Column(nullable = false)
    private BigDecimal price;
    @Column(nullable = false)
    private int maxOwnerAccounts;
    @Column(nullable = false)
    private int maxEmployeeAccounts;
    private UUID paymentTransactionId;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public UUID getSubscriptionPlanId() { return subscriptionPlanId; }
    public void setSubscriptionPlanId(UUID subscriptionPlanId) { this.subscriptionPlanId = subscriptionPlanId; }
    public WorkspaceSubscriptionStatus getStatus() { return status; }
    public void setStatus(WorkspaceSubscriptionStatus status) { this.status = status; }
    public OffsetDateTime getStartDate() { return startDate; }
    public void setStartDate(OffsetDateTime startDate) { this.startDate = startDate; }
    public OffsetDateTime getEndDate() { return endDate; }
    public void setEndDate(OffsetDateTime endDate) { this.endDate = endDate; }
    public OffsetDateTime getRenewalDate() { return renewalDate; }
    public void setRenewalDate(OffsetDateTime renewalDate) { this.renewalDate = renewalDate; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getMaxOwnerAccounts() { return maxOwnerAccounts; }
    public void setMaxOwnerAccounts(int maxOwnerAccounts) { this.maxOwnerAccounts = maxOwnerAccounts; }
    public int getMaxEmployeeAccounts() { return maxEmployeeAccounts; }
    public void setMaxEmployeeAccounts(int maxEmployeeAccounts) { this.maxEmployeeAccounts = maxEmployeeAccounts; }
    public UUID getPaymentTransactionId() { return paymentTransactionId; }
    public void setPaymentTransactionId(UUID paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
