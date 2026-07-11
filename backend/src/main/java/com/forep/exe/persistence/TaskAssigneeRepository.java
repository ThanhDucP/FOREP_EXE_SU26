package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssigneeEntity, UUID> {
    List<TaskAssigneeEntity> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
    List<TaskAssigneeEntity> findByWorkspaceIdAndEmployeeId(UUID workspaceId, UUID employeeId);
    void deleteByTaskId(UUID taskId);
}
