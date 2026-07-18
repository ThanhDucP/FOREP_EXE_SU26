package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.WorkspaceSubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceSubscriptionRepository extends JpaRepository<WorkspaceSubscriptionEntity, UUID> {
    List<WorkspaceSubscriptionEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
    Optional<WorkspaceSubscriptionEntity> findFirstByWorkspaceIdAndStatusOrderByCreatedAtDesc(UUID workspaceId, WorkspaceSubscriptionStatus status);
    boolean existsByPaymentTransactionId(UUID paymentTransactionId);
}
