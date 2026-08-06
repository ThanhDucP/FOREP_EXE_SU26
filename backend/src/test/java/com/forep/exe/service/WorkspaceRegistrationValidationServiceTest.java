package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentStatus;
import com.forep.exe.domain.Enums.RegistrationStatus;
import com.forep.exe.persistence.UserRepository;
import com.forep.exe.persistence.WorkspaceEntity;
import com.forep.exe.persistence.WorkspaceRegistrationEntity;
import com.forep.exe.persistence.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceRegistrationValidationServiceTest {

    private UserRepository users;
    private WorkspaceRepository workspaces;
    private WorkspaceRegistrationValidationService validationService;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        workspaces = mock(WorkspaceRepository.class);
        validationService = new WorkspaceRegistrationValidationService(users, workspaces);
    }

    private WorkspaceRegistrationEntity createValidRegistration() {
        WorkspaceRegistrationEntity reg = new WorkspaceRegistrationEntity();
        reg.setId(UUID.randomUUID());
        reg.setOwnerEmail("owner@forep.vn");
        reg.setOwnerFullName("Nguyễn Văn Owner");
        reg.setOwnerPasswordHash("$2a$10$xyz");
        reg.setBusinessName("Forep Tech");
        reg.setWorkspaceName("Forep Workspace");
        reg.setWorkspaceIdentifier("FT");
        reg.setSubscriptionPlanId(UUID.randomUUID());
        reg.setPaymentStatus(PaymentStatus.CONFIRMED);
        reg.setRegistrationStatus(RegistrationStatus.PAYMENT_CONFIRMED);
        reg.setExpiredAt(OffsetDateTime.now().plusDays(5));
        return reg;
    }

    @Test
    void validateForActivation_success() {
        WorkspaceRegistrationEntity reg = createValidRegistration();

        when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(workspaces.findByShortCodeIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validationService.validateForActivation(reg));
    }

    @Test
    void validateForActivation_missingOwnerEmail() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        reg.setOwnerEmail(null);

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_MISSING_OWNER_EMAIL, exception.getErrorCode());
        assertEquals("Thiếu email của chủ sở hữu Workspace.", exception.getMessage());
    }

    @Test
    void validateForActivation_missingOwnerFullName() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        reg.setOwnerFullName("   ");

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_MISSING_OWNER_NAME, exception.getErrorCode());
        assertEquals("Thiếu họ tên của chủ sở hữu Workspace.", exception.getMessage());
    }

    @Test
    void validateForActivation_missingOwnerPassword() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        reg.setOwnerPasswordHash("");

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_MISSING_OWNER_PASSWORD, exception.getErrorCode());
        assertEquals("Thiếu thông tin mật khẩu của chủ sở hữu Workspace.", exception.getMessage());
    }

    @Test
    void validateForActivation_missingBusinessName() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        reg.setBusinessName(null);

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_MISSING_BUSINESS_NAME, exception.getErrorCode());
        assertEquals("Thiếu tên doanh nghiệp trong hồ sơ đăng ký.", exception.getMessage());
    }

    @Test
    void validateForActivation_missingWorkspaceName() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        reg.setWorkspaceName(null);

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_MISSING_WORKSPACE_NAME, exception.getErrorCode());
        assertEquals("Thiếu tên Workspace trong hồ sơ đăng ký.", exception.getMessage());
    }

    @Test
    void validateForActivation_missingWorkspaceIdentifier() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        reg.setWorkspaceIdentifier(null);

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_MISSING_WORKSPACE_IDENTIFIER, exception.getErrorCode());
        assertEquals("Thiếu mã định danh (shortCode) của Workspace.", exception.getMessage());
    }

    @Test
    void validateForActivation_missingSubscriptionPlan() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        reg.setSubscriptionPlanId(null);

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_MISSING_SUBSCRIPTION_PLAN, exception.getErrorCode());
    }

    @Test
    void validateForActivation_invalidRegistrationStatus_terminal() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        reg.setRegistrationStatus(RegistrationStatus.REJECTED);

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_INVALID_REGISTRATION_STATUS, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("REJECTED"));
    }

    @Test
    void validateForActivation_invalidPaymentStatus() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        reg.setPaymentStatus(PaymentStatus.PENDING);

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_INVALID_PAYMENT_STATUS, exception.getErrorCode());
    }

    @Test
    void validateForActivation_invalidRegistrationStatus_nonActivatable() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        reg.setRegistrationStatus(RegistrationStatus.PENDING_PAYMENT);

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_INVALID_REGISTRATION_STATUS, exception.getErrorCode());
    }

    @Test
    void validateForActivation_registrationExpired() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        reg.setExpiredAt(OffsetDateTime.now().minusMinutes(1));
        reg.setRegistrationStatus(RegistrationStatus.PENDING_PAYMENT); // make it not activatable to trigger expiration

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_REGISTRATION_EXPIRED, exception.getErrorCode());
    }

    @Test
    void validateForActivation_ownerEmailAlreadyExists() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        
        when(users.existsByEmailIgnoreCase("owner@forep.vn")).thenReturn(true);

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_OWNER_EMAIL_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void validateForActivation_workspaceIdentifierConflict() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        
        when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(workspaces.findByShortCodeIgnoreCase("FT")).thenReturn(Optional.of(new WorkspaceEntity()));

        WorkspaceValidationException exception = assertThrows(WorkspaceValidationException.class,
                () -> validationService.validateForActivation(reg));

        assertEquals(WorkspaceRegistrationValidationService.ERR_WORKSPACE_IDENTIFIER_CONFLICT, exception.getErrorCode());
    }

    @Test
    void validateForActivation_adminDirectPath_skipsPaymentAndIdentifierChecks() {
        WorkspaceRegistrationEntity reg = createValidRegistration();
        reg.setWorkspaceId(UUID.randomUUID()); // setting workspaceId simulates the admin direct path
        reg.setPaymentStatus(PaymentStatus.PENDING); // payment doesn't need to be confirmed in activation step for admin path
        reg.setRegistrationStatus(RegistrationStatus.PENDING_PAYMENT);

        when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        assertDoesNotThrow(() -> validationService.validateForActivation(reg));
        verify(workspaces, never()).findByShortCodeIgnoreCase(anyString());
    }
}
