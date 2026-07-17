package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.JobPositionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobPositionRepository extends JpaRepository<JobPositionEntity, UUID> {
    List<JobPositionEntity> findByWorkspaceIdOrderByNameAsc(UUID workspaceId);
    boolean existsByWorkspaceIdAndNameIgnoreCase(UUID workspaceId, String name);
    boolean existsByWorkspaceIdAndNameIgnoreCaseAndDepartmentId(UUID workspaceId, String name, UUID departmentId);
    boolean existsByWorkspaceIdAndCodeIgnoreCase(UUID workspaceId, String code);
    boolean existsByWorkspaceIdAndDepartmentIdAndStatus(UUID workspaceId, UUID departmentId, JobPositionStatus status);
}
