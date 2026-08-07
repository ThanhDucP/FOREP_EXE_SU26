package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.PaymentMethod;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    Optional<PaymentTransactionEntity> findByProviderTransactionId(String providerTransactionId);
    Optional<PaymentTransactionEntity> findByPaymentLinkId(String paymentLinkId);
    List<PaymentTransactionEntity> findAllByOrderByCreatedAtDesc();
    List<PaymentTransactionEntity> findByStatusInAndExpiredAtBefore(Collection<PaymentTransactionStatus> statuses, OffsetDateTime expiredAt);
    List<PaymentTransactionEntity> findByWorkspaceRegistrationIdOrderByCreatedAtDesc(UUID workspaceRegistrationId);
    boolean existsByWorkspaceRegistrationIdAndStatus(UUID workspaceRegistrationId, PaymentTransactionStatus status);

    List<PaymentTransactionEntity> findByPaymentMethodAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
            PaymentMethod paymentMethod,
            Collection<PaymentTransactionStatus> statuses,
            OffsetDateTime createdAt,
            Pageable pageable);

    @Query("""
            select payment
            from PaymentTransactionEntity payment
            where payment.paymentMethod = :paymentMethod
              and payment.status in :statuses
              and exists (
                  select registration.id
                  from WorkspaceRegistrationEntity registration
                  where registration.id = payment.workspaceRegistrationId
                    and registration.workspaceId is null
              )
            order by payment.createdAt desc
            """)
    List<PaymentTransactionEntity> findIncompleteActivations(
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("statuses") Collection<PaymentTransactionStatus> statuses,
            Pageable pageable);
}
