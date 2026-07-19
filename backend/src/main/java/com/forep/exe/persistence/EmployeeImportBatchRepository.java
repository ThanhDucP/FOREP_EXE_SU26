package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeImportBatchRepository extends JpaRepository<EmployeeImportBatchEntity, UUID> {
    List<EmployeeImportBatchEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
    Optional<EmployeeImportBatchEntity> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
