package com.forep.exe.domain;

public final class Enums {
    private Enums() {
    }

    public enum Role {
        OWNER, EMPLOYEE
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, INVITED
    }

    public enum TaskPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum TaskStatus {
        ASSIGNED, IN_PROGRESS, BLOCKED, COMPLETED, CANCELLED
    }

    public enum UpdateType {
        PROGRESS, BLOCKER, COMPLETION
    }

    public enum WorkloadLevel {
        NO_WORK, LOW, NORMAL, HIGH, OVERLOADED
    }

    public enum AiSuggestionType {
        ASSIGNEE_RECOMMENDATION, WORKLOAD_SUMMARY, BUSINESS_SUMMARY, TASK_EXTRACTION
    }

    public enum AiSuggestionStatus {
        GENERATED, ACCEPTED, REJECTED
    }
}
