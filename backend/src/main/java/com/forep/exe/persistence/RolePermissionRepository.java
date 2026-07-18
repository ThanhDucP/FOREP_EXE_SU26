package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {
    List<RolePermissionEntity> findByRoleAndEnabledTrue(Role role);
}
