package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EmployeeImportRowRepository extends JpaRepository<EmployeeImportRowEntity, UUID> {
    List<EmployeeImportRowEntity> findByBatchIdOrderByRowNumberAsc(UUID batchId);
}
