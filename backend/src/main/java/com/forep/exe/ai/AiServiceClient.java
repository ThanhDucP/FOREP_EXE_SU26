package com.forep.exe.ai;

import com.forep.exe.domain.Enums.WorkloadLevel;
import com.forep.exe.service.ForepService.AssigneeRecommendationView;
import com.forep.exe.service.ForepService.BusinessSummaryView;
import com.forep.exe.service.ForepService.TaskView;
import com.forep.exe.service.ForepService.WorkloadView;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
        if (!isConfigured()) {
            throw new IllegalStateException("AI service is not configured.");
        }
        AiRecommendAssigneeResponse response = restClient.post()
                .uri(properties.serviceUrl() + "/internal/ai/recommend-assignee")
                .header("X-Internal-Service-Token", properties.serviceToken())
                .body(input)
                .retrieve()
                .body(AiRecommendAssigneeResponse.class);
        if (response == null || response.recommendations() == null) {
            return List.of();
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
        if (!isConfigured()) {
            throw new IllegalStateException("AI service is not configured.");
        }
        return restClient.post()
                .uri(properties.serviceUrl() + "/internal/ai/workload-summary")
                .header("X-Internal-Service-Token", properties.serviceToken())
                .body(Map.of("employees", workload.stream().map(AiEmployeeWorkload::from).toList()))
                .retrieve()
                .body(Map.class);
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
        return restClient.post()
                .uri(properties.serviceUrl() + "/internal/ai/delay-risks")
                .header("X-Internal-Service-Token", properties.serviceToken())
                .body(Map.of("tasks", payloadTasks))
                .retrieve()
                .body(Map.class);
    }

    public Map<String, Object> dailySummary(BusinessSummaryView summary) {
        if (!isConfigured()) {
            throw new IllegalStateException("AI service is not configured.");
        }
        return restClient.post()
                .uri(properties.serviceUrl() + "/internal/ai/daily-summary")
                .header("X-Internal-Service-Token", properties.serviceToken())
                .body(Map.of(
                        "completedTasks", summary.completedTasks(),
                        "overdueTasks", summary.overdueTasks(),
                        "overloadedEmployees", summary.overloadedEmployees(),
                        "idleEmployees", summary.idleEmployees()
                ))
                .retrieve()
                .body(Map.class);
    }

    private boolean isConfigured() {
        return properties.serviceUrl() != null
                && !properties.serviceUrl().isBlank()
                && properties.serviceToken() != null
                && !properties.serviceToken().isBlank();
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
            WorkloadLevel workloadLevel
    ) {
        public static AiEmployeeWorkload from(WorkloadView item) {
            return new AiEmployeeWorkload(
                    item.employeeId().toString(),
                    item.fullName(),
                    item.openTasks(),
                    item.overdueTasks(),
                    item.blockedTasks(),
                    item.estimatedWorkload().doubleValue(),
                    item.workloadLevel()
            );
        }
    }

    record AiRecommendAssigneeResponse(List<AiRecommendation> recommendations) {
    }

    record AiRecommendation(UUID employeeId, String fullName, int score, WorkloadLevel workloadLevel, String reason, String risk) {
    }
}
