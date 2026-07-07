package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRegistrationRepository extends JpaRepository<WorkspaceRegistrationEntity, UUID> {
    Optional<WorkspaceRegistrationEntity> findByWorkspaceIdentifierIgnoreCase(String workspaceIdentifier);
    Optional<WorkspaceRegistrationEntity> findByRegistrationToken(String registrationToken);
    List<WorkspaceRegistrationEntity> findAllByOrderByCreatedAtDesc();
}
