package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.PaymentMethod;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransactionEntity extends BaseEntity {
    @Column(nullable = false)
    private UUID workspaceRegistrationId;
    @Column(nullable = false)
    private UUID subscriptionPlanId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private String currency = "VND";
    @Column(nullable = false, unique = true)
    private String paymentCode;
    @Column(nullable = false, unique = true)
    private String orderCode;
    @Column(nullable = false, unique = true)
    private String requestId;
    private String providerTransactionId;
    private String providerName;
    private String providerPaymentUrl;
    private String providerDeeplink;
    private String providerQrCodeUrl;
    private String bankCode;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountName;
    private String transferContent;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentTransactionStatus status;
    @Column(columnDefinition = "text")
    private String rawProviderRequest;
    @Column(columnDefinition = "text")
    private String rawProviderResponse;
    @Column(columnDefinition = "text")
    private String paymentConfigurationSnapshot;
    @Column(columnDefinition = "text")
    private String qrDisplayData;
    private OffsetDateTime paidAt;
    private OffsetDateTime confirmedAt;
    private UUID confirmedBy;
    @Column(columnDefinition = "text")
    private String failureReason;
    private OffsetDateTime expiredAt;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getWorkspaceRegistrationId() { return workspaceRegistrationId; }
    public void setWorkspaceRegistrationId(UUID workspaceRegistrationId) { this.workspaceRegistrationId = workspaceRegistrationId; }
    public UUID getSubscriptionPlanId() { return subscriptionPlanId; }
    public void setSubscriptionPlanId(UUID subscriptionPlanId) { this.subscriptionPlanId = subscriptionPlanId; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public void setProviderTransactionId(String providerTransactionId) { this.providerTransactionId = providerTransactionId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getProviderPaymentUrl() { return providerPaymentUrl; }
    public void setProviderPaymentUrl(String providerPaymentUrl) { this.providerPaymentUrl = providerPaymentUrl; }
    public String getProviderDeeplink() { return providerDeeplink; }
    public void setProviderDeeplink(String providerDeeplink) { this.providerDeeplink = providerDeeplink; }
    public String getProviderQrCodeUrl() { return providerQrCodeUrl; }
    public void setProviderQrCodeUrl(String providerQrCodeUrl) { this.providerQrCodeUrl = providerQrCodeUrl; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankAccountName() { return bankAccountName; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }
    public String getTransferContent() { return transferContent; }
    public void setTransferContent(String transferContent) { this.transferContent = transferContent; }
    public PaymentTransactionStatus getStatus() { return status; }
    public void setStatus(PaymentTransactionStatus status) { this.status = status; }
    public String getRawProviderRequest() { return rawProviderRequest; }
    public void setRawProviderRequest(String rawProviderRequest) { this.rawProviderRequest = rawProviderRequest; }
    public String getRawProviderResponse() { return rawProviderResponse; }
    public void setRawProviderResponse(String rawProviderResponse) { this.rawProviderResponse = rawProviderResponse; }
    public String getPaymentConfigurationSnapshot() { return paymentConfigurationSnapshot; }
    public void setPaymentConfigurationSnapshot(String paymentConfigurationSnapshot) { this.paymentConfigurationSnapshot = paymentConfigurationSnapshot; }
    public String getQrDisplayData() { return qrDisplayData; }
    public void setQrDisplayData(String qrDisplayData) { this.qrDisplayData = qrDisplayData; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(OffsetDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public UUID getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(UUID confirmedBy) { this.confirmedBy = confirmedBy; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public OffsetDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(OffsetDateTime expiredAt) { this.expiredAt = expiredAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
