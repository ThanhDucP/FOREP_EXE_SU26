package com.forep.exe.security;

import com.forep.exe.domain.Enums.Permission;
import com.forep.exe.domain.Enums.Role;
import com.forep.exe.persistence.RolePermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationServiceTest {
    private AuthorizationService service;

    @BeforeEach
    void setUp() {
        RolePermissionRepository repository = mock(RolePermissionRepository.class);
        when(repository.findByRoleAndEnabledTrue(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        service = new AuthorizationService(repository, mock(SecurityContext.class));
    }

    @Test
    void businessOwnerCanManageHrAccountsButNotDailyHrOperations() {
        assertTrue(service.hasPermission(Role.BUSINESS_OWNER, Permission.HR_ACCOUNT_MANAGE));
        assertTrue(service.hasPermission(Role.BUSINESS_OWNER, Permission.EMPLOYEE_VIEW));
        assertTrue(service.hasPermission(Role.BUSINESS_OWNER, Permission.TASK_CREATE));
        assertTrue(service.hasPermission(Role.BUSINESS_OWNER, Permission.TASK_ASSIGN));
        assertTrue(service.hasPermission(Role.BUSINESS_OWNER, Permission.TASK_APPROVE));
        assertTrue(service.hasPermission(Role.BUSINESS_OWNER, Permission.TASK_UPDATE_OWN));
        assertTrue(service.hasPermission(Role.BUSINESS_OWNER, Permission.REPORT_VIEW));
        assertTrue(service.hasPermission(Role.BUSINESS_OWNER, Permission.AI_RECOMMENDATION));
        assertFalse(service.hasPermission(Role.BUSINESS_OWNER, Permission.EMPLOYEE_CREATE));
        assertFalse(service.hasPermission(Role.BUSINESS_OWNER, Permission.DEPARTMENT_MANAGE));
        assertFalse(service.hasPermission(Role.BUSINESS_OWNER, Permission.POSITION_MANAGE));
        assertFalse(service.hasPermission(Role.BUSINESS_OWNER, Permission.AI_ANALYZE));
    }

    @Test
    void hrOwnsPersonnelAdministrationAndImport() {
        assertTrue(service.hasPermission(Role.HR, Permission.EMPLOYEE_CREATE));
        assertTrue(service.hasPermission(Role.HR, Permission.DEPARTMENT_MANAGE));
        assertTrue(service.hasPermission(Role.HR, Permission.POSITION_MANAGE));
        assertTrue(service.hasPermission(Role.HR, Permission.EMPLOYEE_IMPORT));
        assertFalse(service.hasPermission(Role.HR, Permission.PACKAGE_MANAGE));
        assertFalse(service.hasPermission(Role.HR, Permission.PAYMENT_CONFIRM));
        assertFalse(service.hasPermission(Role.HR, Permission.TASK_ASSIGN));
        assertFalse(service.hasPermission(Role.HR, Permission.TASK_APPROVE));
    }
}
