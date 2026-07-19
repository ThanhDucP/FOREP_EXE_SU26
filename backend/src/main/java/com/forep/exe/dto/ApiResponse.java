package com.forep.exe.dto;

import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public record ApiResponse<T>(T data, Map<String, Object> meta, List<ApiError> errors) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, metadata(), List.of());
    }

    public static ApiResponse<Object> error(String code, String message, String field) {
        return new ApiResponse<>(null, metadata(), List.of(new ApiError(code, message, field)));
    }

    private static Map<String, Object> metadata() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object requestId = attributes == null ? null : attributes.getAttribute("requestId", RequestAttributes.SCOPE_REQUEST);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("requestId", requestId == null ? UUID.randomUUID().toString() : requestId.toString());
        meta.put("timestamp", OffsetDateTime.now().toString());
        return meta;
    }
}

record ApiError(String code, String message, String field) {
}
