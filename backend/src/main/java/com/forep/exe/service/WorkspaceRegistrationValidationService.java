package com.forep.exe.service;

import com.forep.exe.domain.Enums.RegistrationStatus;
import com.forep.exe.domain.Enums.PaymentStatus;
import com.forep.exe.domain.Enums.Role;
import com.forep.exe.persistence.UserEntity;
import com.forep.exe.persistence.UserRepository;
import com.forep.exe.persistence.WorkspaceRegistrationEntity;
import com.forep.exe.persistence.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Centralised pre-creation validation for workspace registrations.
 * <p>
 * This service is the <em>single</em> location that answers the question
 * "Is this registration in a valid state to create a workspace?".  It is
 * intentionally kept stateless (no mutable fields) so it can be injected
 * as a Spring singleton without concurrency concerns.
 * </p>
 *
 * <h3>Design principles</h3>
 * <ul>
 *   <li>Each rule maps to one {@code errorCode} constant – callers can react
 *       programmatically without string-matching on messages.</li>
 *   <li>Every failure is logged at WARN level <em>before</em> the exception
 *       is thrown so that log aggregation tools capture the context.</li>
 *   <li>No database writes occur here; this service is read-only.</li>
 * </ul>
 */
@Service
public class WorkspaceRegistrationValidationService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceRegistrationValidationService.class);

    // -----------------------------------------------------------------------
    // Error code constants (SCREAMING_SNAKE_CASE for programmatic handling)
    // -----------------------------------------------------------------------
    public static final String ERR_MISSING_OWNER_EMAIL          = "MISSING_OWNER_EMAIL";
    public static final String ERR_MISSING_OWNER_NAME           = "MISSING_OWNER_NAME";
    public static final String ERR_MISSING_OWNER_PASSWORD       = "MISSING_OWNER_PASSWORD";
    public static final String ERR_MISSING_BUSINESS_NAME        = "MISSING_BUSINESS_NAME";
    public static final String ERR_MISSING_WORKSPACE_NAME       = "MISSING_WORKSPACE_NAME";
    public static final String ERR_MISSING_WORKSPACE_IDENTIFIER = "MISSING_WORKSPACE_IDENTIFIER";
    public static final String ERR_MISSING_SUBSCRIPTION_PLAN    = "MISSING_SUBSCRIPTION_PLAN";
    public static final String ERR_INVALID_REGISTRATION_STATUS  = "INVALID_REGISTRATION_STATUS";
    public static final String ERR_INVALID_PAYMENT_STATUS       = "INVALID_PAYMENT_STATUS";
    public static final String ERR_REGISTRATION_EXPIRED         = "REGISTRATION_EXPIRED";
    public static final String ERR_OWNER_EMAIL_ALREADY_EXISTS   = "OWNER_EMAIL_ALREADY_EXISTS";
    public static final String ERR_WORKSPACE_IDENTIFIER_CONFLICT = "WORKSPACE_IDENTIFIER_CONFLICT";

    /**
     * Statuses that are terminal/negative – a registration in one of these
     * states must never proceed to workspace creation.
     */
    private static final Set<RegistrationStatus> TERMINAL_STATUSES = EnumSet.of(
            RegistrationStatus.REJECTED,
            RegistrationStatus.CANCELLED,
            RegistrationStatus.EXPIRED
    );

    /**
     * Statuses that indicate the registration is ready to be converted into an
     * active workspace (public flow after payment).
     */
    private static final Set<RegistrationStatus> ACTIVATABLE_STATUSES = EnumSet.of(
            RegistrationStatus.PAYMENT_CONFIRMED,
            RegistrationStatus.APPROVED,
            RegistrationStatus.ACTIVATED  // idempotent re-try guard
    );

    private final UserRepository users;
    private final WorkspaceRepository workspaces;

    public WorkspaceRegistrationValidationService(UserRepository users,
                                                  WorkspaceRepository workspaces) {
        this.users = users;
        this.workspaces = workspaces;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Validates a {@link WorkspaceRegistrationEntity} before a workspace is
     * created from it.
     *
     * <p>This method is called inside an already-open transaction
     * ({@code ForepService} is {@code @Transactional}), so any exception
     * thrown here will automatically roll back the transaction.</p>
     *
     * <p>The <em>admin-created</em> workspace path (where
     * {@code registration.getWorkspaceId() != null}) uses a slightly different
     * set of rules: payment confirmation is handled separately, so we skip the
     * public-flow payment status check in that case.</p>
     *
     * @param registration the entity to validate
     * @throws WorkspaceValidationException if any rule is violated
     */
    public void validateForActivation(WorkspaceRegistrationEntity registration) {
        String regId = registration.getId() == null ? "<no-id>" : registration.getId().toString();
        RegistrationStatus status = registration.getRegistrationStatus();
        log.debug("Validating registration {} (status={}, paymentStatus={}) for workspace activation",
                regId, status, registration.getPaymentStatus());

        // ── 1. Required string fields ────────────────────────────────────────
        requireNonBlank(regId, registration.getOwnerEmail(),
                ERR_MISSING_OWNER_EMAIL,
                "Registration {} is missing ownerEmail – cannot provision owner account");

        requireNonBlank(regId, registration.getOwnerFullName(),
                ERR_MISSING_OWNER_NAME,
                "Registration {} is missing ownerFullName – cannot provision owner account");

        requireNonBlank(regId, registration.getOwnerPasswordHash(),
                ERR_MISSING_OWNER_PASSWORD,
                "Registration {} is missing ownerPasswordHash – cannot provision owner account");

        requireNonBlank(regId, registration.getBusinessName(),
                ERR_MISSING_BUSINESS_NAME,
                "Registration {} is missing businessName");

        requireNonBlank(regId, registration.getWorkspaceName(),
                ERR_MISSING_WORKSPACE_NAME,
                "Registration {} is missing workspaceName");

        requireNonBlank(regId, registration.getWorkspaceIdentifier(),
                ERR_MISSING_WORKSPACE_IDENTIFIER,
                "Registration {} is missing workspaceIdentifier (shortCode)");

        // ── 2. Subscription plan ─────────────────────────────────────────────
        if (registration.getSubscriptionPlanId() == null) {
            log.warn("Registration {} has no subscriptionPlanId – cannot create workspace", regId);
            throw new WorkspaceValidationException(ERR_MISSING_SUBSCRIPTION_PLAN,
                    "Hồ sơ đăng ký chưa chọn gói subscription.");
        }

        // ── 3. Expiry check ──────────────────────────────────────────────────
        if (registration.getExpiredAt() != null
                && registration.getExpiredAt().isBefore(OffsetDateTime.now())
                && status != RegistrationStatus.APPROVED
                && status != RegistrationStatus.ACTIVATED) {
            log.warn("Registration {} expired at {} – workspace creation blocked", regId, registration.getExpiredAt());
            throw new WorkspaceValidationException(ERR_REGISTRATION_EXPIRED,
                    "Hồ sơ đăng ký đã hết hạn vào " + registration.getExpiredAt()
                            + ". Vui lòng liên hệ quản trị viên.");
        }

        // ── 4. Registration status ───────────────────────────────────────────
        if (status == null || TERMINAL_STATUSES.contains(status)) {
            log.warn("Registration {} has terminal/invalid status {} – workspace creation blocked",
                    regId, status);
            throw new WorkspaceValidationException(ERR_INVALID_REGISTRATION_STATUS,
                    "Hồ sơ đăng ký ở trạng thái " + status + " không thể được kích hoạt.");
        }

        // Admin-direct path: workspaceId is already set; payment is confirmed
        // separately through PayOS server-to-server reconciliation.
        boolean isAdminDirectPath = registration.getWorkspaceId() != null;

        // ── 5. Payment status (public flow only) ─────────────────────────────
        if (!isAdminDirectPath) {
            if (registration.getPaymentStatus() != PaymentStatus.CONFIRMED) {
                log.warn("Registration {} paymentStatus={} is not CONFIRMED – activation blocked",
                        regId, registration.getPaymentStatus());
                throw new WorkspaceValidationException(ERR_INVALID_PAYMENT_STATUS,
                        "Thanh toán của hồ sơ đăng ký chưa được xác nhận (paymentStatus="
                                + registration.getPaymentStatus() + ").");
            }
            if (!ACTIVATABLE_STATUSES.contains(status)) {
                log.warn("Registration {} registrationStatus={} is not in an activatable state",
                        regId, status);
                throw new WorkspaceValidationException(ERR_INVALID_REGISTRATION_STATUS,
                        "Hồ sơ đăng ký ở trạng thái " + status
                                + " chưa sẵn sàng để tạo Workspace (cần PAYMENT_CONFIRMED).");
            }
        }

        // ── 6. Owner email uniqueness / idempotent retry ─────────────────────
        // A provider-confirmed payment can be retried after a partially completed
        // activation. For an admin-direct registration, reusing the existing owner is
        // safe only when that account belongs to THIS workspace and is actually an
        // owner account. Any cross-workspace or non-owner collision remains blocked.
        String ownerEmail = registration.getOwnerEmail().trim().toLowerCase(java.util.Locale.ROOT);
        UserEntity existingOwnerEmailAccount = users.findFirstByEmailIgnoreCase(ownerEmail).orElse(null);
        if (existingOwnerEmailAccount != null) {
            boolean sameWorkspaceOwner = isAdminDirectPath
                    && registration.getWorkspaceId().equals(existingOwnerEmailAccount.getWorkspaceId())
                    && (existingOwnerEmailAccount.getRole() == Role.BUSINESS_OWNER
                        || existingOwnerEmailAccount.getRole() == Role.OWNER);
            if (!sameWorkspaceOwner) {
                log.warn("Registration {} ownerEmail='{}' already belongs to another/non-owner account – activation blocked",
                        regId, ownerEmail);
                throw new WorkspaceValidationException(ERR_OWNER_EMAIL_ALREADY_EXISTS,
                        "Email '" + ownerEmail + "' đã được đăng ký bởi tài khoản khác trong hệ thống.");
            }
            log.info("Registration {} reuses existing owner account {} in workspace {} during idempotent activation retry",
                    regId, existingOwnerEmailAccount.getId(), registration.getWorkspaceId());
        }

        // ── 7. Workspace identifier conflict (re-check to guard race conditions) ──
        if (!isAdminDirectPath) {
            String identifier = registration.getWorkspaceIdentifier();
            if (workspaces.findByShortCodeIgnoreCase(identifier).isPresent()) {
                log.warn("Registration {} workspaceIdentifier='{}' already used by an existing workspace – activation blocked",
                        regId, identifier);
                throw new WorkspaceValidationException(ERR_WORKSPACE_IDENTIFIER_CONFLICT,
                        "Mã định danh workspace '" + identifier + "' đã được sử dụng bởi workspace khác.");
            }
        }

        log.info("Registration {} passed all pre-activation validation checks", regId);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void requireNonBlank(String regId, String value, String errorCode, String logTemplate) {
        if (value == null || value.isBlank()) {
            log.warn(logTemplate, regId);
            // Derive a user-friendly message from the error code
            String friendlyMessage = friendlyMessage(errorCode);
            throw new WorkspaceValidationException(errorCode, friendlyMessage);
        }
    }

    private static String friendlyMessage(String errorCode) {
        return switch (errorCode) {
            case ERR_MISSING_OWNER_EMAIL          -> "Thiếu email của chủ sở hữu Workspace.";
            case ERR_MISSING_OWNER_NAME           -> "Thiếu họ tên của chủ sở hữu Workspace.";
            case ERR_MISSING_OWNER_PASSWORD       -> "Thiếu thông tin mật khẩu của chủ sở hữu Workspace.";
            case ERR_MISSING_BUSINESS_NAME        -> "Thiếu tên doanh nghiệp trong hồ sơ đăng ký.";
            case ERR_MISSING_WORKSPACE_NAME       -> "Thiếu tên Workspace trong hồ sơ đăng ký.";
            case ERR_MISSING_WORKSPACE_IDENTIFIER -> "Thiếu mã định danh (shortCode) của Workspace.";
            default -> "Hồ sơ đăng ký thiếu thông tin bắt buộc (code=" + errorCode + ").";
        };
    }
}
