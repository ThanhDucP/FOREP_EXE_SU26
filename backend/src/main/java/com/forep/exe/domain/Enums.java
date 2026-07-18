package com.forep.exe.domain;

public final class Enums {
    private Enums() {
    }

    public enum Role {
        PLATFORM_ADMIN, BUSINESS_OWNER, HR, EXECUTIVE, MANAGER, EMPLOYEE, SYSTEM,
        SYSTEM_ADMIN, OWNER
    }

    public enum Permission {
        PACKAGE_VIEW,
        PACKAGE_MANAGE,
        WORKSPACE_REGISTER,
        WORKSPACE_VIEW,
        WORKSPACE_UPDATE,
        WORKSPACE_MANAGE,
        PAYMENT_CREATE,
        PAYMENT_CONFIRM,
        PAYMENT_STATUS_VIEW,
        PAYMENT_HISTORY_VIEW,
        PAYMENT_QR_MANAGE,
        SUBSCRIPTION_VIEW,
        SUBSCRIPTION_RENEW,
        SUBSCRIPTION_UPGRADE,
        EMPLOYEE_VIEW,
        EMPLOYEE_CREATE,
        EMPLOYEE_UPDATE,
        EMPLOYEE_DEACTIVATE,
        DEPARTMENT_VIEW,
        DEPARTMENT_MANAGE,
        POSITION_VIEW,
        POSITION_MANAGE,
        ROLE_MANAGE,
        PROJECT_CREATE,
        PROJECT_UPDATE,
        TASK_VIEW,
        TASK_CREATE,
        TASK_ASSIGN,
        TASK_APPROVE,
        TASK_UPDATE_OWN,
        AI_ANALYZE,
        AI_RECOMMENDATION,
        AI_SUMMARY,
        AI_HISTORY,
        REPORT_VIEW,
        REPORT_SUBMIT,
        REPORT_REVIEW,
        REPORT_EXPORT,
        AUDIT_LOG_VIEW,
        SYSTEM_CONFIGURATION,
        REVENUE_VIEW,
        FEEDBACK_CREATE,
        FEEDBACK_MANAGE,
        NOTIFICATION_VIEW
    }

    public enum PermissionGroup {
        EMPLOYEE, MANAGER, EXECUTIVE
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
        ASSIGNED, ACCEPTED, IN_PROGRESS, BLOCKED, SUBMITTED, RETURNED, COMPLETED, CANCELLED
    }

    public enum UpdateType {
        ACCEPTANCE, PROGRESS, BLOCKER, COMPLETION, COMPLETION_APPROVAL, RETURN
    }

    public enum WorkloadLevel {
        NO_WORK, LOW, NORMAL, HIGH, OVERLOADED
    }

    public enum AssignmentType {
        INDIVIDUAL, TEAM
    }

    public enum TaskParticipantRole {
        ASSIGNEE, LEADER, MEMBER
    }

    public enum AttachmentType {
        REQUIREMENT, REFERENCE, RESULT, OTHER
    }

    public enum EmploymentType {
        FULL_TIME, PART_TIME, CONTRACTOR, INTERN
    }

    public enum WorkingStatus {
        WORKING, ON_LEAVE, RESIGNED
    }

    public enum EmployeeLevel {
        INTERN, FRESHER, JUNIOR, MIDDLE, SENIOR, LEAD, MANAGER
    }

    public enum JobPositionStatus {
        ACTIVE, INACTIVE
    }

    public enum DepartmentStatus {
        ACTIVE, INACTIVE
    }

    public enum AiSuggestionType {
        ASSIGNEE_RECOMMENDATION,
        WORKLOAD_SUMMARY,
        BUSINESS_SUMMARY,
        TASK_EXTRACTION,
        DAILY_REPORT_INSIGHTS,
        TASK_SPLIT,
        TASK_ADJUSTMENT,
        TASK_ESTIMATE_HOURS,
        RECOMMENDATION_EXPLANATION,
        RECOMMENDATION_RESULT_EXPLANATION,
        WORKLOAD_RISK,
        EMPLOYEE_REPORT,
        OWNER_OPERATIONAL_SUMMARY,
        PLATFORM_SYSTEM_SUMMARY,
        DELAY_RISK,
        MISSING_REPORT,
        ACTION_SUGGESTION
    }

    public enum AiSuggestionStatus {
        GENERATED, ACCEPTED, REJECTED
    }

    public enum AiHistoryStatus {
        SUCCESS, FAILED, PROCESSING, CANCELLED
    }

    public enum WorkspaceStatus {
        PENDING_PAYMENT, ACTIVE, INACTIVE, SUSPENDED, EXPIRED
    }

    public enum SubscriptionPlanStatus {
        ACTIVE, INACTIVE
    }

    public enum WorkspaceSubscriptionStatus {
        ACTIVE, EXPIRED, CANCELLED, PENDING_RENEWAL, UPGRADED, DOWNGRADED
    }

    public enum PaymentStatus {
        PENDING, CONFIRMED, REJECTED, CORRECTION_REQUESTED
    }

    public enum PaymentMethod {
        MOMO, BANK_TRANSFER
    }

    public enum PaymentTransactionStatus {
        PENDING, PROCESSING, SUCCESS, FAILED, EXPIRED, CANCELLED, REFUNDED, MANUAL_REVIEW
    }

    public enum RegistrationStatus {
        SUBMITTED,
        DRAFT,
        PAYMENT_PENDING,
        PAYMENT_SUBMITTED,
        PENDING_PLAN_SELECTION,
        PENDING_PAYMENT,
        PAYMENT_CONFIRMED,
        APPROVED,
        ACTIVATED,
        REJECTED,
        CANCELLED,
        EXPIRED
    }

    public enum FeedbackStatus {
        NEW, REVIEWED
    }
}
