package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.JobPositionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobPositionRepository extends JpaRepository<JobPositionEntity, UUID> {
    List<JobPositionEntity> findByWorkspaceIdOrderByTitleAsc(UUID workspaceId);
    boolean existsByWorkspaceIdAndTitleIgnoreCase(UUID workspaceId, String title);
    boolean existsByWorkspaceIdAndTitleIgnoreCaseAndDepartmentId(UUID workspaceId, String title, UUID departmentId);
    boolean existsByWorkspaceIdAndCodeIgnoreCase(UUID workspaceId, String code);
    boolean existsByWorkspaceIdAndDepartmentIdAndStatus(UUID workspaceId, UUID departmentId, JobPositionStatus status);
}
