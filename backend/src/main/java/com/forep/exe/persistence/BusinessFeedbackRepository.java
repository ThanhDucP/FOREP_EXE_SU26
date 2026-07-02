package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessFeedbackRepository extends JpaRepository<BusinessFeedbackEntity, UUID> {
    List<BusinessFeedbackEntity> findAllByOrderByCreatedAtDesc();
    List<BusinessFeedbackEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
