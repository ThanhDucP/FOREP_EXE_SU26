package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from PaymentTransactionEntity payment where payment.id = :id")
    Optional<PaymentTransactionEntity> findByIdForUpdate(UUID id);
    Optional<PaymentTransactionEntity> findByPaymentCode(String paymentCode);
    Optional<PaymentTransactionEntity> findByOrderCode(String orderCode);
    Optional<PaymentTransactionEntity> findByRequestId(String requestId);
    List<PaymentTransactionEntity> findAllByOrderByCreatedAtDesc();
    List<PaymentTransactionEntity> findByStatusInAndExpiredAtBefore(Collection<PaymentTransactionStatus> statuses, OffsetDateTime expiredAt);
    List<PaymentTransactionEntity> findByWorkspaceRegistrationIdOrderByCreatedAtDesc(UUID workspaceRegistrationId);
    boolean existsByWorkspaceRegistrationIdAndStatus(UUID workspaceRegistrationId, PaymentTransactionStatus status);
}
