package com.forep.exe.controller;

import com.forep.exe.domain.Enums.Permission;
import com.forep.exe.dto.ApiResponse;
import com.forep.exe.security.AuthorizationService;
import com.forep.exe.service.PayosWebhookRegistrationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments/payos/webhook")
public class AdminPayosWebhookController {
    private final AuthorizationService authorizationService;
    private final PayosWebhookRegistrationService webhookRegistrationService;

    public AdminPayosWebhookController(
            AuthorizationService authorizationService,
            PayosWebhookRegistrationService webhookRegistrationService) {
        this.authorizationService = authorizationService;
        this.webhookRegistrationService = webhookRegistrationService;
    }

    @PostMapping("/confirm")
    ApiResponse<?> confirmWebhook() {
        authorizationService.require(Permission.SYSTEM_CONFIGURATION);
        return ApiResponse.ok(webhookRegistrationService.forceConfirm());
    }
}
