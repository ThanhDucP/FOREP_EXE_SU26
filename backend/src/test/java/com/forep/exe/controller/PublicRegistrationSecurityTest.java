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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
                                  "representativeEmail": "owner@example.com",
                                  "ownerPassword": "OwnerPass!2026"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void publicRegistrationBusinessErrorIsReturnedAsBadRequestInsteadOfForbidden() throws Exception {
        when(service.submitWorkspaceRegistration(any()))
                .thenThrow(new IllegalArgumentException("Owner email already belongs to an existing account."));

        mvc.perform(post("/api/public/workspace-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessName": "FOREP Test",
                                  "workspaceName": "FOREP Workspace",
                                  "contactEmail": "contact@example.com",
                                  "representativeFullName": "Nguyen Van A",
                                  "representativeEmail": "owner@example.com",
                                  "ownerPassword": "OwnerPass!2026"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("BUSINESS_RULE_ERROR"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Owner email already belongs to an existing account."));
    }

    @Test
    void guestCanSelectPlanCreateMomoPaymentAndReadStatusWithoutAuthorizationHeader() throws Exception {
        String registrationId = "20000000-0000-0000-0000-000000000001";
        String planId = "10000000-0000-0000-0000-000000000001";
        String token = "guest-registration-token";

        mvc.perform(get("/api/public/subscription-plans"))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/public/workspace-registrations/{id}/select-plan", registrationId)
                        .queryParam("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "subscriptionPlanId": "%s" }
                                """.formatted(planId)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/public/workspace-registrations/{id}/payments", registrationId)
                        .queryParam("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "paymentMethod": "MOMO" }
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/api/public/payments/{paymentCode}/status", "PAY-GUEST-001")
                        .queryParam("token", token))
                .andExpect(status().isOk());
    }

    @Test
    void momoIpnIsPublicAndAlwaysAcknowledgedWithHttp200() throws Exception {
        when(service.handleMomoCallback(any()))
                .thenThrow(new IllegalArgumentException("Invalid MoMo callback signature."));

        mvc.perform(post("/api/payments/momo/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "partnerCode":"PARTNER",
                                  "orderId":"ORDER-1",
                                  "requestId":"REQUEST-1",
                                  "amount":1000,
                                  "resultCode":0,
                                  "signature":"invalid"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].code").value("MOMO_IPN_REJECTED"));
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
