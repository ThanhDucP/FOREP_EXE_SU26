package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.DepartmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {
    List<DepartmentEntity> findByWorkspaceIdOrderByNameAsc(UUID workspaceId);
    List<DepartmentEntity> findByWorkspaceIdAndStatusOrderByNameAsc(UUID workspaceId, DepartmentStatus status);
    Optional<DepartmentEntity> findByWorkspaceIdAndNameIgnoreCase(UUID workspaceId, String name);
    Optional<DepartmentEntity> findByWorkspaceIdAndCodeIgnoreCase(UUID workspaceId, String code);
    boolean existsByWorkspaceIdAndNameIgnoreCase(UUID workspaceId, String name);
    boolean existsByWorkspaceIdAndCodeIgnoreCase(UUID workspaceId, String code);
}
