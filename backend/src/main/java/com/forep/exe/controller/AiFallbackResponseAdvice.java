package com.forep.exe.controller;

import com.forep.exe.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;
import java.util.Map;

/**
 * Keeps degraded AI responses useful for Vietnamese users without pretending
 * that a provider-generated explanation succeeded.
 */
@RestControllerAdvice
public class AiFallbackResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!(body instanceof ApiResponse<?> apiResponse)
                || !(apiResponse.data() instanceof Map<?, ?> rawData)
                || !"RULE_BASED_FALLBACK".equals(rawData.get("source"))
                || !Boolean.TRUE.equals(rawData.get("aiProviderFailed"))
                || !rawData.containsKey("explanationTitle")) {
            return body;
        }

        @SuppressWarnings("unchecked")
        Map<Object, Object> data = (Map<Object, Object>) rawData;
        try {
            data.put("explanationTitle", "Giải thích lựa chọn từ dữ liệu hệ thống");
            data.put("shortExplanation", "Lựa chọn hiện tại được giữ nguyên theo kết quả xếp hạng do hệ thống tính toán.");
            data.put(
                    "detailedExplanation",
                    "AI tạm thời chưa phản hồi. FOREP vẫn hiển thị nhận định dựa trên thứ hạng ứng viên, mức tải công việc và mức độ phù hợp về vị trí, kỹ năng đã được backend kiểm tra."
            );
            data.put(
                    "keyReasons",
                    List.of("Thứ hạng ứng viên", "Mức tải công việc", "Mức độ phù hợp vị trí và kỹ năng")
            );
            data.put("dataUsed", List.of("Dữ liệu xếp hạng", "Dữ liệu mức tải", "Dữ liệu hiệu suất"));
            data.put(
                    "fallbackReason",
                    "AI đang tạm thời không khả dụng; kết quả này được tạo từ dữ liệu nghiệp vụ hiện có và không tự động thay đổi phân công."
            );
        } catch (UnsupportedOperationException ignored) {
            // Defensive only. Current fallback payload is a mutable LinkedHashMap.
        }
        return body;
    }
}
