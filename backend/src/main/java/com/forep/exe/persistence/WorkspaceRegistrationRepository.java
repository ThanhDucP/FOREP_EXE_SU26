package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forep.exe.domain.Enums.RegistrationStatus;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRegistrationRepository extends JpaRepository<WorkspaceRegistrationEntity, UUID> {
    Optional<WorkspaceRegistrationEntity> findByWorkspaceIdentifierIgnoreCase(String workspaceIdentifier);
    Optional<WorkspaceRegistrationEntity> findByRegistrationToken(String registrationToken);
    List<WorkspaceRegistrationEntity> findAllByOrderByCreatedAtDesc();
    List<WorkspaceRegistrationEntity> findByRegistrationStatusInAndExpiredAtBefore(Collection<RegistrationStatus> statuses, OffsetDateTime expiredAt);
}
