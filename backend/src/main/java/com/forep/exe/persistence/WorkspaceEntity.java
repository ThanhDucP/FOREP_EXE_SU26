package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.PaymentStatus;
import com.forep.exe.domain.Enums.WorkspaceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class WorkspaceEntity extends BaseEntity {
    @Column(nullable = false)
    private String name;
    private String logo;
    @Column(columnDefinition = "text")
    private String address;
    @Column(unique = true)
    private String shortCode;
    @Column(nullable = false)
    private int nextEmployeeNumber = 1;
    private UUID ownerId;
    private UUID subscriptionPlanId;
    private String businessName;
    private String contactEmail;
    private String contactPhone;
    @Column(nullable = false)
    private int maxUsers = 50;
    @Column(nullable = false)
    private int maxOwnerAccounts = 1;
    @Column(nullable = false)
    private int maxEmployeeAccounts = 49;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceStatus status = WorkspaceStatus.ACTIVE;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.CONFIRMED;
    private OffsetDateTime activatedAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime lastActivityAt;
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public int getNextEmployeeNumber() {
        return nextEmployeeNumber;
    }

    public void setNextEmployeeNumber(int nextEmployeeNumber) {
        this.nextEmployeeNumber = nextEmployeeNumber;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getSubscriptionPlanId() { return subscriptionPlanId; }

    public void setSubscriptionPlanId(UUID subscriptionPlanId) { this.subscriptionPlanId = subscriptionPlanId; }

    public String getBusinessName() { return businessName; }

    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getContactEmail() { return contactEmail; }

    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }

    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public int getMaxUsers() { return maxUsers; }

    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }

    public int getMaxOwnerAccounts() { return maxOwnerAccounts; }

    public void setMaxOwnerAccounts(int maxOwnerAccounts) { this.maxOwnerAccounts = maxOwnerAccounts; }

    public int getMaxEmployeeAccounts() { return maxEmployeeAccounts; }

    public void setMaxEmployeeAccounts(int maxEmployeeAccounts) { this.maxEmployeeAccounts = maxEmployeeAccounts; }

    public WorkspaceStatus getStatus() { return status; }

    public void setStatus(WorkspaceStatus status) { this.status = status; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }

    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public OffsetDateTime getActivatedAt() { return activatedAt; }

    public void setActivatedAt(OffsetDateTime activatedAt) { this.activatedAt = activatedAt; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }

    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

    public OffsetDateTime getLastActivityAt() { return lastActivityAt; }

    public void setLastActivityAt(OffsetDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
