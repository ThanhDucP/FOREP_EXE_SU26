package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.Role;
import com.forep.exe.domain.Enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findFirstByEmailIgnoreCase(String email);
    Optional<UserEntity> findFirstByUsernameIgnoreCase(String username);
    Optional<UserEntity> findByWorkspaceIdAndEmailIgnoreCase(UUID workspaceId, String email);
    boolean existsByWorkspaceIdAndEmailIgnoreCase(UUID workspaceId, String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByWorkspaceIdAndDepartmentIdAndStatus(UUID workspaceId, UUID departmentId, UserStatus status);
    boolean existsByWorkspaceIdAndJobPositionIdAndStatus(UUID workspaceId, UUID jobPositionId, UserStatus status);
    List<UserEntity> findByWorkspaceIdAndRoleOrderByFullNameAsc(UUID workspaceId, Role role);
    List<UserEntity> findByWorkspaceIdAndRoleInOrderByFullNameAsc(UUID workspaceId, List<Role> roles);
    List<UserEntity> findByWorkspaceId(UUID workspaceId);
}
