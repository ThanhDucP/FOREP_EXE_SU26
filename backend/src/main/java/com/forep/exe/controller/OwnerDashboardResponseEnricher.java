package com.forep.exe.controller;

import com.forep.exe.dto.ApiResponse;
import com.forep.exe.service.WorkspaceDashboardKpiService;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = WorkspaceOperationsController.class)
public class OwnerDashboardResponseEnricher implements ResponseBodyAdvice<Object> {
    private static final String OWNER_DASHBOARD_PATH = "/api/workspace/business-owner/dashboard";

    private final WorkspaceDashboardKpiService workspaceDashboardKpiService;

    public OwnerDashboardResponseEnricher(WorkspaceDashboardKpiService workspaceDashboardKpiService) {
        this.workspaceDashboardKpiService = workspaceDashboardKpiService;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        String path = request.getURI().getPath();
        if (!OWNER_DASHBOARD_PATH.equals(path) && !(OWNER_DASHBOARD_PATH + "/").equals(path)) {
            return body;
        }
        if (!(body instanceof ApiResponse<?> apiResponse)) {
            return body;
        }
        if (!(apiResponse.data() instanceof Map<?, ?> existingData)) {
            return body;
        }

        Map<String, Object> enriched = new LinkedHashMap<>();
        existingData.forEach((key, value) -> {
            if (key instanceof String stringKey) {
                enriched.put(stringKey, value);
            }
        });
        enriched.putAll(workspaceDashboardKpiService.ownerDashboardKpis());
        return ApiResponse.ok(enriched);
    }
}
