package com.forep.exe.security;

import com.forep.exe.domain.Enums.Permission;
import com.forep.exe.domain.Enums.Role;
import com.forep.exe.persistence.RolePermissionRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuthorizationService {
    private static final Map<Role, Set<Permission>> DEFAULT_PERMISSIONS = defaultPermissions();

    private final RolePermissionRepository rolePermissions;
    private final SecurityContext securityContext;

    public AuthorizationService(RolePermissionRepository rolePermissions, SecurityContext securityContext) {
        this.rolePermissions = rolePermissions;
        this.securityContext = securityContext;
    }

    public static String authority(Permission permission) {
        return "PERMISSION_" + permission.name();
    }

    public Set<Permission> permissionsFor(Role role) {
        Role normalizedRole = normalize(role);
        List<Permission> databasePermissions = rolePermissions.findByRoleAndEnabledTrue(normalizedRole).stream()
                .map(RolePermissionEntity -> RolePermissionEntity.getPermission())
                .toList();
        if (!databasePermissions.isEmpty()) {
            return EnumSet.copyOf(databasePermissions);
        }
        return DEFAULT_PERMISSIONS.getOrDefault(normalizedRole, Set.of());
    }

    public List<String> permissionNamesFor(Role role) {
        return permissionsFor(role).stream().map(Enum::name).sorted().toList();
    }

    public boolean hasPermission(Role role, Permission permission) {
        return permissionsFor(role).contains(permission);
    }

    public void require(Permission permission) {
        if (!hasPermission(securityContext.currentUser().role(), permission)) {
            throw new IllegalArgumentException("Bạn không có quyền " + permission.name() + ".");
        }
    }

    public void requireAny(Permission firstPermission, Permission... otherPermissions) {
        Role role = securityContext.currentUser().role();
        if (hasPermission(role, firstPermission)) {
            return;
        }
        for (Permission permission : otherPermissions) {
            if (hasPermission(role, permission)) {
                return;
            }
        }
        throw new IllegalArgumentException("Bạn không có quyền thực hiện chức năng này.");
    }

    private static Role normalize(Role role) {
        if (role == Role.SYSTEM_ADMIN) {
            return Role.PLATFORM_ADMIN;
        }
        if (role == Role.OWNER) {
            return Role.BUSINESS_OWNER;
        }
        return role;
    }

    private static Map<Role, Set<Permission>> defaultPermissions() {
        EnumMap<Role, Set<Permission>> permissions = new EnumMap<>(Role.class);
        permissions.put(Role.PLATFORM_ADMIN, EnumSet.of(
                Permission.PACKAGE_VIEW,
                Permission.PACKAGE_MANAGE,
                Permission.WORKSPACE_VIEW,
                Permission.WORKSPACE_MANAGE,
                Permission.PAYMENT_CONFIRM,
                Permission.PAYMENT_STATUS_VIEW,
                Permission.PAYMENT_HISTORY_VIEW,
                Permission.PAYMENT_QR_MANAGE,
                Permission.SUBSCRIPTION_VIEW,
                Permission.AI_SUMMARY,
                Permission.AI_HISTORY,
                Permission.AUDIT_LOG_VIEW,
                Permission.SYSTEM_CONFIGURATION,
                Permission.REVENUE_VIEW,
                Permission.FEEDBACK_MANAGE
        ));
        permissions.put(Role.BUSINESS_OWNER, EnumSet.of(
                Permission.WORKSPACE_VIEW,
                Permission.WORKSPACE_UPDATE,
                Permission.PAYMENT_STATUS_VIEW,
                Permission.PAYMENT_HISTORY_VIEW,
                Permission.SUBSCRIPTION_VIEW,
                Permission.SUBSCRIPTION_RENEW,
                Permission.SUBSCRIPTION_UPGRADE,
                Permission.EMPLOYEE_VIEW,
                Permission.DEPARTMENT_VIEW,
                Permission.POSITION_VIEW,
                Permission.HR_ACCOUNT_MANAGE,
                Permission.TASK_VIEW,
                Permission.TASK_CREATE,
                Permission.TASK_ASSIGN,
                Permission.TASK_APPROVE,
                Permission.TASK_UPDATE_OWN,
                Permission.AI_RECOMMENDATION,
                Permission.AI_SUMMARY,
                Permission.AI_HISTORY,
                Permission.REPORT_VIEW,
                Permission.REPORT_REVIEW,
                Permission.REPORT_EXPORT,
                Permission.FEEDBACK_CREATE,
                Permission.NOTIFICATION_VIEW
        ));
        permissions.put(Role.HR, EnumSet.of(
                Permission.WORKSPACE_VIEW,
                Permission.EMPLOYEE_VIEW,
                Permission.EMPLOYEE_CREATE,
                Permission.EMPLOYEE_UPDATE,
                Permission.EMPLOYEE_DEACTIVATE,
                Permission.DEPARTMENT_VIEW,
                Permission.DEPARTMENT_MANAGE,
                Permission.POSITION_VIEW,
                Permission.POSITION_MANAGE,
                Permission.ROLE_MANAGE,
                Permission.EMPLOYEE_IMPORT,
                Permission.REPORT_VIEW,
                Permission.REPORT_REVIEW,
                Permission.REPORT_EXPORT,
                Permission.FEEDBACK_CREATE,
                Permission.NOTIFICATION_VIEW
        ));
        permissions.put(Role.MANAGER, EnumSet.of(
                Permission.WORKSPACE_VIEW,
                Permission.EMPLOYEE_VIEW,
                Permission.DEPARTMENT_VIEW,
                Permission.POSITION_VIEW,
                Permission.PROJECT_CREATE,
                Permission.PROJECT_UPDATE,
                Permission.TASK_VIEW,
                Permission.TASK_CREATE,
                Permission.TASK_ASSIGN,
                Permission.TASK_APPROVE,
                Permission.TASK_UPDATE_OWN,
                Permission.AI_ANALYZE,
                Permission.AI_RECOMMENDATION,
                Permission.AI_HISTORY,
                Permission.REPORT_VIEW,
                Permission.REPORT_REVIEW,
                Permission.FEEDBACK_CREATE,
                Permission.NOTIFICATION_VIEW
        ));
        permissions.put(Role.EXECUTIVE, EnumSet.of(
                Permission.WORKSPACE_VIEW,
                Permission.EMPLOYEE_VIEW,
                Permission.DEPARTMENT_VIEW,
                Permission.POSITION_VIEW,
                Permission.TASK_VIEW,
                Permission.AI_ANALYZE,
                Permission.AI_SUMMARY,
                Permission.AI_HISTORY,
                Permission.REPORT_VIEW,
                Permission.REPORT_EXPORT,
                Permission.FEEDBACK_CREATE,
                Permission.NOTIFICATION_VIEW
        ));
        permissions.put(Role.EMPLOYEE, EnumSet.of(
                Permission.WORKSPACE_VIEW,
                Permission.TASK_VIEW,
                Permission.TASK_UPDATE_OWN,
                Permission.REPORT_VIEW,
                Permission.REPORT_SUBMIT,
                Permission.FEEDBACK_CREATE,
                Permission.NOTIFICATION_VIEW
        ));
        permissions.put(Role.SYSTEM, EnumSet.of(
                Permission.SYSTEM_CONFIGURATION
        ));
        return permissions;
    }
}
