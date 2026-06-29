package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyReportRepository extends JpaRepository<DailyReportEntity, UUID> {
    List<DailyReportEntity> findByWorkspaceIdOrderByReportDateDesc(UUID workspaceId);
    List<DailyReportEntity> findByWorkspaceIdAndUserIdOrderByReportDateDesc(UUID workspaceId, UUID userId);
    Optional<DailyReportEntity> findByWorkspaceIdAndUserIdAndReportDate(UUID workspaceId, UUID userId, LocalDate reportDate);
}
