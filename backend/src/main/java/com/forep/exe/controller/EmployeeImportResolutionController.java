package com.forep.exe.controller;

import com.forep.exe.dto.ApiResponse;
import com.forep.exe.service.EmployeeImportService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/workspace/hr/employees/imports")
public class EmployeeImportResolutionController {
    private final EmployeeImportService employeeImportService;

    public EmployeeImportResolutionController(EmployeeImportService employeeImportService) {
        this.employeeImportService = employeeImportService;
    }

    @PostMapping("/{batchId}/resolve-and-confirm")
    public ApiResponse<?> resolveAndConfirm(
            @PathVariable UUID batchId,
            @RequestBody(required = false) EmployeeImportService.ConfirmRequest request) {
        return ApiResponse.ok(employeeImportService.confirm(batchId, request));
    }
}
