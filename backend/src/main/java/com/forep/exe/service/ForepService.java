package com.forep.exe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.ai.AiProviderException;
import com.forep.exe.ai.AiServiceClient;
import com.forep.exe.ai.AiServiceClient.AiEmployeeWorkload;
import com.forep.exe.ai.AiServiceClient.AiRecommendAssigneeInput;
import com.forep.exe.domain.Enums.AiSuggestionStatus;
import com.forep.exe.domain.Enums.AiSuggestionType;
import com.forep.exe.domain.Enums.FeedbackStatus;
import com.forep.exe.domain.Enums.PaymentMethod;
import com.forep.exe.domain.Enums.PaymentStatus;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.domain.Enums.RegistrationStatus;
import com.forep.exe.domain.Enums.Role;
import com.forep.exe.domain.Enums.SeniorityLevel;
import com.forep.exe.domain.Enums.SubscriptionPlanStatus;
import com.forep.exe.domain.Enums.TaskPriority;
import com.forep.exe.domain.Enums.TaskStatus;
import com.forep.exe.domain.Enums.UpdateType;
import com.forep.exe.domain.Enums.UserStatus;
import com.forep.exe.domain.Enums.WorkloadLevel;
import com.forep.exe.domain.Enums.WorkspaceStatus;
import com.forep.exe.dto.Requests.AdminCreateWorkspaceRequest;
import com.forep.exe.dto.Requests.AdminUpdateWorkspaceRequest;
import com.forep.exe.dto.Requests.AssignTaskRequest;
import com.forep.exe.dto.Requests.BusinessFeedbackRequest;
import com.forep.exe.dto.Requests.ChangePasswordRequest;
import com.forep.exe.dto.Requests.CreateBusinessOwnerRequest;
import com.forep.exe.dto.Requests.CreateEmployeeRequest;
import com.forep.exe.dto.Requests.CreatePaymentRequest;
import com.forep.exe.dto.Requests.CreateSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.CreateTaskRequest;
import com.forep.exe.dto.Requests.DailyReportRequest;
import com.forep.exe.dto.Requests.ExtractTasksRequest;
import com.forep.exe.dto.Requests.LoginRequest;
import com.forep.exe.dto.Requests.PaymentCallbackRequest;
import com.forep.exe.dto.Requests.RecommendAssigneeRequest;
import com.forep.exe.dto.Requests.RegisterWorkspaceRequest;
import com.forep.exe.dto.Requests.ReviewBusinessFeedbackRequest;
import com.forep.exe.dto.Requests.ReviewRegistrationRequest;
import com.forep.exe.dto.Requests.SelectSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.SubmitPaymentRequest;
import com.forep.exe.dto.Requests.UpdateSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.UpdateEmployeeRequest;
import com.forep.exe.dto.Requests.UpdateProgressRequest;
import com.forep.exe.dto.Requests.UpdateTaskRequest;
import com.forep.exe.dto.Requests.UpdateTaskStatusRequest;
import com.forep.exe.dto.Requests.UpdateWorkspaceRequest;
import com.forep.exe.dto.Requests.WorkspaceRegistrationRequest;
import com.forep.exe.persistence.AiSuggestionEntity;
import com.forep.exe.persistence.AiSuggestionRepository;
import com.forep.exe.persistence.AuditLogEntity;
import com.forep.exe.persistence.AuditLogRepository;
import com.forep.exe.persistence.BusinessFeedbackEntity;
import com.forep.exe.persistence.BusinessFeedbackRepository;
import com.forep.exe.persistence.DailyReportEntity;
import com.forep.exe.persistence.DailyReportRepository;
import com.forep.exe.persistence.NotificationEntity;
import com.forep.exe.persistence.NotificationRepository;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.SubscriptionPlanEntity;
import com.forep.exe.persistence.SubscriptionPlanRepository;
import com.forep.exe.persistence.TaskEntity;
import com.forep.exe.persistence.TaskRepository;
import com.forep.exe.persistence.TaskUpdateEntity;
import com.forep.exe.persistence.TaskUpdateRepository;
import com.forep.exe.persistence.UserEntity;
import com.forep.exe.persistence.UserRepository;
import com.forep.exe.persistence.WorkspaceEntity;
import com.forep.exe.persistence.WorkspaceRepository;
import com.forep.exe.persistence.WorkspaceRegistrationEntity;
import com.forep.exe.persistence.WorkspaceRegistrationRepository;
import com.forep.exe.security.AuthenticatedUser;
import com.forep.exe.security.JwtService;
import com.forep.exe.security.SecurityContext;
import com.forep.exe.service.MomoPaymentService.ProviderPaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
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
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final WorkspaceRepository workspaces;
    private final UserRepository users;
    private final TaskRepository tasks;
    private final TaskUpdateRepository taskUpdates;
    private final DailyReportRepository reports;
    private final NotificationRepository notifications;
    private final AiSuggestionRepository aiSuggestions;
    private final SubscriptionPlanRepository subscriptionPlans;
    private final WorkspaceRegistrationRepository workspaceRegistrations;
    private final PaymentTransactionRepository paymentTransactions;
    private final BusinessFeedbackRepository businessFeedback;
    private final AuditLogRepository auditLogs;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityContext securityContext;
    private final AiServiceClient aiServiceClient;
    private final MomoPaymentService momoPaymentService;
    private final BankTransferPaymentService bankTransferPaymentService;
    private final ObjectMapper objectMapper;

    public ForepService(WorkspaceRepository workspaces,
                        UserRepository users,
                        TaskRepository tasks,
                        TaskUpdateRepository taskUpdates,
                        DailyReportRepository reports,
                        NotificationRepository notifications,
                        AiSuggestionRepository aiSuggestions,
                        SubscriptionPlanRepository subscriptionPlans,
                        WorkspaceRegistrationRepository workspaceRegistrations,
                        PaymentTransactionRepository paymentTransactions,
                        BusinessFeedbackRepository businessFeedback,
                        AuditLogRepository auditLogs,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        SecurityContext securityContext,
                        AiServiceClient aiServiceClient,
                        MomoPaymentService momoPaymentService,
                        BankTransferPaymentService bankTransferPaymentService,
                        ObjectMapper objectMapper) {
        this.workspaces = workspaces;
        this.users = users;
        this.tasks = tasks;
        this.taskUpdates = taskUpdates;
        this.reports = reports;
        this.notifications = notifications;
        this.aiSuggestions = aiSuggestions;
        this.subscriptionPlans = subscriptionPlans;
        this.workspaceRegistrations = workspaceRegistrations;
        this.paymentTransactions = paymentTransactions;
        this.businessFeedback = businessFeedback;
        this.auditLogs = auditLogs;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityContext = securityContext;
        this.aiServiceClient = aiServiceClient;
        this.momoPaymentService = momoPaymentService;
        this.bankTransferPaymentService = bankTransferPaymentService;
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
        if (user.getRole() != Role.SYSTEM_ADMIN) {
            WorkspaceEntity workspace = requireWorkspace(user.getWorkspaceId());
            enforceWorkspaceLoginAllowed(workspace);
            workspace.setLastActivityAt(OffsetDateTime.now());
            workspaces.save(workspace);
        }
        String token = jwtService.issue(new AuthenticatedUser(user.getId(), user.getWorkspaceId(), user.getRole(), user.getEmail()));
        return new LoginView(token, toUserView(user));
    }

    public WorkspaceView registerWorkspace(RegisterWorkspaceRequest request) {
        throw new IllegalArgumentException("Direct workspace registration is disabled. Submit a workspace registration, select a subscription plan, and complete payment first.");
    }

    public UserView me() {
        return toUserView(currentUserEntity());
    }

    public UserView changePassword(ChangePasswordRequest request) {
        UserEntity user = currentUserEntity();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setInitialPassword(null);
        user.setUpdatedAt(OffsetDateTime.now());
        user = users.save(user);
        audit(user.getWorkspaceId(), "CHANGE_PASSWORD", "USER", user.getId(), null, Map.of("role", user.getRole().name()));
        return toUserView(user);
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
        enforceWorkspaceUserLimit(workspace);
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

    public UserView resetEmployeePassword(UUID employeeId) {
        requireOwner();
        UserEntity employee = requireEmployee(employeeId);
        String temporaryPassword = hasText(employee.getEmployeeCode()) ? employee.getEmployeeCode() : "Employee" + employee.getId().toString().substring(0, 8);
        employee.setInitialPassword(temporaryPassword);
        employee.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        employee.setUpdatedAt(OffsetDateTime.now());
        employee = users.save(employee);
        audit(employee.getWorkspaceId(), "RESET_EMPLOYEE_PASSWORD", "USER", employee.getId(), null, Map.of("employeeCode", employee.getEmployeeCode()));
        return toUserView(employee);
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
        createNotification(workspaceId, currentUser().userId(), "TASK_CREATED", "Bạn vừa tạo task mới", "Bạn vừa tạo task mới: " + task.getTitle(), "TASK", task.getId());
        createNotification(workspaceId, task.getAssigneeId(), "TASK_ASSIGNED", "Task mới được giao", "Bạn vừa được giao task mới: " + task.getTitle(), "TASK", task.getId());
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
        createNotification(task.getWorkspaceId(), request.assigneeId(), "TASK_ASSIGNED", "Task mới được giao", "Bạn vừa được giao task mới: " + task.getTitle(), "TASK", task.getId());
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
            String assigneeName = employeeNames().getOrDefault(task.getAssigneeId(), "Thành viên");
            createNotification(task.getWorkspaceId(), task.getCreatorId(), "TASK_BLOCKED_OWNER", "Thành viên có vướng mắc về task", assigneeName + " có vướng mắc về task: " + task.getTitle(), "TASK", task.getId());
            if (currentUser().userId().equals(task.getAssigneeId())) {
                createNotification(task.getWorkspaceId(), task.getAssigneeId(), "TASK_BLOCKER_REQUESTED", "Bạn đã yêu cầu hỏi vướng mắc", "Bạn đã yêu cầu hỏi vướng mắc task " + task.getTitle() + ".", "TASK", task.getId());
            }
        } else if (request.updateType() == UpdateType.COMPLETION && currentUser().userId().equals(task.getAssigneeId())) {
            createNotification(task.getWorkspaceId(), task.getAssigneeId(), "TASK_COMPLETED_EMPLOYEE", "Bạn đã hoàn thành task", "Bạn đã hoàn thành task: " + task.getTitle(), "TASK", task.getId());
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
        enforceAiUsageLimit();
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
                normalized = fallbackAssigneeRecommendations(candidates);
            }
        } catch (AiProviderException exception) {
            log.warn("AI recommend assignee failed; using rule-based fallback. message={}", fallbackReason(exception));
            normalized = fallbackAssigneeRecommendations(candidates);
        }
        saveAiSuggestion(AiSuggestionType.ASSIGNEE_RECOMMENDATION, request, normalized);
        return normalized;
    }

    public Map<String, Object> workloadSummary() {
        requireOwner();
        enforceAiUsageLimit();
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
        enforceAiUsageLimit();
        List<TaskEntity> scopedTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(currentUser().workspaceId());
        Map<UUID, String> employeeNames = users.findByWorkspaceId(currentUser().workspaceId()).stream().collect(java.util.stream.Collectors.toMap(UserEntity::getId, UserEntity::getFullName));
        List<TaskView> taskViews = scopedTasks.stream().map(this::toTaskView).toList();
        Map<String, Object> payload = Map.of("tasks", taskViews, "employeeNames", employeeNames);
        Map<String, Object> output;
        try {
            output = aiServiceClient.delayRisks(taskViews, employeeNames);
        } catch (AiProviderException exception) {
            log.warn("AI delay risks failed; using rule-based fallback. message={}", fallbackReason(exception));
            output = fallbackDelayRisks(scopedTasks, employeeNames, exception);
        }
        saveAiSuggestion(AiSuggestionType.DELAY_RISK, payload, output);
        return output;
    }

    public Map<String, Object> businessSummary(String period) {
        requireOwner();
        enforceAiUsageLimit();
        Map<String, Object> payload = businessSummaryPayload(period);
        Map<String, Object> output;
        try {
            output = aiServiceClient.businessSummary(payload);
        } catch (AiProviderException exception) {
            log.warn("AI business summary failed; using rule-based fallback. message={}", fallbackReason(exception));
            output = fallbackBusinessSummary(payload, exception);
        }
        saveAiSuggestion(AiSuggestionType.BUSINESS_SUMMARY, payload, output);
        return output;
    }

    public Map<String, Object> dailyAiSummary() {
        return businessSummary("daily");
    }

    public Map<String, Object> dailyReportInsights() {
        requireOwner();
        enforceAiUsageLimit();
        List<Map<String, Object>> reportPayloads = dailyReportInsightPayloads(LocalDate.now().minusDays(6));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reports", reportPayloads);
        Map<String, Object> output;
        try {
            output = aiServiceClient.dailyReportInsights(payload);
        } catch (AiProviderException exception) {
            log.warn("AI daily report insights failed; using rule-based fallback. message={}", fallbackReason(exception));
            output = fallbackDailyReportInsights(reportPayloads, exception);
        }
        saveAiSuggestion(AiSuggestionType.DAILY_REPORT_INSIGHTS, payload, output);
        return output;
    }

    public Map<String, Object> extractTasks(ExtractTasksRequest request) {
        requireOwner();
        enforceAiUsageLimit();
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
        enforceAiUsageLimit();
        TaskEntity task = requireTask(taskId);
        Map<String, Object> payload = Map.of("task", aiTaskPayload(task, employeeNames()));
        Map<String, Object> output = aiServiceClient.splitTask(payload);
        saveAiSuggestion(AiSuggestionType.TASK_SPLIT, payload, output);
        return output;
    }

    public Map<String, Object> taskAdjustment(UUID taskId) {
        requireOwner();
        enforceAiUsageLimit();
        TaskEntity task = requireTask(taskId);
        Map<String, Object> payload = Map.of("task", aiTaskPayload(task, employeeNames()));
        Map<String, Object> output = aiServiceClient.taskAdjustment(payload);
        saveAiSuggestion(AiSuggestionType.TASK_ADJUSTMENT, payload, output);
        return output;
    }

    public Map<String, Object> missingReports() {
        requireOwner();
        enforceAiUsageLimit();
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
        Map<String, Object> output;
        try {
            output = aiServiceClient.missingReports(payload);
        } catch (AiProviderException exception) {
            log.warn("AI missing reports failed; using rule-based fallback. message={}", fallbackReason(exception));
            output = fallbackMissingReports(payload, exception);
        }
        saveAiSuggestion(AiSuggestionType.MISSING_REPORT, payload, output);
        return output;
    }

    public Map<String, Object> actionSuggestions() {
        requireOwner();
        enforceAiUsageLimit();
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
        generateOperationalNotifications(currentUser().workspaceId());
        AuthenticatedUser user = currentUser();
        List<NotificationEntity> scoped = notifications.findByWorkspaceIdAndUserIdOrderByCreatedAtDesc(user.workspaceId(), user.userId());
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
        List<NotificationEntity> scoped = notifications.findByWorkspaceIdAndUserIdOrderByCreatedAtDesc(currentUser().workspaceId(), currentUser().userId());
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

    public List<PlatformWorkspaceView> adminWorkspaces() {
        requireSystemAdmin();
        return workspaces.findAllByOrderByCreatedAtDesc().stream().map(this::toPlatformWorkspaceView).toList();
    }

    public PlatformWorkspaceView adminWorkspace(UUID workspaceId) {
        requireSystemAdmin();
        return toPlatformWorkspaceView(requireWorkspace(workspaceId));
    }

    public PlatformWorkspaceView adminCreateWorkspace(AdminCreateWorkspaceRequest request) {
        requireSystemAdmin();
        String shortCode = hasText(request.workspaceIdentifier()) ? normalizeShortCode(request.workspaceIdentifier()) : nextAvailableShortCode(request.workspaceName());
        if (workspaces.findByShortCodeIgnoreCase(shortCode).isPresent()) {
            throw new IllegalArgumentException("Mã workspace đã tồn tại.");
        }
        SubscriptionPlanEntity plan = requireSubscriptionPlan(request.subscriptionPlanId());
        OffsetDateTime now = OffsetDateTime.now();
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setName(request.workspaceName());
        workspace.setBusinessName(request.businessName());
        workspace.setShortCode(shortCode);
        workspace.setContactEmail(request.contactEmail());
        workspace.setContactPhone(request.contactPhone());
        workspace.setAddress(request.businessAddress());
        workspace.setSubscriptionPlanId(plan.getId());
        if (request.maxUsers() > plan.getMaxUsers()) {
        }
        workspace.setMaxUsers(request.maxUsers() > 0 ? request.maxUsers() : plan.getMaxUsers());
        workspace.setStatus(request.status() == null ? WorkspaceStatus.INACTIVE : request.status());
        workspace.setPaymentStatus(PaymentStatus.CONFIRMED);
        workspace.setActivatedAt(request.activationDate());
        workspace.setExpiresAt(request.expirationDate());
        workspace.setCreatedAt(now);
        workspace = workspaces.save(workspace);
        audit(workspace.getId(), "ADMIN_CREATE_WORKSPACE", "WORKSPACE", workspace.getId(), null, toPlatformWorkspaceView(workspace));
        return toPlatformWorkspaceView(workspace);
    }

    public PlatformWorkspaceView adminUpdateWorkspace(UUID workspaceId, AdminUpdateWorkspaceRequest request) {
        requireSystemAdmin();
        WorkspaceEntity workspace = requireWorkspace(workspaceId);
        if (hasText(request.businessName())) workspace.setBusinessName(request.businessName());
        if (hasText(request.workspaceName())) workspace.setName(request.workspaceName());
        if (hasText(request.contactEmail())) workspace.setContactEmail(request.contactEmail());
        if (hasText(request.contactPhone())) workspace.setContactPhone(request.contactPhone());
        if (request.businessAddress() != null) workspace.setAddress(request.businessAddress());
        SubscriptionPlanEntity selectedPlan = null;
        if (request.subscriptionPlanId() != null) {
            selectedPlan = requireSubscriptionPlan(request.subscriptionPlanId());
            workspace.setSubscriptionPlanId(selectedPlan.getId());
        } else if (workspace.getSubscriptionPlanId() != null) {
            selectedPlan = requireSubscriptionPlan(workspace.getSubscriptionPlanId());
        }
        if (request.maxUsers() != null) {
            if (request.maxUsers() < currentWorkspaceUserCount(workspaceId)) {
                throw new IllegalArgumentException("Giới hạn người dùng không được nhỏ hơn số người dùng hiện tại.");
            }
            if (selectedPlan != null && request.maxUsers() > selectedPlan.getMaxUsers()) {
            }
            workspace.setMaxUsers(request.maxUsers());
        } else if (selectedPlan != null && workspace.getMaxUsers() > selectedPlan.getMaxUsers()) {
            if (currentWorkspaceUserCount(workspaceId) > selectedPlan.getMaxUsers()) {
            }
            workspace.setMaxUsers(selectedPlan.getMaxUsers());
        }
        if (request.activationDate() != null) workspace.setActivatedAt(request.activationDate());
        if (request.expirationDate() != null) workspace.setExpiresAt(request.expirationDate());
        if (request.status() != null) workspace.setStatus(request.status());
        workspace = workspaces.save(workspace);
        audit(workspace.getId(), "ADMIN_UPDATE_WORKSPACE", "WORKSPACE", workspace.getId(), null, toPlatformWorkspaceView(workspace));
        return toPlatformWorkspaceView(workspace);
    }

    public PlatformWorkspaceView adminUpdateWorkspaceStatus(UUID workspaceId, WorkspaceStatus status) {
        requireSystemAdmin();
        WorkspaceEntity workspace = requireWorkspace(workspaceId);
        workspace.setStatus(status);
        if (status == WorkspaceStatus.ACTIVE && workspace.getActivatedAt() == null) {
            workspace.setActivatedAt(OffsetDateTime.now());
        }
        workspace = workspaces.save(workspace);
        audit(workspace.getId(), "ADMIN_UPDATE_WORKSPACE_STATUS", "WORKSPACE", workspace.getId(), null, Map.of("status", status.name()));
        return toPlatformWorkspaceView(workspace);
    }

    public UserView adminCreateBusinessOwner(UUID workspaceId, CreateBusinessOwnerRequest request) {
        requireSystemAdmin();
        WorkspaceEntity workspace = requireWorkspace(workspaceId);
        enforceWorkspaceUserLimit(workspace);
        if (users.existsByWorkspaceIdAndEmailIgnoreCase(workspaceId, request.email())) {
            throw new IllegalArgumentException("Email đã tồn tại trong workspace.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        String temporaryPassword = hasText(request.temporaryPassword()) ? request.temporaryPassword() : temporaryPassword(workspace);
        UserEntity owner = new UserEntity();
        owner.setWorkspaceId(workspaceId);
        owner.setFullName(request.fullName());
        owner.setEmail(request.email());
        owner.setPhone(request.phone());
        owner.setUsername(hasText(request.username()) ? request.username().trim().toLowerCase(Locale.ROOT) : null);
        owner.setInitialPassword(temporaryPassword);
        owner.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        owner.setRole(Role.OWNER);
        owner.setStatus(request.status() == null ? UserStatus.ACTIVE : request.status());
        owner.setCreatedAt(now);
        owner.setUpdatedAt(now);
        owner = users.save(owner);
        if (workspace.getOwnerId() == null) {
            workspace.setOwnerId(owner.getId());
            workspaces.save(workspace);
        }
        audit(workspaceId, "ADMIN_CREATE_BUSINESS_OWNER", "USER", owner.getId(), null, Map.of("email", owner.getEmail(), "status", owner.getStatus().name()));
        return toUserView(owner);
    }

    public List<UserView> adminBusinessOwners(UUID workspaceId) {
        requireSystemAdmin();
        requireWorkspace(workspaceId);
        return users.findByWorkspaceIdAndRoleOrderByFullNameAsc(workspaceId, Role.OWNER).stream().map(this::toUserView).toList();
    }

    public UserView adminResetOwnerPassword(UUID ownerId) {
        requireSystemAdmin();
        UserEntity owner = users.findById(ownerId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Business Owner."));
        if (owner.getRole() != Role.OWNER) throw new IllegalArgumentException("Tài khoản không phải Business Owner.");
        String temporaryPassword = temporaryPassword(requireWorkspace(owner.getWorkspaceId()));
        owner.setInitialPassword(temporaryPassword);
        owner.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        owner.setUpdatedAt(OffsetDateTime.now());
        owner = users.save(owner);
        audit(owner.getWorkspaceId(), "ADMIN_RESET_OWNER_PASSWORD", "USER", owner.getId(), null, Map.of("email", owner.getEmail()));
        return toUserView(owner);
    }

    public UserView adminUpdateOwnerStatus(UUID ownerId, UserStatus status) {
        requireSystemAdmin();
        UserEntity owner = users.findById(ownerId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Business Owner."));
        if (owner.getRole() != Role.OWNER) throw new IllegalArgumentException("Tài khoản không phải Business Owner.");
        owner.setStatus(status);
        owner.setUpdatedAt(OffsetDateTime.now());
        owner = users.save(owner);
        audit(owner.getWorkspaceId(), "ADMIN_UPDATE_OWNER_STATUS", "USER", owner.getId(), null, Map.of("status", status.name()));
        return toUserView(owner);
    }

    public List<SubscriptionPlanView> subscriptionPlans() {
        requireSystemAdmin();
        return subscriptionPlans.findAllByOrderByCreatedAtDesc().stream().map(this::toSubscriptionPlanView).toList();
    }

    public List<SubscriptionPlanView> publicSubscriptionPlans() {
        return subscriptionPlans.findAllByOrderByCreatedAtDesc().stream()
                .filter(plan -> plan.getStatus() == SubscriptionPlanStatus.ACTIVE)
                .map(this::toSubscriptionPlanView)
                .toList();
    }

    public SubscriptionPlanView createSubscriptionPlan(CreateSubscriptionPlanRequest request) {
        requireSystemAdmin();
        subscriptionPlans.findByNameIgnoreCase(request.name()).ifPresent(existing -> { throw new IllegalArgumentException("Tên gói đã tồn tại."); });
        int maxOwnerAccounts = request.maxOwnerAccounts() == null ? 1 : request.maxOwnerAccounts();
        int maxEmployeeAccounts = request.maxEmployeeAccounts() == null ? request.maxUsers() : request.maxEmployeeAccounts();
        validatePlanValues(request.price(), maxOwnerAccounts, maxEmployeeAccounts);
        OffsetDateTime now = OffsetDateTime.now();
        SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setPrice(request.price());
        plan.setDurationDays(request.durationDays());
        plan.setDurationInMonths(request.durationInMonths() == null ? Math.max(1, request.durationDays() / 30) : request.durationInMonths());
        plan.setMaxUsers(request.maxUsers());
        plan.setMaxOwnerAccounts(maxOwnerAccounts);
        plan.setMaxEmployeeAccounts(maxEmployeeAccounts);
        plan.setMaxWorkspaces(request.maxWorkspaces());
        plan.setAiUsageLimit(request.aiUsageLimit());
        plan.setFeatures(request.features());
        plan.setHasFullFeatures(request.hasFullFeatures() == null || request.hasFullFeatures());
        plan.setStatus(request.status() == null ? SubscriptionPlanStatus.ACTIVE : request.status());
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        plan = subscriptionPlans.save(plan);
        audit(currentUser().workspaceId(), "ADMIN_CREATE_SUBSCRIPTION_PLAN", "SUBSCRIPTION_PLAN", plan.getId(), null, toSubscriptionPlanView(plan));
        return toSubscriptionPlanView(plan);
    }

    public SubscriptionPlanView updateSubscriptionPlan(UUID planId, UpdateSubscriptionPlanRequest request) {
        requireSystemAdmin();
        SubscriptionPlanEntity plan = requireSubscriptionPlan(planId);
        if (hasText(request.name())) plan.setName(request.name());
        if (request.description() != null) plan.setDescription(request.description());
        if (request.price() != null) plan.setPrice(request.price());
        if (request.durationDays() != null) plan.setDurationDays(request.durationDays());
        if (request.durationInMonths() != null) plan.setDurationInMonths(request.durationInMonths());
        if (request.maxUsers() != null) plan.setMaxUsers(request.maxUsers());
        if (request.maxOwnerAccounts() != null) plan.setMaxOwnerAccounts(request.maxOwnerAccounts());
        if (request.maxEmployeeAccounts() != null) plan.setMaxEmployeeAccounts(request.maxEmployeeAccounts());
        if (request.hasFullFeatures() != null) plan.setHasFullFeatures(request.hasFullFeatures());
        if (request.maxWorkspaces() != null) plan.setMaxWorkspaces(request.maxWorkspaces());
        if (request.aiUsageLimit() != null) plan.setAiUsageLimit(request.aiUsageLimit());
        if (request.features() != null) plan.setFeatures(request.features());
        if (request.status() != null) plan.setStatus(request.status());
        validatePlanValues(plan.getPrice(), plan.getMaxOwnerAccounts(), plan.getMaxEmployeeAccounts());
        plan.setUpdatedAt(OffsetDateTime.now());
        plan = subscriptionPlans.save(plan);
        audit(currentUser().workspaceId(), "ADMIN_UPDATE_SUBSCRIPTION_PLAN", "SUBSCRIPTION_PLAN", plan.getId(), null, toSubscriptionPlanView(plan));
        return toSubscriptionPlanView(plan);
    }

    public SubscriptionPlanView activateSubscriptionPlan(UUID planId) {
        requireSystemAdmin();
        SubscriptionPlanEntity plan = requireSubscriptionPlan(planId);
        plan.setStatus(SubscriptionPlanStatus.ACTIVE);
        plan.setUpdatedAt(OffsetDateTime.now());
        return toSubscriptionPlanView(subscriptionPlans.save(plan));
    }

    public SubscriptionPlanView deactivateSubscriptionPlan(UUID planId) {
        requireSystemAdmin();
        SubscriptionPlanEntity plan = requireSubscriptionPlan(planId);
        plan.setStatus(SubscriptionPlanStatus.INACTIVE);
        plan.setUpdatedAt(OffsetDateTime.now());
        return toSubscriptionPlanView(subscriptionPlans.save(plan));
    }

    public WorkspaceRegistrationView submitWorkspaceRegistration(WorkspaceRegistrationRequest request) {
        String shortCode = hasText(request.workspaceIdentifier()) ? normalizeShortCode(request.workspaceIdentifier()) : nextAvailableShortCode(request.workspaceName());
        if (workspaces.findByShortCodeIgnoreCase(shortCode).isPresent() || workspaceRegistrations.findByWorkspaceIdentifierIgnoreCase(shortCode).isPresent()) {
            throw new IllegalArgumentException("Mã workspace đã tồn tại.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        WorkspaceRegistrationEntity registration = new WorkspaceRegistrationEntity();
        registration.setBusinessName(request.businessName());
        registration.setWorkspaceName(request.workspaceName());
        registration.setWorkspaceIdentifier(shortCode);
        registration.setContactEmail(request.contactEmail());
        registration.setContactPhone(request.contactPhone() == null ? "" : request.contactPhone());
        registration.setBusinessAddress(request.businessAddress());
        registration.setRepresentativeFullName(request.representativeFullName());
        registration.setRepresentativeEmail(request.representativeEmail());
        registration.setRepresentativePhone(request.representativePhone());
        registration.setOwnerFullName(hasText(request.ownerFullName()) ? request.ownerFullName() : request.representativeFullName());
        registration.setOwnerEmail(hasText(request.ownerEmail()) ? request.ownerEmail() : request.representativeEmail());
        registration.setOwnerPhone(hasText(request.ownerPhone()) ? request.ownerPhone() : request.representativePhone());
        if (hasText(request.ownerPassword())) {
            registration.setOwnerPasswordHash(passwordEncoder.encode(request.ownerPassword()));
        }
        registration.setPaymentStatus(PaymentStatus.PENDING);
        registration.setRegistrationStatus(RegistrationStatus.PENDING_PLAN_SELECTION);
        registration.setCreatedAt(now);
        registration.setUpdatedAt(now);
        return toWorkspaceRegistrationView(workspaceRegistrations.save(registration));
    }

    public WorkspaceRegistrationView workspaceRegistration(UUID registrationId) {
        return toWorkspaceRegistrationView(requireWorkspaceRegistration(registrationId));
    }

    public WorkspaceRegistrationView selectSubscriptionPlan(UUID registrationId, SelectSubscriptionPlanRequest request) {
        WorkspaceRegistrationEntity registration = requireWorkspaceRegistration(registrationId);
        if (registration.getWorkspaceId() != null || registration.getRegistrationStatus() == RegistrationStatus.APPROVED) {
            throw new IllegalArgumentException("Workspace is already active.");
        }
        SubscriptionPlanEntity plan = requireActiveSubscriptionPlan(request.subscriptionPlanId());
        registration.setSubscriptionPlanId(plan.getId());
        registration.setMaxOwnerAccounts(plan.getMaxOwnerAccounts());
        registration.setMaxEmployeeAccounts(plan.getMaxEmployeeAccounts());
        registration.setMaxUsers(plan.getMaxOwnerAccounts() + plan.getMaxEmployeeAccounts());
        registration.setPaymentStatus(PaymentStatus.PENDING);
        registration.setRegistrationStatus(RegistrationStatus.PENDING_PAYMENT);
        registration.setUpdatedAt(OffsetDateTime.now());
        return toWorkspaceRegistrationView(workspaceRegistrations.save(registration));
    }

    public WorkspaceRegistrationView submitRegistrationPayment(UUID registrationId, SubmitPaymentRequest request) {
        WorkspaceRegistrationEntity registration = workspaceRegistrations.findById(registrationId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký workspace."));
        if (List.of(RegistrationStatus.APPROVED, RegistrationStatus.REJECTED).contains(registration.getRegistrationStatus())) {
            throw new IllegalArgumentException("Hồ sơ đăng ký đã đóng, không thể cập nhật thanh toán.");
        }
        registration.setPaymentProofUrl(request.paymentProofUrl());
        registration.setPaymentNote(request.paymentNote());
        registration.setPaymentStatus(PaymentStatus.PENDING);
        registration.setRegistrationStatus(RegistrationStatus.PAYMENT_SUBMITTED);
        registration.setUpdatedAt(OffsetDateTime.now());
        return toWorkspaceRegistrationView(workspaceRegistrations.save(registration));
    }

    public PaymentTransactionView createPayment(UUID registrationId, CreatePaymentRequest request) {
        WorkspaceRegistrationEntity registration = requireWorkspaceRegistration(registrationId);
        if (registration.getWorkspaceId() != null || registration.getRegistrationStatus() == RegistrationStatus.APPROVED) {
            throw new IllegalArgumentException("Workspace is already active.");
        }
        if (registration.getSubscriptionPlanId() == null) {
            throw new IllegalArgumentException("Select an active subscription plan before payment.");
        }
        SubscriptionPlanEntity plan = requireActiveSubscriptionPlan(registration.getSubscriptionPlanId());
        OffsetDateTime now = OffsetDateTime.now();
        PaymentTransactionEntity payment = new PaymentTransactionEntity();
        payment.setWorkspaceRegistrationId(registration.getId());
        payment.setSubscriptionPlanId(plan.getId());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setAmount(plan.getPrice());
        payment.setCurrency("VND");
        payment.setOrderCode(uniqueOrderCode());
        payment.setRequestId(uniqueRequestId());
        payment.setTransferContent("FOREP " + registration.getWorkspaceIdentifier() + " " + payment.getOrderCode());
        payment.setStatus(PaymentTransactionStatus.PENDING);
        payment.setExpiredAt(now.plusMinutes(30));
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        ProviderPaymentResult providerResult = request.paymentMethod() == PaymentMethod.MOMO
                ? momoPaymentService.createPayment(payment)
                : bankTransferPaymentService.createPayment(payment);
        payment.setProviderPaymentUrl(providerResult.paymentUrl());
        payment.setProviderDeeplink(providerResult.deeplink());
        payment.setProviderQrCodeUrl(providerResult.qrCodeUrl());
        payment.setBankCode(providerResult.bankCode());
        payment.setBankName(providerResult.bankName());
        payment.setBankAccountNumber(providerResult.bankAccountNumber());
        payment.setBankAccountName(providerResult.bankAccountName());
        payment.setRawProviderRequest(providerResult.rawRequest());
        payment.setRawProviderResponse(providerResult.rawResponse());
        payment = paymentTransactions.save(payment);

        registration.setPaymentStatus(PaymentStatus.PENDING);
        registration.setRegistrationStatus(RegistrationStatus.PENDING_PAYMENT);
        registration.setUpdatedAt(now);
        workspaceRegistrations.save(registration);
        return toPaymentTransactionView(payment);
    }

    public PaymentTransactionView payment(UUID paymentId) {
        return toPaymentTransactionView(requirePayment(paymentId));
    }

    public PaymentTransactionView handleMomoCallback(PaymentCallbackRequest request) {
        PaymentTransactionEntity payment = requirePaymentByCallback(request);
        Map<String, Object> payload = paymentCallbackPayload(request);
        if (!momoPaymentService.verifyCallbackSignature(payload, request.signature())) {
            throw new IllegalArgumentException("Invalid MoMo callback signature.");
        }
        boolean success = "0".equals(request.resultCode()) || "SUCCESS".equalsIgnoreCase(request.resultCode());
        return success ? confirmPayment(payment.getId(), false, request.rawPayload()) : failPayment(payment.getId(), request.rawPayload());
    }

    public PaymentTransactionView handleBankTransferCallback(PaymentCallbackRequest request) {
        PaymentTransactionEntity payment = requirePaymentByCallback(request);
        return confirmPayment(payment.getId(), false, request.rawPayload());
    }

    public PaymentTransactionView adminConfirmPayment(UUID paymentId, ReviewRegistrationRequest request) {
        requireSystemAdmin();
        return confirmPayment(paymentId, true, request == null ? null : request.note());
    }

    public PaymentTransactionView adminRejectPayment(UUID paymentId, ReviewRegistrationRequest request) {
        requireSystemAdmin();
        return failPayment(paymentId, request == null ? null : request.note());
    }

    public List<WorkspaceRegistrationView> adminWorkspaceRegistrations() {
        requireSystemAdmin();
        return workspaceRegistrations.findAllByOrderByCreatedAtDesc().stream().map(this::toWorkspaceRegistrationView).toList();
    }

    public WorkspaceRegistrationView confirmRegistrationPayment(UUID registrationId, ReviewRegistrationRequest request) {
        requireSystemAdmin();
        WorkspaceRegistrationEntity registration = workspaceRegistrations.findById(registrationId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký workspace."));
        if (List.of(RegistrationStatus.APPROVED, RegistrationStatus.REJECTED).contains(registration.getRegistrationStatus())) {
            throw new IllegalArgumentException("Không thể xác nhận thanh toán cho hồ sơ đã đóng.");
        }
        if (!hasText(registration.getPaymentProofUrl())) {
            throw new IllegalArgumentException("Chưa có minh chứng thanh toán để xác nhận.");
        }
        registration.setPaymentStatus(PaymentStatus.CONFIRMED);
        registration.setRegistrationStatus(RegistrationStatus.PAYMENT_SUBMITTED);
        registration.setReviewedBy(currentUser().userId());
        registration.setReviewedAt(OffsetDateTime.now());
        registration.setReviewNote(request == null ? null : request.note());
        registration.setUpdatedAt(OffsetDateTime.now());
        registration = workspaceRegistrations.save(registration);
        audit(currentUser().workspaceId(), "ADMIN_CONFIRM_REGISTRATION_PAYMENT", "WORKSPACE_REGISTRATION", registration.getId(), null, toWorkspaceRegistrationView(registration));
        return toWorkspaceRegistrationView(registration);
    }

    public WorkspaceRegistrationView requestRegistrationPaymentCorrection(UUID registrationId, ReviewRegistrationRequest request) {
        requireSystemAdmin();
        WorkspaceRegistrationEntity registration = workspaceRegistrations.findById(registrationId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký workspace."));
        if (List.of(RegistrationStatus.APPROVED, RegistrationStatus.REJECTED).contains(registration.getRegistrationStatus())) {
            throw new IllegalArgumentException("Không thể yêu cầu sửa thanh toán cho hồ sơ đã đóng.");
        }
        registration.setPaymentStatus(PaymentStatus.CORRECTION_REQUESTED);
        registration.setRegistrationStatus(RegistrationStatus.PAYMENT_PENDING);
        registration.setReviewedBy(currentUser().userId());
        registration.setReviewedAt(OffsetDateTime.now());
        registration.setReviewNote(request == null ? null : request.note());
        registration.setUpdatedAt(OffsetDateTime.now());
        registration = workspaceRegistrations.save(registration);
        audit(currentUser().workspaceId(), "ADMIN_REQUEST_PAYMENT_CORRECTION", "WORKSPACE_REGISTRATION", registration.getId(), null, toWorkspaceRegistrationView(registration));
        return toWorkspaceRegistrationView(registration);
    }

    public WorkspaceRegistrationView approveWorkspaceRegistration(UUID registrationId, ReviewRegistrationRequest request) {
        requireSystemAdmin();
        WorkspaceRegistrationEntity registration = requireWorkspaceRegistration(registrationId);
        activateWorkspaceForRegistration(registration, request == null ? null : request.note());
        return toWorkspaceRegistrationView(requireWorkspaceRegistration(registrationId));
    }

    public WorkspaceRegistrationView rejectWorkspaceRegistration(UUID registrationId, ReviewRegistrationRequest request) {
        requireSystemAdmin();
        WorkspaceRegistrationEntity registration = workspaceRegistrations.findById(registrationId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký workspace."));
        if (registration.getRegistrationStatus() == RegistrationStatus.APPROVED || registration.getWorkspaceId() != null) {
            throw new IllegalArgumentException("Không thể từ chối hồ sơ đã được duyệt.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        registration.setPaymentStatus(PaymentStatus.REJECTED);
        registration.setRegistrationStatus(RegistrationStatus.REJECTED);
        registration.setReviewedBy(currentUser().userId());
        registration.setReviewedAt(now);
        registration.setReviewNote(request == null ? null : request.note());
        registration.setUpdatedAt(now);
        registration = workspaceRegistrations.save(registration);
        audit(currentUser().workspaceId(), "ADMIN_REJECT_WORKSPACE_REGISTRATION", "WORKSPACE_REGISTRATION", registration.getId(), null, toWorkspaceRegistrationView(registration));
        return toWorkspaceRegistrationView(registration);
    }

    public Map<String, Object> adminMonitoring() {
        requireSystemAdmin();
        List<WorkspaceEntity> all = workspaces.findAll();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("totalWorkspaces", all.size());
        output.put("activeWorkspaces", all.stream().filter(item -> item.getStatus() == WorkspaceStatus.ACTIVE).count());
        output.put("suspendedWorkspaces", all.stream().filter(item -> item.getStatus() == WorkspaceStatus.SUSPENDED).count());
        output.put("expiredWorkspaces", all.stream().filter(item -> item.getStatus() == WorkspaceStatus.EXPIRED).count());
        output.put("workspaces", all.stream().map(this::toPlatformWorkspaceView).toList());
        return output;
    }

    public BusinessFeedbackView submitBusinessFeedback(BusinessFeedbackRequest request) {
        AuthenticatedUser user = currentUser();
        if (user.role() == Role.SYSTEM_ADMIN) throw new IllegalArgumentException("System Admin không gửi feedback thay workspace.");
        OffsetDateTime now = OffsetDateTime.now();
        BusinessFeedbackEntity feedback = new BusinessFeedbackEntity();
        feedback.setWorkspaceId(user.workspaceId());
        feedback.setRating(request.rating());
        feedback.setContent(request.content());
        feedback.setStatus(FeedbackStatus.NEW);
        feedback.setCreatedAt(now);
        feedback.setUpdatedAt(now);
        return toBusinessFeedbackView(businessFeedback.save(feedback));
    }

    public List<BusinessFeedbackView> adminBusinessFeedback() {
        requireSystemAdmin();
        return businessFeedback.findAllByOrderByCreatedAtDesc().stream().map(this::toBusinessFeedbackView).toList();
    }

    public BusinessFeedbackView reviewBusinessFeedback(UUID feedbackId, ReviewBusinessFeedbackRequest request) {
        requireSystemAdmin();
        BusinessFeedbackEntity feedback = businessFeedback.findById(feedbackId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy feedback."));
        feedback.setSupportNote(request == null ? null : request.supportNote());
        feedback.setStatus(FeedbackStatus.REVIEWED);
        feedback.setReviewedBy(currentUser().userId());
        feedback.setReviewedAt(OffsetDateTime.now());
        feedback.setUpdatedAt(OffsetDateTime.now());
        return toBusinessFeedbackView(businessFeedback.save(feedback));
    }

    private List<DashboardAiRecommendationView> cachedDashboardRecommendations() {
        return aiSuggestions.findByWorkspaceIdOrderByCreatedAtDesc(currentUser().workspaceId()).stream()
                .filter(suggestion -> suggestion.getStatus() == AiSuggestionStatus.GENERATED)
                .filter(suggestion -> List.of(
                        AiSuggestionType.ASSIGNEE_RECOMMENDATION,
                        AiSuggestionType.ACTION_SUGGESTION,
                        AiSuggestionType.MISSING_REPORT,
                        AiSuggestionType.DELAY_RISK,
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
        boolean taskNeedsProfileFit = request != null && hasText(request.title() + " " + request.requirements());
        boolean employeeHasProfile = hasText(employee.getJobTitle()) || hasText(employee.getSkills());
        int profileMismatchPenalty = taskNeedsProfileFit && employeeHasProfile && taskProfileMatchScore == 0 ? 55 : 0;
        int missingProfilePenalty = taskNeedsProfileFit && !employeeHasProfile ? 35 : 0;
        double openPenalty = workload.openTasks() * 6.0;
        double overduePenalty = workload.overdueTasks() * 18.0;
        double blockedPenalty = workload.blockedTasks() * 12.0;
        double workloadPenalty = workload.estimatedWorkload().doubleValue() / 2.0;
        int candidateScore = (int) Math.max(0, Math.min(100, Math.round(100 - openPenalty - overduePenalty - blockedPenalty - workloadPenalty - levelPenalty - profilePenalty - profileMismatchPenalty - missingProfilePenalty + taskProfileMatchScore)));
        if (profileMismatchPenalty > 0) {
            candidateScore = Math.min(candidateScore, 30);
        } else if (missingProfilePenalty > 0) {
            candidateScore = Math.min(candidateScore, 55);
        }
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("candidateScore", candidateScore);
        components.put("taskNeedsProfileFit", taskNeedsProfileFit);
        components.put("employeeHasProfile", employeeHasProfile);
        components.put("profileMismatchPenalty", profileMismatchPenalty);
        components.put("missingProfilePenalty", missingProfilePenalty);
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
        String taskText = normalizedSearchText((request.title() == null ? "" : request.title()) + " " + (request.requirements() == null ? "" : request.requirements()));
        String profileText = normalizedSearchText((employee.getJobTitle() == null ? "" : employee.getJobTitle()) + "," + (employee.getSkills() == null ? "" : employee.getSkills()));
        if (taskText.isBlank() || profileText.isBlank()) return 0;
        long matches = java.util.Arrays.stream(taskText.split("\\s+"))
                .map(String::trim)
                .filter(term -> term.length() >= 3)
                .distinct()
                .filter(profileText::contains)
                .limit(6)
                .count();
        return (int) Math.min(30, matches * 5);
    }

    private List<AssigneeRecommendationView> fallbackAssigneeRecommendations(List<AiEmployeeWorkload> candidates) {
        return candidates.stream()
                .sorted(Comparator.comparing(AiEmployeeWorkload::candidateScore).reversed())
                .limit(3)
                .map(candidate -> new AssigneeRecommendationView(
                        UUID.fromString(candidate.employeeId()),
                        displayText(candidate.fullName()),
                        candidate.candidateScore(),
                        candidate.workloadLevel(),
                        null,
                        roleFitLabel(candidate),
                        fallbackRoleFitReason(candidate),
                        fallbackAssigneeReason(candidate),
                        fallbackAssigneeRisk(candidate)
                ))
                .toList();
    }

    private String fallbackAssigneeReason(AiEmployeeWorkload candidate) {
        Object taskProfileMatchScore = candidate.scoreComponents().getOrDefault("taskProfileMatchScore", 0);
        int profileMismatchPenalty = numberComponent(candidate, "profileMismatchPenalty");
        int missingProfilePenalty = numberComponent(candidate, "missingProfilePenalty");
        String profileNote = profileMismatchPenalty > 0
                ? " Hồ sơ hiện chưa có tín hiệu khớp với nội dung task."
                : missingProfilePenalty > 0
                ? " Hồ sơ chuyên môn chưa đủ dữ liệu để đối chiếu với task."
                : "";
        return "Hệ thống đang dùng dữ liệu hiện có để gợi ý tạm thời. "
                + displayText(candidate.fullName()) + " đạt " + candidate.candidateScore()
                + " điểm: " + candidate.openTasks() + " task đang mở, "
                + candidate.overdueTasks() + " task quá hạn, "
                + candidate.blockedTasks() + " task có vướng mắc, mức tải "
                + vietnameseWorkloadLevel(candidate.workloadLevel()) + ", độ khớp hồ sơ +" + taskProfileMatchScore + "."
                + profileNote;
    }

    private String roleFitLabel(AiEmployeeWorkload candidate) {
        if (numberComponent(candidate, "profileMismatchPenalty") > 0) {
            return "UNCERTAIN";
        }
        if (numberComponent(candidate, "missingProfilePenalty") > 0) {
            return "UNCERTAIN";
        }
        return numberComponent(candidate, "taskProfileMatchScore") >= 10 ? "STRONG" : "PARTIAL";
    }

    private String fallbackRoleFitReason(AiEmployeeWorkload candidate) {
        if (numberComponent(candidate, "profileMismatchPenalty") > 0) {
            return "Hồ sơ hiện chưa có tín hiệu chuyên môn khớp với nội dung task; cần owner kiểm tra lại trước khi giao.";
        }
        if (numberComponent(candidate, "missingProfilePenalty") > 0) {
            return "Hồ sơ chuyên môn còn thiếu job title hoặc skills nên hệ thống chưa đủ dữ liệu để xác nhận vai trò phù hợp.";
        }
        return "Hồ sơ có tín hiệu chuyên môn khớp một phần với nội dung task theo dữ liệu job title và skills hiện có.";
    }

    private int numberComponent(AiEmployeeWorkload candidate, String key) {
        Object value = candidate.scoreComponents().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private String fallbackAssigneeRisk(AiEmployeeWorkload candidate) {
        if (candidate.workloadLevel() == WorkloadLevel.OVERLOADED) {
            return "Rủi ro cao: nhân viên đang quá tải.";
        }
        if (candidate.overdueTasks() > 0) {
            return "Rủi ro trung bình: nhân viên có task quá hạn.";
        }
        if (candidate.blockedTasks() > 0) {
            return "Rủi ro trung bình: nhân viên đang có task bị vướng mắc.";
        }
        if (candidate.workloadLevel() == WorkloadLevel.HIGH) {
            return "Rủi ro trung bình: tải công việc đang cao.";
        }
        return "Rủi ro thấp: tải công việc hiện tại còn khả năng nhận việc.";
    }

    private Map<String, Object> fallbackDailyReportInsights(List<Map<String, Object>> reportPayloads, AiProviderException exception) {
        List<Map<String, Object>> blockers = new ArrayList<>();
        List<Map<String, Object>> actionSuggestions = new ArrayList<>();
        long reviewedReports = reportPayloads.stream().filter(report -> Boolean.TRUE.equals(report.get("reviewed"))).count();

        for (Map<String, Object> report : reportPayloads) {
            String blockersText = stringValue(report, "blockers");
            String currentWork = stringValue(report, "currentWork");
            String todayCompleted = stringValue(report, "todayCompleted");
            String detectedBlocker = firstText(blockersText, blockerFromText(currentWork), blockerFromText(todayCompleted));
            if (!hasMeaningfulBlocker(detectedBlocker)) {
                continue;
            }

            Map<String, Object> blocker = new LinkedHashMap<>();
            blocker.put("severity", fallbackBlockerSeverity(detectedBlocker));
            blocker.put("description", stringValue(report, "employeeName") + " (" + stringValue(report, "reportDate") + "): " + detectedBlocker);
            blockers.add(blocker);

            if (actionSuggestions.size() < 8) {
                Map<String, Object> action = new LinkedHashMap<>();
                action.put("actionType", "REVIEW_BLOCKER");
                action.put("targetEntityType", "DAILY_REPORT");
                action.put("targetEntityId", stringValue(report, "reportId"));
                action.put("title", "Xử lý vướng mắc trong daily report");
                action.put("reason", displayText(stringValue(report, "employeeName")) + " báo vướng mắc trong daily report ngày " + stringValue(report, "reportDate") + ".");
                action.put("confidence", 0.82);
                actionSuggestions.add(action);
            }

            if (blockers.size() >= 8) {
                break;
            }
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("summary", "Đã nhận " + reportPayloads.size()
                + " daily report trong 7 ngày gần nhất, " + blockers.size()
                + " report có vướng mắc, " + reviewedReports + " report đã được review.");
        output.put("blockers", blockers);
        output.put("actionSuggestions", actionSuggestions);
        return withFallbackMetadata(output, exception);
    }

    private Map<String, Object> fallbackMissingReports(Map<String, Object> payload, AiProviderException exception) {
        String reportDate = String.valueOf(payload.getOrDefault("reportDate", LocalDate.now().toString()));
        Object employeesObject = payload.get("employees");
        List<?> employeePayloads = employeesObject instanceof List<?> list ? list : List.of();
        List<Map<String, Object>> missingReports = new ArrayList<>();

        for (Object employeePayload : employeePayloads) {
            if (!(employeePayload instanceof Map<?, ?> employee)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("employeeId", stringValue(employee, "employeeId"));
            item.put("employeeName", stringValue(employee, "fullName"));
            item.put("reportDate", reportDate);
            item.put("daysMissing", 1);
            item.put("recommendedAction", "Nhắc nhân viên gửi daily report hôm nay và cập nhật vướng mắc nếu có.");
            item.put("confidence", 1.0);
            missingReports.add(item);
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("missingReports", missingReports);
        return withFallbackMetadata(output, exception);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasMeaningfulBlocker(value)) {
                return value;
            }
        }
        return "";
    }

    private String blockerFromText(String value) {
        if (!hasText(value)) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("block") || lower.contains("stuck") || lower.contains("fail") || lower.contains("loi")
                || lower.contains("lỗi") || lower.contains("tre") || lower.contains("trễ") || lower.contains("cham")
                || lower.contains("chậm") || lower.contains("thieu") || lower.contains("thiếu") || lower.contains("vướng")) {
            return value;
        }
        return "";
    }

    private boolean hasMeaningfulBlocker(String value) {
        if (!hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !List.of("none", "no", "na", "n/a", "khong", "không", "khong co", "không có", "khong co blocker", "không có blocker", "không có vướng mắc").contains(normalized);
    }

    private String fallbackBlockerSeverity(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        if (lower.contains("block") || lower.contains("fail") || lower.contains("loi") || lower.contains("lỗi") || lower.contains("tre") || lower.contains("trễ") || lower.contains("urgent") || lower.contains("khẩn")) {
            return "HIGH";
        }
        return value != null && value.length() > 120 ? "MEDIUM" : "LOW";
    }

    private String stringValue(Map<?, ?> item, String key) {
        Object value = item.get(key);
        return value == null ? "" : String.valueOf(value);
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
                            displayText(candidate.fullName()),
                            candidate.candidateScore(),
                            candidate.workloadLevel(),
                            item.requiredRole(),
                            item.roleFit(),
                            item.roleFitReason(),
                            item.reason(),
                            item.risk()
                    );
                })
                .limit(3)
                .toList();
    }

    private Map<String, Object> fallbackWorkloadSummary(List<WorkloadView> currentWorkload, AiProviderException exception) {
        List<String> overloadedEmployees = currentWorkload.stream()
                .filter(item -> item.workloadLevel() == WorkloadLevel.OVERLOADED)
                .map(item -> displayText(item.fullName()))
                .toList();
        List<String> idleEmployees = currentWorkload.stream()
                .filter(item -> item.workloadLevel() == WorkloadLevel.NO_WORK)
                .map(item -> displayText(item.fullName()))
                .toList();
        List<String> overdueEmployees = currentWorkload.stream()
                .filter(item -> item.overdueTasks() > 0)
                .map(item -> displayText(item.fullName()))
                .toList();

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("summary", "Đang theo dõi " + currentWorkload.size()
                + " nhân viên, " + overloadedEmployees.size()
                + " người quá tải, " + idleEmployees.size()
                + " người chưa có task, " + overdueEmployees.size()
                + " người có task quá hạn.");
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
            return "Task đã quá deadline và chưa hoàn thành.";
        }
        if (task.getStatus() == TaskStatus.BLOCKED) {
            return "Task đang có vướng mắc.";
        }
        if (hoursUntilDeadline <= 48 && task.getProgressPercent() < 30) {
            return "Tiến độ dưới 30% trong khi deadline còn dưới 48 giờ.";
        }
        if (hoursUntilDeadline <= 48) {
            return "Deadline sắp đến trong vòng 48 giờ.";
        }
        if (task.getProgressPercent() < 50) {
            return "Tiến độ task dưới 50%.";
        }
        if (task.getPriority() == TaskPriority.CRITICAL || task.getPriority() == TaskPriority.HIGH) {
            return "Task ưu tiên cao cần được theo dõi.";
        }
        return "Task có dấu hiệu cần theo dõi thêm.";
    }

    private String fallbackDelayAction(TaskEntity task, Map<UUID, String> employeeNames) {
        String assigneeName = displayText(employeeNames.getOrDefault(task.getAssigneeId(), "nhân viên phụ trách"));
        if (task.getStatus() == TaskStatus.BLOCKED) {
            return "Làm việc với " + assigneeName + " để gỡ vướng mắc và chốt người hỗ trợ.";
        }
        if (isOverdue(task)) {
            return "Liên hệ " + assigneeName + " để cập nhật ETA, điều chỉnh deadline hoặc bổ sung nguồn lực.";
        }
        if (Duration.between(OffsetDateTime.now(), task.getDeadline()).toHours() <= 48) {
            return "Kiểm tra tiến độ với " + assigneeName + " trong hôm nay.";
        }
        return "Yêu cầu " + assigneeName + " cập nhật tiến độ và rủi ro hiện tại.";
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
                    "Xử lý vướng mắc",
                    "Task \"" + task.getTitle() + "\" đang có vướng mắc; người phụ trách: " + displayText(names.getOrDefault(task.getAssigneeId(), "Chưa rõ")) + ".",
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
                    "Theo dõi task quá hạn",
                    "Task \"" + task.getTitle() + "\" đã quá deadline; cần cập nhật ETA với " + displayText(names.getOrDefault(task.getAssigneeId(), "Chưa rõ")) + ".",
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
                    "Kiểm tra task sắp đến deadline",
                    "Task \"" + task.getTitle() + "\" còn dưới 48 giờ nhưng tiến độ mới " + task.getProgressPercent() + "%.",
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
                    "Cân bằng tải công việc",
                    displayText(workload.fullName()) + " đang quá tải với " + workload.openTasks() + " task đang mở và " + workload.overdueTasks() + " task quá hạn.",
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
                    "Xử lý vướng mắc từ daily report",
                    displayText(names.getOrDefault(report.getUserId(), "Chưa rõ")) + " có vướng mắc trong report ngày " + report.getReportDate() + ".",
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
                    "Yêu cầu daily report",
                    displayText(employee.getFullName()) + " chưa gửi daily report hôm nay.",
                    0.68
            );
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("suggestions", suggestions);
        return withFallbackMetadata(output, exception);
    }

    private Map<String, Object> fallbackBusinessSummary(Map<String, Object> payload, AiProviderException exception) {
        long completedTasks = longValue(payload, "completedTasks");
        long activeTasks = longValue(payload, "activeTasks");
        long overdueTasks = longValue(payload, "overdueTasks");
        long blockedTasks = longValue(payload, "blockedTasks");
        long missingDailyReports = longValue(payload, "missingDailyReports");
        long overloadedEmployees = longValue(payload, "overloadedEmployees");
        long idleEmployees = longValue(payload, "idleEmployees");

        List<String> highlights = new ArrayList<>();
        if (completedTasks > 0) {
            highlights.add("Đã hoàn thành " + completedTasks + " task trong kỳ.");
        }
        if (activeTasks > 0) {
            highlights.add("Đang theo dõi " + activeTasks + " task còn hoạt động.");
        }
        if (idleEmployees > 0) {
            highlights.add(idleEmployees + " nhân viên đang có tải công việc thấp.");
        }
        if (highlights.isEmpty()) {
            highlights.add("Chưa có điểm nổi bật mới trong kỳ.");
        }

        List<String> risks = new ArrayList<>();
        if (overdueTasks > 0) {
            risks.add(overdueTasks + " task đã quá hạn deadline.");
        }
        if (blockedTasks > 0) {
            risks.add(blockedTasks + " task đang có vướng mắc.");
        }
        if (missingDailyReports > 0) {
            risks.add(missingDailyReports + " nhân viên chưa gửi daily report hôm nay.");
        }
        if (overloadedEmployees > 0) {
            risks.add(overloadedEmployees + " nhân viên đang quá tải.");
        }
        if (risks.isEmpty()) {
            risks.add("Chưa phát hiện rủi ro vận hành nổi bật.");
        }

        String summary = "Đã hoàn thành " + completedTasks + " task, còn " + activeTasks + " task đang hoạt động.\n"
                + "Có " + overdueTasks + " task quá hạn và " + blockedTasks + " task có vướng mắc.\n"
                + "Daily report còn thiếu: " + missingDailyReports + "; nhân sự quá tải: " + overloadedEmployees + ".";

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("periodType", stringValue(payload, "periodType"));
        output.put("periodStart", stringValue(payload, "periodStart"));
        output.put("periodEnd", stringValue(payload, "periodEnd"));
        output.put("summary", summary);
        output.put("highlights", highlights);
        output.put("risks", risks);
        output.put("actionSuggestions", List.of());
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
        return "AI chưa phản hồi kịp, hệ thống đang dùng dữ liệu hiện có để tạo kết quả tạm thời.";
    }

    private String displayText(String value) {
        return value == null ? "" : value
                .replace('\u00D0', '\u0110')
                .replace('\u00F0', '\u0111');
    }

    private String normalizedSearchText(String value) {
        return Normalizer.normalize(displayText(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String vietnameseWorkloadLevel(WorkloadLevel workloadLevel) {
        return switch (workloadLevel) {
            case NO_WORK -> "chưa có việc";
            case LOW -> "tải thấp";
            case NORMAL -> "bình thường";
            case HIGH -> "tải cao";
            case OVERLOADED -> "quá tải";
        };
    }

    private long longValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
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

    @Scheduled(fixedDelayString = "${forep.notifications.fixed-delay-ms:300000}", initialDelayString = "${forep.notifications.initial-delay-ms:60000}")
    public void generateScheduledOperationalNotifications() {
        workspaces.findAll().stream()
                .filter(workspace -> workspace.getStatus() == WorkspaceStatus.ACTIVE)
                .filter(workspace -> workspace.getPaymentStatus() == PaymentStatus.CONFIRMED)
                .filter(workspace -> workspace.getExpiresAt() == null || workspace.getExpiresAt().isAfter(OffsetDateTime.now()))
                .forEach(workspace -> generateOperationalNotifications(workspace.getId()));
    }

    private void generateOperationalNotifications(UUID workspaceId) {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDate today = LocalDate.now();
        Map<UUID, String> names = users.findByWorkspaceId(workspaceId).stream()
                .collect(java.util.stream.Collectors.toMap(UserEntity::getId, UserEntity::getFullName));
        tasks.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .filter(task -> !List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED).contains(task.getStatus()))
                .forEach(task -> {
                    if (task.getDeadline().isBefore(now)) {
                        String assigneeName = names.getOrDefault(task.getAssigneeId(), "Thành viên");
                        createNotificationIfAbsent(task.getWorkspaceId(), task.getCreatorId(), "TASK_OVERDUE_OWNER", "Thành viên đã quá hạn deadline", assigneeName + " đã quá hạn deadline task: " + task.getTitle(), "TASK", task.getId());
                        createNotificationIfAbsent(task.getWorkspaceId(), task.getAssigneeId(), "TASK_OVERDUE_EMPLOYEE", "Bạn đã trễ deadline", "Bạn đã trễ deadline task: " + task.getTitle(), "TASK", task.getId());
                    } else if (task.getDeadline().isBefore(now.plusHours(24))) {
                        createNotificationIfAbsent(task.getWorkspaceId(), task.getAssigneeId(), "DEADLINE_SOON", "Deadline sắp đến", "Task " + task.getTitle() + " sắp đến deadline.", "TASK", task.getId());
                    }
                });
        users.findByWorkspaceIdAndRoleOrderByFullNameAsc(workspaceId, Role.EMPLOYEE).forEach(employee -> {
            if (reports.findByWorkspaceIdAndUserIdAndReportDate(workspaceId, employee.getId(), today).isEmpty()) {
                createNotificationIfAbsent(workspaceId, employee.getId(), "DAILY_REPORT_MISSING_" + today, "Chưa gửi báo cáo hôm nay", "Bạn chưa gửi daily report cho ngày hôm nay.", "USER", employee.getId());
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

    private void audit(UUID workspaceId, String action, String entityType, UUID entityId, Object oldValue, Object newValue) {
        try {
            AuditLogEntity logItem = new AuditLogEntity();
            logItem.setWorkspaceId(workspaceId == null ? currentUser().workspaceId() : workspaceId);
            logItem.setActorId(currentUser().userId());
            logItem.setAction(action);
            logItem.setEntityType(entityType);
            logItem.setEntityId(entityId);
            logItem.setOldValue(oldValue == null ? null : objectMapper.writeValueAsString(oldValue));
            logItem.setNewValue(newValue == null ? null : objectMapper.writeValueAsString(newValue));
            logItem.setCreatedAt(OffsetDateTime.now());
            auditLogs.save(logItem);
        } catch (Exception exception) {
            log.warn("Could not write audit log action={} entityType={} entityId={}", action, entityType, entityId, exception);
        }
    }

    private void enforceAiUsageLimit() {
        WorkspaceEntity workspace = requireWorkspace(currentUser().workspaceId());
        if (workspace.getSubscriptionPlanId() == null) {
            return;
        }
        SubscriptionPlanEntity plan = requireSubscriptionPlan(workspace.getSubscriptionPlanId());
        if (plan.getAiUsageLimit() == null || plan.getAiUsageLimit() <= 0) {
            return;
        }
        OffsetDateTime periodStart = workspace.getActivatedAt() == null ? workspace.getCreatedAt() : workspace.getActivatedAt();
        long used = aiSuggestions.findByWorkspaceIdOrderByCreatedAtDesc(workspace.getId()).stream()
                .filter(suggestion -> periodStart == null || !suggestion.getCreatedAt().isBefore(periodStart))
                .filter(suggestion -> workspace.getExpiresAt() == null || suggestion.getCreatedAt().isBefore(workspace.getExpiresAt()))
                .count();
        if (used >= plan.getAiUsageLimit()) {
        }
    }

    private PaymentTransactionView confirmPayment(UUID paymentId, boolean adminOverride, String rawPayloadOrNote) {
        PaymentTransactionEntity payment = requirePayment(paymentId);
        if (payment.getStatus() == PaymentTransactionStatus.SUCCESS) {
            return toPaymentTransactionView(payment);
        }
        if (payment.getStatus() == PaymentTransactionStatus.EXPIRED && !adminOverride) {
            throw new IllegalArgumentException("Expired payments require admin override.");
        }
        if (payment.getExpiredAt() != null && payment.getExpiredAt().isBefore(OffsetDateTime.now()) && !adminOverride) {
            payment.setStatus(PaymentTransactionStatus.EXPIRED);
            payment.setUpdatedAt(OffsetDateTime.now());
            paymentTransactions.save(payment);
            throw new IllegalArgumentException("Payment transaction has expired.");
        }
        WorkspaceRegistrationEntity registration = requireWorkspaceRegistration(payment.getWorkspaceRegistrationId());
        SubscriptionPlanEntity plan = requireSubscriptionPlan(payment.getSubscriptionPlanId());
        if (payment.getAmount().compareTo(plan.getPrice()) != 0) {
            throw new IllegalArgumentException("Payment amount does not match the selected subscription plan.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        payment.setStatus(PaymentTransactionStatus.SUCCESS);
        payment.setPaidAt(now);
        payment.setUpdatedAt(now);
        if (hasText(rawPayloadOrNote)) {
            payment.setRawProviderResponse(rawPayloadOrNote);
        }
        payment = paymentTransactions.save(payment);

        registration.setPaymentStatus(PaymentStatus.CONFIRMED);
        registration.setRegistrationStatus(RegistrationStatus.PAYMENT_CONFIRMED);
        registration.setUpdatedAt(now);
        workspaceRegistrations.save(registration);

        activateWorkspaceForRegistration(registration, adminOverride ? rawPayloadOrNote : null);
        return toPaymentTransactionView(payment);
    }

    private PaymentTransactionView failPayment(UUID paymentId, String rawPayloadOrNote) {
        PaymentTransactionEntity payment = requirePayment(paymentId);
        if (payment.getStatus() == PaymentTransactionStatus.SUCCESS) {
            throw new IllegalArgumentException("Successful payment transactions cannot be rejected.");
        }
        payment.setStatus(PaymentTransactionStatus.FAILED);
        payment.setUpdatedAt(OffsetDateTime.now());
        if (hasText(rawPayloadOrNote)) {
            payment.setRawProviderResponse(rawPayloadOrNote);
        }
        payment = paymentTransactions.save(payment);
        WorkspaceRegistrationEntity registration = requireWorkspaceRegistration(payment.getWorkspaceRegistrationId());
        registration.setPaymentStatus(PaymentStatus.REJECTED);
        registration.setRegistrationStatus(RegistrationStatus.PENDING_PAYMENT);
        registration.setUpdatedAt(OffsetDateTime.now());
        workspaceRegistrations.save(registration);
        return toPaymentTransactionView(payment);
    }

    private void activateWorkspaceForRegistration(WorkspaceRegistrationEntity registration, String reviewNote) {
        if (registration.getWorkspaceId() != null) {
            return;
        }
        if (registration.getPaymentStatus() != PaymentStatus.CONFIRMED && registration.getRegistrationStatus() != RegistrationStatus.PAYMENT_CONFIRMED) {
            throw new IllegalArgumentException("Workspace can only be activated after confirmed payment.");
        }
        SubscriptionPlanEntity plan = requireSubscriptionPlan(registration.getSubscriptionPlanId());
        OffsetDateTime now = OffsetDateTime.now();
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setName(registration.getWorkspaceName());
        workspace.setBusinessName(registration.getBusinessName());
        workspace.setShortCode(registration.getWorkspaceIdentifier());
        workspace.setContactEmail(registration.getContactEmail());
        workspace.setContactPhone(registration.getContactPhone());
        workspace.setAddress(registration.getBusinessAddress());
        workspace.setSubscriptionPlanId(plan.getId());
        workspace.setMaxOwnerAccounts(plan.getMaxOwnerAccounts());
        workspace.setMaxEmployeeAccounts(plan.getMaxEmployeeAccounts());
        workspace.setMaxUsers(plan.getMaxOwnerAccounts() + plan.getMaxEmployeeAccounts());
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspace.setPaymentStatus(PaymentStatus.CONFIRMED);
        workspace.setActivatedAt(now);
        workspace.setExpiresAt(now.plusMonths(plan.getDurationInMonths()));
        workspace.setCreatedAt(now);
        workspace = workspaces.save(workspace);

        List<UserEntity> owners = createInitialOwners(registration, workspace, plan, now);
        if (!owners.isEmpty()) {
            workspace.setOwnerId(owners.getFirst().getId());
            workspaces.save(workspace);
        }
        registration.setWorkspaceId(workspace.getId());
        registration.setPaymentStatus(PaymentStatus.CONFIRMED);
        registration.setRegistrationStatus(RegistrationStatus.APPROVED);
        registration.setReviewedBy(safeCurrentUserId());
        registration.setReviewedAt(now);
        registration.setReviewNote(reviewNote);
        registration.setUpdatedAt(now);
        workspaceRegistrations.save(registration);
        audit(workspace.getId(), "ACTIVATE_WORKSPACE_AFTER_PAYMENT", "WORKSPACE_REGISTRATION", registration.getId(), null, toWorkspaceRegistrationView(registration));
    }

    private List<UserEntity> createInitialOwners(WorkspaceRegistrationEntity registration, WorkspaceEntity workspace, SubscriptionPlanEntity plan, OffsetDateTime now) {
        if (users.findFirstByEmailIgnoreCase(registration.getOwnerEmail()).isPresent()) {
            throw new IllegalArgumentException("Owner email already exists.");
        }
        List<UserEntity> created = new ArrayList<>();
        for (int index = 1; index <= plan.getMaxOwnerAccounts(); index++) {
            UserEntity owner = new UserEntity();
            owner.setWorkspaceId(workspace.getId());
            owner.setFullName(index == 1 ? registration.getOwnerFullName() : registration.getOwnerFullName() + " " + index);
            owner.setEmail(index == 1 ? registration.getOwnerEmail() : ownerAliasEmail(registration.getOwnerEmail(), index));
            owner.setPhone(registration.getOwnerPhone());
            String temporaryPassword = secureTemporaryPassword();
            owner.setInitialPassword(temporaryPassword);
            owner.setPasswordHash(hasText(registration.getOwnerPasswordHash()) && index == 1 ? registration.getOwnerPasswordHash() : passwordEncoder.encode(temporaryPassword));
            owner.setRole(Role.OWNER);
            owner.setStatus(UserStatus.ACTIVE);
            owner.setCreatedAt(now);
            owner.setUpdatedAt(now);
            created.add(users.save(owner));
        }
        return created;
    }

    private String ownerAliasEmail(String email, int index) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "owner" + index + "-" + UUID.randomUUID() + "@workspace.local";
        }
        return email.substring(0, at) + "+owner" + index + email.substring(at);
    }

    private String secureTemporaryPassword() {
        byte[] bytes = new byte[12];
        SECURE_RANDOM.nextBytes(bytes);
        return "Ow!" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validatePlanValues(BigDecimal price, Integer maxOwnerAccounts, Integer maxEmployeeAccounts) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Subscription plan price must be greater than 0.");
        }
        if (maxOwnerAccounts == null || maxOwnerAccounts <= 0) {
            throw new IllegalArgumentException("Owner account limit must be greater than 0.");
        }
        if (maxEmployeeAccounts == null || maxEmployeeAccounts <= 0) {
            throw new IllegalArgumentException("Employee account limit must be greater than 0.");
        }
    }

    private WorkspaceRegistrationEntity requireWorkspaceRegistration(UUID registrationId) {
        return workspaceRegistrations.findById(registrationId).orElseThrow(() -> new IllegalArgumentException("Workspace registration not found."));
    }

    private PaymentTransactionEntity requirePayment(UUID paymentId) {
        return paymentTransactions.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("Payment transaction not found."));
    }

    private PaymentTransactionEntity requirePaymentByCallback(PaymentCallbackRequest request) {
        if (hasText(request.orderCode())) {
            return paymentTransactions.findByOrderCode(request.orderCode()).orElseThrow(() -> new IllegalArgumentException("Payment transaction not found."));
        }
        if (hasText(request.requestId())) {
            return paymentTransactions.findByRequestId(request.requestId()).orElseThrow(() -> new IllegalArgumentException("Payment transaction not found."));
        }
        throw new IllegalArgumentException("Callback must include orderCode or requestId.");
    }

    private Map<String, Object> paymentCallbackPayload(PaymentCallbackRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderCode", request.orderCode());
        payload.put("requestId", request.requestId());
        payload.put("providerTransactionId", request.providerTransactionId());
        payload.put("resultCode", request.resultCode());
        payload.put("amount", request.amount());
        return payload;
    }

    private SubscriptionPlanEntity requireActiveSubscriptionPlan(UUID planId) {
        SubscriptionPlanEntity plan = requireSubscriptionPlan(planId);
        if (plan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new IllegalArgumentException("Selected subscription plan is not active.");
        }
        validatePlanValues(plan.getPrice(), plan.getMaxOwnerAccounts(), plan.getMaxEmployeeAccounts());
        return plan;
    }

    private String uniqueOrderCode() {
        String value;
        do {
            value = "FOREP-" + OffsetDateTime.now().toInstant().toEpochMilli() + "-" + SECURE_RANDOM.nextInt(100000, 999999);
        } while (paymentTransactions.findByOrderCode(value).isPresent());
        return value;
    }

    private String uniqueRequestId() {
        String value;
        do {
            value = UUID.randomUUID().toString();
        } while (paymentTransactions.findByRequestId(value).isPresent());
        return value;
    }

    private String nextAvailableShortCode(String workspaceName) {
        String base = normalizeAccountText(workspaceName).replaceAll("[^a-z0-9]", "").toUpperCase(Locale.ROOT);
        if (base.length() < 2) base = "WS";
        String prefix = base.substring(0, 2);
        for (int index = 0; index < 100; index++) {
            String candidate = index == 0 ? prefix : prefix.charAt(0) + Integer.toString(index % 10);
            if (workspaces.findByShortCodeIgnoreCase(candidate).isEmpty() && workspaceRegistrations.findByWorkspaceIdentifierIgnoreCase(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Could not generate a unique workspace code.");
    }

    private UUID safeCurrentUserId() {
        try {
            return currentUser().userId();
        } catch (Exception exception) {
            return null;
        }
    }

    private AuthenticatedUser currentUser() { return securityContext.currentUser(); }
    private void requireOwner() { if (currentUser().role() != Role.OWNER) throw new IllegalArgumentException("Chỉ OWNER được sử dụng chức năng này."); }
    private void requireSystemAdmin() { if (currentUser().role() != Role.SYSTEM_ADMIN) throw new IllegalArgumentException("Chỉ System Admin được sử dụng chức năng này."); }
    private UserEntity currentUserEntity() { return users.findById(currentUser().userId()).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng hiện tại.")); }
    private WorkspaceEntity requireWorkspace(UUID workspaceId) { return workspaces.findById(workspaceId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy workspace.")); }
    private SubscriptionPlanEntity requireSubscriptionPlan(UUID planId) { return subscriptionPlans.findById(planId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói subscription.")); }
    private int currentWorkspaceUserCount(UUID workspaceId) { return users.findByWorkspaceId(workspaceId).size(); }
    private void enforceWorkspaceLoginAllowed(WorkspaceEntity workspace) {
        OffsetDateTime now = OffsetDateTime.now();
        if (workspace.getExpiresAt() != null && workspace.getExpiresAt().isBefore(now)) {
            workspace.setStatus(WorkspaceStatus.EXPIRED);
            workspaces.save(workspace);
        }
        if (workspace.getStatus() != WorkspaceStatus.ACTIVE || workspace.getPaymentStatus() != PaymentStatus.CONFIRMED) {
            throw new IllegalArgumentException("Workspace hiện không hoạt động hoặc chưa được xác nhận thanh toán. Vui lòng liên hệ quản trị viên.");
        }
    }
    private void enforceWorkspaceUserLimit(WorkspaceEntity workspace) {
        if (currentWorkspaceUserCount(workspace.getId()) >= workspace.getMaxUsers()) {
        }
    }
    private String temporaryPassword(WorkspaceEntity workspace) {
        return (workspace.getShortCode() == null ? "OWNER" : workspace.getShortCode()) + "Owner" + String.format("%04d", currentWorkspaceUserCount(workspace.getId()) + 1);
    }
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
        String source = (value == null ? "" : value)
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .replace('\u00F0', 'd')
                .replace('\u00D0', 'D');
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
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
    private SubscriptionPlanView toSubscriptionPlanView(SubscriptionPlanEntity item) { return new SubscriptionPlanView(item.getId(), item.getName(), item.getDescription(), item.getPrice(), item.getDurationDays(), item.getDurationInMonths(), item.getMaxUsers(), item.getMaxOwnerAccounts(), item.getMaxEmployeeAccounts(), item.isHasFullFeatures(), item.getMaxWorkspaces(), item.getAiUsageLimit(), item.getFeatures(), item.getStatus(), item.getCreatedAt(), item.getUpdatedAt()); }
    private PlatformWorkspaceView toPlatformWorkspaceView(WorkspaceEntity item) { return new PlatformWorkspaceView(item.getId(), item.getBusinessName(), item.getName(), item.getShortCode(), item.getContactEmail(), item.getContactPhone(), item.getAddress(), item.getSubscriptionPlanId(), item.getMaxUsers(), item.getMaxOwnerAccounts(), item.getMaxEmployeeAccounts(), currentWorkspaceUserCount(item.getId()), item.getStatus(), item.getPaymentStatus(), item.getOwnerId(), item.getActivatedAt(), item.getExpiresAt(), item.getLastActivityAt(), item.getCreatedAt()); }
    private WorkspaceRegistrationView toWorkspaceRegistrationView(WorkspaceRegistrationEntity item) { return new WorkspaceRegistrationView(item.getId(), item.getBusinessName(), item.getWorkspaceName(), item.getWorkspaceIdentifier(), item.getContactEmail(), item.getContactPhone(), item.getBusinessAddress(), item.getRepresentativeFullName(), item.getRepresentativeEmail(), item.getRepresentativePhone(), item.getSubscriptionPlanId(), item.getMaxUsers(), item.getMaxOwnerAccounts(), item.getMaxEmployeeAccounts(), item.getOwnerFullName(), item.getOwnerEmail(), item.getOwnerPhone(), item.getPaymentProofUrl(), item.getPaymentStatus(), item.getRegistrationStatus(), item.getWorkspaceId(), item.getReviewedBy(), item.getReviewedAt(), item.getReviewNote(), item.getCreatedAt(), item.getUpdatedAt()); }
    private PaymentTransactionView toPaymentTransactionView(PaymentTransactionEntity item) { return new PaymentTransactionView(item.getId(), item.getWorkspaceRegistrationId(), item.getSubscriptionPlanId(), item.getPaymentMethod(), item.getAmount(), item.getCurrency(), item.getOrderCode(), item.getRequestId(), item.getProviderTransactionId(), item.getProviderPaymentUrl(), item.getProviderDeeplink(), item.getProviderQrCodeUrl(), item.getBankCode(), item.getBankName(), item.getBankAccountNumber(), item.getBankAccountName(), item.getTransferContent(), item.getStatus(), item.getPaidAt(), item.getExpiredAt(), item.getCreatedAt(), item.getUpdatedAt()); }
    private BusinessFeedbackView toBusinessFeedbackView(BusinessFeedbackEntity item) { return new BusinessFeedbackView(item.getId(), item.getWorkspaceId(), item.getRating(), item.getContent(), item.getSupportNote(), item.getStatus(), item.getReviewedBy(), item.getReviewedAt(), item.getCreatedAt(), item.getUpdatedAt()); }

    public record WorkspaceView(UUID id, String name, String shortCode, String logo, String address, UUID ownerId, OffsetDateTime createdAt) {}
    public record UserView(UUID id, UUID workspaceId, String fullName, String email, String phone, String username, String employeeCode, String initialPassword, Role role, String avatar, UserStatus status, String jobTitle, SeniorityLevel seniorityLevel, Integer skillRating, Integer yearsOfExperience, String skills, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record TaskView(UUID id, UUID workspaceId, String title, String requirements, String description, UUID assigneeId, UUID creatorId, TaskPriority priority, OffsetDateTime deadline, BigDecimal estimatedHours, int progressPercent, TaskStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime completedAt) {}
    public record TaskUpdateView(UUID id, UUID taskId, UUID userId, int progressPercent, String content, String attachment, UpdateType updateType, OffsetDateTime createdAt) {}
    public record DailyReportView(UUID id, UUID workspaceId, UUID userId, LocalDate reportDate, String todayCompleted, String currentWork, String blockers, String tomorrowPlan, OffsetDateTime reviewedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record NotificationView(UUID id, UUID workspaceId, UUID userId, String type, String title, String message, String relatedEntityType, UUID relatedEntityId, boolean isRead, OffsetDateTime createdAt) {}
    public record WorkloadView(UUID employeeId, String fullName, long openTasks, long inProgressTasks, long blockedTasks, long completedTasks, long overdueTasks, BigDecimal estimatedWorkload, double workloadScore, WorkloadLevel workloadLevel) {}
    public record AssigneeRecommendationView(UUID employeeId, String fullName, int score, WorkloadLevel workloadLevel, String requiredRole, String roleFit, String roleFitReason, String reason, String risk) {}
    public record OwnerDashboardView(long totalTasks, long activeTasks, long completedTasks, long overdueTasks, List<WorkloadView> employeeWorkload, List<TaskView> recentlyUpdatedTasks, List<DashboardAiRecommendationView> aiRecommendations) {}
    public record DashboardAiRecommendationView(UUID suggestionId, AiSuggestionType type, String source, String outputData, OffsetDateTime createdAt) {}
    public record BusinessSummaryView(long completedTasks, long overdueTasks, long overloadedEmployees, long idleEmployees, String summary) {}
    public record LoginView(String token, UserView user) {}
    public record AiSuggestionView(UUID id, UUID workspaceId, AiSuggestionType type, String inputData, String outputData, AiSuggestionStatus status, UUID createdBy, OffsetDateTime createdAt) {}
    public record SubscriptionPlanView(UUID id, String name, String description, BigDecimal price, int durationDays, int durationInMonths, int maxUsers, int maxOwnerAccounts, int maxEmployeeAccounts, boolean hasFullFeatures, Integer maxWorkspaces, Integer aiUsageLimit, String features, SubscriptionPlanStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record PlatformWorkspaceView(UUID id, String businessName, String workspaceName, String workspaceIdentifier, String contactEmail, String contactPhone, String businessAddress, UUID subscriptionPlanId, int maxUsers, int maxOwnerAccounts, int maxEmployeeAccounts, int currentUsers, WorkspaceStatus status, PaymentStatus paymentStatus, UUID ownerId, OffsetDateTime activatedAt, OffsetDateTime expiresAt, OffsetDateTime lastActivityAt, OffsetDateTime createdAt) {}
    public record WorkspaceRegistrationView(UUID id, String businessName, String workspaceName, String workspaceIdentifier, String contactEmail, String contactPhone, String businessAddress, String representativeFullName, String representativeEmail, String representativePhone, UUID subscriptionPlanId, int maxUsers, int maxOwnerAccounts, int maxEmployeeAccounts, String ownerFullName, String ownerEmail, String ownerPhone, String paymentProofUrl, PaymentStatus paymentStatus, RegistrationStatus registrationStatus, UUID workspaceId, UUID reviewedBy, OffsetDateTime reviewedAt, String reviewNote, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record PaymentTransactionView(UUID id, UUID workspaceRegistrationId, UUID subscriptionPlanId, PaymentMethod paymentMethod, BigDecimal amount, String currency, String orderCode, String requestId, String providerTransactionId, String providerPaymentUrl, String providerDeeplink, String providerQrCodeUrl, String bankCode, String bankName, String bankAccountNumber, String bankAccountName, String transferContent, PaymentTransactionStatus status, OffsetDateTime paidAt, OffsetDateTime expiredAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record BusinessFeedbackView(UUID id, UUID workspaceId, int rating, String content, String supportNote, FeedbackStatus status, UUID reviewedBy, OffsetDateTime reviewedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
}

