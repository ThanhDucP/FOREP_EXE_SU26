package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findFirstByEmailIgnoreCase(String email);
    Optional<UserEntity> findByWorkspaceIdAndEmailIgnoreCase(UUID workspaceId, String email);
    boolean existsByWorkspaceIdAndEmailIgnoreCase(UUID workspaceId, String email);
    List<UserEntity> findByWorkspaceIdAndRoleOrderByFullNameAsc(UUID workspaceId, Role role);
    List<UserEntity> findByWorkspaceId(UUID workspaceId);
}
