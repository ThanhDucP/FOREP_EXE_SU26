package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskUpdateRepository extends JpaRepository<TaskUpdateEntity, UUID> {
    List<TaskUpdateEntity> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
}
