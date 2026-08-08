package com.forep.exe.service;

import com.forep.exe.domain.Enums.Permission;
import com.forep.exe.domain.Enums.Role;
import com.forep.exe.domain.Enums.UserStatus;
import com.forep.exe.persistence.UserEntity;
import com.forep.exe.persistence.UserRepository;
import com.forep.exe.security.AuthorizationService;
import com.forep.exe.security.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class HrAccountManagementService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "@#$%!?";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;

    private final UserRepository users;
    private final AuthorizationService authorization;
    private final SecurityContext securityContext;
    private final PasswordEncoder passwordEncoder;

    public HrAccountManagementService(UserRepository users,
                                      AuthorizationService authorization,
                                      SecurityContext securityContext,
                                      PasswordEncoder passwordEncoder) {
        this.users = users;
        this.authorization = authorization;
        this.securityContext = securityContext;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public HrAccountView account(UUID accountId) {
        authorization.require(Permission.HR_ACCOUNT_MANAGE);
        requireBusinessOwner();
        return view(requireWorkspaceHr(accountId), null, false);
    }

    @Transactional
    public HrAccountView resetPassword(UUID accountId) {
        authorization.require(Permission.HR_ACCOUNT_MANAGE);
        requireBusinessOwner();
        UserEntity hr = requireWorkspaceHr(accountId);
        String temporaryPassword = temporaryPassword();
        hr.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        hr.setMustChangePassword(true);
        hr.setInitialAccountGenerated(true);
        hr.setUpdatedAt(OffsetDateTime.now());
        hr = users.save(hr);
        return view(hr, temporaryPassword, true);
    }

    private UserEntity requireWorkspaceHr(UUID accountId) {
        UUID workspaceId = securityContext.currentUser().workspaceId();
        if (workspaceId == null) throw new IllegalArgumentException("Tài khoản hiện tại không thuộc workspace.");
        return users.findById(accountId)
                .filter(user -> workspaceId.equals(user.getWorkspaceId()) && user.getRole() == Role.HR)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản HR trong workspace này."));
    }

    private void requireBusinessOwner() {
        Role role = securityContext.currentUser().role();
        if (role != Role.BUSINESS_OWNER && role != Role.OWNER) {
            throw new IllegalArgumentException("Chỉ Business Owner mới được quản lý tài khoản HR.");
        }
    }

    private String temporaryPassword() {
        char[] value = new char[14];
        value[0] = random(UPPER);
        value[1] = random(LOWER);
        value[2] = random(DIGITS);
        value[3] = random(SPECIAL);
        for (int index = 4; index < value.length; index++) value[index] = random(ALL);
        for (int index = value.length - 1; index > 0; index--) {
            int swap = RANDOM.nextInt(index + 1);
            char temp = value[index];
            value[index] = value[swap];
            value[swap] = temp;
        }
        return new String(value);
    }

    private char random(String values) {
        return values.charAt(RANDOM.nextInt(values.length()));
    }

    private HrAccountView view(UserEntity user, String temporaryPassword, boolean credentialsVisibleOnce) {
        return new HrAccountView(
                user.getId(), user.getWorkspaceId(), user.getFullName(), user.getEmail(),
                user.getUsername(), user.getPhone(), user.getRole(), user.getStatus(),
                user.isMustChangePassword(), user.isInitialAccountGenerated(),
                temporaryPassword, credentialsVisibleOnce, user.getCreatedAt(), user.getUpdatedAt()
        );
    }

    public record HrAccountView(
            UUID id,
            UUID workspaceId,
            String fullName,
            String email,
            String username,
            String phone,
            Role role,
            UserStatus status,
            boolean mustChangePassword,
            boolean initialAccountGenerated,
            String temporaryPassword,
            boolean credentialsVisibleOnce,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}
}
