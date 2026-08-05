package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentMethod;
import com.forep.exe.domain.Enums.PaymentStatus;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.domain.Enums.Permission;
import com.forep.exe.domain.Enums.RegistrationStatus;
import com.forep.exe.domain.Enums.Role;
import com.forep.exe.domain.Enums.SubscriptionPlanStatus;
import com.forep.exe.domain.Enums.UserStatus;
import com.forep.exe.domain.Enums.WorkspaceStatus;
import com.forep.exe.dto.Requests.CreateHrAccountRequest;
import com.forep.exe.dto.Requests.ConfirmMomoPaymentRequest;
import com.forep.exe.persistence.AuditLogRepository;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.RolePermissionEntity;
import com.forep.exe.persistence.RolePermissionRepository;
import com.forep.exe.persistence.SubscriptionPlanEntity;
import com.forep.exe.persistence.SubscriptionPlanRepository;
import com.forep.exe.persistence.UserEntity;
import com.forep.exe.persistence.UserRepository;
import com.forep.exe.persistence.WorkspaceEntity;
import com.forep.exe.persistence.WorkspaceRegistrationEntity;
import com.forep.exe.persistence.WorkspaceRegistrationRepository;
import com.forep.exe.persistence.WorkspaceRepository;
import com.forep.exe.persistence.WorkspaceSubscriptionRepository;
import com.forep.exe.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:workspace-account-flow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false",
        "forep.ai.service-url="
})
class WorkspaceAccountFlowIntegrationTest {
    @Autowired private ForepService service;
    @Autowired private WorkspaceRepository workspaces;
    @Autowired private WorkspaceRegistrationRepository registrations;
    @Autowired private WorkspaceSubscriptionRepository workspaceSubscriptions;
    @Autowired private SubscriptionPlanRepository plans;
    @Autowired private PaymentTransactionRepository payments;
    @Autowired private UserRepository users;
    @Autowired private RolePermissionRepository rolePermissions;
    @Autowired private AuditLogRepository auditLogs;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabaseAndSeedPermissions() {
        SecurityContextHolder.clearContext();
        auditLogs.deleteAll();
        workspaceSubscriptions.deleteAll();
        payments.deleteAll();
        registrations.deleteAll();
        users.deleteAll();
        workspaces.deleteAll();
        plans.deleteAll();
        rolePermissions.deleteAll();
        seed(Role.PLATFORM_ADMIN, Permission.SYSTEM_CONFIGURATION, Permission.WORKSPACE_MANAGE,
                Permission.PAYMENT_CONFIRM);
        seedBusinessOwnerPermissions();
        seedHrPermissions();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activationCreatesExactlyOneOwnerAndIsSequentiallyIdempotent() {
        SubscriptionPlanEntity plan = plan(5);
        WorkspaceRegistrationEntity registration = registration(plan, "FO", "owner@forep.vn", "OwnerPass!2026");
        successfulPayment(registration, plan);
        authenticate(Role.PLATFORM_ADMIN, null);

        var first = service.approveWorkspaceRegistration(registration.getId(), null);
        var second = service.approveWorkspaceRegistration(registration.getId(), null);

        assertNotNull(first.workspaceId());
        assertEquals(first.workspaceId(), second.workspaceId());
        assertEquals(1, first.generatedOwnerAccounts().size());
        assertTrue(second.generatedOwnerAccounts().isEmpty());
        List<UserEntity> owners = users.findByWorkspaceIdAndRoleOrderByFullNameAsc(first.workspaceId(), Role.BUSINESS_OWNER);
        assertEquals(1, owners.size());
        assertEquals("owner.fo", owners.getFirst().getUsername());
        assertEquals("owner@forep.vn", owners.getFirst().getEmail());
        assertTrue(passwordEncoder.matches("OwnerPass!2026", owners.getFirst().getPasswordHash()));
        assertFalse(owners.getFirst().isMustChangePassword());
        assertEquals(1, workspaceSubscriptions.findByWorkspaceIdOrderByCreatedAtDesc(first.workspaceId()).size());
    }

    @Test
    void duplicatePaymentConfirmationCannotCreateTwoWorkspacesOrOwners() throws Exception {
        SubscriptionPlanEntity plan = plan(3);
        WorkspaceRegistrationEntity registration = registration(plan, "CC", "owner@concurrent.vn", "OwnerPass!2026");
        PaymentTransactionEntity payment = payment(registration, plan);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> confirmAfter(start, payment.getId()));
            Future<?> second = executor.submit(() -> confirmAfter(start, payment.getId()));
            start.countDown();
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }

        WorkspaceRegistrationEntity activated = registrations.findById(registration.getId()).orElseThrow();
        assertNotNull(activated.getWorkspaceId());
        assertEquals(1, workspaces.count());
        assertEquals(1, users.findByWorkspaceIdAndRoleOrderByFullNameAsc(activated.getWorkspaceId(), Role.BUSINESS_OWNER).size());
        assertEquals(1, workspaceSubscriptions.findByWorkspaceIdOrderByCreatedAtDesc(activated.getWorkspaceId()).size());
        assertEquals(PaymentTransactionStatus.SUCCESS, payments.findById(payment.getId()).orElseThrow().getStatus());
    }

    @Test
    void ownerUsernameCollisionGetsNumericSuffix() {
        WorkspaceEntity occupiedWorkspace = workspace("XX", "occupied@forep.vn");
        UserEntity occupied = account(occupiedWorkspace, Role.EMPLOYEE, "owner.fo", "occupied-user@forep.vn", null);
        users.saveAndFlush(occupied);
        SubscriptionPlanEntity plan = plan(2);
        WorkspaceRegistrationEntity registration = registration(plan, "FO", "new-owner@forep.vn", "OwnerPass!2026");
        successfulPayment(registration, plan);
        authenticate(Role.PLATFORM_ADMIN, null);

        var activated = service.approveWorkspaceRegistration(registration.getId(), null);

        UserEntity owner = users.findByWorkspaceIdAndRoleOrderByFullNameAsc(activated.workspaceId(), Role.BUSINESS_OWNER).getFirst();
        assertEquals("owner.fo2", owner.getUsername());
    }

    @Test
    void activationFailsAtomicallyWhenOwnerPermissionSeedIsMissing() {
        rolePermissions.deleteAll(rolePermissions.findByRoleAndEnabledTrue(Role.BUSINESS_OWNER));
        SubscriptionPlanEntity plan = plan(1);
        WorkspaceRegistrationEntity registration = registration(plan, "MS", "owner@missing-seed.vn", "OwnerPass!2026");
        successfulPayment(registration, plan);
        authenticate(Role.PLATFORM_ADMIN, null);

        assertThrows(IllegalStateException.class,
                () -> service.approveWorkspaceRegistration(registration.getId(), null));

        assertEquals(0, workspaces.count());
        assertEquals(0, workspaceSubscriptions.count());
        assertEquals(0, users.count());
        assertEquals(null, registrations.findById(registration.getId()).orElseThrow().getWorkspaceId());
    }

    @Test
    void businessOwnerCreatesWorkspaceScopedHrWithRandomTemporaryPassword() {
        WorkspaceEntity workspace = workspace("FO", "contact@forep.vn");
        UserEntity owner = account(workspace, Role.BUSINESS_OWNER, "owner.fo", "owner@forep.vn", "0900000000");
        users.saveAndFlush(owner);
        authenticate(owner);

        var created = service.createInitialHrAccount(new CreateHrAccountRequest(
                "Nguyễn Văn An", "AN@EXAMPLE.COM", "0900000001"));

        assertEquals("hr.fo.nguyenvanan", created.username());
        assertEquals("an@example.com", created.email());
        assertEquals(Role.HR, created.role());
        assertEquals(workspace.getId(), created.workspaceId());
        assertTrue(created.mustChangePassword());
        assertTrue(created.credentialsVisibleOnce());
        assertNotNull(created.temporaryPassword());
        UserEntity stored = users.findById(created.id()).orElseThrow();
        assertTrue(passwordEncoder.matches(created.temporaryPassword(), stored.getPasswordHash()));
        assertEquals(1, auditLogs.findByWorkspaceIdOrderByCreatedAtDesc(workspace.getId()).stream()
                .filter(item -> "BUSINESS_OWNER_CREATE_HR_ACCOUNT".equals(item.getAction())).count());

        assertThrows(IllegalArgumentException.class, () -> service.createInitialHrAccount(
                new CreateHrAccountRequest("Trần Thị Bích", "bich@example.com", "0900000001")));
    }

    @Test
    void hrEmployeeAndPlatformAdminCannotUseBusinessOwnerHrService() {
        WorkspaceEntity workspace = workspace("FO", "contact@forep.vn");
        UserEntity hr = account(workspace, Role.HR, "hr.fo.user", "hr@forep.vn", null);
        UserEntity employee = account(workspace, Role.EMPLOYEE, "emp.fo.user", "employee@forep.vn", null);
        users.saveAllAndFlush(List.of(hr, employee));

        authenticate(hr);
        assertThrows(IllegalArgumentException.class, () -> service.createInitialHrAccount(
                new CreateHrAccountRequest("HR Two", "hr2@forep.vn", null)));

        authenticate(employee);
        assertThrows(IllegalArgumentException.class, () -> service.createInitialHrAccount(
                new CreateHrAccountRequest("HR Three", "hr3@forep.vn", null)));

        seed(Role.PLATFORM_ADMIN, Permission.HR_ACCOUNT_MANAGE);
        authenticate(Role.PLATFORM_ADMIN, null);
        assertThrows(IllegalArgumentException.class, () -> service.createInitialHrAccount(
                new CreateHrAccountRequest("HR Four", "hr4@forep.vn", null)));
    }

    @Test
    void businessOwnerCannotCreateUpdateOrDeactivateEmployeesEvenWithMisconfiguredPermissions() {
        WorkspaceEntity workspace = workspace("BO", "bo@forep.vn");
        UserEntity owner = account(workspace, Role.BUSINESS_OWNER, "owner.bo", "owner-bo@forep.vn", null);
        users.saveAndFlush(owner);
        seed(Role.BUSINESS_OWNER, Permission.EMPLOYEE_CREATE, Permission.EMPLOYEE_UPDATE,
                Permission.EMPLOYEE_DEACTIVATE);
        authenticate(owner);

        assertThrows(IllegalArgumentException.class, () -> service.createEmployee(null));
        assertThrows(IllegalArgumentException.class, () -> service.updateEmployee(UUID.randomUUID(), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateEmployeeStatus(UUID.randomUUID(), UserStatus.INACTIVE));
    }

    @Test
    void ownerCannotChangeHrStatusInAnotherWorkspace() {
        WorkspaceEntity workspaceA = workspace("AA", "a@forep.vn");
        WorkspaceEntity workspaceB = workspace("BB", "b@forep.vn");
        UserEntity ownerA = account(workspaceA, Role.BUSINESS_OWNER, "owner.aa", "owner-a@forep.vn", null);
        UserEntity hrB = account(workspaceB, Role.HR, "hr.bb.user", "hr-b@forep.vn", null);
        users.saveAllAndFlush(List.of(ownerA, hrB));
        authenticate(ownerA);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateHrAccountStatus(hrB.getId(), UserStatus.INACTIVE));
        assertEquals(UserStatus.ACTIVE, users.findById(hrB.getId()).orElseThrow().getStatus());
    }

    private void confirmAfter(CountDownLatch start, UUID paymentId) {
        try {
            start.await();
            authenticate(Role.PLATFORM_ADMIN, null);
            String orderId = payments.findById(paymentId).orElseThrow().getOrderCode();
            service.adminConfirmPayment(paymentId, new ConfirmMomoPaymentRequest("MOMO-TRANS-CONCURRENT", orderId, null));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private SubscriptionPlanEntity plan(int maxOwners) {
        OffsetDateTime now = OffsetDateTime.now();
        SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
        plan.setName("Plan-" + UUID.randomUUID());
        plan.setPrice(BigDecimal.valueOf(399_000));
        plan.setDurationDays(30);
        plan.setDurationInMonths(1);
        plan.setMaxUsers(maxOwners + 20);
        plan.setMaxOwnerAccounts(maxOwners);
        plan.setMaxEmployeeAccounts(20);
        plan.setHasFullFeatures(true);
        plan.setStatus(SubscriptionPlanStatus.ACTIVE);
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        return plans.saveAndFlush(plan);
    }

    private WorkspaceRegistrationEntity registration(SubscriptionPlanEntity plan, String code, String ownerEmail, String password) {
        OffsetDateTime now = OffsetDateTime.now();
        WorkspaceRegistrationEntity registration = new WorkspaceRegistrationEntity();
        registration.setBusinessName("FOREP " + code);
        registration.setWorkspaceName("FOREP Workspace " + code);
        registration.setWorkspaceIdentifier(code);
        registration.setContactEmail("contact-" + code.toLowerCase() + "@forep.vn");
        registration.setContactPhone("091" + Math.abs(code.hashCode()));
        registration.setRepresentativeFullName("Nguyễn Văn Owner");
        registration.setRepresentativeEmail(ownerEmail);
        registration.setOwnerFullName("Nguyễn Văn Owner");
        registration.setOwnerEmail(ownerEmail);
        registration.setOwnerPhone("092" + Math.abs(code.hashCode()));
        registration.setOwnerPasswordHash(passwordEncoder.encode(password));
        registration.setSubscriptionPlanId(plan.getId());
        registration.setMaxUsers(plan.getMaxUsers());
        registration.setMaxOwnerAccounts(plan.getMaxOwnerAccounts());
        registration.setMaxEmployeeAccounts(plan.getMaxEmployeeAccounts());
        registration.setPaymentStatus(PaymentStatus.CONFIRMED);
        registration.setRegistrationStatus(RegistrationStatus.PAYMENT_CONFIRMED);
        registration.setRegistrationToken(UUID.randomUUID().toString());
        registration.setExpiredAt(now.plusDays(1));
        registration.setCreatedAt(now);
        registration.setUpdatedAt(now);
        return registrations.saveAndFlush(registration);
    }

    private PaymentTransactionEntity payment(WorkspaceRegistrationEntity registration, SubscriptionPlanEntity plan) {
        OffsetDateTime now = OffsetDateTime.now();
        PaymentTransactionEntity payment = new PaymentTransactionEntity();
        payment.setWorkspaceRegistrationId(registration.getId());
        payment.setSubscriptionPlanId(plan.getId());
        payment.setPaymentMethod(PaymentMethod.MOMO);
        payment.setAmount(plan.getPrice());
        payment.setCurrency("VND");
        payment.setPaymentCode("PAY-" + UUID.randomUUID());
        payment.setOrderCode("ORDER-" + UUID.randomUUID());
        payment.setRequestId(UUID.randomUUID().toString());
        payment.setStatus(PaymentTransactionStatus.PENDING);
        payment.setExpiredAt(now.plusMinutes(30));
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        return payments.saveAndFlush(payment);
    }

    private PaymentTransactionEntity successfulPayment(WorkspaceRegistrationEntity registration, SubscriptionPlanEntity plan) {
        PaymentTransactionEntity payment = payment(registration, plan);
        payment.setProviderTransactionId("MOMO-TRANS-" + UUID.randomUUID());
        payment.setStatus(PaymentTransactionStatus.SUCCESS);
        payment.setPaidAt(OffsetDateTime.now());
        return payments.saveAndFlush(payment);
    }

    private WorkspaceEntity workspace(String code, String contactEmail) {
        OffsetDateTime now = OffsetDateTime.now();
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setName("Workspace " + code);
        workspace.setBusinessName("Business " + code);
        workspace.setShortCode(code);
        workspace.setOrganizationAbbreviation(code);
        workspace.setContactEmail(contactEmail);
        workspace.setContactPhone("093" + Math.abs(code.hashCode()));
        workspace.setMaxUsers(50);
        workspace.setMaxOwnerAccounts(5);
        workspace.setMaxEmployeeAccounts(45);
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspace.setPaymentStatus(PaymentStatus.CONFIRMED);
        workspace.setActivatedAt(now);
        workspace.setCreatedAt(now);
        return workspaces.saveAndFlush(workspace);
    }

    private UserEntity account(WorkspaceEntity workspace, Role role, String username, String email, String phone) {
        OffsetDateTime now = OffsetDateTime.now();
        UserEntity account = new UserEntity();
        account.setWorkspaceId(workspace.getId());
        account.setFullName(role.name() + " User");
        account.setEmail(email);
        account.setPhone(phone);
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode("ExistingPass!2026"));
        account.setRole(role);
        account.setStatus(UserStatus.ACTIVE);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        return account;
    }

    private void authenticate(UserEntity user) {
        authenticate(new AuthenticatedUser(user.getId(), user.getWorkspaceId(), user.getRole(), user.getEmail()));
    }

    private void authenticate(Role role, UUID workspaceId) {
        authenticate(new AuthenticatedUser(UUID.randomUUID(), workspaceId, role, role.name().toLowerCase() + "@forep.vn"));
    }

    private void authenticate(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private void seedBusinessOwnerPermissions() {
        seed(Role.BUSINESS_OWNER,
                Permission.WORKSPACE_VIEW, Permission.WORKSPACE_UPDATE, Permission.EMPLOYEE_VIEW,
                Permission.DEPARTMENT_VIEW, Permission.POSITION_VIEW, Permission.HR_ACCOUNT_MANAGE,
                Permission.TASK_VIEW, Permission.AI_SUMMARY, Permission.AI_HISTORY,
                Permission.REPORT_VIEW, Permission.FEEDBACK_CREATE);
    }

    private void seedHrPermissions() {
        seed(Role.HR,
                Permission.WORKSPACE_VIEW, Permission.EMPLOYEE_VIEW, Permission.EMPLOYEE_CREATE,
                Permission.EMPLOYEE_UPDATE, Permission.EMPLOYEE_DEACTIVATE, Permission.EMPLOYEE_IMPORT,
                Permission.DEPARTMENT_VIEW, Permission.DEPARTMENT_MANAGE,
                Permission.POSITION_VIEW, Permission.POSITION_MANAGE);
    }

    private void seed(Role role, Permission... permissions) {
        OffsetDateTime now = OffsetDateTime.now();
        List<RolePermissionEntity> entities = Arrays.stream(permissions).map(permission -> {
            RolePermissionEntity entity = new RolePermissionEntity();
            entity.setRole(role);
            entity.setPermission(permission);
            entity.setEnabled(true);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            return entity;
        }).toList();
        rolePermissions.saveAllAndFlush(entities);
    }
}
