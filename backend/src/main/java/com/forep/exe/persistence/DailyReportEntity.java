package com.forep.exe.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_reports")
public class DailyReportEntity extends BaseEntity {
    @Column(nullable = false)
    private UUID workspaceId;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private LocalDate reportDate;
    @Column(nullable = false, columnDefinition = "text")
    private String todayCompleted;
    @Column(nullable = false, columnDefinition = "text")
    private String currentWork;
    @Column(columnDefinition = "text")
    private String blockers;
    @Column(columnDefinition = "text")
    private String tomorrowPlan;
    private OffsetDateTime reviewedAt;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public String getTodayCompleted() { return todayCompleted; }
    public void setTodayCompleted(String todayCompleted) { this.todayCompleted = todayCompleted; }
    public String getCurrentWork() { return currentWork; }
    public void setCurrentWork(String currentWork) { this.currentWork = currentWork; }
    public String getBlockers() { return blockers; }
    public void setBlockers(String blockers) { this.blockers = blockers; }
    public String getTomorrowPlan() { return tomorrowPlan; }
    public void setTomorrowPlan(String tomorrowPlan) { this.tomorrowPlan = tomorrowPlan; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
