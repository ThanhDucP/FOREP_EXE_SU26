package com.forep.exe.controller;

import com.forep.exe.config.SecurityConfig;
import com.forep.exe.security.AuthorizationService;
import com.forep.exe.security.JwtAuthenticationFilter;
import com.forep.exe.security.JwtService;
import com.forep.exe.service.EmployeeImportService;
import com.forep.exe.service.ForepService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.forep.exe.security.AuthorizationService.authority;
import static com.forep.exe.domain.Enums.Permission.AI_SUMMARY;
import static com.forep.exe.domain.Enums.Permission.HR_ACCOUNT_MANAGE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkspaceOperationsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class WorkspaceAccountSecurityTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private ForepService service;

    @MockBean
    private EmployeeImportService employeeImportService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AuthorizationService authorizationService;

    @Test
    void businessOwnerPermissionCanCreateHr() throws Exception {
        mvc.perform(post("/api/workspace/business-owner/hr-accounts")
                        .with(user("owner").authorities(() -> authority(HR_ACCOUNT_MANAGE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Nguyễn Văn An","email":"an@example.com","phone":"0900000001"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void aiSummaryDoesNotAccidentallyAuthorizeHrAccountManagement() throws Exception {
        mvc.perform(post("/api/workspace/business-owner/hr-accounts")
                        .with(user("hr").authorities(() -> authority(AI_SUMMARY)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Nguyễn Văn An","email":"an@example.com"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeWithoutHrAccountManageCannotCreateHr() throws Exception {
        mvc.perform(post("/api/workspace/business-owner/hr-accounts")
                        .with(user("employee"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Nguyễn Văn An","email":"an@example.com"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void legacyEmployeeEndpointDoesNotLetBusinessOwnerBypassEmployeeCreatePermission() throws Exception {
        mvc.perform(post("/api/v1/employees")
                        .with(user("owner").authorities(() -> authority(HR_ACCOUNT_MANAGE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Nguyễn Văn An","email":"an@example.com"}
                                """))
                .andExpect(status().isForbidden());
    }
}
