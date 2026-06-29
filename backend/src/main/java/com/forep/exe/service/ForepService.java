package com.forep.exe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.ai.AiServiceClient;
import com.forep.exe.ai.AiServiceClient.AiEmployeeWorkload;
import com.forep.exe.ai.AiServiceClient.AiRecommendAssigneeInput;
import com.forep.exe.domain.Enums.AiSuggestionStatus;
import com.forep.exe.domain.Enums.AiSuggestionType;
import com.forep.exe.domain.Enums.Role;
import com.forep.exe.domain.Enums.TaskPriority;
import com.forep.exe.domain.Enums.TaskStatus;
import com.forep.exe.domain.Enums.UpdateType;
import com.forep.exe.domain.Enums.UserStatus;
import com.forep.exe.domain.Enums.WorkloadLevel;
import com.forep.exe.dto.Requests.AssignTaskRequest;
import com.forep.exe.dto.Requests.CreateEmployeeRequest;
import com.forep.exe.dto.Requests.CreateTaskRequest;
import com.forep.exe.dto.Requests.DailyReportRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ForepService {
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
        UserEntity user = users.findFirstByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()) || user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng.");
        }
        String token = jwtService.issue(new AuthenticatedUser(user.getId(), user.getWorkspaceId(), user.getRole(), user.getEmail()));
        return new LoginView(token, toUserView(user));
    }

    public WorkspaceView registerWorkspace(RegisterWorkspaceRequest request) {
        if (users.findFirstByEmailIgnoreCase(request.ownerEmail()).isPresent()) {
            throw new IllegalArgumentException("Email owner đã tồn tại.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setName(request.workspaceName());
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
        OffsetDateTime now = OffsetDateTime.now();
        UserEntity employee = new UserEntity();
        employee.setWorkspaceId(workspaceId);
        employee.setFullName(request.fullName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        employee.setRole(Role.EMPLOYEE);
        employee.setStatus(UserStatus.ACTIVE);
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        return toUserView(users.save(employee));
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
        return new OwnerDashboardView(total, active, completed, overdue, workload(), scopedTasks.stream().limit(5).map(this::toTaskView).toList(), recommendAssignee(null));
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
        List<WorkloadView> currentWorkload = workload();
        List<AssigneeRecommendationView> recommendations;
        if (request != null) {
            try {
                recommendations = aiServiceClient.recommendAssignee(new AiRecommendAssigneeInput(
                        request.title(),
                        request.requirements(),
                        request.deadline().toString(),
                        request.estimatedHours() == null ? 0 : request.estimatedHours().doubleValue(),
                        currentWorkload.stream().map(AiEmployeeWorkload::from).toList()
                ));
                saveAiSuggestion(AiSuggestionType.ASSIGNEE_RECOMMENDATION, request, recommendations);
                return recommendations;
            } catch (RuntimeException ignored) {
            }
        }
        recommendations = currentWorkload.stream()
                .filter(item -> item.workloadLevel() != WorkloadLevel.OVERLOADED)
                .sorted(Comparator.comparing(WorkloadView::workloadScore))
                .limit(3)
                .map(item -> new AssigneeRecommendationView(item.employeeId(), item.fullName(), score(item), item.workloadLevel(), reason(item), item.overdueTasks() > 0 ? "Có task quá hạn, cần cân nhắc." : "Không có"))
                .toList();
        if (request != null) saveAiSuggestion(AiSuggestionType.ASSIGNEE_RECOMMENDATION, request, recommendations);
        return recommendations;
    }

    public Map<String, Object> workloadSummary() {
        requireOwner();
        List<WorkloadView> currentWorkload = workload();
        Map<String, Object> output;
        try {
            output = aiServiceClient.workloadSummary(currentWorkload);
        } catch (RuntimeException ignored) {
            long overloaded = currentWorkload.stream().filter(item -> item.workloadLevel() == WorkloadLevel.OVERLOADED).count();
            long idle = currentWorkload.stream().filter(item -> item.workloadLevel() == WorkloadLevel.NO_WORK).count();
            long overdue = currentWorkload.stream().filter(item -> item.overdueTasks() > 0).count();
            output = Map.of("summary", "Có " + overloaded + " nhân viên quá tải, " + idle + " nhân viên đang rảnh và " + overdue + " nhân viên có task quá hạn.", "source", "fallback");
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
        } catch (RuntimeException ignored) {
            return Map.of("risks", scopedTasks.stream().filter(this::isOverdue).map(task -> Map.of("taskId", task.getId(), "title", task.getTitle(), "riskLevel", "HIGH", "reason", "Task đã quá hạn.", "recommendedAction", "Yêu cầu nhân viên cập nhật trạng thái ngay.", "source", "fallback")).toList());
        }
    }

    public BusinessSummaryView businessSummary() {
        requireOwner();
        List<TaskEntity> scopedTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(currentUser().workspaceId());
        long completed = scopedTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        long overdue = scopedTasks.stream().filter(this::isOverdue).count();
        long overloaded = workload().stream().filter(item -> item.workloadLevel() == WorkloadLevel.OVERLOADED).count();
        long idle = workload().stream().filter(item -> item.workloadLevel() == WorkloadLevel.NO_WORK).count();
        BusinessSummaryView summary = new BusinessSummaryView(completed, overdue, overloaded, idle, "Tuần này có " + completed + " task hoàn thành, " + overdue + " task quá hạn, " + overloaded + " nhân viên quá tải và " + idle + " nhân viên đang rảnh.");
        saveAiSuggestion(AiSuggestionType.BUSINESS_SUMMARY, scopedTasks.stream().map(this::toTaskView).toList(), summary);
        return summary;
    }

    public Map<String, Object> dailyAiSummary() {
        BusinessSummaryView summary = businessSummary();
        try {
            Map<String, Object> output = aiServiceClient.dailySummary(summary);
            saveAiSuggestion(AiSuggestionType.BUSINESS_SUMMARY, summary, output);
            return output;
        } catch (RuntimeException ignored) {
            Map<String, Object> output = Map.of("summary", summary.summary(), "source", "fallback");
            saveAiSuggestion(AiSuggestionType.BUSINESS_SUMMARY, summary, output);
            return output;
        }
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

    private int score(WorkloadView workload) {
        int calculated = 100 - (int) workload.openTasks() * 6 - (int) workload.overdueTasks() * 12 - (int) workload.blockedTasks() * 8 - (int) Math.round(workload.estimatedWorkload().doubleValue() / 2);
        return Math.max(35, Math.min(98, calculated));
    }

    private WorkloadLevel level(long openTasks) {
        if (openTasks == 0) return WorkloadLevel.NO_WORK;
        if (openTasks <= 2) return WorkloadLevel.LOW;
        if (openTasks <= 5) return WorkloadLevel.NORMAL;
        if (openTasks <= 9) return WorkloadLevel.HIGH;
        return WorkloadLevel.OVERLOADED;
    }

    private String reason(WorkloadView workload) {
        if (workload.workloadLevel() == WorkloadLevel.NO_WORK) return "Chưa có task đang mở, phù hợp để nhận task tiếp theo.";
        if (workload.workloadLevel() == WorkloadLevel.LOW) return "Chỉ có " + workload.openTasks() + " task đang mở và không bị quá tải.";
        return "Workload ở mức bình thường, có thể nhận thêm nếu deadline phù hợp.";
    }

    private boolean isOverdue(TaskEntity task) {
        return task.getDeadline().isBefore(OffsetDateTime.now()) && !List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED).contains(task.getStatus());
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

    private WorkspaceView toWorkspaceView(WorkspaceEntity item) { return new WorkspaceView(item.getId(), item.getName(), item.getLogo(), item.getAddress(), item.getOwnerId(), item.getCreatedAt()); }
    private UserView toUserView(UserEntity item) { return new UserView(item.getId(), item.getWorkspaceId(), item.getFullName(), item.getEmail(), item.getPhone(), item.getRole(), item.getAvatar(), item.getStatus(), item.getCreatedAt(), item.getUpdatedAt()); }
    private TaskView toTaskView(TaskEntity item) { return new TaskView(item.getId(), item.getWorkspaceId(), item.getTitle(), item.getRequirements(), item.getDescription(), item.getAssigneeId(), item.getCreatorId(), item.getPriority(), item.getDeadline(), item.getEstimatedHours(), item.getProgressPercent(), item.getStatus(), item.getCreatedAt(), item.getUpdatedAt(), item.getCompletedAt()); }
    private TaskUpdateView toTaskUpdateView(TaskUpdateEntity item) { return new TaskUpdateView(item.getId(), item.getTaskId(), item.getUserId(), item.getProgressPercent(), item.getContent(), item.getAttachment(), item.getUpdateType(), item.getCreatedAt()); }
    private DailyReportView toDailyReportView(DailyReportEntity item) { return new DailyReportView(item.getId(), item.getWorkspaceId(), item.getUserId(), item.getReportDate(), item.getTodayCompleted(), item.getCurrentWork(), item.getBlockers(), item.getTomorrowPlan(), item.getReviewedAt(), item.getCreatedAt(), item.getUpdatedAt()); }
    private NotificationView toNotificationView(NotificationEntity item) { return new NotificationView(item.getId(), item.getWorkspaceId(), item.getUserId(), item.getType(), item.getTitle(), item.getMessage(), item.getRelatedEntityType(), item.getRelatedEntityId(), item.isRead(), item.getCreatedAt()); }
    private AiSuggestionView toAiSuggestionView(AiSuggestionEntity item) { return new AiSuggestionView(item.getId(), item.getWorkspaceId(), item.getType(), item.getInputData(), item.getOutputData(), item.getStatus(), item.getCreatedBy(), item.getCreatedAt()); }

    public record WorkspaceView(UUID id, String name, String logo, String address, UUID ownerId, OffsetDateTime createdAt) {}
    public record UserView(UUID id, UUID workspaceId, String fullName, String email, String phone, Role role, String avatar, UserStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record TaskView(UUID id, UUID workspaceId, String title, String requirements, String description, UUID assigneeId, UUID creatorId, TaskPriority priority, OffsetDateTime deadline, BigDecimal estimatedHours, int progressPercent, TaskStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime completedAt) {}
    public record TaskUpdateView(UUID id, UUID taskId, UUID userId, int progressPercent, String content, String attachment, UpdateType updateType, OffsetDateTime createdAt) {}
    public record DailyReportView(UUID id, UUID workspaceId, UUID userId, LocalDate reportDate, String todayCompleted, String currentWork, String blockers, String tomorrowPlan, OffsetDateTime reviewedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record NotificationView(UUID id, UUID workspaceId, UUID userId, String type, String title, String message, String relatedEntityType, UUID relatedEntityId, boolean isRead, OffsetDateTime createdAt) {}
    public record WorkloadView(UUID employeeId, String fullName, long openTasks, long inProgressTasks, long blockedTasks, long completedTasks, long overdueTasks, BigDecimal estimatedWorkload, double workloadScore, WorkloadLevel workloadLevel) {}
    public record AssigneeRecommendationView(UUID employeeId, String fullName, int score, WorkloadLevel workloadLevel, String reason, String risk) {}
    public record OwnerDashboardView(long totalTasks, long activeTasks, long completedTasks, long overdueTasks, List<WorkloadView> employeeWorkload, List<TaskView> recentlyUpdatedTasks, List<AssigneeRecommendationView> aiRecommendations) {}
    public record BusinessSummaryView(long completedTasks, long overdueTasks, long overloadedEmployees, long idleEmployees, String summary) {}
    public record LoginView(String token, UserView user) {}
    public record AiSuggestionView(UUID id, UUID workspaceId, AiSuggestionType type, String inputData, String outputData, AiSuggestionStatus status, UUID createdBy, OffsetDateTime createdAt) {}
}
