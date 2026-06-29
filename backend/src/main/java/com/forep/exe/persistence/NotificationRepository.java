package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
    List<NotificationEntity> findByWorkspaceIdAndUserIdOrderByCreatedAtDesc(UUID workspaceId, UUID userId);
    boolean existsByWorkspaceIdAndUserIdAndTypeAndRelatedEntityId(UUID workspaceId, UUID userId, String type, UUID relatedEntityId);
}
