package com.forep.exe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.ai.AiProviderException;
import com.forep.exe.ai.AiServiceClient;
import com.forep.exe.ai.AiServiceClient.AiEmployeeWorkload;
import com.forep.exe.ai.AiServiceClient.AiRecommendAssigneeInput;
import com.forep.exe.domain.Enums.AiSuggestionStatus;
import com.forep.exe.domain.Enums.AiSuggestionType;
import com.forep.exe.domain.Enums.Role;
import com.forep.exe.domain.Enums.SeniorityLevel;
import com.forep.exe.domain.Enums.TaskPriority;
import com.forep.exe.domain.Enums.TaskStatus;
import com.forep.exe.domain.Enums.UpdateType;
import com.forep.exe.domain.Enums.UserStatus;
import com.forep.exe.domain.Enums.WorkloadLevel;
import com.forep.exe.dto.Requests.AssignTaskRequest;
import com.forep.exe.dto.Requests.CreateEmployeeRequest;
import com.forep.exe.dto.Requests.CreateTaskRequest;
import com.forep.exe.dto.Requests.DailyReportRequest;
import com.forep.exe.dto.Requests.ExtractTasksRequest;
import com.forep.exe.dto.Requests.LoginRequest;
import com.forep.exe.dto.Requests.RecommendAssigneeRequest;
import com.forep.exe.dto.Requests.RegisterWorkspaceRequest;
import com.forep.exe.dto.Requests.UpdateEmployeeRequest;
import com.forep.exe.dto.Requests.UpdateProgressRequest;
import com.forep.exe.dto.Requests.UpdateTaskRequest;
import com.forep.exe.dto.Requests.UpdateTaskStatusRequest;
import com.forep.exe.dto.Requests.UpdateWorkspaceRequest;
import com.forep.exe.persistence.AiSuggestionEntity;
import com.forep.exe.persistence.AiSuggestionRepository;
import com.forep.exe.persistence.DailyReportEntity;
import com.forep.exe.persistence.DailyReportRepository;
import com.forep.exe.persistence.NotificationEntity;
import com.forep.exe.persistence.NotificationRepository;
import com.forep.exe.persistence.TaskEntity;
import com.forep.exe.persistence.TaskRepository;
import com.forep.exe.persistence.TaskUpdateEntity;
import com.forep.exe.persistence.TaskUpdateRepository;
import com.forep.exe.persistence.UserEntity;
import com.forep.exe.persistence.UserRepository;
import com.forep.exe.persistence.WorkspaceEntity;
import com.forep.exe.persistence.WorkspaceRepository;
import com.forep.exe.security.AuthenticatedUser;
import com.forep.exe.security.JwtService;
import com.forep.exe.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ForepService {
    private static final Logger log = LoggerFactory.getLogger(ForepService.class);
    private static final String RULE_BASED_FALLBACK_SOURCE = "RULE_BASED_FALLBACK";

    private final WorkspaceRepository workspaces;
    private final UserRepository users;
    private final TaskRepository tasks;
    private final TaskUpdateRepository taskUpdates;
    private final DailyReportRepository reports;
    private final NotificationRepository notifications;
    private final AiSuggestionRepository aiSuggestions;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityContext securityContext;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    public ForepService(WorkspaceRepository workspaces,
                        UserRepository users,
                        TaskRepository tasks,
                        TaskUpdateRepository taskUpdates,
                        DailyReportRepository reports,
                        NotificationRepository notifications,
                        AiSuggestionRepository aiSuggestions,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        SecurityContext securityContext,
                        AiServiceClient aiServiceClient,
                        ObjectMapper objectMapper) {
        this.workspaces = workspaces;
        this.users = users;
        this.tasks = tasks;
        this.taskUpdates = taskUpdates;
        this.reports = reports;
        this.notifications = notifications;
        this.aiSuggestions = aiSuggestions;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityContext = securityContext;
        this.aiServiceClient = aiServiceClient;
        this.objectMapper = objectMapper;
    }

    public LoginView login(LoginRequest request) {
        String identifier = loginIdentifier(request);
        UserEntity user = identifier.contains("@")
                ? users.findFirstByEmailIgnoreCase(identifier).orElseThrow(() -> new IllegalArgumentException("Tài khoản hoặc mật khẩu không đúng."))
                : users.findFirstByUsernameIgnoreCase(identifier).orElseThrow(() -> new IllegalArgumentException("Tài khoản hoặc mật khẩu không đúng."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()) || user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không đúng.");
        }
        String token = jwtService.issue(new AuthenticatedUser(user.getId(), user.getWorkspaceId(), user.getRole(), user.getEmail()));
        return new LoginView(token, toUserView(user));
    }

    public WorkspaceView registerWorkspace(RegisterWorkspaceRequest request) {
        String shortCode = normalizeShortCode(request.shortCode());
        if (users.findFirstByEmailIgnoreCase(request.ownerEmail()).isPresent()) {
            throw new IllegalArgumentException("Email owner đã tồn tại.");
        }
        if (workspaces.findByShortCodeIgnoreCase(shortCode).isPresent()) {
            throw new IllegalArgumentException("Mã viết tắt tổ chức đã tồn tại.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setName(request.workspaceName());
        workspace.setShortCode(shortCode);
        workspace.setNextEmployeeNumber(1);
        workspace.setAddress(request.address());
        workspace.setCreatedAt(now);
        workspace = workspaces.save(workspace);

        UserEntity owner = new UserEntity();
        owner.setWorkspaceId(workspace.getId());
        owner.setFullName(request.ownerFullName());
        owner.setEmail(request.ownerEmail());
        owner.setPhone(request.ownerPhone());
        owner.setPasswordHash(passwordEncoder.encode(request.ownerPassword()));
        owner.setRole(Role.OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        owner.setCreatedAt(now);
        owner.setUpdatedAt(now);
        owner = users.save(owner);

        workspace.setOwnerId(owner.getId());
        return toWorkspaceView(workspaces.save(workspace));
    }

    public UserView me() {
        return toUserView(currentUserEntity());
    }

    public WorkspaceView currentWorkspace() {
        return toWorkspaceView(requireWorkspace(currentUser().workspaceId()));
    }

    public WorkspaceView updateWorkspace(UpdateWorkspaceRequest request) {
        requireOwner();
        WorkspaceEntity workspace = requireWorkspace(currentUser().workspaceId());
        if (request.name() != null && !request.name().isBlank()) workspace.setName(request.name());
        if (request.shortCode() != null && !request.shortCode().isBlank()) {
            String shortCode = normalizeShortCode(request.shortCode());
            if (workspace.getShortCode() != null && !workspace.getShortCode().equals(shortCode)) {
                throw new IllegalArgumentException("Không thể đổi mã tổ chức sau khi đã cấu hình.");
            }
            workspaces.findByShortCodeIgnoreCase(shortCode)
                    .filter(existing -> !existing.getId().equals(workspace.getId()))
                    .ifPresent(existing -> { throw new IllegalArgumentException("Mã viết tắt tổ chức đã tồn tại."); });
            workspace.setShortCode(shortCode);
        }
        if (request.logo() != null) workspace.setLogo(request.logo());
        if (request.address() != null) workspace.setAddress(request.address());
        return toWorkspaceView(workspaces.save(workspace));
    }

    public List<UserView> employees() {
        requireOwner();
        return users.findByWorkspaceIdAndRoleOrderByFullNameAsc(currentUser().workspaceId(), Role.EMPLOYEE).stream().map(this::toUserView).toList();
    }

    public UserView createEmployee(CreateEmployeeRequest request) {
        requireOwner();
        UUID workspaceId = currentUser().workspaceId();
        if (users.existsByWorkspaceIdAndEmailIgnoreCase(workspaceId, request.email())) {
            throw new IllegalArgumentException("Email đã tồn tại trong workspace.");
        }
        WorkspaceEntity workspace = requireWorkspace(workspaceId);
        String employeeCode = nextEmployeeCode(workspace);
        String username = buildUsername(request.fullName(), employeeCode);
        String initialPassword = employeeCode;
        OffsetDateTime now = OffsetDateTime.now();
        UserEntity employee = new UserEntity();
        employee.setWorkspaceId(workspaceId);
        employee.setFullName(request.fullName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setUsername(username);
        employee.setEmployeeCode(employeeCode);
        employee.setInitialPassword(initialPassword);
        employee.setJobTitle(request.jobTitle());
        employee.setSeniorityLevel(request.seniorityLevel());
        employee.setSkillRating(request.skillRating());
        employee.setYearsOfExperience(request.yearsOfExperience());
        employee.setSkills(request.skills());
        employee.setPasswordHash(passwordEncoder.encode(initialPassword));
        employee.setRole(Role.EMPLOYEE);
        employee.setStatus(UserStatus.ACTIVE);
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        UserView created = toUserView(users.save(employee));
        workspace.setNextEmployeeNumber(workspace.getNextEmployeeNumber() + 1);
        workspaces.save(workspace);
        return created;
    }

    public UserView employee(UUID employeeId) {
        requireOwner();
        UserEntity employee = requireEmployee(employeeId);
        return toUserView(employee);
    }

    public UserView updateEmployee(UUID employeeId, UpdateEmployeeRequest request) {
        requireOwner();
        UserEntity employee = requireEmployee(employeeId);
        employee.setFullName(request.fullName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        if (request.status() != null) employee.setStatus(request.status());
        employee.setJobTitle(request.jobTitle());
        employee.setSeniorityLevel(request.seniorityLevel());
        employee.setSkillRating(request.skillRating());
        employee.setYearsOfExperience(request.yearsOfExperience());
        employee.setSkills(request.skills());
        employee.setUpdatedAt(OffsetDateTime.now());
        return toUserView(users.save(employee));
    }

    public UserView updateEmployeeStatus(UUID employeeId, UserStatus status) {
        requireOwner();
        UserEntity employee = requireEmployee(employeeId);
        employee.setStatus(status);
        employee.setUpdatedAt(OffsetDateTime.now());
        return toUserView(users.save(employee));
    }

    public List<TaskView> tasks() {
        AuthenticatedUser user = currentUser();
        List<TaskEntity> scoped = user.role() == Role.OWNER
                ? tasks.findByWorkspaceIdOrderByCreatedAtDesc(user.workspaceId())
                : tasks.findByWorkspaceIdAndAssigneeIdOrderByCreatedAtDesc(user.workspaceId(), user.userId());
        return scoped.stream().map(this::toTaskView).toList();
    }

    public TaskView createTask(CreateTaskRequest request) {
        requireOwner();
        UUID workspaceId = currentUser().workspaceId();
        requireActiveEmployee(request.assigneeId());
        OffsetDateTime now = OffsetDateTime.now();
        TaskEntity task = new TaskEntity();
        task.setWorkspaceId(workspaceId);
        task.setTitle(request.title());
        task.setRequirements(request.requirements());
        task.setDescription(request.description());
        task.setAssigneeId(request.assigneeId());
        task.setCreatorId(currentUser().userId());
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setDeadline(request.deadline());
        task.setEstimatedHours(request.estimatedHours() == null ? BigDecimal.ZERO : request.estimatedHours());
        task.setProgressPercent(0);
        task.setStatus(TaskStatus.ASSIGNED);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task = tasks.save(task);
        createNotification(workspaceId, task.getAssigneeId(), "TASK_ASSIGNED", "Task mới được giao", "Bạn vừa được giao task: " + task.getTitle(), "TASK", task.getId());
        return toTaskView(task);
    }

    public TaskView task(UUID taskId) {
        TaskEntity task = requireTask(taskId);
        requireTaskVisible(task);
        return toTaskView(task);
    }

    public TaskView updateTask(UUID taskId, UpdateTaskRequest request) {
        requireOwner();
        TaskEntity task = requireTask(taskId);
        requireActiveEmployee(request.assigneeId());
        task.setTitle(request.title());
        task.setRequirements(request.requirements());
        task.setDescription(request.description());
        task.setAssigneeId(request.assigneeId());
        task.setPriority(request.priority() == null ? task.getPriority() : request.priority());
        task.setDeadline(request.deadline());
        task.setEstimatedHours(request.estimatedHours() == null ? task.getEstimatedHours() : request.estimatedHours());
        task.setUpdatedAt(OffsetDateTime.now());
        return toTaskView(tasks.save(task));
    }

    public TaskView assignTask(UUID taskId, AssignTaskRequest request) {
        requireOwner();
        requireActiveEmployee(request.assigneeId());
        TaskEntity task = requireTask(taskId);
        task.setAssigneeId(request.assigneeId());
        task.setStatus(TaskStatus.ASSIGNED);
        task.setUpdatedAt(OffsetDateTime.now());
        task = tasks.save(task);
        createNotification(task.getWorkspaceId(), request.assigneeId(), "TASK_ASSIGNED", "Task được giao lại", "Bạn vừa được giao task: " + task.getTitle(), "TASK", task.getId());
        return toTaskView(task);
    }

    public TaskView updateStatus(UUID taskId, UpdateTaskStatusRequest request) {
        TaskEntity task = requireTask(taskId);
        requireOwnerOrAssignee(task);
        applyTaskStatus(task, request.status(), task.getProgressPercent());
        return toTaskView(tasks.save(task));
    }

    public TaskView cancelTask(UUID taskId) {
        requireOwner();
        TaskEntity task = requireTask(taskId);
        applyTaskStatus(task, TaskStatus.CANCELLED, task.getProgressPercent());
        task = tasks.save(task);
        createNotification(task.getWorkspaceId(), task.getAssigneeId(), "TASK_CANCELLED", "Task đã bị hủy", task.getTitle() + " đã bị hủy.", "TASK", task.getId());
        return toTaskView(task);
    }

    public TaskUpdateView updateProgress(UUID taskId, UpdateProgressRequest request) {
        TaskEntity task = requireTask(taskId);
        requireOwnerOrAssignee(task);
        if (request.updateType() == UpdateType.BLOCKER && request.content().isBlank()) {
            throw new IllegalArgumentException("Task bị blocker phải có nội dung cập nhật.");
        }
        int nextProgress = request.updateType() == UpdateType.COMPLETION ? 100 : request.progressPercent();
        TaskStatus nextStatus = switch (request.updateType()) {
            case BLOCKER -> TaskStatus.BLOCKED;
            case COMPLETION -> TaskStatus.COMPLETED;
            case PROGRESS -> nextProgress >= 100 ? TaskStatus.COMPLETED : TaskStatus.IN_PROGRESS;
        };
        applyTaskStatus(task, nextStatus, nextProgress);
        tasks.save(task);

        TaskUpdateEntity update = new TaskUpdateEntity();
        update.setTaskId(task.getId());
        update.setUserId(currentUser().userId());
        update.setProgressPercent(task.getProgressPercent());
        update.setContent(request.content());
        update.setAttachment(request.attachment());
        update.setUpdateType(request.updateType());
        update.setCreatedAt(OffsetDateTime.now());
        update = taskUpdates.save(update);
        if (request.updateType() == UpdateType.BLOCKER) {
            createNotification(task.getWorkspaceId(), task.getCreatorId(), "TASK_BLOCKED", "Task có vướng mắc", task.getTitle() + " vừa được báo vướng mắc.", "TASK", task.getId());
        }
        return toTaskUpdateView(update);
    }

    public List<TaskUpdateView> taskUpdates(UUID taskId) {
        TaskEntity task = requireTask(taskId);
        requireTaskVisible(task);
        return taskUpdates.findByTaskIdOrderByCreatedAtAsc(taskId).stream().map(this::toTaskUpdateView).toList();
    }

    public DailyReportView createReport(DailyReportRequest request) {
        AuthenticatedUser user = currentUser();
        reports.findByWorkspaceIdAndUserIdAndReportDate(user.workspaceId(), user.userId(), request.reportDate())
                .ifPresent(existing -> { throw new IllegalArgumentException("Bạn đã gửi báo cáo cho ngày này."); });
        OffsetDateTime now = OffsetDateTime.now();
        DailyReportEntity report = new DailyReportEntity();
        report.setWorkspaceId(user.workspaceId());
        report.setUserId(user.userId());
        report.setReportDate(request.reportDate());
        report.setTodayCompleted(request.todayCompleted());
        report.setCurrentWork(request.currentWork());
        report.setBlockers(request.blockers());
        report.setTomorrowPlan(request.tomorrowPlan());
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        return toDailyReportView(reports.save(report));
    }

    public List<DailyReportView> reports() {
        AuthenticatedUser user = currentUser();
        List<DailyReportEntity> scoped = user.role() == Role.OWNER
                ? reports.findByWorkspaceIdOrderByReportDateDesc(user.workspaceId())
                : reports.findByWorkspaceIdAndUserIdOrderByReportDateDesc(user.workspaceId(), user.userId());
        return scoped.stream().map(this::toDailyReportView).toList();
    }

    public DailyReportView report(UUID reportId) {
        DailyReportEntity report = requireReport(reportId);
        if (currentUser().role() != Role.OWNER && !report.getUserId().equals(currentUser().userId())) {
            throw new IllegalArgumentException("Bạn không có quyền xem báo cáo này.");
        }
        return toDailyReportView(report);
    }

    public DailyReportView reviewReport(UUID reportId) {
        requireOwner();
        DailyReportEntity report = requireReport(reportId);
        report.setReviewedAt(OffsetDateTime.now());
        report.setUpdatedAt(OffsetDateTime.now());
        return toDailyReportView(reports.save(report));
    }

    public OwnerDashboardView ownerDashboard() {
        requireOwner();
        List<TaskEntity> scopedTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(currentUser().workspaceId());
        long total = scopedTasks.size();
        long active = scopedTasks.stream().filter(task -> List.of(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED).contains(task.getStatus())).count();
        long completed = scopedTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        long overdue = scopedTasks.stream().filter(this::isOverdue).count();
        List<WorkloadView> currentWorkload = workload();
        return new OwnerDashboardView(total, active, completed, overdue, currentWorkload, scopedTasks.stream().limit(5).map(this::toTaskView).toList(), cachedDashboardRecommendations());
    }

    public WorkloadView employeeWorkload(UUID employeeId) {
        requireOwner();
        UserEntity employee = requireEmployee(employeeId);
        return workloadForEmployee(employee, tasks.findByWorkspaceIdOrderByCreatedAtDesc(currentUser().workspaceId()));
    }

    public List<WorkloadView> workload() {
        requireOwner();
        UUID workspaceId = currentUser().workspaceId();
        List<TaskEntity> scopedTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        return users.findByWorkspaceIdAndRoleOrderByFullNameAsc(workspaceId, Role.EMPLOYEE).stream()
                .map(employee -> workloadForEmployee(employee, scopedTasks))
                .sorted(Comparator.comparing(WorkloadView::workloadScore).reversed())
                .toList();
    }

    public List<AssigneeRecommendationView> recommendAssignee(RecommendAssigneeRequest request) {
        requireOwner();
        if (request == null) return List.of();
        List<AiEmployeeWorkload> candidates = assigneeCandidates(request);
        if (candidates.isEmpty()) return List.of();
        List<AssigneeRecommendationView> normalized;
        try {
            List<AssigneeRecommendationView> recommendations = aiServiceClient.recommendAssignee(new AiRecommendAssigneeInput(
                    request.title(),
                    request.requirements(),
                    request.deadline().toString(),
                    request.estimatedHours() == null ? 0 : request.estimatedHours().doubleValue(),
                    candidates
            ));
            normalized = normalizeRecommendations(recommendations, candidates);
            if (normalized.isEmpty()) {
                log.warn("AI recommend assignee returned no valid candidates; using rule-based fallback.");
                normalized = fallbackAssigneeRecommendations(candidates, "AI returned no valid recommendations.");
            }
        } catch (AiProviderException exception) {
            log.warn("AI recommend assignee failed; using rule-based fallback. message={}", fallbackReason(exception));
            normalized = fallbackAssigneeRecommendations(candidates, fallbackReason(exception));
        }
        saveAiSuggestion(AiSuggestionType.ASSIGNEE_RECOMMENDATION, request, normalized);
        return normalized;
    }

    public Map<String, Object> workloadSummary() {
        requireOwner();
        List<WorkloadView> currentWorkload = workload();
        Map<String, Object> output;
        try {
            output = aiServiceClient.workloadSummary(currentWorkload);
        } catch (AiProviderException exception) {
            log.warn("AI workload summary failed; using rule-based fallback. message={}", fallbackReason(exception));
            output = fallbackWorkloadSummary(currentWorkload, exception);
        }
        saveAiSuggestion(AiSuggestionType.WORKLOAD_SUMMARY, currentWorkload, output);
        return output;
    }

    public Map<String, Object> delayRisks() {
        requireOwner();
        List<TaskEntity> scopedTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(currentUser().workspaceId());
        Map<UUID, String> employeeNames = users.findByWorkspaceId(currentUser().workspaceId()).stream().collect(java.util.stream.Collectors.toMap(UserEntity::getId, UserEntity::getFullName));
        List<TaskView> taskViews = scopedTasks.stream().map(this::toTaskView).toList();
        try {
            return aiServiceClient.delayRisks(taskViews, employeeNames);
        } catch (AiProviderException exception) {
            log.warn("AI delay risks failed; using rule-based fallback. message={}", fallbackReason(exception));
            return fallbackDelayRisks(scopedTasks, employeeNames, exception);
        }
    }

    public Map<String, Object> businessSummary(String period) {
        requireOwner();
        Map<String, Object> payload = businessSummaryPayload(period);
        Map<String, Object> output = aiServiceClient.businessSummary(payload);
        saveAiSuggestion(AiSuggestionType.BUSINESS_SUMMARY, payload, output);
        return output;
    }

    public Map<String, Object> dailyAiSummary() {
        return businessSummary("daily");
    }

    public Map<String, Object> dailyReportInsights() {
        requireOwner();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reports", dailyReportInsightPayloads(LocalDate.now().minusDays(6)));
        Map<String, Object> output = aiServiceClient.dailyReportInsights(payload);
        saveAiSuggestion(AiSuggestionType.DAILY_REPORT_INSIGHTS, payload, output);
        return output;
    }

    public Map<String, Object> extractTasks(ExtractTasksRequest request) {
        requireOwner();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", request.text());
        payload.put("defaultDeadline", request.defaultDeadline() == null ? null : request.defaultDeadline().toString());
        payload.put("employees", assigneeCandidates());
        Map<String, Object> output = aiServiceClient.extractTasks(payload);
        saveAiSuggestion(AiSuggestionType.TASK_EXTRACTION, payload, output);
        return output;
    }

    public Map<String, Object> splitTask(UUID taskId) {
        requireOwner();
        TaskEntity task = requireTask(taskId);
        Map<String, Object> payload = Map.of("task", aiTaskPayload(task, employeeNames()));
        Map<String, Object> output = aiServiceClient.splitTask(payload);
        saveAiSuggestion(AiSuggestionType.TASK_SPLIT, payload, output);
        return output;
    }

    public Map<String, Object> taskAdjustment(UUID taskId) {
        requireOwner();
        TaskEntity task = requireTask(taskId);
        Map<String, Object> payload = Map.of("task", aiTaskPayload(task, employeeNames()));
        Map<String, Object> output = aiServiceClient.taskAdjustment(payload);
        saveAiSuggestion(AiSuggestionType.TASK_ADJUSTMENT, payload, output);
        return output;
    }

    public Map<String, Object> missingReports() {
        requireOwner();
        LocalDate reportDate = LocalDate.now();
        Map<UUID, String> names = employeeNames();
        List<UUID> submittedEmployeeIds = reports.findByWorkspaceIdOrderByReportDateDesc(currentUser().workspaceId()).stream()
                .filter(report -> report.getReportDate().equals(reportDate))
                .map(DailyReportEntity::getUserId)
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reportDate", reportDate.toString());
        payload.put("employees", users.findByWorkspaceIdAndRoleOrderByFullNameAsc(currentUser().workspaceId(), Role.EMPLOYEE).stream()
                .filter(employee -> employee.getStatus() == UserStatus.ACTIVE)
                .filter(employee -> !submittedEmployeeIds.contains(employee.getId()))
                .map(employee -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("employeeId", employee.getId().toString());
                    item.put("fullName", employee.getFullName());
                    item.put("status", employee.getStatus().name());
                    return item;
                })
                .toList());
        payload.put("reports", reports.findByWorkspaceIdOrderByReportDateDesc(currentUser().workspaceId()).stream()
                .filter(report -> report.getReportDate().equals(reportDate))
                .map(report -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("reportId", report.getId().toString());
                    item.put("userId", report.getUserId().toString());
                    item.put("userName", names.getOrDefault(report.getUserId(), "Unknown"));
                    item.put("reportDate", report.getReportDate().toString());
                    return item;
                })
                .toList());
        Map<String, Object> output = aiServiceClient.missingReports(payload);
        saveAiSuggestion(AiSuggestionType.MISSING_REPORT, payload, output);
        return output;
    }

    public Map<String, Object> actionSuggestions() {
        requireOwner();
        List<WorkloadView> currentWorkload = workload();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tasks", taskPayloads(LocalDate.now().minusDays(30)));
        payload.put("reports", reportPayloads(LocalDate.now().minusDays(6)));
        payload.put("workload", currentWorkload.stream().map(AiEmployeeWorkload::from).toList());
        Map<String, Object> output;
        try {
            output = aiServiceClient.actionSuggestions(payload);
        } catch (AiProviderException exception) {
            log.warn("AI action suggestions failed; using rule-based fallback. message={}", fallbackReason(exception));
            output = fallbackActionSuggestions(currentWorkload, exception);
        }
        saveAiSuggestion(AiSuggestionType.ACTION_SUGGESTION, payload, output);
        return output;
    }

    public List<NotificationView> notifications() {
        generateOperationalNotifications();
        AuthenticatedUser user = currentUser();
        List<NotificationEntity> scoped = user.role() == Role.OWNER
                ? notifications.findByWorkspaceIdOrderByCreatedAtDesc(user.workspaceId())
                : notifications.findByWorkspaceIdAndUserIdOrderByCreatedAtDesc(user.workspaceId(), user.userId());
        return scoped.stream().map(this::toNotificationView).toList();
    }

    public NotificationView readNotification(UUID notificationId) {
        NotificationEntity notification = notifications.findById(notificationId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo."));
        if (!notification.getWorkspaceId().equals(currentUser().workspaceId()) || (currentUser().role() != Role.OWNER && !notification.getUserId().equals(currentUser().userId()))) {
            throw new IllegalArgumentException("Bạn không có quyền cập nhật thông báo này.");
        }
        notification.setRead(true);
        return toNotificationView(notifications.save(notification));
    }

    public List<NotificationView> readAllNotifications() {
        List<NotificationEntity> scoped = currentUser().role() == Role.OWNER
                ? notifications.findByWorkspaceIdOrderByCreatedAtDesc(currentUser().workspaceId())
                : notifications.findByWorkspaceIdAndUserIdOrderByCreatedAtDesc(currentUser().workspaceId(), currentUser().userId());
        scoped.forEach(notification -> notification.setRead(true));
        notifications.saveAll(scoped);
        return notifications();
    }

    public List<AiSuggestionView> aiSuggestions() {
        requireOwner();
        return aiSuggestions.findByWorkspaceIdOrderByCreatedAtDesc(currentUser().workspaceId()).stream().map(this::toAiSuggestionView).toList();
    }

    public AiSuggestionView updateAiSuggestionStatus(UUID suggestionId, AiSuggestionStatus status) {
        requireOwner();
        AiSuggestionEntity suggestion = aiSuggestions.findById(suggestionId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy AI suggestion."));
        if (!suggestion.getWorkspaceId().equals(currentUser().workspaceId())) throw new IllegalArgumentException("AI suggestion không thuộc workspace hiện tại.");
        suggestion.setStatus(status);
        return toAiSuggestionView(aiSuggestions.save(suggestion));
    }

    private List<DashboardAiRecommendationView> cachedDashboardRecommendations() {
        return aiSuggestions.findByWorkspaceIdOrderByCreatedAtDesc(currentUser().workspaceId()).stream()
                .filter(suggestion -> suggestion.getStatus() == AiSuggestionStatus.GENERATED)
                .filter(suggestion -> List.of(
                        AiSuggestionType.ASSIGNEE_RECOMMENDATION,
                        AiSuggestionType.ACTION_SUGGESTION,
                        AiSuggestionType.MISSING_REPORT,
                        AiSuggestionType.TASK_ADJUSTMENT,
                        AiSuggestionType.DAILY_REPORT_INSIGHTS,
                        AiSuggestionType.BUSINESS_SUMMARY
                ).contains(suggestion.getType()))
                .limit(5)
                .map(suggestion -> new DashboardAiRecommendationView(
                        suggestion.getId(),
                        suggestion.getType(),
                        "CACHE",
                        suggestion.getOutputData(),
                        suggestion.getCreatedAt()
                ))
                .toList();
    }

    private List<AiEmployeeWorkload> assigneeCandidates() {
        return assigneeCandidates(null);
    }

    private List<AiEmployeeWorkload> assigneeCandidates(RecommendAssigneeRequest request) {
        UUID workspaceId = currentUser().workspaceId();
        List<TaskEntity> scopedTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        return users.findByWorkspaceIdAndRoleOrderByFullNameAsc(workspaceId, Role.EMPLOYEE).stream()
                .filter(employee -> employee.getStatus() == UserStatus.ACTIVE)
                .map(employee -> {
                    WorkloadView workload = workloadForEmployee(employee, scopedTasks);
                    Map<String, Object> scoreComponents = scoreComponents(workload, employee, request);
                    return new AiEmployeeWorkload(
                            workload.employeeId().toString(),
                            workload.fullName(),
                            workload.openTasks(),
                            workload.overdueTasks(),
                            workload.blockedTasks(),
                            workload.estimatedWorkload().doubleValue(),
                            workload.workloadLevel(),
                            employee.getStatus().name(),
                            employee.getJobTitle(),
                            employee.getSeniorityLevel(),
                            employee.getSkillRating(),
                            employee.getYearsOfExperience(),
                            employee.getSkills(),
                            (int) scoreComponents.get("candidateScore"),
                            scoreComponents
                    );
                })
                .sorted(Comparator.comparing(AiEmployeeWorkload::candidateScore).reversed())
                .limit(10)
                .toList();
    }

    private Map<String, Object> scoreComponents(WorkloadView workload, UserEntity employee, RecommendAssigneeRequest request) {
        int levelPenalty = switch (workload.workloadLevel()) {
            case NO_WORK -> 0;
            case LOW -> 4;
            case NORMAL -> 12;
            case HIGH -> 28;
            case OVERLOADED -> 50;
        };
        int seniorityPenalty = seniorityPenalty(employee.getSeniorityLevel());
        int skillRatingPenalty = skillRatingPenalty(employee.getSkillRating());
        int experiencePenalty = experiencePenalty(employee.getYearsOfExperience());
        int profilePenalty = seniorityPenalty + skillRatingPenalty + experiencePenalty;
        int taskProfileMatchScore = taskProfileMatchScore(request, employee);
        double openPenalty = workload.openTasks() * 6.0;
        double overduePenalty = workload.overdueTasks() * 18.0;
        double blockedPenalty = workload.blockedTasks() * 12.0;
        double workloadPenalty = workload.estimatedWorkload().doubleValue() / 2.0;
        int candidateScore = (int) Math.max(0, Math.min(100, Math.round(100 - openPenalty - overduePenalty - blockedPenalty - workloadPenalty - levelPenalty - profilePenalty + taskProfileMatchScore)));
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("candidateScore", candidateScore);
        components.put("openTasksPenalty", openPenalty);
        components.put("overdueTasksPenalty", overduePenalty);
        components.put("blockedTasksPenalty", blockedPenalty);
        components.put("estimatedWorkloadPenalty", workloadPenalty);
        components.put("workloadLevelPenalty", levelPenalty);
        components.put("seniorityPenalty", seniorityPenalty);
        components.put("skillRatingPenalty", skillRatingPenalty);
        components.put("experiencePenalty", experiencePenalty);
        components.put("profilePenalty", profilePenalty);
        components.put("taskProfileMatchScore", taskProfileMatchScore);
        return components;
    }

    private int seniorityPenalty(SeniorityLevel seniorityLevel) {
        if (seniorityLevel == null) return 4;
        return switch (seniorityLevel) {
            case LEAD, SENIOR -> 0;
            case MIDDLE -> 4;
            case JUNIOR -> 8;
            case INTERN -> 12;
        };
    }

    private int skillRatingPenalty(Integer skillRating) {
        if (skillRating == null) return 4;
        return switch (Math.max(1, Math.min(5, skillRating))) {
            case 5 -> 0;
            case 4 -> 2;
            case 3 -> 6;
            case 2 -> 12;
            default -> 20;
        };
    }

    private int experiencePenalty(Integer yearsOfExperience) {
        if (yearsOfExperience == null) return 4;
        if (yearsOfExperience >= 5) return 0;
        if (yearsOfExperience >= 3) return 2;
        if (yearsOfExperience >= 1) return 6;
        return 10;
    }

    private int taskProfileMatchScore(RecommendAssigneeRequest request, UserEntity employee) {
        if (request == null) return 0;
        String taskText = ((request.title() == null ? "" : request.title()) + " " + (request.requirements() == null ? "" : request.requirements())).toLowerCase();
        String profileText = ((employee.getJobTitle() == null ? "" : employee.getJobTitle()) + "," + (employee.getSkills() == null ? "" : employee.getSkills())).toLowerCase();
        if (taskText.isBlank() || profileText.isBlank()) return 0;
        long matches = java.util.Arrays.stream(profileText.split("[,;/|\\n]"))
                .map(String::trim)
                .filter(term -> term.length() >= 3)
                .filter(taskText::contains)
                .limit(3)
                .count();
        return (int) matches * 4;
    }

    private List<AssigneeRecommendationView> fallbackAssigneeRecommendations(List<AiEmployeeWorkload> candidates, String fallbackReason) {
        return candidates.stream()
                .sorted(Comparator.comparing(AiEmployeeWorkload::candidateScore).reversed())
                .limit(3)
                .map(candidate -> new AssigneeRecommendationView(
                        UUID.fromString(candidate.employeeId()),
                        candidate.fullName(),
                        candidate.candidateScore(),
                        candidate.workloadLevel(),
                        fallbackAssigneeReason(candidate, fallbackReason),
                        fallbackAssigneeRisk(candidate)
                ))
                .toList();
    }

    private String fallbackAssigneeReason(AiEmployeeWorkload candidate, String fallbackReason) {
        Object taskProfileMatchScore = candidate.scoreComponents().getOrDefault("taskProfileMatchScore", 0);
        return "Rule-based fallback vi LLM chua phan hoi kip (" + fallbackReason + "). "
                + candidate.fullName() + " dat " + candidate.candidateScore()
                + " diem: " + candidate.openTasks() + " task mo, "
                + candidate.overdueTasks() + " qua han, "
                + candidate.blockedTasks() + " bi blocker, workload "
                + candidate.workloadLevel() + ", profile match +" + taskProfileMatchScore + ".";
    }

    private String fallbackAssigneeRisk(AiEmployeeWorkload candidate) {
        if (candidate.workloadLevel() == WorkloadLevel.OVERLOADED) {
            return "Rui ro cao: nhan vien dang qua tai.";
        }
        if (candidate.overdueTasks() > 0) {
            return "Rui ro trung binh: nhan vien co task qua han.";
        }
        if (candidate.blockedTasks() > 0) {
            return "Rui ro trung binh: nhan vien dang co task bi blocker.";
        }
        if (candidate.workloadLevel() == WorkloadLevel.HIGH) {
            return "Rui ro trung binh: workload dang cao.";
        }
        return "Rui ro thap: workload hien tai con kha nang nhan viec.";
    }

    private List<AssigneeRecommendationView> normalizeRecommendations(List<AssigneeRecommendationView> recommendations, List<AiEmployeeWorkload> candidates) {
        Map<UUID, AiEmployeeWorkload> candidateById = candidates.stream()
                .collect(java.util.stream.Collectors.toMap(item -> UUID.fromString(item.employeeId()), item -> item));
        return recommendations.stream()
                .filter(item -> candidateById.containsKey(item.employeeId()))
                .map(item -> {
                    AiEmployeeWorkload candidate = candidateById.get(item.employeeId());
                    return new AssigneeRecommendationView(
                            item.employeeId(),
                            candidate.fullName(),
                            candidate.candidateScore(),
                            candidate.workloadLevel(),
                            item.reason(),
                            item.risk()
                    );
                })
                .sorted(Comparator.comparing(AssigneeRecommendationView::score).reversed())
                .limit(3)
                .toList();
    }

    private Map<String, Object> fallbackWorkloadSummary(List<WorkloadView> currentWorkload, AiProviderException exception) {
        List<String> overloadedEmployees = currentWorkload.stream()
                .filter(item -> item.workloadLevel() == WorkloadLevel.OVERLOADED)
                .map(WorkloadView::fullName)
                .toList();
        List<String> idleEmployees = currentWorkload.stream()
                .filter(item -> item.workloadLevel() == WorkloadLevel.NO_WORK)
                .map(WorkloadView::fullName)
                .toList();
        List<String> overdueEmployees = currentWorkload.stream()
                .filter(item -> item.overdueTasks() > 0)
                .map(WorkloadView::fullName)
                .toList();

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("summary", "Rule-based fallback: " + currentWorkload.size()
                + " nhan vien, " + overloadedEmployees.size()
                + " qua tai, " + idleEmployees.size()
                + " chua co task, " + overdueEmployees.size()
                + " co task qua han.");
        output.put("overloadedEmployees", overloadedEmployees);
        output.put("idleEmployees", idleEmployees);
        output.put("overdueEmployees", overdueEmployees);
        return withFallbackMetadata(output, exception);
    }

    private Map<String, Object> fallbackDelayRisks(List<TaskEntity> scopedTasks, Map<UUID, String> employeeNames, AiProviderException exception) {
        List<Map<String, Object>> risks = new ArrayList<>();
        for (TaskEntity task : scopedTasks.stream()
                .filter(this::isOpenTask)
                .sorted(Comparator.comparingDouble(this::taskRiskScore).reversed())
                .toList()) {
            Map<String, Object> risk = fallbackDelayRisk(task, employeeNames);
            if (risk != null) {
                risks.add(risk);
            }
            if (risks.size() >= 8) {
                break;
            }
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("risks", risks);
        return withFallbackMetadata(output, exception);
    }

    private Map<String, Object> fallbackDelayRisk(TaskEntity task, Map<UUID, String> employeeNames) {
        String riskLevel = fallbackDelayRiskLevel(task);
        if (riskLevel == null) {
            return null;
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("taskId", task.getId().toString());
        item.put("title", task.getTitle());
        item.put("riskLevel", riskLevel);
        item.put("reason", fallbackDelayReason(task));
        item.put("recommendedAction", fallbackDelayAction(task, employeeNames));
        return item;
    }

    private String fallbackDelayRiskLevel(TaskEntity task) {
        long hoursUntilDeadline = Duration.between(OffsetDateTime.now(), task.getDeadline()).toHours();
        if (isOverdue(task) || task.getStatus() == TaskStatus.BLOCKED || (hoursUntilDeadline <= 48 && task.getProgressPercent() < 30)) {
            return "HIGH";
        }
        if (hoursUntilDeadline <= 48 || task.getProgressPercent() < 50 || task.getPriority() == TaskPriority.CRITICAL) {
            return "MEDIUM";
        }
        if (hoursUntilDeadline <= 120 || task.getProgressPercent() < 75 || task.getPriority() == TaskPriority.HIGH) {
            return "LOW";
        }
        return null;
    }

    private String fallbackDelayReason(TaskEntity task) {
        long hoursUntilDeadline = Duration.between(OffsetDateTime.now(), task.getDeadline()).toHours();
        if (isOverdue(task)) {
            return "Task da qua deadline va chua hoan thanh.";
        }
        if (task.getStatus() == TaskStatus.BLOCKED) {
            return "Task dang bi blocker.";
        }
        if (hoursUntilDeadline <= 48 && task.getProgressPercent() < 30) {
            return "Tien do duoi 30% trong khi deadline con duoi 48 gio.";
        }
        if (hoursUntilDeadline <= 48) {
            return "Deadline sap den trong vong 48 gio.";
        }
        if (task.getProgressPercent() < 50) {
            return "Tien do task duoi 50%.";
        }
        if (task.getPriority() == TaskPriority.CRITICAL || task.getPriority() == TaskPriority.HIGH) {
            return "Task uu tien cao can duoc theo doi.";
        }
        return "Task co dau hieu can theo doi them.";
    }

    private String fallbackDelayAction(TaskEntity task, Map<UUID, String> employeeNames) {
        String assigneeName = employeeNames.getOrDefault(task.getAssigneeId(), "nhan vien phu trach");
        if (task.getStatus() == TaskStatus.BLOCKED) {
            return "Lam viec voi " + assigneeName + " de go blocker va chot nguoi ho tro.";
        }
        if (isOverdue(task)) {
            return "Lien he " + assigneeName + " de cap nhat ETA, dieu chinh deadline hoac bo sung nguon luc.";
        }
        if (Duration.between(OffsetDateTime.now(), task.getDeadline()).toHours() <= 48) {
            return "Kiem tra tien do voi " + assigneeName + " trong hom nay.";
        }
        return "Yeu cau " + assigneeName + " cap nhat tien do va rui ro hien tai.";
    }

    private Map<String, Object> fallbackActionSuggestions(List<WorkloadView> currentWorkload, AiProviderException exception) {
        UUID workspaceId = currentUser().workspaceId();
        Map<UUID, String> names = employeeNames();
        List<TaskEntity> scopedTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        LocalDate reportStart = LocalDate.now().minusDays(6);
        List<DailyReportEntity> recentReports = reports.findByWorkspaceIdOrderByReportDateDesc(workspaceId).stream()
                .filter(report -> !report.getReportDate().isBefore(reportStart))
                .toList();

        List<Map<String, Object>> suggestions = new ArrayList<>();
        Set<String> usedTargets = new HashSet<>();

        for (TaskEntity task : scopedTasks.stream()
                .filter(this::isOpenTask)
                .filter(task -> task.getStatus() == TaskStatus.BLOCKED)
                .sorted(Comparator.comparingDouble(this::taskRiskScore).reversed())
                .toList()) {
            addFallbackActionSuggestion(
                    suggestions,
                    usedTargets,
                    "TASK:" + task.getId(),
                    "REVIEW_BLOCKER",
                    "TASK",
                    task.getId().toString(),
                    "Xu ly blocker",
                    "Task \"" + task.getTitle() + "\" dang bi blocker; assignee: " + names.getOrDefault(task.getAssigneeId(), "Unknown") + ".",
                    0.95
            );
        }

        for (TaskEntity task : scopedTasks.stream()
                .filter(this::isOpenTask)
                .filter(this::isOverdue)
                .sorted(Comparator.comparingDouble(this::taskRiskScore).reversed())
                .toList()) {
            addFallbackActionSuggestion(
                    suggestions,
                    usedTargets,
                    "TASK:" + task.getId(),
                    "FOLLOW_UP_TASK",
                    "TASK",
                    task.getId().toString(),
                    "Theo doi task qua han",
                    "Task \"" + task.getTitle() + "\" da qua deadline; can cap nhat ETA voi " + names.getOrDefault(task.getAssigneeId(), "Unknown") + ".",
                    0.92
            );
        }

        for (TaskEntity task : scopedTasks.stream()
                .filter(this::isOpenTask)
                .filter(task -> !isOverdue(task))
                .filter(task -> Duration.between(OffsetDateTime.now(), task.getDeadline()).toHours() <= 48)
                .filter(task -> task.getProgressPercent() < 50)
                .sorted(Comparator.comparingDouble(this::taskRiskScore).reversed())
                .toList()) {
            addFallbackActionSuggestion(
                    suggestions,
                    usedTargets,
                    "TASK:" + task.getId(),
                    "FOLLOW_UP_TASK",
                    "TASK",
                    task.getId().toString(),
                    "Kiem tra task sap den deadline",
                    "Task \"" + task.getTitle() + "\" con duoi 48 gio nhung tien do moi " + task.getProgressPercent() + "%.",
                    0.86
            );
        }

        for (WorkloadView workload : currentWorkload.stream()
                .filter(item -> item.workloadLevel() == WorkloadLevel.OVERLOADED)
                .sorted(Comparator.comparingDouble(WorkloadView::workloadScore).reversed())
                .toList()) {
            addFallbackActionSuggestion(
                    suggestions,
                    usedTargets,
                    "EMPLOYEE:" + workload.employeeId(),
                    "REASSIGN_TASK",
                    "EMPLOYEE",
                    workload.employeeId().toString(),
                    "Can bang workload",
                    workload.fullName() + " dang qua tai voi " + workload.openTasks() + " task mo va " + workload.overdueTasks() + " task qua han.",
                    0.82
            );
        }

        for (DailyReportEntity report : recentReports.stream()
                .filter(report -> hasText(report.getBlockers()))
                .toList()) {
            addFallbackActionSuggestion(
                    suggestions,
                    usedTargets,
                    "DAILY_REPORT:" + report.getId(),
                    "REVIEW_BLOCKER",
                    "DAILY_REPORT",
                    report.getId().toString(),
                    "Xu ly blocker tu daily report",
                    names.getOrDefault(report.getUserId(), "Unknown") + " co blocker trong report ngay " + report.getReportDate() + ".",
                    0.78
            );
        }

        Set<UUID> submittedToday = reports.findByWorkspaceIdOrderByReportDateDesc(workspaceId).stream()
                .filter(report -> report.getReportDate().equals(LocalDate.now()))
                .map(DailyReportEntity::getUserId)
                .collect(java.util.stream.Collectors.toSet());
        for (UserEntity employee : users.findByWorkspaceIdAndRoleOrderByFullNameAsc(workspaceId, Role.EMPLOYEE).stream()
                .filter(employee -> employee.getStatus() == UserStatus.ACTIVE)
                .filter(employee -> !submittedToday.contains(employee.getId()))
                .toList()) {
            addFallbackActionSuggestion(
                    suggestions,
                    usedTargets,
                    "EMPLOYEE:" + employee.getId(),
                    "REQUEST_REPORT",
                    "EMPLOYEE",
                    employee.getId().toString(),
                    "Yeu cau daily report",
                    employee.getFullName() + " chua gui daily report hom nay.",
                    0.68
            );
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("suggestions", suggestions);
        return withFallbackMetadata(output, exception);
    }

    private void addFallbackActionSuggestion(List<Map<String, Object>> suggestions,
                                             Set<String> usedTargets,
                                             String targetKey,
                                             String actionType,
                                             String targetEntityType,
                                             String targetEntityId,
                                             String title,
                                             String reason,
                                             double confidence) {
        if (suggestions.size() >= 8 || !usedTargets.add(targetKey)) {
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("actionType", actionType);
        item.put("targetEntityType", targetEntityType);
        item.put("targetEntityId", targetEntityId);
        item.put("title", title);
        item.put("reason", reason);
        item.put("confidence", confidence);
        suggestions.add(item);
    }

    private Map<String, Object> withFallbackMetadata(Map<String, Object> output, AiProviderException exception) {
        output.put("source", RULE_BASED_FALLBACK_SOURCE);
        output.put("aiProviderFailed", true);
        output.put("fallbackReason", fallbackReason(exception));
        return output;
    }

    private String fallbackReason(AiProviderException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "AI provider unavailable";
        }
        return message.length() > 240 ? message.substring(0, 240) : message;
    }

    private double taskRiskScore(TaskEntity task) {
        double score = 0;
        if (isOverdue(task)) score += 100;
        if (task.getStatus() == TaskStatus.BLOCKED) score += 70;
        score += Math.max(0, 100 - task.getProgressPercent()) / 10.0;
        if (task.getDeadline().isBefore(OffsetDateTime.now().plusDays(2))) score += 20;
        return score;
    }

    private Map<String, Object> businessSummaryPayload(String period) {
        LocalDate start = periodStart(period);
        LocalDate end = LocalDate.now();
        List<Map<String, Object>> taskPayloads = taskPayloads(start);
        List<Map<String, Object>> reportPayloads = reportPayloads(start);
        List<WorkloadView> currentWorkload = workload();
        List<TaskEntity> periodTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(currentUser().workspaceId()).stream()
                .filter(task -> isRelevantToPeriod(task, start))
                .toList();
        long completedTasks = periodTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        long activeTasks = periodTasks.stream().filter(task -> List.of(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED).contains(task.getStatus())).count();
        long overdueTasks = periodTasks.stream().filter(this::isOverdue).count();
        long blockedTasks = periodTasks.stream().filter(task -> task.getStatus() == TaskStatus.BLOCKED).count();
        long totalTracked = Math.max(1, periodTasks.size());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("period", period);
        payload.put("periodType", (period == null ? "DAILY" : period.toUpperCase()));
        payload.put("periodStart", start.toString());
        payload.put("periodEnd", end.toString());
        payload.put("completedTasks", completedTasks);
        payload.put("activeTasks", activeTasks);
        payload.put("overdueTasks", overdueTasks);
        payload.put("blockedTasks", blockedTasks);
        payload.put("completionRate", completedTasks * 1.0 / totalTracked);
        payload.put("missingDailyReports", missingReportCount(end));
        payload.put("overloadedEmployees", currentWorkload.stream().filter(item -> item.workloadLevel() == WorkloadLevel.OVERLOADED).count());
        payload.put("idleEmployees", currentWorkload.stream().filter(item -> item.workloadLevel() == WorkloadLevel.NO_WORK).count());
        payload.put("tasks", taskPayloads);
        payload.put("reports", reportPayloads);
        payload.put("workload", currentWorkload.stream().map(AiEmployeeWorkload::from).toList());
        return payload;
    }

    private LocalDate periodStart(String period) {
        LocalDate today = LocalDate.now();
        return switch (period == null ? "daily" : period.toLowerCase()) {
            case "monthly" -> today.withDayOfMonth(1);
            case "weekly" -> today.minusDays(6);
            default -> today;
        };
    }

    private List<Map<String, Object>> taskPayloads(LocalDate start) {
        Map<UUID, String> names = employeeNames();
        return tasks.findByWorkspaceIdOrderByCreatedAtDesc(currentUser().workspaceId()).stream()
                .filter(task -> isRelevantToPeriod(task, start))
                .map(task -> aiTaskPayload(task, names))
                .toList();
    }

    private List<Map<String, Object>> reportPayloads(LocalDate start) {
        Map<UUID, String> names = employeeNames();
        return reports.findByWorkspaceIdOrderByReportDateDesc(currentUser().workspaceId()).stream()
                .filter(report -> !report.getReportDate().isBefore(start))
                .map(report -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("reportId", report.getId().toString());
                    item.put("employeeId", report.getUserId().toString());
                    item.put("userName", names.getOrDefault(report.getUserId(), "Unknown"));
                    item.put("reportDate", report.getReportDate().toString());
                    item.put("todayCompleted", report.getTodayCompleted());
                    item.put("currentWork", report.getCurrentWork());
                    item.put("blockers", report.getBlockers());
                    item.put("tomorrowPlan", report.getTomorrowPlan());
                    item.put("reviewed", report.getReviewedAt() != null);
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> dailyReportInsightPayloads(LocalDate start) {
        Map<UUID, String> names = employeeNames();
        return reports.findByWorkspaceIdOrderByReportDateDesc(currentUser().workspaceId()).stream()
                .filter(report -> !report.getReportDate().isBefore(start))
                .map(report -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("reportId", report.getId().toString());
                    item.put("employeeId", report.getUserId().toString());
                    item.put("employeeName", names.getOrDefault(report.getUserId(), "Unknown"));
                    item.put("reportDate", report.getReportDate().toString());
                    item.put("todayCompleted", report.getTodayCompleted());
                    item.put("currentWork", report.getCurrentWork());
                    item.put("blockers", report.getBlockers());
                    item.put("tomorrowPlan", report.getTomorrowPlan());
                    return item;
                })
                .toList();
    }

    private long missingReportCount(LocalDate reportDate) {
        List<UUID> submitted = reports.findByWorkspaceIdOrderByReportDateDesc(currentUser().workspaceId()).stream()
                .filter(report -> report.getReportDate().equals(reportDate))
                .map(DailyReportEntity::getUserId)
                .toList();
        return users.findByWorkspaceIdAndRoleOrderByFullNameAsc(currentUser().workspaceId(), Role.EMPLOYEE).stream()
                .filter(employee -> employee.getStatus() == UserStatus.ACTIVE)
                .filter(employee -> !submitted.contains(employee.getId()))
                .count();
    }

    private Map<String, Object> aiTaskPayload(TaskEntity task, Map<UUID, String> employeeNames) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("taskId", task.getId().toString());
        item.put("title", task.getTitle());
        item.put("requirements", task.getRequirements());
        item.put("description", task.getDescription());
        item.put("assigneeName", employeeNames.getOrDefault(task.getAssigneeId(), "Unknown"));
        item.put("priority", task.getPriority().name());
        item.put("status", task.getStatus().name());
        item.put("deadline", task.getDeadline().toString());
        item.put("progressPercent", task.getProgressPercent());
        item.put("estimatedHours", task.getEstimatedHours() == null ? 0 : task.getEstimatedHours().doubleValue());
        item.put("overdue", isOverdue(task));
        return item;
    }

    private Map<UUID, String> employeeNames() {
        return users.findByWorkspaceId(currentUser().workspaceId()).stream()
                .collect(java.util.stream.Collectors.toMap(UserEntity::getId, UserEntity::getFullName));
    }

    private boolean isRelevantToPeriod(TaskEntity task, LocalDate start) {
        return !task.getCreatedAt().toLocalDate().isBefore(start)
                || !task.getUpdatedAt().toLocalDate().isBefore(start)
                || (task.getCompletedAt() != null && !task.getCompletedAt().toLocalDate().isBefore(start))
                || !task.getDeadline().toLocalDate().isBefore(start);
    }

    private void applyTaskStatus(TaskEntity task, TaskStatus status, int progressPercent) {
        task.setStatus(status);
        task.setProgressPercent(status == TaskStatus.COMPLETED ? 100 : Math.max(0, Math.min(100, progressPercent)));
        task.setCompletedAt(status == TaskStatus.COMPLETED ? OffsetDateTime.now() : task.getCompletedAt());
        task.setUpdatedAt(OffsetDateTime.now());
    }

    private WorkloadView workloadForEmployee(UserEntity employee, List<TaskEntity> scopedTasks) {
        List<TaskEntity> assigned = scopedTasks.stream().filter(task -> employee.getId().equals(task.getAssigneeId())).toList();
        long open = assigned.stream().filter(task -> !List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED).contains(task.getStatus())).count();
        long inProgress = assigned.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS).count();
        long blocked = assigned.stream().filter(task -> task.getStatus() == TaskStatus.BLOCKED).count();
        long completed = assigned.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        long overdue = assigned.stream().filter(this::isOverdue).count();
        BigDecimal estimated = assigned.stream().filter(task -> !List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED).contains(task.getStatus())).map(task -> task.getEstimatedHours() == null ? BigDecimal.ZERO : task.getEstimatedHours()).reduce(BigDecimal.ZERO, BigDecimal::add);
        double score = open + overdue * 2.0 + estimated.doubleValue() / 8.0;
        return new WorkloadView(employee.getId(), employee.getFullName(), open, inProgress, blocked, completed, overdue, estimated, score, level(open));
    }

    private WorkloadLevel level(long openTasks) {
        if (openTasks == 0) return WorkloadLevel.NO_WORK;
        if (openTasks <= 2) return WorkloadLevel.LOW;
        if (openTasks <= 5) return WorkloadLevel.NORMAL;
        if (openTasks <= 9) return WorkloadLevel.HIGH;
        return WorkloadLevel.OVERLOADED;
    }

    private boolean isOverdue(TaskEntity task) {
        return task.getDeadline().isBefore(OffsetDateTime.now()) && !List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED).contains(task.getStatus());
    }

    private boolean isOpenTask(TaskEntity task) {
        return !List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED).contains(task.getStatus());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void generateOperationalNotifications() {
        AuthenticatedUser user = currentUser();
        OffsetDateTime now = OffsetDateTime.now();
        tasks.findByWorkspaceIdOrderByCreatedAtDesc(user.workspaceId()).stream()
                .filter(task -> !List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED).contains(task.getStatus()))
                .forEach(task -> {
                    if (task.getDeadline().isBefore(now)) {
                        createNotificationIfAbsent(task.getWorkspaceId(), task.getCreatorId(), "TASK_OVERDUE", "Task quá hạn", task.getTitle() + " đã quá hạn.", "TASK", task.getId());
                        createNotificationIfAbsent(task.getWorkspaceId(), task.getAssigneeId(), "TASK_OVERDUE", "Task quá hạn", task.getTitle() + " đã quá hạn.", "TASK", task.getId());
                    } else if (task.getDeadline().isBefore(now.plusHours(24))) {
                        createNotificationIfAbsent(task.getWorkspaceId(), task.getAssigneeId(), "DEADLINE_SOON", "Deadline sắp đến", task.getTitle() + " sắp đến deadline.", "TASK", task.getId());
                    }
                });
        users.findByWorkspaceIdAndRoleOrderByFullNameAsc(user.workspaceId(), Role.EMPLOYEE).forEach(employee -> {
            if (reports.findByWorkspaceIdAndUserIdAndReportDate(user.workspaceId(), employee.getId(), LocalDate.now()).isEmpty()) {
                createNotificationIfAbsent(user.workspaceId(), employee.getId(), "DAILY_REPORT_MISSING", "Chưa gửi báo cáo hôm nay", "Bạn chưa gửi daily report cho ngày hôm nay.", "USER", employee.getId());
            }
        });
    }

    private void createNotification(UUID workspaceId, UUID userId, String type, String title, String message, String relatedEntityType, UUID relatedEntityId) {
        NotificationEntity notification = new NotificationEntity();
        notification.setWorkspaceId(workspaceId);
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRead(false);
        notification.setCreatedAt(OffsetDateTime.now());
        notifications.save(notification);
    }

    private void createNotificationIfAbsent(UUID workspaceId, UUID userId, String type, String title, String message, String relatedEntityType, UUID relatedEntityId) {
        if (!notifications.existsByWorkspaceIdAndUserIdAndTypeAndRelatedEntityId(workspaceId, userId, type, relatedEntityId)) {
            createNotification(workspaceId, userId, type, title, message, relatedEntityType, relatedEntityId);
        }
    }

    private void saveAiSuggestion(AiSuggestionType type, Object inputData, Object outputData) {
        try {
            AiSuggestionEntity suggestion = new AiSuggestionEntity();
            suggestion.setWorkspaceId(currentUser().workspaceId());
            suggestion.setType(type);
            suggestion.setInputData(objectMapper.writeValueAsString(inputData));
            suggestion.setOutputData(objectMapper.writeValueAsString(outputData));
            suggestion.setStatus(AiSuggestionStatus.GENERATED);
            suggestion.setCreatedBy(currentUser().userId());
            suggestion.setCreatedAt(OffsetDateTime.now());
            aiSuggestions.save(suggestion);
        } catch (Exception exception) {
            throw new IllegalStateException("Không lưu được AI suggestion.", exception);
        }
    }

    private AuthenticatedUser currentUser() { return securityContext.currentUser(); }
    private void requireOwner() { if (currentUser().role() != Role.OWNER) throw new IllegalArgumentException("Chỉ OWNER được sử dụng chức năng này."); }
    private UserEntity currentUserEntity() { return users.findById(currentUser().userId()).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng hiện tại.")); }
    private WorkspaceEntity requireWorkspace(UUID workspaceId) { return workspaces.findById(workspaceId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy workspace.")); }
    private UserEntity requireEmployee(UUID employeeId) {
        UserEntity user = users.findById(employeeId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));
        if (!user.getWorkspaceId().equals(currentUser().workspaceId()) || user.getRole() != Role.EMPLOYEE) throw new IllegalArgumentException("Không tìm thấy nhân viên trong workspace.");
        return user;
    }
    private UserEntity requireActiveEmployee(UUID employeeId) {
        UserEntity employee = requireEmployee(employeeId);
        if (employee.getStatus() != UserStatus.ACTIVE) throw new IllegalArgumentException("Người nhận phải là nhân viên đang hoạt động.");
        return employee;
    }
    private TaskEntity requireTask(UUID taskId) {
        TaskEntity task = tasks.findById(taskId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy task."));
        if (!task.getWorkspaceId().equals(currentUser().workspaceId())) throw new IllegalArgumentException("Task không thuộc workspace hiện tại.");
        return task;
    }
    private DailyReportEntity requireReport(UUID reportId) {
        DailyReportEntity report = reports.findById(reportId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy báo cáo."));
        if (!report.getWorkspaceId().equals(currentUser().workspaceId())) throw new IllegalArgumentException("Báo cáo không thuộc workspace hiện tại.");
        return report;
    }
    private void requireTaskVisible(TaskEntity task) {
        if (currentUser().role() != Role.OWNER && !task.getAssigneeId().equals(currentUser().userId())) throw new IllegalArgumentException("Bạn không có quyền xem task này.");
    }
    private void requireOwnerOrAssignee(TaskEntity task) {
        if (currentUser().role() != Role.OWNER && !task.getAssigneeId().equals(currentUser().userId())) throw new IllegalArgumentException("Bạn không có quyền cập nhật task này.");
    }

    private String loginIdentifier(LoginRequest request) {
        String identifier = request.username();
        if (identifier == null || identifier.isBlank()) identifier = request.email();
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập email hoặc tên đăng nhập.");
        }
        return identifier.trim();
    }

    private String normalizeShortCode(String value) {
        String shortCode = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!shortCode.matches("^[A-Z0-9]{2}$")) {
            throw new IllegalArgumentException("Mã viết tắt tổ chức phải gồm đúng 2 ký tự chữ hoặc số.");
        }
        return shortCode;
    }

    private String nextEmployeeCode(WorkspaceEntity workspace) {
        String shortCode = workspace.getShortCode();
        if (shortCode == null || shortCode.isBlank()) {
            throw new IllegalArgumentException("Tổ chức chưa có mã viết tắt để tạo tài khoản nhân viên.");
        }
        int employeeNumber = workspace.getNextEmployeeNumber();
        if (employeeNumber < 1 || employeeNumber > 1000) {
            throw new IllegalArgumentException("Tổ chức đã đạt giới hạn 1000 nhân viên.");
        }
        return shortCode + String.format("%04d", employeeNumber);
    }

    private String buildUsername(String fullName, String employeeCode) {
        String[] parts = Arrays.stream(normalizeAccountText(fullName).split("\\s+"))
                .filter(part -> !part.isBlank())
                .toArray(String[]::new);
        String namePart;
        if (parts.length == 0) {
            namePart = "user";
        } else {
            StringBuilder builder = new StringBuilder(parts[parts.length - 1]);
            for (int index = 0; index < parts.length - 1; index++) {
                builder.append(parts[index].charAt(0));
            }
            namePart = builder.toString();
        }
        return (namePart + employeeCode).toLowerCase(Locale.ROOT);
    }

    private String normalizeAccountText(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9\\s]", " ").trim();
    }

    private WorkspaceView toWorkspaceView(WorkspaceEntity item) { return new WorkspaceView(item.getId(), item.getName(), item.getShortCode(), item.getLogo(), item.getAddress(), item.getOwnerId(), item.getCreatedAt()); }
    private UserView toUserView(UserEntity item) { return new UserView(item.getId(), item.getWorkspaceId(), item.getFullName(), item.getEmail(), item.getPhone(), item.getUsername(), item.getEmployeeCode(), item.getInitialPassword(), item.getRole(), item.getAvatar(), item.getStatus(), item.getJobTitle(), item.getSeniorityLevel(), item.getSkillRating(), item.getYearsOfExperience(), item.getSkills(), item.getCreatedAt(), item.getUpdatedAt()); }
    private TaskView toTaskView(TaskEntity item) { return new TaskView(item.getId(), item.getWorkspaceId(), item.getTitle(), item.getRequirements(), item.getDescription(), item.getAssigneeId(), item.getCreatorId(), item.getPriority(), item.getDeadline(), item.getEstimatedHours(), item.getProgressPercent(), item.getStatus(), item.getCreatedAt(), item.getUpdatedAt(), item.getCompletedAt()); }
    private TaskUpdateView toTaskUpdateView(TaskUpdateEntity item) { return new TaskUpdateView(item.getId(), item.getTaskId(), item.getUserId(), item.getProgressPercent(), item.getContent(), item.getAttachment(), item.getUpdateType(), item.getCreatedAt()); }
    private DailyReportView toDailyReportView(DailyReportEntity item) { return new DailyReportView(item.getId(), item.getWorkspaceId(), item.getUserId(), item.getReportDate(), item.getTodayCompleted(), item.getCurrentWork(), item.getBlockers(), item.getTomorrowPlan(), item.getReviewedAt(), item.getCreatedAt(), item.getUpdatedAt()); }
    private NotificationView toNotificationView(NotificationEntity item) { return new NotificationView(item.getId(), item.getWorkspaceId(), item.getUserId(), item.getType(), item.getTitle(), item.getMessage(), item.getRelatedEntityType(), item.getRelatedEntityId(), item.isRead(), item.getCreatedAt()); }
    private AiSuggestionView toAiSuggestionView(AiSuggestionEntity item) { return new AiSuggestionView(item.getId(), item.getWorkspaceId(), item.getType(), item.getInputData(), item.getOutputData(), item.getStatus(), item.getCreatedBy(), item.getCreatedAt()); }

    public record WorkspaceView(UUID id, String name, String shortCode, String logo, String address, UUID ownerId, OffsetDateTime createdAt) {}
    public record UserView(UUID id, UUID workspaceId, String fullName, String email, String phone, String username, String employeeCode, String initialPassword, Role role, String avatar, UserStatus status, String jobTitle, SeniorityLevel seniorityLevel, Integer skillRating, Integer yearsOfExperience, String skills, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record TaskView(UUID id, UUID workspaceId, String title, String requirements, String description, UUID assigneeId, UUID creatorId, TaskPriority priority, OffsetDateTime deadline, BigDecimal estimatedHours, int progressPercent, TaskStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime completedAt) {}
    public record TaskUpdateView(UUID id, UUID taskId, UUID userId, int progressPercent, String content, String attachment, UpdateType updateType, OffsetDateTime createdAt) {}
    public record DailyReportView(UUID id, UUID workspaceId, UUID userId, LocalDate reportDate, String todayCompleted, String currentWork, String blockers, String tomorrowPlan, OffsetDateTime reviewedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record NotificationView(UUID id, UUID workspaceId, UUID userId, String type, String title, String message, String relatedEntityType, UUID relatedEntityId, boolean isRead, OffsetDateTime createdAt) {}
    public record WorkloadView(UUID employeeId, String fullName, long openTasks, long inProgressTasks, long blockedTasks, long completedTasks, long overdueTasks, BigDecimal estimatedWorkload, double workloadScore, WorkloadLevel workloadLevel) {}
    public record AssigneeRecommendationView(UUID employeeId, String fullName, int score, WorkloadLevel workloadLevel, String reason, String risk) {}
    public record OwnerDashboardView(long totalTasks, long activeTasks, long completedTasks, long overdueTasks, List<WorkloadView> employeeWorkload, List<TaskView> recentlyUpdatedTasks, List<DashboardAiRecommendationView> aiRecommendations) {}
    public record DashboardAiRecommendationView(UUID suggestionId, AiSuggestionType type, String source, String outputData, OffsetDateTime createdAt) {}
    public record BusinessSummaryView(long completedTasks, long overdueTasks, long overloadedEmployees, long idleEmployees, String summary) {}
    public record LoginView(String token, UserView user) {}
    public record AiSuggestionView(UUID id, UUID workspaceId, AiSuggestionType type, String inputData, String outputData, AiSuggestionStatus status, UUID createdBy, OffsetDateTime createdAt) {}
}
