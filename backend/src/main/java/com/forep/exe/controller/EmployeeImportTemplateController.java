package com.forep.exe.controller;

import com.forep.exe.service.EmployeeImportService;
import com.forep.exe.service.EmployeeImportTemplateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace/hr/employees")
public class EmployeeImportTemplateController {
    private final EmployeeImportTemplateService templateService;

    public EmployeeImportTemplateController(EmployeeImportTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/import-template-v2")
    public ResponseEntity<byte[]> template() {
        EmployeeImportService.FilePayload file = templateService.template();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.content().length)
                .body(file.content());
    }
}
