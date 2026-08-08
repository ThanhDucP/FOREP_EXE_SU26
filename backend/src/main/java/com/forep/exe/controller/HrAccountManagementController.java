package com.forep.exe.controller;

import com.forep.exe.dto.ApiResponse;
import com.forep.exe.service.HrAccountManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/workspace/business-owner/hr-accounts")
public class HrAccountManagementController {
    private final HrAccountManagementService hrAccountManagementService;

    public HrAccountManagementController(HrAccountManagementService hrAccountManagementService) {
        this.hrAccountManagementService = hrAccountManagementService;
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable UUID id) {
        return ApiResponse.ok(hrAccountManagementService.account(id));
    }

    @PatchMapping("/{id}/reset-password")
    public ApiResponse<?> resetPassword(@PathVariable UUID id) {
        return ApiResponse.ok(hrAccountManagementService.resetPassword(id));
    }
}
