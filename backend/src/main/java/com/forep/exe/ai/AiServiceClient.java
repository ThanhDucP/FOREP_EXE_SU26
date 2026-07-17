package com.forep.exe.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.domain.Enums.SeniorityLevel;
import com.forep.exe.domain.Enums.PermissionGroup;
import com.forep.exe.domain.Enums.WorkloadLevel;
import com.forep.exe.service.ForepService.AssigneeRecommendationView;
import com.forep.exe.service.ForepService.BusinessSummaryView;
import com.forep.exe.service.ForepService.TaskView;
import com.forep.exe.service.ForepService.WorkloadView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AiServiceClient {
    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestClient restClient;
    private final AiServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final Semaphore concurrencyLimiter;
    private final ConcurrentMap<String, CompletableFuture<Object>> inFlightRequests = new ConcurrentHashMap<>();
    private final AtomicInteger consecutiveProviderFailures = new AtomicInteger();
    private final AtomicLong circuitOpenUntilMillis = new AtomicLong();

    public AiServiceClient(RestClient.Builder builder, AiServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.concurrencyLimiter = new Semaphore(properties.effectiveMaxConcurrentRequests(), true);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.effectiveConnectTimeoutMillis()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.effectiveReadTimeoutMillis()));
        this.restClient = builder.requestFactory(requestFactory).build();
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
                        item.requiredRole(),
                        item.roleFit(),
                        item.roleFitReason(),
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
            throw new AiProviderException("AI service is not configured.");
        }
        List<Map<String, Object>> payloadTasks = tasks.stream()
                .map(task -> Map.<String, Object>of(
                        "taskId", task.id().toString(),
                        "title", task.title(),
                        "assigneeName", employeeNames.getOrDefault(task.assigneeId(), "Chưa rõ"),
                        "deadline", task.deadline().toString(),
                        "progressPercent", task.progressPercent(),
                        "status", task.status().name(),
                        "priority", task.priority().name(),
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

    public Map<String, Object> analyzeTaskDescription(Map<String, Object> payload) {
        return post("/internal/ai/tasks/analyze", payload, Map.class);
    }

    public Map<String, Object> estimateHours(Map<String, Object> payload) {
        return post("/internal/ai/tasks/estimate-hours", payload, Map.class);
    }

    public Map<String, Object> explainIndividualRecommendation(Map<String, Object> payload) {
        return post("/internal/ai/recommendations/individual/explain", payload, Map.class);
    }

    public Map<String, Object> explainTeamLeaderRecommendation(Map<String, Object> payload) {
        return post("/internal/ai/recommendations/team-leader/explain", payload, Map.class);
    }

    public Map<String, Object> explainTeamMemberRecommendation(Map<String, Object> payload) {
        return post("/internal/ai/recommendations/team-member/explain", payload, Map.class);
    }

    public Map<String, Object> explainRecommendationResult(Map<String, Object> payload) {
        return post("/internal/ai/recommendations/result/explain", payload, Map.class);
    }

    public Map<String, Object> workloadRisk(Map<String, Object> payload) {
        return post("/internal/ai/workload-risk", payload, Map.class);
    }

    public Map<String, Object> employeeReport(Map<String, Object> payload) {
        return post("/internal/ai/employee-report", payload, Map.class);
    }

    public Map<String, Object> businessOwnerOperationalSummary(Map<String, Object> payload) {
        return post("/internal/ai/business-owner/summary", payload, Map.class);
    }

    public Map<String, Object> platformAdminSystemSummary(Map<String, Object> payload) {
        return post("/internal/ai/platform-admin/summary", payload, Map.class);
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
        String requestKey = requestKey(path, payload);
        CompletableFuture<Object> ownFuture = new CompletableFuture<>();
        CompletableFuture<Object> existingFuture = inFlightRequests.putIfAbsent(requestKey, ownFuture);
        if (existingFuture != null) {
            return waitForInFlight(path, existingFuture, responseType);
        }
        try {
            T response = executePost(path, payload, responseType);
            ownFuture.complete(response);
            return response;
        } catch (RuntimeException exception) {
            ownFuture.completeExceptionally(exception);
            throw exception;
        } finally {
            inFlightRequests.remove(requestKey, ownFuture);
        }
    }

    private <T> T executePost(String path, Object payload, Class<T> responseType) {
        assertCircuitClosed(path);
        acquireAiSlot(path);
        long startedAt = System.nanoTime();
        try {
            T response = restClient.post()
                    .uri(properties.serviceUrl() + path)
                    .header("X-Internal-Service-Token", properties.serviceToken())
                    .body(payload)
                    .retrieve()
                    .body(responseType);
            consecutiveProviderFailures.set(0);
            log.info("AI service call succeeded path={} elapsedMs={} activeRequests={}", path, elapsedMillis(startedAt), activeRequests());
            return response;
        } catch (RestClientException exception) {
            recordProviderFailure(path);
            log.warn(
                    "AI service call failed path={} elapsedMs={} exception={} message={} activeRequests={}",
                    path,
                    elapsedMillis(startedAt),
                    exception.getClass().getSimpleName(),
                    shortMessage(exception),
                    activeRequests()
            );
            throw new AiProviderException("AI service did not return a successful response.", exception);
        } finally {
            concurrencyLimiter.release();
        }
    }

    private <T> T waitForInFlight(String path, CompletableFuture<Object> existingFuture, Class<T> responseType) {
        long startedAt = System.nanoTime();
        try {
            Object response = existingFuture.get(properties.effectiveDedupeWaitMillis(), TimeUnit.MILLISECONDS);
            log.info("AI duplicate request reused path={} waitMs={}", path, elapsedMillis(startedAt));
            return responseType.cast(response);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiRateLimitException("Interrupted while waiting for in-flight AI request.", properties.effectiveRetryAfterSeconds(), exception);
        } catch (TimeoutException exception) {
            throw new AiRateLimitException("Timed out waiting for in-flight AI request.", properties.effectiveRetryAfterSeconds(), exception);
        } catch (ExecutionException exception) {
            throw propagateAiException(exception.getCause());
        }
    }

    private void acquireAiSlot(String path) {
        try {
            boolean acquired = concurrencyLimiter.tryAcquire(properties.effectiveAcquireTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn(
                        "AI concurrency limit reached path={} maxConcurrent={} activeRequests={}",
                        path,
                        properties.effectiveMaxConcurrentRequests(),
                        activeRequests()
                );
                throw new AiRateLimitException("Too many AI requests are already running.", properties.effectiveRetryAfterSeconds());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiRateLimitException("Interrupted while waiting for AI capacity.", properties.effectiveRetryAfterSeconds(), exception);
        }
    }

    private void assertCircuitClosed(String path) {
        long openUntil = circuitOpenUntilMillis.get();
        long remainingMillis = openUntil - System.currentTimeMillis();
        if (remainingMillis > 0) {
            log.warn("AI circuit breaker open path={} retryAfterMs={}", path, remainingMillis);
            throw new AiProviderException("AI circuit breaker is open. Retry later.");
        }
        if (openUntil > 0) {
            circuitOpenUntilMillis.compareAndSet(openUntil, 0);
        }
    }

    private void recordProviderFailure(String path) {
        int threshold = properties.effectiveCircuitBreakerFailureThreshold();
        int openMillis = properties.effectiveCircuitBreakerOpenMillis();
        if (threshold <= 0 || openMillis <= 0) {
            return;
        }
        int failures = consecutiveProviderFailures.incrementAndGet();
        if (failures >= threshold) {
            long openUntil = System.currentTimeMillis() + openMillis;
            circuitOpenUntilMillis.set(openUntil);
            consecutiveProviderFailures.set(0);
            log.warn("AI circuit breaker opened path={} openMillis={} failureThreshold={}", path, openMillis, threshold);
        }
    }

    private RuntimeException propagateAiException(Throwable cause) {
        if (cause instanceof AiProviderException exception) {
            return exception;
        }
        if (cause instanceof RuntimeException exception) {
            return exception;
        }
        return new AiProviderException("AI service call failed.", cause);
    }

    private String requestKey(String path, Object payload) {
        try {
            return path + ":" + objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return path + ":" + String.valueOf(payload);
        }
    }

    private int activeRequests() {
        return properties.effectiveMaxConcurrentRequests() - concurrencyLimiter.availablePermits();
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private String shortMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null) return "";
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    public record AiRecommendAssigneeInput(
            String title,
            String requirements,
            String deadline,
            double estimatedHours,
            UUID departmentId,
            UUID requiredJobPositionId,
            List<AiEmployeeWorkload> employees
    ) {
    }

    public record AiEmployeeWorkload(
            String employeeId,
            String fullName,
            long openTasks,
            long completedTasks,
            long overdueTasks,
            long blockedTasks,
            double estimatedWorkload,
            WorkloadLevel workloadLevel,
            String status,
            String jobTitle,
            SeniorityLevel seniorityLevel,
            Integer skillRating,
            Integer yearsOfExperience,
            String skills,
            UUID departmentId,
            UUID businessPositionId,
            String businessPositionName,
            PermissionGroup permissionGroup,
            int candidateScore,
            int leadershipScore,
            int teamMemberScore,
            long leadTaskCount,
            long leadCompletedTasks,
            double leadCompletionRate,
            int similarTaskCount,
            int domainMatchScore,
            Map<String, Object> scoreComponents
    ) {
        public static AiEmployeeWorkload from(WorkloadView item) {
            return new AiEmployeeWorkload(
                    item.employeeId().toString(),
                    item.fullName(),
                    item.openTasks(),
                    item.completedTasks(),
                    item.overdueTasks(),
                    item.blockedTasks(),
                    item.estimatedWorkload().doubleValue(),
                    item.workloadLevel(),
                    "ACTIVE",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    Map.of()
            );
        }
    }

    record AiRecommendAssigneeResponse(List<AiRecommendation> recommendations) {
    }

    record AiRecommendation(UUID employeeId, String fullName, int score, WorkloadLevel workloadLevel, String requiredRole, String roleFit, String roleFitReason, String reason, String risk) {
    }
}
