package com.forep.exe.controller;

import com.forep.exe.dto.ApiResponse;
import com.forep.exe.service.ForepService;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = AdminPlatformController.class)
public class PlatformDashboardResponseEnricher implements ResponseBodyAdvice<Object> {
    private static final String PLATFORM_OVERVIEW_PATH = "/api/admin/dashboard/overview";

    private final ForepService service;

    public PlatformDashboardResponseEnricher(ForepService service) {
        this.service = service;
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
        if (!PLATFORM_OVERVIEW_PATH.equals(path) && !(PLATFORM_OVERVIEW_PATH + "/").equals(path)) {
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

        putAliasIfMissing(enriched, "newWorkspaces", existingData.get("newWorkspacesThisMonth"));
        putAliasIfMissing(enriched, "feedbackAverage", existingData.get("businessFeedbackRatingAverage"));

        Object aiUsageStatistics = existingData.get("aiUsageStatistics");
        if (!enriched.containsKey("aiUsage") && aiUsageStatistics instanceof Map<?, ?> aiMap) {
            Object totalHistoryCalls = aiMap.get("totalHistoryCalls");
            if (totalHistoryCalls instanceof Number number) {
                enriched.put("aiUsage", number.longValue());
            }
        }

        if (!enriched.containsKey("totalRevenue") || !enriched.containsKey("revenue")) {
            Map<String, Object> paymentSummary = service.adminDashboardPaymentsSummary();
            Object totalSuccessfulRevenue = paymentSummary.get("totalSuccessfulRevenue");
            putAliasIfMissing(enriched, "totalRevenue", totalSuccessfulRevenue);
            putAliasIfMissing(enriched, "revenue", totalSuccessfulRevenue);
        }

        return ApiResponse.ok(enriched);
    }

    private void putAliasIfMissing(Map<String, Object> target, String key, Object value) {
        if (!target.containsKey(key) && value != null) {
            target.put(key, value);
        }
    }
}
