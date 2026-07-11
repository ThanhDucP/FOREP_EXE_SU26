package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachmentEntity, UUID> {
    List<TaskAttachmentEntity> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
}
