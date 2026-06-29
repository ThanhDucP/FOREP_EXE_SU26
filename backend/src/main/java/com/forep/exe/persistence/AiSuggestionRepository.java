package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestionEntity, UUID> {
    List<AiSuggestionEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
