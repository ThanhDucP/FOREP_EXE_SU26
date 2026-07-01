package com.forep.exe.ai;

import com.forep.exe.domain.Enums.WorkloadLevel;
import com.forep.exe.service.ForepService.AssigneeRecommendationView;
import com.forep.exe.service.ForepService.BusinessSummaryView;
import com.forep.exe.service.ForepService.TaskView;
import com.forep.exe.service.ForepService.WorkloadView;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AiServiceClient {
    private final RestClient restClient;
    private final AiServiceProperties properties;

    public AiServiceClient(RestClient.Builder builder, AiServiceProperties properties) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    public List<AssigneeRecommendationView> recommendAssignee(AiRecommendAssigneeInput input) {
        AiRecommendAssigneeResponse response = post("/internal/ai/recommend-assignee", input, AiRecommendAssigneeResponse.class);
        if (response == null || response.recommendations() == null) {
            throw new AiProviderException("AI service returned an invalid assignee recommendation response.");
        }
        return response.recommendations().stream()
                .map(item -> new AssigneeRecommendationView(
                        item.employeeId(),
                        item.fullName(),
                        item.score(),
                        item.workloadLevel(),
                        item.reason(),
                        item.risk()
                ))
                .toList();
    }

    public Map<String, Object> workloadSummary(List<WorkloadView> workload) {
        return post("/internal/ai/workload-summary", Map.of("employees", workload.stream().map(AiEmployeeWorkload::from).toList()), Map.class);
    }

    public Map<String, Object> delayRisks(List<TaskView> tasks, Map<UUID, String> employeeNames) {
        if (!isConfigured()) {
            throw new IllegalStateException("AI service is not configured.");
        }
        List<Map<String, Object>> payloadTasks = tasks.stream()
                .map(task -> Map.<String, Object>of(
                        "taskId", task.id().toString(),
                        "title", task.title(),
                        "assigneeName", employeeNames.getOrDefault(task.assigneeId(), "Chưa rõ"),
                        "deadline", task.deadline().toString(),
                        "progressPercent", task.progressPercent(),
                        "overdue", task.deadline().isBefore(OffsetDateTime.now()) && task.completedAt() == null
                ))
                .toList();
        return post("/internal/ai/delay-risks", Map.of("tasks", payloadTasks), Map.class);
    }

    public Map<String, Object> dailySummary(BusinessSummaryView summary) {
        return post("/internal/ai/daily-summary", Map.of(
                "completedTasks", summary.completedTasks(),
                "overdueTasks", summary.overdueTasks(),
                "overloadedEmployees", summary.overloadedEmployees(),
                "idleEmployees", summary.idleEmployees()
        ), Map.class);
    }

    public Map<String, Object> businessSummary(Map<String, Object> payload) {
        return post("/internal/ai/business-summary", payload, Map.class);
    }

    public Map<String, Object> dailyReportInsights(Map<String, Object> payload) {
        return post("/internal/ai/daily-report-insights", payload, Map.class);
    }

    public Map<String, Object> extractTasks(Map<String, Object> payload) {
        return post("/internal/ai/tasks/extract", payload, Map.class);
    }

    public Map<String, Object> splitTask(Map<String, Object> payload) {
        return post("/internal/ai/tasks/split", payload, Map.class);
    }

    public Map<String, Object> taskAdjustment(Map<String, Object> payload) {
        return post("/internal/ai/tasks/adjust", payload, Map.class);
    }

    public Map<String, Object> missingReports(Map<String, Object> payload) {
        return post("/internal/ai/missing-reports", payload, Map.class);
    }

    public Map<String, Object> actionSuggestions(Map<String, Object> payload) {
        return post("/internal/ai/action-suggestions", payload, Map.class);
    }

    private boolean isConfigured() {
        return properties.serviceUrl() != null
                && !properties.serviceUrl().isBlank()
                && properties.serviceToken() != null
                && !properties.serviceToken().isBlank();
    }

    private <T> T post(String path, Object payload, Class<T> responseType) {
        if (!isConfigured()) {
            throw new AiProviderException("AI service is not configured.");
        }
        try {
            return restClient.post()
                    .uri(properties.serviceUrl() + path)
                    .header("X-Internal-Service-Token", properties.serviceToken())
                    .body(payload)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException exception) {
            throw new AiProviderException("Gemini and Groq both failed", exception);
        }
    }

    public record AiRecommendAssigneeInput(
            String title,
            String requirements,
            String deadline,
            double estimatedHours,
            List<AiEmployeeWorkload> employees
    ) {
    }

    public record AiEmployeeWorkload(
            String employeeId,
            String fullName,
            long openTasks,
            long overdueTasks,
            long blockedTasks,
            double estimatedWorkload,
            WorkloadLevel workloadLevel,
            String status,
            int candidateScore,
            Map<String, Object> scoreComponents
    ) {
        public static AiEmployeeWorkload from(WorkloadView item) {
            return new AiEmployeeWorkload(
                    item.employeeId().toString(),
                    item.fullName(),
                    item.openTasks(),
                    item.overdueTasks(),
                    item.blockedTasks(),
                    item.estimatedWorkload().doubleValue(),
                    item.workloadLevel(),
                    "ACTIVE",
                    0,
                    Map.of()
            );
        }
    }

    record AiRecommendAssigneeResponse(List<AiRecommendation> recommendations) {
    }

    record AiRecommendation(UUID employeeId, String fullName, int score, WorkloadLevel workloadLevel, String reason, String risk) {
    }
}
