package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select workspace from WorkspaceEntity workspace where workspace.id = :id")
    Optional<WorkspaceEntity> findByIdForUpdate(UUID id);
    Optional<WorkspaceEntity> findByShortCodeIgnoreCase(String shortCode);
    List<WorkspaceEntity> findAllByOrderByCreatedAtDesc();
}
