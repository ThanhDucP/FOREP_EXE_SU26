package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, UUID> {
    Optional<PaymentTransactionEntity> findByOrderCode(String orderCode);
    Optional<PaymentTransactionEntity> findByRequestId(String requestId);
    List<PaymentTransactionEntity> findByWorkspaceRegistrationIdOrderByCreatedAtDesc(UUID workspaceRegistrationId);
    boolean existsByWorkspaceRegistrationIdAndStatus(UUID workspaceRegistrationId, PaymentTransactionStatus status);
}
