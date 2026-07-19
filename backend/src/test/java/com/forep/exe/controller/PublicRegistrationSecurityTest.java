package com.forep.exe.controller;

import com.forep.exe.config.SecurityConfig;
import com.forep.exe.security.AuthorizationService;
import com.forep.exe.security.JwtAuthenticationFilter;
import com.forep.exe.security.JwtService;
import com.forep.exe.service.ForepService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicRegistrationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PublicRegistrationSecurityTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private ForepService service;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AuthorizationService authorizationService;

    @Test
    void guestCanSubmitWorkspaceRegistrationWithoutAuthorizationHeader() throws Exception {
        mvc.perform(post("/api/public/workspace-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessName": "FOREP Test",
                                  "workspaceName": "FOREP Workspace",
                                  "contactEmail": "contact@example.com",
                                  "representativeFullName": "Nguyen Van A",
                                  "representativeEmail": "owner@example.com"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void publicNamespaceIsNotBroadlyPermitted() throws Exception {
        mvc.perform(get("/api/public/internal-data"))
                .andExpect(status().isForbidden());
    }

    @Test
    void productionCorsPreflightAllowsAuthorizationAndContractHeaders() throws Exception {
        mvc.perform(options("/api/workspace/ai/business-owner/operational-summary")
                        .header("Origin", "https://forep-ai.vercel.app")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,X-Workspace-Id,Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://forep-ai.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("Authorization")))
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("X-Workspace-Id")))
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("Idempotency-Key")));
    }
}
