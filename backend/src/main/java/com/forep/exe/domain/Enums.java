package com.forep.exe.domain;

public final class Enums {
    private Enums() {
    }

    public enum Role {
        SYSTEM_ADMIN, OWNER, EMPLOYEE
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, INVITED
    }

    public enum SeniorityLevel {
        INTERN, JUNIOR, MIDDLE, SENIOR, LEAD
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
        ASSIGNEE_RECOMMENDATION,
        WORKLOAD_SUMMARY,
        BUSINESS_SUMMARY,
        TASK_EXTRACTION,
        DAILY_REPORT_INSIGHTS,
        TASK_SPLIT,
        TASK_ADJUSTMENT,
        DELAY_RISK,
        MISSING_REPORT,
        ACTION_SUGGESTION
    }

    public enum AiSuggestionStatus {
        GENERATED, ACCEPTED, REJECTED
    }

    public enum WorkspaceStatus {
        PENDING_PAYMENT, ACTIVE, INACTIVE, SUSPENDED, EXPIRED
    }

    public enum SubscriptionPlanStatus {
        ACTIVE, INACTIVE
    }

    public enum PaymentStatus {
        PENDING, CONFIRMED, REJECTED, CORRECTION_REQUESTED
    }

    public enum PaymentMethod {
        MOMO, BANK_TRANSFER
    }

    public enum PaymentTransactionStatus {
        PENDING, SUCCESS, FAILED, EXPIRED, CANCELLED
    }

    public enum RegistrationStatus {
        SUBMITTED,
        PAYMENT_PENDING,
        PAYMENT_SUBMITTED,
        PENDING_PLAN_SELECTION,
        PENDING_PAYMENT,
        PAYMENT_CONFIRMED,
        APPROVED,
        REJECTED,
        CANCELLED
    }

    public enum FeedbackStatus {
        NEW, REVIEWED
    }
}
