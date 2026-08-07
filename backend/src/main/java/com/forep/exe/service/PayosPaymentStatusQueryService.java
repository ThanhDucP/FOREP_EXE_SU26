package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentStatus;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.domain.Enums.RegistrationStatus;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.WorkspaceRegistrationEntity;
import com.forep.exe.persistence.WorkspaceRegistrationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read model for the public PayOS return/status endpoint.
 *
 * This intentionally does not delegate to the legacy ForepService.payosPaymentStatus()
 * implementation because that method still contains webhook-era wording and collapses
 * several provider states into PENDING. Payment truth comes from the local transaction
 * after PayosPaymentReconciliationService has queried PayOS server-to-server.
 */
@Service
public class PayosPaymentStatusQueryService {
    private final PaymentTransactionRepository paymentTransactions;
    private final WorkspaceRegistrationRepository workspaceRegistrations;

    public PayosPaymentStatusQueryService(
            PaymentTransactionRepository paymentTransactions,
            WorkspaceRegistrationRepository workspaceRegistrations) {
        this.paymentTransactions = paymentTransactions;
        this.workspaceRegistrations = workspaceRegistrations;
    }

    public PayosPaymentStatusView statusByOrderCode(String orderCode) {
        PaymentTransactionEntity payment = paymentTransactions.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found."));
        WorkspaceRegistrationEntity registration = workspaceRegistrations
                .findById(payment.getWorkspaceRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace registration not found."));

        String status = apiStatus(payment.getStatus());
        String message = statusMessage(status, registration.getWorkspaceId());
        String ownerEmail = hasText(registration.getOwnerEmail())
                ? registration.getOwnerEmail()
                : registration.getRepresentativeEmail();

        return new PayosPaymentStatusView(
                payment.getOrderCode(),
                status,
                payment.getAmount(),
                message,
                payment.getPaymentCode(),
                payment.getStatus(),
                registration.getId(),
                registration.getRegistrationStatus(),
                registration.getPaymentStatus(),
                registration.getWorkspaceId(),
                ownerEmail,
                payment.getPaidAt()
        );
    }

    private String apiStatus(PaymentTransactionStatus status) {
        if (status == null) return "PENDING";
        return switch (status) {
            case PAID, SUCCESS -> "COMPLETED";
            case PROCESSING -> "PROCESSING";
            case FAILED -> "FAILED";
            case EXPIRED -> "EXPIRED";
            case CANCELLED -> "CANCELLED";
            case REFUNDED -> "REFUNDED";
            case MANUAL_REVIEW -> "MANUAL_REVIEW";
            case PENDING -> "PENDING";
        };
    }

    private String statusMessage(String status, UUID workspaceId) {
        return switch (status) {
            case "COMPLETED" -> workspaceId == null
                    ? "PayOS đã xác nhận thanh toán. FOREP đang kích hoạt workspace và tạo tài khoản chủ workspace."
                    : "Thanh toán thành công. Workspace đã được kích hoạt và thông tin đăng nhập đã được gửi qua email.";
            case "PROCESSING" -> "PayOS đang xử lý giao dịch. FOREP sẽ tự đồng bộ khi giao dịch hoàn tất.";
            case "FAILED" -> "Thanh toán không thành công. Bạn có thể tạo lại giao dịch.";
            case "EXPIRED" -> "Giao dịch PayOS đã hết hạn. Vui lòng tạo giao dịch mới.";
            case "CANCELLED" -> "Giao dịch PayOS đã bị hủy. Workspace chưa được kích hoạt.";
            case "REFUNDED" -> "Giao dịch đã được hoàn tiền.";
            case "MANUAL_REVIEW" -> "Giao dịch cần được kiểm tra trước khi tiếp tục.";
            default -> "FOREP đang kiểm tra trạng thái giao dịch trực tiếp với PayOS.";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record PayosPaymentStatusView(
            String orderCode,
            String status,
            BigDecimal amount,
            String message,
            String paymentCode,
            PaymentTransactionStatus paymentStatus,
            UUID workspaceRegistrationId,
            RegistrationStatus registrationStatus,
            PaymentStatus registrationPaymentStatus,
            UUID workspaceId,
            String ownerEmail,
            OffsetDateTime paidAt
    ) {}
}
