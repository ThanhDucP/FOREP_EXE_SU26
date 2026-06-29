package com.forep.exe.dto;

import java.util.List;
import java.util.Map;

public record ApiResponse<T>(T data, Map<String, Object> meta, List<ApiError> errors) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, Map.of(), List.of());
    }

    public static ApiResponse<Object> error(String code, String message, String field) {
        return new ApiResponse<>(null, Map.of(), List.of(new ApiError(code, message, field)));
    }
}

record ApiError(String code, String message, String field) {
}

