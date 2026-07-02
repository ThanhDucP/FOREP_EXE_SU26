package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.PaymentStatus;
import com.forep.exe.domain.Enums.RegistrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspace_registrations")
public class WorkspaceRegistrationEntity extends BaseEntity {
    @Column(nullable = false)
    private String businessName;
    @Column(nullable = false)
    private String workspaceName;
    @Column(nullable = false, unique = true)
    private String workspaceIdentifier;
    @Column(nullable = false)
    private String contactEmail;
    @Column(nullable = false)
    private String contactPhone;
    @Column(columnDefinition = "text")
    private String businessAddress;
    @Column(nullable = false)
    private UUID subscriptionPlanId;
    @Column(nullable = false)
    private int maxUsers;
    private String ownerFullName;
    private String ownerEmail;
    private String ownerPhone;
    private String ownerPasswordHash;
    private OffsetDateTime activationDate;
    private OffsetDateTime expirationDate;
    private String paymentProofUrl;
    @Column(columnDefinition = "text")
    private String paymentNote;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus registrationStatus;
    private UUID workspaceId;
    private UUID reviewedBy;
    private OffsetDateTime reviewedAt;
    @Column(columnDefinition = "text")
    private String reviewNote;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getWorkspaceName() { return workspaceName; }
    public void setWorkspaceName(String workspaceName) { this.workspaceName = workspaceName; }
    public String getWorkspaceIdentifier() { return workspaceIdentifier; }
    public void setWorkspaceIdentifier(String workspaceIdentifier) { this.workspaceIdentifier = workspaceIdentifier; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }
    public UUID getSubscriptionPlanId() { return subscriptionPlanId; }
    public void setSubscriptionPlanId(UUID subscriptionPlanId) { this.subscriptionPlanId = subscriptionPlanId; }
    public int getMaxUsers() { return maxUsers; }
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }
    public String getOwnerFullName() { return ownerFullName; }
    public void setOwnerFullName(String ownerFullName) { this.ownerFullName = ownerFullName; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
    public String getOwnerPasswordHash() { return ownerPasswordHash; }
    public void setOwnerPasswordHash(String ownerPasswordHash) { this.ownerPasswordHash = ownerPasswordHash; }
    public OffsetDateTime getActivationDate() { return activationDate; }
    public void setActivationDate(OffsetDateTime activationDate) { this.activationDate = activationDate; }
    public OffsetDateTime getExpirationDate() { return expirationDate; }
    public void setExpirationDate(OffsetDateTime expirationDate) { this.expirationDate = expirationDate; }
    public String getPaymentProofUrl() { return paymentProofUrl; }
    public void setPaymentProofUrl(String paymentProofUrl) { this.paymentProofUrl = paymentProofUrl; }
    public String getPaymentNote() { return paymentNote; }
    public void setPaymentNote(String paymentNote) { this.paymentNote = paymentNote; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public RegistrationStatus getRegistrationStatus() { return registrationStatus; }
    public void setRegistrationStatus(RegistrationStatus registrationStatus) { this.registrationStatus = registrationStatus; }
    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public UUID getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
