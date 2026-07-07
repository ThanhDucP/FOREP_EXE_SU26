package com.forep.exe.controller;

import com.forep.exe.dto.ApiResponse;
import com.forep.exe.dto.Requests.BusinessFeedbackRequest;
import com.forep.exe.service.ForepService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceFeedbackController {
    private final ForepService service;

    public WorkspaceFeedbackController(ForepService service) {
        this.service = service;
    }

    @PostMapping("/feedback")
    ApiResponse<?> submitBusinessFeedback(@RequestBody @Valid BusinessFeedbackRequest request) {
        return ApiResponse.ok(service.submitBusinessFeedback(request));
    }
}
