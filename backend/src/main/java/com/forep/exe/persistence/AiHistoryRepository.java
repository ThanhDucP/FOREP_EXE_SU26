package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiHistoryRepository extends JpaRepository<AiHistoryEntity, UUID> {
    List<AiHistoryEntity> findByWorkspaceIdOrderByCalledAtDesc(UUID workspaceId);
}