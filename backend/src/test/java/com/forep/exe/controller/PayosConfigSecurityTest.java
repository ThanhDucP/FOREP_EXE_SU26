package com.forep.exe.controller;

import com.forep.exe.config.SecurityConfig;
import com.forep.exe.domain.Enums.Permission;
import com.forep.exe.security.AuthorizationService;
import com.forep.exe.security.JwtAuthenticationFilter;
import com.forep.exe.security.JwtService;
import com.forep.exe.service.ForepService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static com.forep.exe.security.AuthorizationService.authority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

@WebMvcTest(AdminPlatformController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PayosConfigSecurityTest {
    @Autowired private MockMvc mvc;
    @MockBean private ForepService service;
    @MockBean private JwtService jwtService;
    @MockBean private AuthorizationService authorizationService;

    @Test
    void unauthenticatedAndNonAdminUsersCannotReadPayosConfig() throws Exception {
        mvc.perform(get("/api/admin/payos-config")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/payos-config").with(user("employee"))).andExpect(status().isForbidden());
    }

    @Test
    void paymentConfigurationPermissionCanReadPayosConfig() throws Exception {
        mvc.perform(get("/api/admin/payos-config")
                        .with(user("platform-admin").authorities(() -> authority(Permission.PAYMENT_QR_MANAGE))))
                .andExpect(status().isOk());
    }

    @Test
    void nonAdminCannotUpdatePayosConfig() throws Exception {
        mvc.perform(put("/api/admin/payos-config")
                        .with(user("employee"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
