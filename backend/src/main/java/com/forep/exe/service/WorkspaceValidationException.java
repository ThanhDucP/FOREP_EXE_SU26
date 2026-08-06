package com.forep.exe.service;

/**
 * Thrown when a workspace registration fails pre-creation validation.
 * <p>
 * Each failure carries a machine-readable {@code errorCode} so that controllers
 * can map it to an appropriate HTTP status and a localised message, while the
 * {@code message} field describes the root cause for logging and debugging.
 * </p>
 */
public class WorkspaceValidationException extends RuntimeException {

    private final String errorCode;

    public WorkspaceValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * A short, SCREAMING_SNAKE_CASE identifier for the validation rule that was
     * violated.  Examples:
     * <ul>
     *   <li>{@code MISSING_OWNER_EMAIL}</li>
     *   <li>{@code OWNER_EMAIL_ALREADY_EXISTS}</li>
     *   <li>{@code WORKSPACE_IDENTIFIER_CONFLICT}</li>
     *   <li>{@code INVALID_REGISTRATION_STATUS}</li>
     *   <li>{@code REGISTRATION_EXPIRED}</li>
     *   <li>{@code MISSING_SUBSCRIPTION_PLAN}</li>
     * </ul>
     */
    public String getErrorCode() {
        return errorCode;
    }
}
