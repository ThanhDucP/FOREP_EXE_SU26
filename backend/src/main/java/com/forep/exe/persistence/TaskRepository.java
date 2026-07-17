package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
    List<TaskEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
    List<TaskEntity> findByWorkspaceIdAndAssigneeIdOrderByCreatedAtDesc(UUID workspaceId, UUID assigneeId);
    boolean existsByWorkspaceIdAndDepartmentIdAndStatusIn(UUID workspaceId, UUID departmentId, List<TaskStatus> statuses);
    boolean existsByWorkspaceIdAndRequiredJobPositionIdAndStatusIn(UUID workspaceId, UUID requiredJobPositionId, List<TaskStatus> statuses);
}
