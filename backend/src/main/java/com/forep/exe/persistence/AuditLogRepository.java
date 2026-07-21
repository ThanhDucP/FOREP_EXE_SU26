package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID>, JpaSpecificationExecutor<AuditLogEntity> {
    List<AuditLogEntity> findAllByOrderByCreatedAtDesc();
    List<AuditLogEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
