package com.forep.exe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.ai.AiProviderException;
import com.forep.exe.ai.AiServiceClient;
import com.forep.exe.ai.AiServiceClient.AiEmployeeWorkload;
import com.forep.exe.ai.AiServiceClient.AiRecommendAssigneeInput;
import com.forep.exe.domain.Enums.AssignmentType;
import com.forep.exe.domain.Enums.AttachmentType;
import com.forep.exe.domain.Enums.AiHistoryStatus;
import com.forep.exe.domain.Enums.AiSuggestionStatus;
import com.forep.exe.domain.Enums.AiSuggestionType;
import com.forep.exe.domain.Enums.EmployeeLevel;
import com.forep.exe.domain.Enums.EmploymentType;
import com.forep.exe.domain.Enums.DepartmentStatus;
import com.forep.exe.domain.Enums.FeedbackStatus;
import com.forep.exe.domain.Enums.JobPositionStatus;
import com.forep.exe.domain.Enums.PaymentMethod;
import com.forep.exe.domain.Enums.PaymentStatus;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.domain.Enums.Permission;
import com.forep.exe.domain.Enums.RegistrationStatus;
import com.forep.exe.domain.Enums.PermissionGroup;
import com.forep.exe.domain.Enums.Role;
import com.forep.exe.domain.Enums.SeniorityLevel;
import com.forep.exe.domain.Enums.SubscriptionPlanStatus;
import com.forep.exe.domain.Enums.TaskParticipantRole;
import com.forep.exe.domain.Enums.TaskPriority;
import com.forep.exe.domain.Enums.TaskStatus;
import com.forep.exe.domain.Enums.UpdateType;
import com.forep.exe.domain.Enums.UserStatus;
import com.forep.exe.domain.Enums.WorkingStatus;
import com.forep.exe.domain.Enums.WorkloadLevel;
import com.forep.exe.domain.Enums.WorkspaceStatus;
import com.forep.exe.domain.Enums.WorkspaceSubscriptionStatus;
import com.forep.exe.dto.Requests.AdminCreateWorkspaceRequest;
import com.forep.exe.dto.Requests.AdminUpdateWorkspaceRequest;
import com.forep.exe.dto.Requests.AssignIndividualRequest;
import com.forep.exe.dto.Requests.AssignTaskRequest;
import com.forep.exe.dto.Requests.AssignTeamRequest;
import com.forep.exe.dto.Requests.BusinessFeedbackRequest;
import com.forep.exe.dto.Requests.BusinessPositionRequest;
import com.forep.exe.dto.Requests.ChangePasswordRequest;
import com.forep.exe.dto.Requests.CreateBusinessOwnerRequest;
import com.forep.exe.dto.Requests.CreateEmployeeRequest;
import com.forep.exe.dto.Requests.CreateHrAccountRequest;
import com.forep.exe.dto.Requests.CreatePaymentRequest;
import com.forep.exe.dto.Requests.CreateSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.CreateTaskRequest;
import com.forep.exe.dto.Requests.DailyReportRequest;
import com.forep.exe.dto.Requests.DepartmentRequest;
import com.forep.exe.dto.Requests.EmployeeReportAiRequest;
import com.forep.exe.dto.Requests.EstimateHoursRequest;
import com.forep.exe.dto.Requests.ExtractTasksRequest;
import com.forep.exe.dto.Requests.JobPositionRequest;
import com.forep.exe.dto.Requests.LoginRequest;
import com.forep.exe.dto.Requests.PaymentCallbackRequest;
import com.forep.exe.dto.Requests.RecommendAssigneeRequest;
import com.forep.exe.dto.Requests.RecommendationExplanationRequest;
import com.forep.exe.dto.Requests.RecommendationResultExplanationRequest;
import com.forep.exe.dto.Requests.RegisterWorkspaceRequest;
import com.forep.exe.dto.Requests.ReturnTaskRequest;
import com.forep.exe.dto.Requests.ReviewBusinessFeedbackRequest;
import com.forep.exe.dto.Requests.ReviewRegistrationRequest;
import com.forep.exe.dto.Requests.SelectSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.SubmitTaskCompletionRequest;
import com.forep.exe.dto.Requests.SubmitPaymentRequest;
import com.forep.exe.dto.Requests.TaskAttachmentRequest;
import com.forep.exe.dto.Requests.TaskDomainAnalysisRequest;
import com.forep.exe.dto.Requests.UpdateSubscriptionPlanRequest;
import com.forep.exe.dto.Requests.UpdatePaymentQrSettingRequest;
import com.forep.exe.dto.Requests.UpdateEmployeeRequest;
import com.forep.exe.dto.Requests.UpdateProgressRequest;
import com.forep.exe.dto.Requests.UpdateTaskCustomerInfoRequest;
import com.forep.exe.dto.Requests.UpdateTaskRequest;
import com.forep.exe.dto.Requests.UpdateTaskStatusRequest;
import com.forep.exe.dto.Requests.UpdateWorkspaceRequest;
import com.forep.exe.dto.Requests.WorkloadRiskExplanationRequest;
import com.forep.exe.dto.Requests.WorkspaceRegistrationRequest;
import com.forep.exe.persistence.AiSuggestionEntity;
import com.forep.exe.persistence.AiSuggestionRepository;
import com.forep.exe.persistence.AiHistoryEntity;
import com.forep.exe.persistence.AiHistoryRepository;
import com.forep.exe.persistence.AuditLogEntity;
import com.forep.exe.persistence.AuditLogRepository;
import com.forep.exe.persistence.BusinessFeedbackEntity;
import com.forep.exe.persistence.BusinessFeedbackRepository;
import com.forep.exe.persistence.DailyReportEntity;
import com.forep.exe.persistence.DailyReportRepository;
import com.forep.exe.persistence.DepartmentEntity;
import com.forep.exe.persistence.DepartmentRepository;
import com.forep.exe.persistence.JobPositionEntity;
import com.forep.exe.persistence.JobPositionRepository;
import com.forep.exe.persistence.NotificationEntity;
import com.forep.exe.persistence.NotificationRepository;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.PaymentQrSettingEntity;
import com.forep.exe.persistence.PaymentQrSettingRepository;
import com.forep.exe.persistence.PaymentQrFileEntity;
import com.forep.exe.persistence.PaymentQrFileRepository;
import com.forep.exe.persistence.SubscriptionPlanEntity;
import com.forep.exe.persistence.SubscriptionPlanRepository;
import com.forep.exe.persistence.TaskAssigneeEntity;
import com.forep.exe.persistence.TaskAssigneeRepository;
import com.forep.exe.persistence.TaskAttachmentEntity;
import com.forep.exe.persistence.TaskAttachmentRepository;
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
import com.forep.exe.persistence.WorkspaceSubscriptionEntity;
import com.forep.exe.persistence.WorkspaceSubscriptionRepository;
import com.forep.exe.security.AuthenticatedUser;
import com.forep.exe.security.AuthorizationService;
import com.forep.exe.security.JwtService;
import com.forep.exe.security.SecurityContext;
import com.forep.exe.service.MomoPaymentService.ProviderPaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.Function;

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
    private final AiHistoryRepository aiHistory;
    private final SubscriptionPlanRepository subscriptionPlans;
    private final WorkspaceRegistrationRepository workspaceRegistrations;
    private final WorkspaceSubscriptionRepository workspaceSubscriptions;
    private final PaymentTransactionRepository paymentTransactions;
    private final PaymentQrSettingRepository paymentQrSettings;
    private final PaymentQrFileRepository paymentQrFiles;
    private final BusinessFeedbackRepository businessFeedback;
    private final TaskAssigneeRepository taskAssignees;
    private final TaskAttachmentRepository taskAttachments;
    private final DepartmentRepository departments;
    private final JobPositionRepository jobPositions;
    private final AuditLogRepository auditLogs;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityContext securityContext;
    private final AuthorizationService authorizationService;
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
                        AiHistoryRepository aiHistory,
                        SubscriptionPlanRepository subscriptionPlans,
                        WorkspaceRegistrationRepository workspaceRegistrations,
                        WorkspaceSubscriptionRepository workspaceSubscriptions,
                        PaymentTransactionRepository paymentTransactions,
                        PaymentQrSettingRepository paymentQrSettings,
                        PaymentQrFileRepository paymentQrFiles,
                        BusinessFeedbackRepository businessFeedback,
                        TaskAssigneeRepository taskAssignees,
                        TaskAttachmentRepository taskAttachments,
                        DepartmentRepository departments,
                        JobPositionRepository jobPositions,
                        AuditLogRepository auditLogs,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        SecurityContext securityContext,
                        AuthorizationService authorizationService,
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
        this.aiHistory = aiHistory;
        this.subscriptionPlans = subscriptionPlans;
        this.workspaceRegistrations = workspaceRegistrations;
        this.workspaceSubscriptions = workspaceSubscriptions;
        this.paymentTransactions = paymentTransactions;
        this.paymentQrSettings = paymentQrSettings;
        this.paymentQrFiles = paymentQrFiles;
        this.businessFeedback = businessFeedback;
        this.taskAssignees = taskAssignees;
        this.taskAttachments = taskAttachments;
        this.departments = departments;
        this.jobPositions = jobPositions;
        this.auditLogs = auditLogs;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityContext = securityContext;
        this.authorizationService = authorizationService;
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
        if (!isPlatformAdminRole(user.getRole())) {
            WorkspaceEntity workspace = requireWorkspace(user.getWorkspaceId());
            enforceWorkspaceLoginAllowed(workspace);
            workspace.setLastActivityAt(OffsetDateTime.now());
            workspaces.save(workspace);
        }
        String token = jwtService.issue(new AuthenticatedUser(user.getId(), user.getWorkspaceId(), user.getRole(), user.getEmail()));
        return new LoginView(token, toUserView(user), authorizationService.permissionNamesFor(user.getRole()));
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
        user.setMustChangePassword(false);
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
        requireOwnerOrHr();
        return workspaceEmployees(currentUser().workspaceId()).stream().map(this::toUserView).toList();
    }

    public CreatedUserAccountView createEmployee(CreateEmployeeRequest request) {
        authorizationService.require(Permission.EMPLOYEE_CREATE);
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
        employee.setJobTitle(request.jobTitle());
        employee.setSeniorityLevel(request.seniorityLevel());
        employee.setSkillRating(request.skillRating());
        employee.setYearsOfExperience(request.yearsOfExperience());
        employee.setSkills(request.skills());
        applyEmployeeProfile(employee, request.departmentId(), request.jobPositionId(), request.dateOfBirth(), request.gender(), request.address(), request.personalSummary(), request.employmentType(), request.workingStatus(), request.employeeLevel(), request.monthlyWorkingCapacityHours(), request.mainExpertise(), request.secondaryExpertise());
        employee.setPasswordHash(passwordEncoder.encode(initialPassword));
        employee.setRole(roleForBusinessPosition(request.jobPositionId()));
        employee.setStatus(UserStatus.ACTIVE);
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        UserView created = toUserView(users.save(employee));
        workspace.setNextEmployeeNumber(workspace.getNextEmployeeNumber() + 1);
        workspaces.save(workspace);
        return new CreatedUserAccountView(created, username, initialPassword, true);
    }

    public UserView employee(UUID employeeId) {
        requireOwnerOrHr();
        UserEntity employee = requireEmployee(employeeId);
        return toUserView(employee);
    }

    public UserView updateEmployee(UUID employeeId, UpdateEmployeeRequest request) {
        authorizationService.require(Permission.EMPLOYEE_UPDATE);
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
        applyEmployeeProfile(employee, request.departmentId(), request.jobPositionId(), request.dateOfBirth(), request.gender(), request.address(), request.personalSummary(), request.employmentType(), request.workingStatus(), request.employeeLevel(), request.monthlyWorkingCapacityHours(), request.mainExpertise(), request.secondaryExpertise());
        employee.setRole(roleForBusinessPosition(request.jobPositionId()));
        employee.setUpdatedAt(OffsetDateTime.now());
        return toUserView(users.save(employee));
    }

    public UserView updateEmployeeStatus(UUID employeeId, UserStatus status) {
        authorizationService.require(Permission.EMPLOYEE_DEACTIVATE);
        UserEntity employee = requireEmployee(employeeId);
        employee.setStatus(status);
        employee.setUpdatedAt(OffsetDateTime.now());
        return toUserView(users.save(employee));
    }

    public CreatedUserAccountView resetEmployeePassword(UUID employeeId) {
        authorizationService.require(Permission.EMPLOYEE_UPDATE);
        UserEntity employee = requireEmployee(employeeId);
        String temporaryPassword = hasText(employee.getEmployeeCode()) ? employee.getEmployeeCode() : "Employee" + employee.getId().toString().substring(0, 8);
        employee.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        employee.setUpdatedAt(OffsetDateTime.now());
        employee = users.save(employee);
        audit(employee.getWorkspaceId(), "RESET_EMPLOYEE_PASSWORD", "USER", employee.getId(), null, Map.of("employeeCode", employee.getEmployeeCode()));
        return new CreatedUserAccountView(toUserView(employee), employee.getUsername(), temporaryPassword, true);
    }

    public List<UserView> hrAccounts() {
        authorizationService.require(Permission.HR_ACCOUNT_MANAGE);
        return users.findByWorkspaceIdAndRoleOrderByFullNameAsc(currentUser().workspaceId(), Role.HR).stream()
                .map(this::toUserView)
                .toList();
    }

    public GeneratedHrAccountView createInitialHrAccount(CreateHrAccountRequest request) {
        authorizationService.require(Permission.HR_ACCOUNT_MANAGE);
        UUID workspaceId = currentUser().workspaceId();
        if (users.existsByWorkspaceIdAndEmailIgnoreCase(workspaceId, request.email())) {
            throw new IllegalArgumentException("Email already exists in this workspace.");
        }
        WorkspaceEntity workspace = requireWorkspace(workspaceId);
        enforceWorkspaceUserLimit(workspace);
        String username = nextHrUsername(workspace);
        String temporaryPassword = secureTemporaryPassword();
        OffsetDateTime now = OffsetDateTime.now();
        UserEntity hr = new UserEntity();
        hr.setWorkspaceId(workspaceId);
        hr.setFullName(request.fullName());
        hr.setEmail(request.email());
        hr.setPhone(request.phone());
        hr.setUsername(username);
        hr.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        hr.setMustChangePassword(true);
        hr.setInitialAccountGenerated(true);
        hr.setRole(Role.HR);
        hr.setStatus(UserStatus.ACTIVE);
        hr.setCreatedAt(now);
        hr.setUpdatedAt(now);
        hr = users.save(hr);
        audit(workspaceId, "BUSINESS_OWNER_CREATE_HR_ACCOUNT", "USER", hr.getId(), null,
                Map.of("username", username, "email", request.email()));
        return new GeneratedHrAccountView(hr.getId(), username, temporaryPassword, hr.getFullName(), true);
    }

    public UserView updateHrAccountStatus(UUID hrAccountId, UserStatus status) {
        authorizationService.require(Permission.HR_ACCOUNT_MANAGE);
        UserEntity hr = users.findById(hrAccountId)
                .filter(user -> currentUser().workspaceId().equals(user.getWorkspaceId()) && user.getRole() == Role.HR)
                .orElseThrow(() -> new IllegalArgumentException("HR account not found in this workspace."));
        hr.setStatus(status);
        hr.setUpdatedAt(OffsetDateTime.now());
        hr = users.save(hr);
        audit(hr.getWorkspaceId(), "BUSINESS_OWNER_UPDATE_HR_STATUS", "USER", hr.getId(), null, Map.of("status", status.name()));
        return toUserView(hr);
    }

    public List<TaskView> tasks() {
        AuthenticatedUser user = currentUser();
        List<TaskEntity> scoped = isBusinessOwnerRole(user.role()) || isManagerOrExecutiveRole(user.role()) || user.role() == Role.HR
                ? tasks.findByWorkspaceIdOrderByCreatedAtDesc(user.workspaceId())
                : visibleTasksForEmployee(user);
        return scoped.stream().map(this::toTaskView).toList();
    }

    public List<AiHistoryView> aiHistory(String functionName, AiHistoryStatus status, OffsetDateTime from, OffsetDateTime to, String caller, Integer limit, Integer offset) {
        requireOwnerOrHrOrManager();
        UUID workspaceId = currentUser().workspaceId();
        Specification<AiHistoryEntity> spec = (root, query, builder) -> builder.equal(root.get("workspaceId"), workspaceId);
        if (hasText(functionName)) {
            String keyword = "%" + functionName.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, builder) -> builder.like(builder.lower(root.get("functionName")), keyword));
        }
        if (status != null) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("calledAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("calledAt"), to));
        }
        if (hasText(caller)) {
            String keyword = "%" + caller.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, builder) -> builder.like(builder.lower(root.get("callerName")), keyword));
        }
        int safeLimit = Math.max(1, Math.min(limit == null ? 100 : limit, 500));
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        return aiHistory.findAll(spec, Sort.by(Sort.Direction.DESC, "calledAt")).stream()
                .skip(safeOffset)
                .limit(safeLimit)
                .map(this::toAiHistoryView)
                .toList();
    }

    public TaskView createTask(CreateTaskRequest request) {
        requireTaskManager();
        UUID workspaceId = currentUser().workspaceId();
        AssignmentPlan assignment = assignmentPlan(request.assignmentType(), request.assigneeId(), request.teamLeaderId(), request.teamMemberIds());
        OffsetDateTime now = OffsetDateTime.now();
        TaskEntity task = new TaskEntity();
        task.setWorkspaceId(workspaceId);
        task.setTitle(request.title());
        task.setRequirements(request.requirements());
        task.setDescription(request.description());
        task.setAssignmentType(assignment.assignmentType());
        task.setAssigneeId(assignment.primaryAssigneeId());
        task.setCreatorId(currentUser().userId());
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setDeadline(request.deadline());
        task.setStartDate(request.startDate());
        task.setEstimatedHours(validEstimatedHours(request.estimatedHours()));
        task.setDifficulty(request.difficulty());
        task.setRequiredSkills(request.requiredSkills());
        task.setTaskDomain(request.taskDomain());
        task.setProjectId(request.projectId());
        applyTaskRequirementContext(task, request.departmentId(), request.requiredJobPositionId());
        task.setProgressPercent(0);
        task.setStatus(TaskStatus.ASSIGNED);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task = tasks.save(task);
        saveTaskParticipants(task, assignment);
        saveTaskAttachments(task, request.attachments());
        createNotification(workspaceId, currentUser().userId(), "TASK_CREATED", "Bạn vừa tạo task mới", "Bạn vừa tạo task mới: " + task.getTitle(), "TASK", task.getId());
        notifyParticipants(task, "TASK_ASSIGNED", "Task mới được giao", "Bạn vừa được giao task mới: " + task.getTitle());
        return toTaskView(task);
    }

    public TaskView task(UUID taskId) {
        TaskEntity task = requireTask(taskId);
        requireTaskVisible(task);
        return toTaskView(task);
    }

    public TaskView updateTask(UUID taskId, UpdateTaskRequest request) {
        requireTaskManager();
        TaskEntity task = requireTask(taskId);
        AssignmentPlan assignment = assignmentPlan(request.assignmentType(), request.assigneeId(), request.teamLeaderId(), request.teamMemberIds());
        task.setTitle(request.title());
        task.setRequirements(request.requirements());
        task.setDescription(request.description());
        task.setCustomerPhone(request.customerPhone());
        task.setCustomerEmail(request.customerEmail());
        task.setCustomerDescription(request.customerDescription());
        task.setAssignmentType(assignment.assignmentType());
        task.setAssigneeId(assignment.primaryAssigneeId());
        task.setPriority(request.priority() == null ? task.getPriority() : request.priority());
        task.setDeadline(request.deadline());
        task.setStartDate(request.startDate());
        task.setEstimatedHours(validEstimatedHours(request.estimatedHours()));
        task.setDifficulty(request.difficulty());
        task.setRequiredSkills(request.requiredSkills());
        task.setTaskDomain(request.taskDomain());
        task.setProjectId(request.projectId());
        applyTaskRequirementContext(task, request.departmentId(), request.requiredJobPositionId());
        task.setUpdatedAt(OffsetDateTime.now());
        task = tasks.save(task);
        saveTaskParticipants(task, assignment);
        replaceTaskAttachments(task, request.attachments());
        return toTaskView(task);
    }

    public TaskView updateTaskCustomerInfo(UUID taskId, UpdateTaskCustomerInfoRequest request) {
        TaskEntity task = requireTask(taskId);
        requireCanUpdateTaskCustomerInfo(task);
        task.setCustomerPhone(request.customerPhone());
        task.setCustomerEmail(request.customerEmail());
        task.setCustomerDescription(request.customerDescription());
        task.setUpdatedAt(OffsetDateTime.now());
        return toTaskView(tasks.save(task));
    }

    public TaskView assignTask(UUID taskId, AssignTaskRequest request) {
        return assignIndividual(taskId, new AssignIndividualRequest(request.assigneeId()));
    }

    public TaskView assignIndividual(UUID taskId, AssignIndividualRequest request) {
        requireTaskManager();
        requireActiveEmployee(request.employeeId());
        TaskEntity task = requireTask(taskId);
        task.setAssignmentType(AssignmentType.INDIVIDUAL);
        task.setAssigneeId(request.employeeId());
        task.setStatus(TaskStatus.ASSIGNED);
        task.setUpdatedAt(OffsetDateTime.now());
        task = tasks.save(task);
        saveTaskParticipants(task, new AssignmentPlan(AssignmentType.INDIVIDUAL, request.employeeId(), request.employeeId(), List.of(request.employeeId())));
        createNotification(task.getWorkspaceId(), request.employeeId(), "TASK_ASSIGNED", "Task mới được giao", "Bạn vừa được giao task mới: " + task.getTitle(), "TASK", task.getId());
        return toTaskView(task);
    }

    public TaskView assignTeam(UUID taskId, AssignTeamRequest request) {
        requireTaskManager();
        TaskEntity task = requireTask(taskId);
        AssignmentPlan assignment = assignmentPlan(AssignmentType.TEAM, null, request.teamLeaderId(), request.teamMemberIds());
        task.setAssignmentType(AssignmentType.TEAM);
        task.setAssigneeId(assignment.primaryAssigneeId());
        task.setStatus(TaskStatus.ASSIGNED);
        task.setUpdatedAt(OffsetDateTime.now());
        task = tasks.save(task);
        saveTaskParticipants(task, assignment);
        notifyParticipants(task, "TASK_ASSIGNED", "Task nhóm mới được giao", "Bạn vừa được phân công vào task nhóm: " + task.getTitle());
        return toTaskView(task);
    }

    public TaskView updateStatus(UUID taskId, UpdateTaskStatusRequest request) {
        TaskEntity task = requireTask(taskId);
        if (List.of(TaskStatus.ACCEPTED, TaskStatus.SUBMITTED, TaskStatus.RETURNED, TaskStatus.COMPLETED).contains(request.status())) {
            throw new IllegalArgumentException("Vui lòng dùng endpoint workflow tương ứng: accept, submit-completion, approve-completion hoặc return.");
        }
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

    public TaskView acceptTask(UUID taskId) {
        TaskEntity task = requireTask(taskId);
        requireTaskParticipant(task);
        if (!List.of(TaskStatus.ASSIGNED, TaskStatus.RETURNED).contains(task.getStatus())) {
            throw new IllegalArgumentException("Chỉ task mới được giao hoặc bị trả lại mới có thể accept.");
        }
        applyTaskStatus(task, TaskStatus.ACCEPTED, task.getProgressPercent());
        task = tasks.save(task);
        saveTaskUpdate(task, task.getProgressPercent(), "Đã accept task.", null, UpdateType.ACCEPTANCE);
        createNotification(task.getWorkspaceId(), task.getCreatorId(), "TASK_ACCEPTED", "Nhân viên đã accept task", currentUserEntity().getFullName() + " đã accept task: " + task.getTitle(), "TASK", task.getId());
        return toTaskView(task);
    }

    public TaskView submitTaskCompletion(UUID taskId, SubmitTaskCompletionRequest request) {
        TaskEntity task = requireTask(taskId);
        requireCompletionSubmitter(task);
        if (List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED).contains(task.getStatus())) {
            throw new IllegalArgumentException("Task đã đóng nên không thể submit completion.");
        }
        applyTaskStatus(task, TaskStatus.SUBMITTED, 100);
        task = tasks.save(task);
        saveTaskUpdate(task, task.getProgressPercent(), request.content(), request.attachment(), UpdateType.COMPLETION);
        createNotification(task.getWorkspaceId(), task.getCreatorId(), "TASK_COMPLETION_SUBMITTED", "Task chờ xác nhận hoàn thành", currentUserEntity().getFullName() + " đã gửi hoàn thành task: " + task.getTitle(), "TASK", task.getId());
        return toTaskView(task);
    }

    public TaskView approveTaskCompletion(UUID taskId) {
        requireTaskManager();
        TaskEntity task = requireTask(taskId);
        if (task.getStatus() != TaskStatus.SUBMITTED) {
            throw new IllegalArgumentException("Chỉ task đang chờ xác nhận hoàn thành mới được approve.");
        }
        applyTaskStatus(task, TaskStatus.COMPLETED, 100);
        task = tasks.save(task);
        saveTaskUpdate(task, task.getProgressPercent(), "Manager đã xác nhận task hoàn thành.", null, UpdateType.COMPLETION_APPROVAL);
        notifyParticipants(task, "TASK_COMPLETION_APPROVED", "Task đã được xác nhận hoàn thành", "Task đã được xác nhận hoàn thành: " + task.getTitle());
        return toTaskView(task);
    }

    public TaskView returnTask(UUID taskId, ReturnTaskRequest request) {
        requireTaskManager();
        TaskEntity task = requireTask(taskId);
        if (task.getStatus() != TaskStatus.SUBMITTED) {
            throw new IllegalArgumentException("Chỉ task đang chờ xác nhận hoàn thành mới được return.");
        }
        applyTaskStatus(task, TaskStatus.RETURNED, Math.min(task.getProgressPercent(), 95));
        task = tasks.save(task);
        saveTaskUpdate(task, task.getProgressPercent(), request.reason(), request.attachment(), UpdateType.RETURN);
        notifyParticipants(task, "TASK_RETURNED", "Task bị trả lại để chỉnh sửa", "Task cần chỉnh sửa thêm: " + task.getTitle() + ". Lý do: " + request.reason());
        return toTaskView(task);
    }

    public TaskUpdateView updateProgress(UUID taskId, UpdateProgressRequest request) {
        TaskEntity task = requireTask(taskId);
        requireOwnerOrAssignee(task);
        if (List.of(UpdateType.ACCEPTANCE, UpdateType.COMPLETION_APPROVAL, UpdateType.RETURN).contains(request.updateType())) {
            throw new IllegalArgumentException("Update type này chỉ được tạo bởi workflow endpoint tương ứng.");
        }
        if (request.updateType() == UpdateType.BLOCKER && request.content().isBlank()) {
            throw new IllegalArgumentException("Task bị blocker phải có nội dung cập nhật.");
        }
        int nextProgress = request.updateType() == UpdateType.COMPLETION ? 100 : request.progressPercent();
        TaskStatus nextStatus = switch (request.updateType()) {
            case BLOCKER -> TaskStatus.BLOCKED;
            case COMPLETION -> TaskStatus.SUBMITTED;
            case ACCEPTANCE -> TaskStatus.ACCEPTED;
            case COMPLETION_APPROVAL -> TaskStatus.COMPLETED;
            case RETURN -> TaskStatus.RETURNED;
            case PROGRESS -> nextProgress >= 100 ? TaskStatus.SUBMITTED : TaskStatus.IN_PROGRESS;
        };
        applyTaskStatus(task, nextStatus, nextProgress);
        tasks.save(task);

        TaskUpdateEntity update = saveTaskUpdate(task, task.getProgressPercent(), request.content(), request.attachment(), request.updateType());
        if (request.updateType() == UpdateType.BLOCKER) {
            String assigneeName = employeeNames().getOrDefault(task.getAssigneeId(), "Thành viên");
            createNotification(task.getWorkspaceId(), task.getCreatorId(), "TASK_BLOCKED_OWNER", "Thành viên có vướng mắc về task", assigneeName + " có vướng mắc về task: " + task.getTitle(), "TASK", task.getId());
            if (currentUser().userId().equals(task.getAssigneeId())) {
                createNotification(task.getWorkspaceId(), task.getAssigneeId(), "TASK_BLOCKER_REQUESTED", "Bạn đã yêu cầu hỏi vướng mắc", "Bạn đã yêu cầu hỏi vướng mắc task " + task.getTitle() + ".", "TASK", task.getId());
            }
        } else if (request.updateType() == UpdateType.COMPLETION && currentUser().userId().equals(task.getAssigneeId())) {
            createNotification(task.getWorkspaceId(), task.getCreatorId(), "TASK_COMPLETION_SUBMITTED", "Task chờ xác nhận hoàn thành", currentUserEntity().getFullName() + " đã gửi hoàn thành task: " + task.getTitle(), "TASK", task.getId());
        }
        return toTaskUpdateView(update);
    }

    public List<TaskUpdateView> taskUpdates(UUID taskId) {
        TaskEntity task = requireTask(taskId);
        requireTaskVisible(task);
        return taskUpdates.findByTaskIdOrderByCreatedAtAsc(taskId).stream().map(this::toTaskUpdateView).toList();
    }

    public List<TaskAttachmentView> taskAttachments(UUID taskId) {
        TaskEntity task = requireTask(taskId);
        requireTaskVisible(task);
        return taskAttachments.findByTaskIdOrderByCreatedAtAsc(task.getId()).stream().map(this::toTaskAttachmentView).toList();
    }

    public TaskAttachmentView addTaskAttachment(UUID taskId, TaskAttachmentRequest request) {
        requireTaskManager();
        TaskEntity task = requireTask(taskId);
        return toTaskAttachmentView(saveTaskAttachment(task, request));
    }

    public List<DepartmentView> departments() {
        requireOwnerOrHr();
        return departments.findByWorkspaceIdOrderByNameAsc(currentUser().workspaceId()).stream().map(this::toDepartmentView).toList();
    }

    public DepartmentView department(UUID id) {
        requireOwnerOrHr();
        return toDepartmentView(requireDepartment(id));
    }

    public DepartmentView createDepartment(DepartmentRequest request) {
        requireHr();
        DepartmentEntity department = new DepartmentEntity();
        applyDepartmentRequest(department, request);
        OffsetDateTime now = OffsetDateTime.now();
        department.setCreatedAt(now);
        department.setUpdatedAt(now);
        return toDepartmentView(departments.save(department));
    }

    public DepartmentView updateDepartment(UUID id, DepartmentRequest request) {
        requireHr();
        DepartmentEntity department = requireDepartment(id);
        applyDepartmentRequest(department, request);
        department.setUpdatedAt(OffsetDateTime.now());
        return toDepartmentView(departments.save(department));
    }

    public DepartmentView updateDepartmentStatus(UUID id, DepartmentStatus status) {
        requireHr();
        DepartmentEntity department = requireDepartment(id);
        if (status == DepartmentStatus.INACTIVE && jobPositions.existsByWorkspaceIdAndDepartmentIdAndStatus(department.getWorkspaceId(), department.getId(), JobPositionStatus.ACTIVE)) {
            throw new IllegalArgumentException("Không thể deactivate phòng ban khi còn business position ACTIVE. Hãy deactivate các business position trước.");
        }
        if (status == DepartmentStatus.INACTIVE && users.existsByWorkspaceIdAndDepartmentIdAndStatus(department.getWorkspaceId(), department.getId(), UserStatus.ACTIVE)) {
            throw new IllegalArgumentException("Không thể deactivate phòng ban khi còn nhân viên ACTIVE. Hãy chuyển nhân viên sang phòng ban khác trước.");
        }
        if (status == DepartmentStatus.INACTIVE && tasks.existsByWorkspaceIdAndDepartmentIdAndStatusIn(department.getWorkspaceId(), department.getId(), openTaskStatuses())) {
            throw new IllegalArgumentException("Không thể deactivate phòng ban khi còn task chưa hoàn tất đang tham chiếu phòng ban này.");
        }
        department.setStatus(status);
        department.setUpdatedAt(OffsetDateTime.now());
        return toDepartmentView(departments.save(department));
    }

    public List<JobPositionView> jobPositions() {
        requireOwnerOrHr();
        return jobPositions.findByWorkspaceIdOrderByNameAsc(currentUser().workspaceId()).stream().map(this::toJobPositionView).toList();
    }

    public JobPositionView createJobPosition(JobPositionRequest request) {
        requireHr();
        JobPositionEntity item = legacyBusinessPosition(null, request.title(), request.permissionGroup(), request.departmentId(), request.departmentName(), request.description(), request.requiredSkills(), request.status());
        return toJobPositionView(jobPositions.save(item));
    }

    public JobPositionView updateJobPosition(UUID id, JobPositionRequest request) {
        requireHr();
        JobPositionEntity item = requireJobPosition(id);
        item = legacyBusinessPosition(item, request.title(), request.permissionGroup(), request.departmentId(), request.departmentName(), request.description(), request.requiredSkills(), request.status());
        return toJobPositionView(jobPositions.save(item));
    }

    public JobPositionView updateJobPositionStatus(UUID id, JobPositionStatus status) {
        requireHr();
        JobPositionEntity item = requireJobPosition(id);
        item.setStatus(status);
        item.setUpdatedAt(OffsetDateTime.now());
        return toJobPositionView(jobPositions.save(item));
    }

    public List<BusinessPositionView> businessPositions(String search, UUID departmentId, PermissionGroup permissionGroup, JobPositionStatus status) {
        requireOwnerOrHr();
        return jobPositions.findByWorkspaceIdOrderByNameAsc(currentUser().workspaceId()).stream()
                .filter(position -> !hasText(search) || normalizedSearchText(position.getTitle()).contains(normalizedSearchText(search)))
                .filter(position -> departmentId == null || departmentId.equals(position.getDepartmentId()))
                .filter(position -> permissionGroup == null || permissionGroup == position.getPermissionGroup())
                .filter(position -> status == null || status == position.getStatus())
                .map(this::toBusinessPositionView)
                .toList();
    }

    public BusinessPositionView businessPosition(UUID id) {
        requireOwnerOrHr();
        return toBusinessPositionView(requireJobPosition(id));
    }

    public BusinessPositionView createBusinessPosition(BusinessPositionRequest request) {
        requireHr();
        JobPositionEntity item = new JobPositionEntity();
        applyBusinessPositionRequest(item, request);
        OffsetDateTime now = OffsetDateTime.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return toBusinessPositionView(jobPositions.save(item));
    }

    public BusinessPositionView updateBusinessPosition(UUID id, BusinessPositionRequest request) {
        requireHr();
        JobPositionEntity item = requireJobPosition(id);
        applyBusinessPositionRequest(item, request);
        item.setUpdatedAt(OffsetDateTime.now());
        return toBusinessPositionView(jobPositions.save(item));
    }

    public BusinessPositionView updateBusinessPositionStatus(UUID id, JobPositionStatus status) {
        requireHr();
        JobPositionEntity item = requireJobPosition(id);
        if (status == JobPositionStatus.INACTIVE && users.existsByWorkspaceIdAndJobPositionIdAndStatus(item.getWorkspaceId(), item.getId(), UserStatus.ACTIVE)) {
            throw new IllegalArgumentException("Không thể deactivate business position khi còn nhân viên ACTIVE đang sử dụng vị trí này.");
        }
        if (status == JobPositionStatus.INACTIVE && tasks.existsByWorkspaceIdAndRequiredJobPositionIdAndStatusIn(item.getWorkspaceId(), item.getId(), openTaskStatuses())) {
            throw new IllegalArgumentException("Không thể deactivate business position khi còn task chưa hoàn tất đang yêu cầu vị trí này.");
        }
        item.setStatus(status);
        item.setUpdatedAt(OffsetDateTime.now());
        return toBusinessPositionView(jobPositions.save(item));
    }

    public List<AssigneeRecommendationView> recommendTeamLeaders(RecommendAssigneeRequest request) {
        requireTaskManager();
        if (request == null) return List.of();
        RecommendAssigneeRequest effectiveRequest = enrichRecommendationRequestWithDomainAnalysis(request);
        List<AiEmployeeWorkload> candidates = assigneeCandidates(effectiveRequest);
        if (candidates.isEmpty()) return List.of();
        enforceAiUsageLimit();
        List<AssigneeRecommendationView> recommendations = candidates.stream()
                .sorted(Comparator.comparing(AiEmployeeWorkload::leadershipScore).reversed())
                .limit(3)
                .map(this::teamLeaderRecommendation)
                .toList();
        saveAiSuggestion(AiSuggestionType.ASSIGNEE_RECOMMENDATION, Map.of("type", "TEAM_LEADER", "request", effectiveRequest), recommendations);
        return recommendations;
    }

    public List<AssigneeRecommendationView> recommendTeamMembers(RecommendAssigneeRequest request) {
        requireTaskManager();
        if (request == null) return List.of();
        RecommendAssigneeRequest effectiveRequest = enrichRecommendationRequestWithDomainAnalysis(request);
        List<AiEmployeeWorkload> candidates = assigneeCandidates(effectiveRequest);
        if (candidates.isEmpty()) return List.of();
        enforceAiUsageLimit();
        List<AssigneeRecommendationView> recommendations = candidates.stream()
                .sorted(Comparator.comparing(AiEmployeeWorkload::teamMemberScore).reversed())
                .limit(5)
                .map(this::teamMemberRecommendation)
                .toList();
        saveAiSuggestion(AiSuggestionType.ASSIGNEE_RECOMMENDATION, Map.of("type", "TEAM_MEMBER", "request", effectiveRequest), recommendations);
        return recommendations;
    }

    public List<MonthlyWorkloadView> monthlyWorkload(int year, int month) {
        requireTaskManager();
        YearMonth yearMonth = YearMonth.of(year, month);
        List<TaskEntity> scopedTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(currentUser().workspaceId());
        return workspaceEmployees(currentUser().workspaceId()).stream()
                .map(employee -> monthlyWorkloadForEmployee(employee, scopedTasks, yearMonth))
                .toList();
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
        List<DailyReportEntity> scoped = isBusinessOwnerRole(user.role()) || isManagerOrExecutiveRole(user.role())
                ? reports.findByWorkspaceIdOrderByReportDateDesc(user.workspaceId())
                : reports.findByWorkspaceIdAndUserIdOrderByReportDateDesc(user.workspaceId(), user.userId());
        return scoped.stream().map(this::toDailyReportView).toList();
    }

    public DailyReportView report(UUID reportId) {
        DailyReportEntity report = requireReport(reportId);
        if (!isBusinessOwnerRole(currentUser().role()) && !isManagerOrExecutiveRole(currentUser().role()) && !report.getUserId().equals(currentUser().userId())) {
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

    public Map<String, Object> ownerDashboard() {
        requireOwner();
        UUID workspaceId = currentUser().workspaceId();
        List<TaskEntity> scopedTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        List<WorkloadView> currentWorkload = workload();
        LocalDate today = LocalDate.now();

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("overviewCards", Map.of(
                "today", ownerDashboardOverview(today, today, scopedTasks, currentWorkload),
                "week", ownerDashboardOverview(today.minusDays(6), today, scopedTasks, currentWorkload),
                "month", ownerDashboardOverview(today.withDayOfMonth(1), today, scopedTasks, currentWorkload)
        ));
        output.put("dailyReportInsight", ownerDashboardDailyReportInsight(workspaceId, today));
        output.put("workloadInsight", ownerDashboardWorkloadInsight(currentWorkload));
        output.put("deadlineRisks", scopedTasks.stream()
                .filter(this::isOpenTask)
                .filter(task -> isOverdue(task) || !task.getDeadline().isAfter(OffsetDateTime.now().plusDays(3)))
                .sorted(Comparator.comparingDouble(this::taskRiskScore).reversed())
                .limit(10)
                .map(this::dashboardTaskItem)
                .toList());
        output.put("blockedTasks", scopedTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.BLOCKED)
                .sorted(Comparator.comparing(TaskEntity::getUpdatedAt).reversed())
                .limit(10)
                .map(this::dashboardTaskItem)
                .toList());
        output.put("taskStatusChart", enumCountChart(
                "Task theo trạng thái",
                Arrays.stream(TaskStatus.values()).map(Enum::name).toList(),
                status -> scopedTasks.stream().filter(task -> task.getStatus().name().equals(status)).count()
        ));
        output.put("workloadDistributionChart", enumCountChart(
                "Phân bổ tải công việc",
                Arrays.stream(WorkloadLevel.values()).map(Enum::name).toList(),
                level -> currentWorkload.stream().filter(item -> item.workloadLevel().name().equals(level)).count()
        ));
        output.put("recentlyUpdatedTasks", scopedTasks.stream().limit(5).map(this::toTaskView).toList());
        output.put("aiRecommendations", cachedDashboardRecommendations());
        output.put("recommendedActions", ownerDashboardRecommendedActions(scopedTasks, currentWorkload, output.get("dailyReportInsight")));
        output.put("metadata", Map.of(
                "generatedAt", OffsetDateTime.now().toString(),
                "dataSource", "BACKEND_COMPUTED",
                "emptyState", scopedTasks.isEmpty() && currentWorkload.isEmpty(),
                "note", scopedTasks.isEmpty() ? "Chưa có task trong workspace; các chỉ số trả về 0 và danh sách rỗng." : "Backend đã tính số liệu từ task, workload và daily report hiện có."
        ));
        return output;
    }

    private Map<String, Object> ownerDashboardOverview(LocalDate start, LocalDate end, List<TaskEntity> scopedTasks, List<WorkloadView> currentWorkload) {
        List<TaskEntity> periodTasks = scopedTasks.stream()
                .filter(task -> isTaskInPeriod(task, start, end))
                .toList();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("completedTasks", periodTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count());
        output.put("activeTasks", periodTasks.stream().filter(task -> openTaskStatuses().contains(task.getStatus())).count());
        output.put("overdueTasks", periodTasks.stream().filter(this::isOverdue).count());
        output.put("blockedTasks", periodTasks.stream().filter(task -> task.getStatus() == TaskStatus.BLOCKED).count());
        output.put("submittedTasks", periodTasks.stream().filter(task -> task.getStatus() == TaskStatus.SUBMITTED).count());
        output.put("missingDailyReports", missingReportCount(end));
        output.put("overloadedEmployees", currentWorkload.stream().filter(item -> item.workloadLevel() == WorkloadLevel.OVERLOADED).count());
        output.put("completionRate", percentage(
                periodTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count(),
                periodTasks.size()
        ));
        return output;
    }

    private Map<String, Object> ownerDashboardDailyReportInsight(UUID workspaceId, LocalDate reportDate) {
        List<UserEntity> activeEmployees = workspaceEmployees(workspaceId).stream()
                .filter(employee -> employee.getStatus() == UserStatus.ACTIVE)
                .toList();
        List<DailyReportEntity> reportList = reports.findByWorkspaceIdOrderByReportDateDesc(workspaceId).stream()
                .filter(report -> report.getReportDate().equals(reportDate))
                .toList();
        Set<UUID> submitted = reportList.stream().map(DailyReportEntity::getUserId).collect(java.util.stream.Collectors.toSet());
        List<Map<String, Object>> missingEmployees = activeEmployees.stream()
                .filter(employee -> !submitted.contains(employee.getId()))
                .map(employee -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("employeeId", employee.getId().toString());
                    item.put("fullName", employee.getFullName());
                    item.put("departmentId", employee.getDepartmentId() == null ? null : employee.getDepartmentId().toString());
                    item.put("businessPositionId", employee.getJobPositionId() == null ? null : employee.getJobPositionId().toString());
                    return item;
                })
                .toList();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("reportDate", reportDate.toString());
        output.put("expectedReports", activeEmployees.size());
        output.put("receivedReports", reportList.size());
        output.put("missingReports", missingEmployees.size());
        output.put("reportsWithIssues", reportList.stream().filter(report -> hasText(report.getBlockers())).count());
        output.put("reviewedReports", reportList.stream().filter(report -> report.getReviewedAt() != null).count());
        output.put("missingEmployees", missingEmployees);
        output.put("metadata", Map.of(
                "hasEnoughData", !activeEmployees.isEmpty(),
                "note", activeEmployees.isEmpty() ? "Workspace chưa có nhân viên active để kỳ vọng daily report." : "Backend tính từ danh sách nhân viên active và daily report ngày hiện tại."
        ));
        return output;
    }

    private Map<String, Object> ownerDashboardWorkloadInsight(List<WorkloadView> currentWorkload) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("idleEmployees", workloadItems(currentWorkload, WorkloadLevel.NO_WORK));
        output.put("lightEmployees", workloadItems(currentWorkload, WorkloadLevel.LOW));
        output.put("normalEmployees", workloadItems(currentWorkload, WorkloadLevel.NORMAL));
        output.put("highEmployees", workloadItems(currentWorkload, WorkloadLevel.HIGH));
        output.put("overloadedEmployees", workloadItems(currentWorkload, WorkloadLevel.OVERLOADED));
        output.put("metadata", Map.of(
                "hasEnoughData", !currentWorkload.isEmpty(),
                "note", currentWorkload.isEmpty() ? "Workspace chưa có nhân viên để tính workload." : "Backend tính workload từ task mở, task quá hạn và estimated hours."
        ));
        return output;
    }

    private List<Map<String, Object>> workloadItems(List<WorkloadView> workloads, WorkloadLevel level) {
        return workloads.stream()
                .filter(item -> item.workloadLevel() == level)
                .sorted(Comparator.comparingDouble(WorkloadView::workloadScore).reversed())
                .map(item -> {
                    Map<String, Object> output = new LinkedHashMap<>();
                    output.put("employeeId", item.employeeId().toString());
                    output.put("fullName", item.fullName());
                    output.put("openTasks", item.openTasks());
                    output.put("overdueTasks", item.overdueTasks());
                    output.put("estimatedWorkload", item.estimatedWorkload());
                    output.put("workloadScore", item.workloadScore());
                    output.put("workloadLevel", item.workloadLevel().name());
                    return output;
                })
                .toList();
    }

    private List<Map<String, Object>> ownerDashboardRecommendedActions(List<TaskEntity> scopedTasks, List<WorkloadView> currentWorkload, Object dailyReportInsight) {
        List<Map<String, Object>> actions = new ArrayList<>();
        scopedTasks.stream()
                .filter(this::isOpenTask)
                .filter(this::isOverdue)
                .sorted(Comparator.comparingDouble(this::taskRiskScore).reversed())
                .limit(3)
                .forEach(task -> actions.add(dashboardAction("FOLLOW_UP_OVERDUE_TASK", "TASK", task.getId(), "Theo dõi task quá hạn", "Task \"" + task.getTitle() + "\" đã quá deadline và cần cập nhật ETA.")));
        scopedTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.BLOCKED)
                .limit(3)
                .forEach(task -> actions.add(dashboardAction("RESOLVE_BLOCKER", "TASK", task.getId(), "Xử lý blocker", "Task \"" + task.getTitle() + "\" đang bị block.")));
        currentWorkload.stream()
                .filter(item -> item.workloadLevel() == WorkloadLevel.OVERLOADED)
                .limit(3)
                .forEach(item -> actions.add(dashboardAction("BALANCE_WORKLOAD", "EMPLOYEE", item.employeeId(), "Cân bằng workload", item.fullName() + " đang quá tải với " + item.openTasks() + " task mở.")));
        if (dailyReportInsight instanceof Map<?, ?> insight && numberFrom(insight.get("missingReports")).longValue() > 0) {
            actions.add(dashboardAction("REQUEST_DAILY_REPORT", "DAILY_REPORT", null, "Nhắc daily report", "Có " + numberFrom(insight.get("missingReports")).longValue() + " nhân viên chưa gửi daily report hôm nay."));
        }
        if (actions.isEmpty()) {
            actions.add(dashboardAction("MONITOR_OPERATIONS", "WORKSPACE", currentUser().workspaceId(), "Tiếp tục theo dõi vận hành", "Chưa phát hiện rủi ro nổi bật từ task, workload hoặc daily report."));
        }
        return actions.stream().limit(8).toList();
    }

    private Map<String, Object> dashboardAction(String actionType, String targetEntityType, UUID targetEntityId, String title, String reason) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("actionType", actionType);
        output.put("targetEntityType", targetEntityType);
        output.put("targetEntityId", targetEntityId == null ? null : targetEntityId.toString());
        output.put("title", title);
        output.put("reason", reason);
        return output;
    }

    private Map<String, Object> dashboardTaskItem(TaskEntity task) {
        Map<UUID, String> names = employeeNames();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("taskId", task.getId().toString());
        output.put("title", task.getTitle());
        output.put("status", task.getStatus().name());
        output.put("priority", task.getPriority().name());
        output.put("assigneeId", task.getAssigneeId() == null ? null : task.getAssigneeId().toString());
        output.put("assigneeName", task.getAssigneeId() == null ? null : names.getOrDefault(task.getAssigneeId(), "Unknown"));
        output.put("deadline", task.getDeadline().toString());
        output.put("progressPercent", task.getProgressPercent());
        output.put("overdue", isOverdue(task));
        output.put("riskReason", fallbackDelayReason(task));
        return output;
    }

    private Map<String, Object> enumCountChart(String title, List<String> labels, java.util.function.Function<String, Long> counter) {
        List<Map<String, Object>> series = labels.stream()
                .map(label -> Map.<String, Object>of("label", label, "value", counter.apply(label)))
                .toList();
        long total = series.stream().mapToLong(item -> numberFrom(item.get("value")).longValue()).sum();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("title", title);
        output.put("series", series);
        output.put("total", total);
        return output;
    }

    private Map<String, Object> countChart(String title, Map<String, Long> grouped) {
        List<Map<String, Object>> series = grouped.entrySet().stream()
                .map(entry -> Map.<String, Object>of("label", entry.getKey(), "value", entry.getValue()))
                .toList();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("title", title);
        output.put("series", series);
        output.put("total", grouped.values().stream().mapToLong(Long::longValue).sum());
        return output;
    }

    private Map<String, Object> revenueChart(String title, java.util.function.Function<PaymentTransactionEntity, String> labelResolver) {
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        paymentTransactions.findAll().stream()
                .filter(this::isSuccessfulPayment)
                .sorted(Comparator.comparing(this::effectivePaymentDate))
                .forEach(payment -> grouped.merge(labelResolver.apply(payment), payment.getAmount(), BigDecimal::add));
        List<Map<String, Object>> series = grouped.entrySet().stream()
                .map(entry -> Map.<String, Object>of("label", entry.getKey(), "value", entry.getValue(), "currency", "VND"))
                .toList();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("title", title);
        output.put("series", series);
        output.put("total", grouped.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        output.put("currency", "VND");
        output.put("metadata", Map.of("generatedAt", OffsetDateTime.now().toString(), "dataSource", "BACKEND_COMPUTED"));
        return output;
    }

    private boolean isSuccessfulPayment(PaymentTransactionEntity payment) {
        return payment.getStatus() == PaymentTransactionStatus.SUCCESS;
    }

    private boolean isFailedPayment(PaymentTransactionEntity payment) {
        return List.of(PaymentTransactionStatus.FAILED, PaymentTransactionStatus.EXPIRED, PaymentTransactionStatus.CANCELLED, PaymentTransactionStatus.REFUNDED).contains(payment.getStatus());
    }

    private List<PaymentTransactionEntity> pendingManualPayments(List<PaymentTransactionEntity> payments) {
        return payments.stream()
                .filter(payment -> List.of(PaymentTransactionStatus.PENDING, PaymentTransactionStatus.PROCESSING, PaymentTransactionStatus.MANUAL_REVIEW).contains(payment.getStatus()))
                .toList();
    }

    private BigDecimal successfulRevenueSince(List<PaymentTransactionEntity> payments, OffsetDateTime start) {
        return payments.stream()
                .filter(this::isSuccessfulPayment)
                .filter(payment -> !effectivePaymentDate(payment).isBefore(start))
                .map(PaymentTransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OffsetDateTime effectivePaymentDate(PaymentTransactionEntity payment) {
        if (payment.getPaidAt() != null) return payment.getPaidAt();
        if (payment.getUpdatedAt() != null) return payment.getUpdatedAt();
        return payment.getCreatedAt();
    }

    private double feedbackRatingAverage(List<BusinessFeedbackEntity> feedback) {
        return feedback.isEmpty()
                ? 0
                : Math.round(feedback.stream().mapToInt(BusinessFeedbackEntity::getRating).average().orElse(0) * 100.0) / 100.0;
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
        return workspaceEmployees(workspaceId).stream()
                .map(employee -> workloadForEmployee(employee, scopedTasks))
                .sorted(Comparator.comparing(WorkloadView::workloadScore).reversed())
                .toList();
    }

    public List<AssigneeRecommendationView> recommendAssignee(RecommendAssigneeRequest request) {
        requireTaskManager();
        if (request == null) return List.of();
        RecommendAssigneeRequest effectiveRequest = enrichRecommendationRequestWithDomainAnalysis(request);
        List<AiEmployeeWorkload> candidates = assigneeCandidates(effectiveRequest);
        if (candidates.isEmpty()) return List.of();
        enforceAiUsageLimit();
        List<AssigneeRecommendationView> normalized;
        try {
            List<AssigneeRecommendationView> recommendations = aiServiceClient.recommendAssignee(new AiRecommendAssigneeInput(
                    effectiveRequest.title(),
                    effectiveRequest.requirements(),
                    effectiveRequest.deadline().toString(),
                    effectiveRequest.estimatedHours() == null ? 0 : effectiveRequest.estimatedHours().doubleValue(),
                    effectiveRequest.departmentId(),
                    effectiveRequest.requiredJobPositionId(),
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
        saveAiSuggestion(AiSuggestionType.ASSIGNEE_RECOMMENDATION, effectiveRequest, normalized);
        return normalized;
    }

    public Map<String, Object> workloadSummary() {
        requireOwner();
        List<WorkloadView> currentWorkload = workload();
        if (isAiUsageLimitReached()) {
            return fallbackWorkloadSummary(currentWorkload, new AiProviderException("AI usage limit reached."));
        }
        enforceAiUsageLimit();
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
        if (isAiUsageLimitReached()) {
            return fallbackDelayRisks(scopedTasks, employeeNames, new AiProviderException("AI usage limit reached."));
        }
        enforceAiUsageLimit();
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

    public Map<String, Object> analyzeTaskDomain(TaskDomainAnalysisRequest request) {
        requireTaskManager();
        enforceAiUsageLimit();
        UUID workspaceId = currentUser().workspaceId();
        Map<String, Object> payload = taskDomainAnalysisPayload(request, workspaceId);
        try {
            Map<String, Object> output = aiServiceClient.analyzeTaskDescription(payload);
            saveAiSuggestion(AiSuggestionType.TASK_EXTRACTION, payload, output);
            saveAiHistory("TASK_DOMAIN_ANALYSIS", AiHistoryStatus.SUCCESS);
            return output;
        } catch (AiProviderException exception) {
            saveAiHistory("TASK_DOMAIN_ANALYSIS", AiHistoryStatus.FAILED);
            log.warn("AI task domain analysis failed; using rule-based fallback. message={}", fallbackReason(exception));
            return fallbackTaskDomainAnalysis(request, workspaceId, exception);
        }
    }

    public Map<String, Object> estimateTaskHours(EstimateHoursRequest request) {
        requireTaskManager();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspaceId", currentUser().workspaceId().toString());
        payload.put("taskTitle", request.taskTitle());
        payload.put("taskDescription", request.taskDescription());
        payload.put("difficulty", request.difficulty());
        payload.put("taskType", request.taskType());
        payload.put("startDate", request.startDate() == null ? null : request.startDate().toString());
        payload.put("deadline", request.deadline() == null ? null : request.deadline().toString());
        payload.put("backendWorkingDays", request.backendWorkingDays());
        payload.put("backendDefaultHours", request.backendDefaultHours() == null ? null : request.backendDefaultHours().doubleValue());
        return invokeAiMapWithFallback("TASK_ESTIMATE_HOURS", AiSuggestionType.TASK_ESTIMATE_HOURS, payload,
                () -> aiServiceClient.estimateHours(payload), exception -> fallbackEstimateHours(payload, exception));
    }

    public Map<String, Object> explainRecommendation(RecommendationExplanationRequest request) {
        requireTaskManager();
        String recommendationType = request.recommendationType().trim().toUpperCase(Locale.ROOT);
        if (!List.of("INDIVIDUAL", "TEAM_LEADER", "TEAM_MEMBER").contains(recommendationType)) {
            throw new IllegalArgumentException("recommendationType phải là INDIVIDUAL, TEAM_LEADER hoặc TEAM_MEMBER.");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspaceId", currentUser().workspaceId().toString());
        payload.put("recommendationType", recommendationType);
        payload.put("task", request.task());
        payload.put("candidates", request.candidates());
        Supplier<Map<String, Object>> call = switch (recommendationType) {
            case "TEAM_LEADER" -> () -> aiServiceClient.explainTeamLeaderRecommendation(payload);
            case "TEAM_MEMBER" -> () -> aiServiceClient.explainTeamMemberRecommendation(payload);
            default -> () -> aiServiceClient.explainIndividualRecommendation(payload);
        };
        return invokeAiMapWithFallback("RECOMMENDATION_EXPLANATION_" + recommendationType, AiSuggestionType.RECOMMENDATION_EXPLANATION,
                payload, call, exception -> fallbackRecommendationExplanation(recommendationType, request, exception));
    }

    public Map<String, Object> explainRecommendationResult(RecommendationResultExplanationRequest request) {
        requireTaskManager();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspaceId", currentUser().workspaceId().toString());
        payload.put("task", request.task());
        payload.put("selectedAssigneeOrTeam", request.selectedAssigneeOrTeam());
        payload.put("rankingData", request.rankingData() == null ? List.of() : request.rankingData());
        payload.put("comparisonWithOtherCandidates", request.comparisonWithOtherCandidates() == null ? List.of() : request.comparisonWithOtherCandidates());
        payload.put("workloadData", request.workloadData() == null ? Map.of() : request.workloadData());
        payload.put("performanceData", request.performanceData() == null ? Map.of() : request.performanceData());
        return invokeAiMapWithFallback("RECOMMENDATION_RESULT_EXPLANATION", AiSuggestionType.RECOMMENDATION_RESULT_EXPLANATION,
                payload, () -> aiServiceClient.explainRecommendationResult(payload),
                exception -> fallbackRecommendationResult(request, exception));
    }

    public Map<String, Object> explainWorkloadRisk(WorkloadRiskExplanationRequest request) {
        requireTaskManager();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspaceId", currentUser().workspaceId().toString());
        payload.put("employeeName", request.employeeName());
        payload.put("monthlyCapacityHours", request.monthlyCapacityHours().doubleValue());
        payload.put("monthlyWorkloadEvaluation", request.monthlyWorkloadEvaluation());
        payload.put("backendOverallRisk", request.backendOverallRisk());
        return invokeAiMapWithFallback("WORKLOAD_RISK_EXPLANATION", AiSuggestionType.WORKLOAD_RISK, payload,
                () -> aiServiceClient.workloadRisk(payload), exception -> fallbackWorkloadRisk(request, exception));
    }

    public Map<String, Object> generateEmployeeReport(EmployeeReportAiRequest request) {
        requireOwnerOrHrOrManager();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspaceId", currentUser().workspaceId().toString());
        payload.put("employee", request.employee());
        payload.put("period", request.period());
        payload.put("metrics", request.metrics());
        payload.put("notableTasks", request.notableTasks() == null ? List.of() : request.notableTasks());
        payload.put("risks", request.risks() == null ? List.of() : request.risks());
        return invokeAiMapWithFallback("EMPLOYEE_REPORT", AiSuggestionType.EMPLOYEE_REPORT, payload,
                () -> aiServiceClient.employeeReport(payload), exception -> fallbackEmployeeReport(request, exception));
    }

    public Map<String, Object> businessOwnerOperationalSummary() {
        requireOwner();
        Map<String, Object> payload = businessOwnerOperationalSummaryPayload();
        return invokeAiMapWithFallback("OWNER_OPERATIONAL_SUMMARY", AiSuggestionType.OWNER_OPERATIONAL_SUMMARY, payload,
                () -> aiServiceClient.businessOwnerOperationalSummary(payload), exception -> fallbackOwnerOperationalSummary(payload, exception));
    }

    public Map<String, Object> platformAdminSystemSummary() {
        requireSystemAdmin();
        Map<String, Object> payload = platformAdminSystemSummaryPayload();
        return invokeAiMapWithFallback("PLATFORM_SYSTEM_SUMMARY", AiSuggestionType.PLATFORM_SYSTEM_SUMMARY, payload,
                () -> aiServiceClient.platformAdminSystemSummary(payload), exception -> fallbackPlatformSystemSummary(payload, exception));
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
        payload.put("employees", workspaceEmployees(currentUser().workspaceId()).stream()
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
        List<WorkloadView> currentWorkload = workload();
        if (isAiUsageLimitReached()) {
            return fallbackActionSuggestions(currentWorkload, new AiProviderException("AI usage limit reached."));
        }
        enforceAiUsageLimit();
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
        if (!notification.getWorkspaceId().equals(currentUser().workspaceId()) || (!isBusinessOwnerRole(currentUser().role()) && !isManagerOrExecutiveRole(currentUser().role()) && !notification.getUserId().equals(currentUser().userId()))) {
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
        workspace.setOrganizationAbbreviation(shortCode);
        workspace.setContactEmail(request.contactEmail());
        workspace.setContactPhone(request.contactPhone());
        workspace.setAddress(request.businessAddress());
        workspace.setSubscriptionPlanId(plan.getId());
        if (request.maxUsers() > plan.getMaxUsers()) {
        }
        workspace.setMaxUsers(request.maxUsers() > 0 ? request.maxUsers() : plan.getMaxUsers());
        workspace.setMaxOwnerAccounts(plan.getMaxOwnerAccounts());
        workspace.setMaxEmployeeAccounts(plan.getMaxEmployeeAccounts());
        workspace.setStatus(request.status() == null ? WorkspaceStatus.INACTIVE : request.status());
        workspace.setPaymentStatus(PaymentStatus.CONFIRMED);
        workspace.setActivatedAt(request.activationDate() == null && request.status() == WorkspaceStatus.ACTIVE ? now : request.activationDate());
        workspace.setExpiresAt(request.expirationDate() == null && workspace.getActivatedAt() != null ? workspace.getActivatedAt().plusMonths(plan.getDurationInMonths()) : request.expirationDate());
        workspace.setCreatedAt(now);
        workspace = workspaces.save(workspace);
        List<GeneratedOwnerAccountView> generatedOwnerAccounts = List.of();
        if (workspace.getStatus() == WorkspaceStatus.ACTIVE || workspace.getActivatedAt() != null) {
            createWorkspaceSubscription(workspace, plan, workspace.getActivatedAt() == null ? now : workspace.getActivatedAt(), null);
        }
        generatedOwnerAccounts = provisionOwnerAccounts(workspace, plan, now);
        workspace.setOwnerAccountProvisionedAt(now);
        workspace.setOwnerAccountCount(ownerAccounts(workspace.getId()).size());
        if (!generatedOwnerAccounts.isEmpty()) {
            workspace.setOwnerId(generatedOwnerAccounts.getFirst().userId());
        }
        workspace = workspaces.save(workspace);
        audit(workspace.getId(), "ADMIN_CREATE_WORKSPACE", "WORKSPACE", workspace.getId(), null, toPlatformWorkspaceView(workspace, generatedOwnerAccounts));
        return toPlatformWorkspaceView(workspace, generatedOwnerAccounts);
    }

    public PlatformWorkspaceView adminUpdateWorkspace(UUID workspaceId, AdminUpdateWorkspaceRequest request) {
        requireSystemAdmin();
        WorkspaceEntity workspace = requireWorkspace(workspaceId);
        UUID previousPlanId = workspace.getSubscriptionPlanId();
        boolean planChanged = false;
        if (hasText(request.businessName())) workspace.setBusinessName(request.businessName());
        if (hasText(request.workspaceName())) workspace.setName(request.workspaceName());
        if (hasText(request.contactEmail())) workspace.setContactEmail(request.contactEmail());
        if (hasText(request.contactPhone())) workspace.setContactPhone(request.contactPhone());
        if (request.businessAddress() != null) workspace.setAddress(request.businessAddress());
        SubscriptionPlanEntity selectedPlan = null;
        if (request.subscriptionPlanId() != null) {
            selectedPlan = requireSubscriptionPlan(request.subscriptionPlanId());
            planChanged = previousPlanId == null || !previousPlanId.equals(selectedPlan.getId());
            workspace.setSubscriptionPlanId(selectedPlan.getId());
            workspace.setMaxOwnerAccounts(selectedPlan.getMaxOwnerAccounts());
            workspace.setMaxEmployeeAccounts(selectedPlan.getMaxEmployeeAccounts());
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
        OffsetDateTime now = OffsetDateTime.now();
        if (selectedPlan != null && workspace.getStatus() == WorkspaceStatus.ACTIVE && workspace.getActivatedAt() == null) {
            workspace.setActivatedAt(now);
        }
        if (selectedPlan != null && planChanged && workspace.getStatus() == WorkspaceStatus.ACTIVE && request.expirationDate() == null) {
            workspace.setExpiresAt(now.plusMonths(selectedPlan.getDurationInMonths()));
        }
        workspace = workspaces.save(workspace);
        if (selectedPlan != null && workspace.getStatus() == WorkspaceStatus.ACTIVE) {
            if (planChanged) {
                replaceActiveWorkspaceSubscription(workspace, selectedPlan, now);
            } else {
                ensureActiveWorkspaceSubscription(workspace, selectedPlan, now);
            }
        }
        audit(workspace.getId(), "ADMIN_UPDATE_WORKSPACE", "WORKSPACE", workspace.getId(), null, toPlatformWorkspaceView(workspace));
        return toPlatformWorkspaceView(workspace);
    }

    public PlatformWorkspaceView adminUpdateWorkspaceStatus(UUID workspaceId, WorkspaceStatus status) {
        requireSystemAdmin();
        WorkspaceEntity workspace = requireWorkspace(workspaceId);
        List<GeneratedOwnerAccountView> generatedOwnerAccounts = List.of();
        workspace.setStatus(status);
        if (status == WorkspaceStatus.ACTIVE && workspace.getActivatedAt() == null) {
            workspace.setActivatedAt(OffsetDateTime.now());
        }
        if (status == WorkspaceStatus.ACTIVE && workspace.getOwnerAccountProvisionedAt() == null && workspace.getSubscriptionPlanId() != null) {
            SubscriptionPlanEntity plan = requireSubscriptionPlan(workspace.getSubscriptionPlanId());
            workspace.setMaxOwnerAccounts(plan.getMaxOwnerAccounts());
            workspace.setMaxEmployeeAccounts(plan.getMaxEmployeeAccounts());
            if (workspace.getExpiresAt() == null) {
                workspace.setExpiresAt(workspace.getActivatedAt().plusMonths(plan.getDurationInMonths()));
            }
            ensureActiveWorkspaceSubscription(workspace, plan, workspace.getActivatedAt());
            generatedOwnerAccounts = provisionOwnerAccounts(workspace, plan, OffsetDateTime.now());
            workspace.setOwnerAccountProvisionedAt(OffsetDateTime.now());
            workspace.setOwnerAccountCount(ownerAccounts(workspace.getId()).size());
            if (!generatedOwnerAccounts.isEmpty() && workspace.getOwnerId() == null) {
                workspace.setOwnerId(generatedOwnerAccounts.getFirst().userId());
            }
        } else if (status == WorkspaceStatus.ACTIVE && workspace.getSubscriptionPlanId() != null) {
            SubscriptionPlanEntity plan = requireSubscriptionPlan(workspace.getSubscriptionPlanId());
            if (workspace.getExpiresAt() == null) {
                workspace.setExpiresAt(workspace.getActivatedAt().plusMonths(plan.getDurationInMonths()));
            }
            ensureActiveWorkspaceSubscription(workspace, plan, workspace.getActivatedAt());
        }
        workspace = workspaces.save(workspace);
        audit(workspace.getId(), "ADMIN_UPDATE_WORKSPACE_STATUS", "WORKSPACE", workspace.getId(), null, Map.of("status", status.name()));
        return toPlatformWorkspaceView(workspace, generatedOwnerAccounts);
    }

    public CreatedUserAccountView adminCreateBusinessOwner(UUID workspaceId, CreateBusinessOwnerRequest request) {
        requireSystemAdmin();
        WorkspaceEntity workspace = requireWorkspace(workspaceId);
        enforceWorkspaceUserLimit(workspace);
        if (ownerAccounts(workspaceId).size() >= workspace.getMaxOwnerAccounts()) {
            throw new IllegalArgumentException("Số tài khoản Business Owner đã đạt giới hạn gói hiện tại.");
        }
        if (users.existsByWorkspaceIdAndEmailIgnoreCase(workspaceId, request.email())) {
            throw new IllegalArgumentException("Email đã tồn tại trong workspace.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        String username = hasText(request.username()) ? request.username().trim().toUpperCase(Locale.ROOT) : nextOwnerUsername(workspace);
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Username Business Owner đã tồn tại.");
        }
        String temporaryPassword = hasText(request.temporaryPassword()) ? request.temporaryPassword() : defaultOwnerPassword();
        UserEntity owner = new UserEntity();
        owner.setWorkspaceId(workspaceId);
        owner.setFullName(request.fullName());
        owner.setEmail(request.email());
        owner.setPhone(request.phone());
        owner.setUsername(username);
        owner.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        owner.setMustChangePassword(true);
        owner.setInitialAccountGenerated(!hasText(request.temporaryPassword()));
        owner.setRole(Role.BUSINESS_OWNER);
        owner.setStatus(request.status() == null ? UserStatus.ACTIVE : request.status());
        owner.setCreatedAt(now);
        owner.setUpdatedAt(now);
        owner = users.save(owner);
        if (workspace.getOwnerId() == null) {
            workspace.setOwnerId(owner.getId());
        }
        workspace.setOwnerAccountCount(ownerAccounts(workspaceId).size());
        workspace.setOwnerAccountProvisionedAt(workspace.getOwnerAccountProvisionedAt() == null ? now : workspace.getOwnerAccountProvisionedAt());
        workspaces.save(workspace);
        audit(workspaceId, "ADMIN_CREATE_BUSINESS_OWNER", "USER", owner.getId(), null, Map.of("email", owner.getEmail(), "status", owner.getStatus().name()));
        return new CreatedUserAccountView(toUserView(owner), username, temporaryPassword, true);
    }

    public List<UserView> adminBusinessOwners(UUID workspaceId) {
        requireSystemAdmin();
        requireWorkspace(workspaceId);
        return users.findByWorkspaceIdAndRoleInOrderByFullNameAsc(workspaceId, List.of(Role.BUSINESS_OWNER, Role.OWNER)).stream().map(this::toUserView).toList();
    }

    public CreatedUserAccountView adminResetOwnerPassword(UUID ownerId) {
        requireSystemAdmin();
        UserEntity owner = users.findById(ownerId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Business Owner."));
        if (!isBusinessOwnerRole(owner.getRole())) throw new IllegalArgumentException("Tài khoản không phải Business Owner.");
        String temporaryPassword = "123456";
        owner.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        owner.setMustChangePassword(true);
        owner.setInitialAccountGenerated(true);
        owner.setUpdatedAt(OffsetDateTime.now());
        owner = users.save(owner);
        audit(owner.getWorkspaceId(), "ADMIN_RESET_OWNER_PASSWORD", "USER", owner.getId(), null, Map.of("email", owner.getEmail()));
        return new CreatedUserAccountView(toUserView(owner), owner.getUsername(), temporaryPassword, true);
    }

    public UserView adminUpdateOwnerStatus(UUID ownerId, UserStatus status) {
        requireSystemAdmin();
        UserEntity owner = users.findById(ownerId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Business Owner."));
        if (!isBusinessOwnerRole(owner.getRole())) throw new IllegalArgumentException("Tài khoản không phải Business Owner.");
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

    public SubscriptionPlanView publicSubscriptionPlan(UUID planId) {
        return toSubscriptionPlanView(requireActiveSubscriptionPlan(planId));
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
        registration.setRegistrationToken(uniqueRegistrationToken());
        registration.setExpiredAt(now.plusDays(14));
        registration.setCreatedAt(now);
        registration.setUpdatedAt(now);
        return toWorkspaceRegistrationView(workspaceRegistrations.save(registration));
    }

    public WorkspaceRegistrationView workspaceRegistration(UUID registrationId) {
        requireSystemAdmin();
        return toWorkspaceRegistrationView(requireWorkspaceRegistration(registrationId));
    }

    public WorkspaceRegistrationView publicWorkspaceRegistration(UUID registrationId, String token) {
        return toWorkspaceRegistrationView(requireWorkspaceRegistrationWithToken(registrationId, token));
    }

    public WorkspaceRegistrationView selectSubscriptionPlan(UUID registrationId, SelectSubscriptionPlanRequest request) {
        requireSystemAdmin();
        return selectSubscriptionPlanForRegistration(registrationId, request);
    }

    private WorkspaceRegistrationView selectSubscriptionPlanForRegistration(UUID registrationId, SelectSubscriptionPlanRequest request) {
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

    public WorkspaceRegistrationView publicSelectSubscriptionPlan(UUID registrationId, String token, SelectSubscriptionPlanRequest request) {
        requireWorkspaceRegistrationWithToken(registrationId, token);
        return selectSubscriptionPlanForRegistration(registrationId, request);
    }

    public WorkspaceRegistrationView publicCancelWorkspaceRegistration(UUID registrationId, String token) {
        WorkspaceRegistrationEntity registration = requireWorkspaceRegistrationWithToken(registrationId, token);
        if (registration.getWorkspaceId() != null || registration.getRegistrationStatus() == RegistrationStatus.APPROVED) {
            throw new IllegalArgumentException("Workspace is already active.");
        }
        registration.setRegistrationStatus(RegistrationStatus.CANCELLED);
        registration.setUpdatedAt(OffsetDateTime.now());
        return toWorkspaceRegistrationView(workspaceRegistrations.save(registration));
    }

    public WorkspaceRegistrationView submitRegistrationPayment(UUID registrationId, SubmitPaymentRequest request) {
        requireSystemAdmin();
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
        requireSystemAdmin();
        return createPaymentForRegistration(registrationId, request);
    }

    private PaymentTransactionView createPaymentForRegistration(UUID registrationId, CreatePaymentRequest request) {
        WorkspaceRegistrationEntity registration = requireWorkspaceRegistration(registrationId);
        if (registration.getWorkspaceId() != null || registration.getRegistrationStatus() == RegistrationStatus.APPROVED) {
            throw new IllegalArgumentException("Workspace is already active.");
        }
        if (registration.getSubscriptionPlanId() == null) {
            throw new IllegalArgumentException("Select an active subscription plan before payment.");
        }
        SubscriptionPlanEntity plan = requireActiveSubscriptionPlan(registration.getSubscriptionPlanId());
        OffsetDateTime now = OffsetDateTime.now();
        expireStalePaymentsForRegistration(registration.getId(), now);
        PaymentTransactionEntity pendingPayment = reusablePendingPayment(registration.getId(), now);
        if (pendingPayment != null) {
            return toPaymentTransactionView(pendingPayment);
        }
        PaymentTransactionEntity payment = new PaymentTransactionEntity();
        payment.setWorkspaceRegistrationId(registration.getId());
        payment.setSubscriptionPlanId(plan.getId());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setAmount(plan.getPrice());
        payment.setCurrency("VND");
        payment.setPaymentCode(uniquePaymentCode());
        payment.setOrderCode(uniqueOrderCode());
        payment.setRequestId(uniqueRequestId());
        PaymentQrSettingEntity qrSetting = requireEnabledPaymentQrSetting(request.paymentMethod());
        payment.setTransferContent(transferContent(qrSetting, registration, payment));
        payment.setPaymentConfigurationSnapshot(paymentConfigurationSnapshot(qrSetting));
        payment.setStatus(PaymentTransactionStatus.PENDING);
        payment.setExpiredAt(now.plusMinutes(30));
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        ProviderPaymentResult providerResult;
        if (request.paymentMethod() == PaymentMethod.MOMO) {
            if (!momoPaymentService.isRealProviderConfigured()) {
                throw new IllegalArgumentException("MoMo payment provider is not configured.");
            }
            providerResult = momoPaymentService.createPayment(payment);
            payment.setProviderName("MOMO");
        } else {
            providerResult = bankTransferPaymentService.createPayment(
                    payment,
                    qrSetting.getBankCode(),
                    qrSetting.getBankName(),
                    qrSetting.getBankAccountNumber(),
                    qrSetting.getBankAccountName()
            );
            if (qrSetting.getQrFileId() != null) {
                providerResult = withUploadedBankQr(providerResult, qrSetting.getQrFileId());
            }
            payment.setProviderName(qrSetting.getQrFileId() == null ? "VIETQR" : "BANK_TRANSFER");
        }
        payment.setProviderPaymentUrl(providerResult.paymentUrl());
        payment.setProviderDeeplink(providerResult.deeplink());
        payment.setProviderQrCodeUrl(providerResult.qrCodeUrl());
        payment.setQrDisplayData(providerResult.qrCodeUrl());
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
        requireSystemAdmin();
        return toPaymentTransactionView(requirePayment(paymentId));
    }

    public List<PaymentQrSettingView> adminPaymentQrSettings() {
        requireSystemAdmin();
        return paymentQrSettings.findAll(Sort.by(Sort.Direction.ASC, "paymentMethod")).stream()
                .map(this::toPaymentQrSettingView)
                .toList();
    }

    public PaymentQrSettingView updatePaymentQrSetting(PaymentMethod paymentMethod, UpdatePaymentQrSettingRequest request) {
        requireSystemAdmin();
        if (hasText(request.qrCodeUrl())) {
            throw new IllegalArgumentException("External QR image URLs are not accepted. Upload a bank QR image or use dynamic provider data.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        PaymentQrSettingEntity setting = paymentQrSettings.findByPaymentMethod(paymentMethod).orElseGet(() -> {
            PaymentQrSettingEntity item = new PaymentQrSettingEntity();
            item.setPaymentMethod(paymentMethod);
            item.setCreatedAt(now);
            return item;
        });
        setting.setQrCodeUrl(null);
        setting.setPaymentUrl(null);
        setting.setDeeplink(null);
        if (paymentMethod == PaymentMethod.MOMO) {
            setting.setBankCode(null);
            setting.setBankName(null);
            setting.setBankAccountNumber(null);
            setting.setBankAccountName(null);
        } else {
            setting.setBankCode(blankToNull(request.bankCode()));
            setting.setBankName(blankToNull(request.bankName()));
            setting.setBankAccountNumber(blankToNull(request.bankAccountNumber()));
            setting.setBankAccountName(blankToNull(request.bankAccountName()));
            if (request.enabled() && (!hasText(setting.getBankCode()) || !hasText(setting.getBankAccountNumber()) || !hasText(setting.getBankAccountName()))) {
                throw new IllegalArgumentException("Bank code, account number and account name are required before enabling bank transfer.");
            }
        }
        setting.setTransferContentPrefix(blankToNull(request.transferContentPrefix()));
        setting.setEnabled(request.enabled());
        setting.setUpdatedBy(currentUser().userId());
        setting.setUpdatedAt(now);
        setting = paymentQrSettings.save(setting);
        audit(null, "ADMIN_UPDATE_PAYMENT_QR_SETTING", "PAYMENT_QR_SETTING", setting.getId(), null, toPaymentQrSettingView(setting));
        return toPaymentQrSettingView(setting);
    }

    public PaymentQrFileView uploadPaymentQrImage(PaymentMethod paymentMethod, MultipartFile file) {
        requireSystemAdmin();
        if (paymentMethod != PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("Only bank transfer configuration accepts an uploaded QR image.");
        }
        byte[] content = validatedQrImage(file);
        OffsetDateTime now = OffsetDateTime.now();
        PaymentQrFileEntity stored = new PaymentQrFileEntity();
        stored.setFileName(uniqueQrFileName(file.getContentType()));
        stored.setContentType(normalizedImageContentType(file.getContentType()));
        stored.setFileSize(content.length);
        stored.setContent(content);
        stored.setUploadedBy(currentUser().userId());
        stored.setCreatedAt(now);
        stored = paymentQrFiles.save(stored);

        PaymentQrSettingEntity setting = paymentQrSettings.findByPaymentMethod(paymentMethod).orElseGet(() -> {
            PaymentQrSettingEntity item = new PaymentQrSettingEntity();
            item.setPaymentMethod(paymentMethod);
            item.setEnabled(false);
            item.setCreatedAt(now);
            return item;
        });
        setting.setQrCodeUrl(null);
        setting.setQrFileId(stored.getId());
        setting.setUpdatedBy(currentUser().userId());
        setting.setUpdatedAt(now);
        paymentQrSettings.save(setting);
        audit(null, "ADMIN_UPLOAD_BANK_QR_IMAGE", "PAYMENT_QR_FILE", stored.getId(), null,
                Map.of("fileName", stored.getFileName(), "size", stored.getFileSize()));
        return toPaymentQrFileView(stored);
    }

    public PaymentQrSettingView removePaymentQrImage(PaymentMethod paymentMethod) {
        requireSystemAdmin();
        if (paymentMethod != PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("MoMo QR data is generated by the provider and cannot be removed here.");
        }
        PaymentQrSettingEntity setting = paymentQrSettings.findByPaymentMethod(paymentMethod)
                .orElseThrow(() -> new IllegalArgumentException("Payment configuration not found."));
        UUID oldFileId = setting.getQrFileId();
        setting.setQrFileId(null);
        setting.setQrCodeUrl(null);
        setting.setUpdatedBy(currentUser().userId());
        setting.setUpdatedAt(OffsetDateTime.now());
        setting = paymentQrSettings.save(setting);
        audit(null, "ADMIN_REMOVE_BANK_QR_IMAGE", "PAYMENT_QR_SETTING", setting.getId(),
                Map.of("fileId", oldFileId == null ? "" : oldFileId.toString()), null);
        return toPaymentQrSettingView(setting);
    }

    @Transactional(readOnly = true)
    public PaymentQrFileContent paymentQrFile(UUID fileId) {
        PaymentQrFileEntity file = paymentQrFiles.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Payment QR image not found."));
        return new PaymentQrFileContent(file.getContentType(), file.getContent());
    }

    public PublicPaymentStatusView publicCreatePayment(UUID registrationId, String token, CreatePaymentRequest request) {
        requireWorkspaceRegistrationWithToken(registrationId, token);
        PaymentTransactionView payment = createPaymentForRegistration(registrationId, request);
        return toPublicPaymentStatusView(requirePayment(payment.id()));
    }

    public PublicPaymentStatusView publicPaymentStatus(String paymentCode, String token) {
        PaymentTransactionEntity payment = paymentTransactions.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found."));
        WorkspaceRegistrationEntity registration = requireWorkspaceRegistrationWithToken(payment.getWorkspaceRegistrationId(), token);
        payment = refreshExpiredPayment(payment, OffsetDateTime.now());
        return toPublicPaymentStatusView(payment, registration);
    }

    public PaymentTransactionView handleMomoCallback(PaymentCallbackRequest request) {
        PaymentTransactionEntity payment = requirePaymentByCallback(request);
        Map<String, Object> payload = paymentCallbackPayload(request);
        if (!momoPaymentService.verifyCallbackSignature(payload, request.signature())) {
            throw new IllegalArgumentException("Invalid MoMo callback signature.");
        }
        assertCallbackAmountMatches(payment, request);
        payment.setProviderTransactionId(blankToNull(request.providerTransactionId()));
        boolean success = "0".equals(request.resultCode()) || "SUCCESS".equalsIgnoreCase(request.resultCode());
        return success ? confirmPayment(payment.getId(), false, request.rawPayload()) : failPayment(payment.getId(), request.rawPayload());
    }

    public PaymentTransactionView handleBankTransferCallback(PaymentCallbackRequest request) {
        PaymentTransactionEntity payment = requirePaymentByCallback(request);
        Map<String, Object> payload = paymentCallbackPayload(request);
        if (!bankTransferPaymentService.verifyCallbackSignature(payload, request.signature())) {
            throw new IllegalArgumentException("Invalid bank transfer callback signature.");
        }
        assertCallbackAmountMatches(payment, request);
        payment.setProviderTransactionId(blankToNull(request.providerTransactionId()));
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

    public List<PaymentTransactionView> adminPayments() {
        requireSystemAdmin();
        return paymentTransactions.findAllByOrderByCreatedAtDesc().stream().map(this::toPaymentTransactionView).toList();
    }

    public PaymentTransactionView adminPayment(UUID paymentId) {
        requireSystemAdmin();
        return toPaymentTransactionView(requirePayment(paymentId));
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
        List<GeneratedOwnerAccountView> generatedOwnerAccounts = activateWorkspaceForRegistration(registration, request == null ? null : request.note(), null);
        return toWorkspaceRegistrationView(requireWorkspaceRegistration(registrationId), generatedOwnerAccounts);
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

    public Map<String, Object> adminDashboardOverview() {
        requireSystemAdmin();
        List<WorkspaceEntity> allWorkspaces = workspaces.findAll();
        List<UserEntity> allUsers = users.findAll();
        List<PaymentTransactionEntity> allPayments = paymentTransactions.findAll();
        LocalDate today = LocalDate.now();
        OffsetDateTime monthStart = today.withDayOfMonth(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime quarterStart = today.withMonth(((today.getMonthValue() - 1) / 3) * 3 + 1).withDayOfMonth(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime yearStart = today.withDayOfYear(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        long successfulPayments = allPayments.stream().filter(this::isSuccessfulPayment).count();
        long failedPayments = allPayments.stream().filter(this::isFailedPayment).count();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("totalWorkspaces", allWorkspaces.size());
        output.put("activeWorkspaces", allWorkspaces.stream().filter(workspace -> workspace.getStatus() == WorkspaceStatus.ACTIVE).count());
        output.put("suspendedWorkspaces", allWorkspaces.stream().filter(workspace -> workspace.getStatus() == WorkspaceStatus.SUSPENDED).count());
        output.put("expiredWorkspaces", allWorkspaces.stream().filter(workspace -> workspace.getStatus() == WorkspaceStatus.EXPIRED).count());
        output.put("newWorkspacesThisMonth", allWorkspaces.stream().filter(workspace -> workspace.getCreatedAt() != null && !workspace.getCreatedAt().isBefore(monthStart)).count());
        output.put("totalUsers", allUsers.size());
        output.put("totalBusinessesByPackage", adminDashboardWorkspacesByPlan().get("series"));
        output.put("revenueThisMonth", successfulRevenueSince(allPayments, monthStart));
        output.put("revenueThisQuarter", successfulRevenueSince(allPayments, quarterStart));
        output.put("revenueThisYear", successfulRevenueSince(allPayments, yearStart));
        output.put("currency", "VND");
        output.put("paymentSuccessRate", percentage(successfulPayments, successfulPayments + failedPayments));
        output.put("failedPayments", failedPayments);
        output.put("pendingManualPayments", pendingManualPayments(allPayments).size());
        output.put("businessFeedbackRatingAverage", feedbackRatingAverage(businessFeedback.findAllByOrderByCreatedAtDesc()));
        output.put("aiUsageStatistics", Map.of(
                "totalHistoryCalls", aiHistory.count(),
                "totalSuggestions", aiSuggestions.count(),
                "generatedSuggestions", aiSuggestions.findAll().stream().filter(item -> item.getStatus() == AiSuggestionStatus.GENERATED).count()
        ));
        output.put("metadata", Map.of("generatedAt", OffsetDateTime.now().toString(), "dataSource", "BACKEND_COMPUTED"));
        return output;
    }

    public Map<String, Object> adminDashboardRevenueMonthly() {
        requireSystemAdmin();
        return revenueChart("Doanh thu theo tháng", payment -> effectivePaymentDate(payment).getYear() + "-" + String.format("%02d", effectivePaymentDate(payment).getMonthValue()));
    }

    public Map<String, Object> adminDashboardRevenueQuarterly() {
        requireSystemAdmin();
        return revenueChart("Doanh thu theo quý", payment -> effectivePaymentDate(payment).getYear() + "-Q" + ((effectivePaymentDate(payment).getMonthValue() - 1) / 3 + 1));
    }

    public Map<String, Object> adminDashboardRevenueYearly() {
        requireSystemAdmin();
        return revenueChart("Doanh thu theo năm", payment -> String.valueOf(effectivePaymentDate(payment).getYear()));
    }

    public Map<String, Object> adminDashboardRevenueByPlan() {
        requireSystemAdmin();
        Map<UUID, String> planNames = subscriptionPlans.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(SubscriptionPlanEntity::getId, SubscriptionPlanEntity::getName));
        return revenueChart("Doanh thu theo gói", payment -> planNames.getOrDefault(payment.getSubscriptionPlanId(), "Unknown plan"));
    }

    public Map<String, Object> adminDashboardWorkspacesByStatus() {
        requireSystemAdmin();
        List<WorkspaceEntity> allWorkspaces = workspaces.findAll();
        return enumCountChart(
                "Workspace theo trạng thái",
                Arrays.stream(WorkspaceStatus.values()).map(Enum::name).toList(),
                status -> allWorkspaces.stream().filter(workspace -> workspace.getStatus().name().equals(status)).count()
        );
    }

    public Map<String, Object> adminDashboardWorkspacesByPlan() {
        requireSystemAdmin();
        Map<UUID, String> planNames = subscriptionPlans.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(SubscriptionPlanEntity::getId, SubscriptionPlanEntity::getName));
        Map<String, Long> grouped = new LinkedHashMap<>();
        workspaces.findAll().forEach(workspace -> {
            String label = workspace.getSubscriptionPlanId() == null ? "NO_PLAN" : planNames.getOrDefault(workspace.getSubscriptionPlanId(), "Unknown plan");
            grouped.merge(label, 1L, Long::sum);
        });
        return countChart("Workspace theo gói", grouped);
    }

    public Map<String, Object> adminDashboardPaymentsSummary() {
        requireSystemAdmin();
        List<PaymentTransactionEntity> allPayments = paymentTransactions.findAllByOrderByCreatedAtDesc();
        long successfulPayments = allPayments.stream().filter(this::isSuccessfulPayment).count();
        long failedPayments = allPayments.stream().filter(this::isFailedPayment).count();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("totalPayments", allPayments.size());
        output.put("successfulPayments", successfulPayments);
        output.put("failedPayments", failedPayments);
        output.put("pendingManualPayments", pendingManualPayments(allPayments).size());
        output.put("paymentSuccessRate", percentage(successfulPayments, successfulPayments + failedPayments));
        output.put("totalSuccessfulRevenue", allPayments.stream().filter(this::isSuccessfulPayment).map(PaymentTransactionEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        output.put("currency", "VND");
        output.put("byStatus", enumCountChart(
                "Payment theo trạng thái",
                Arrays.stream(PaymentTransactionStatus.values()).map(Enum::name).toList(),
                status -> allPayments.stream().filter(payment -> payment.getStatus().name().equals(status)).count()
        ).get("series"));
        output.put("pendingManualPaymentTable", pendingManualPayments(allPayments).stream().limit(20).map(this::toPaymentTransactionView).toList());
        return output;
    }

    public Map<String, Object> adminDashboardFeedbackSummary() {
        requireSystemAdmin();
        List<BusinessFeedbackEntity> feedback = businessFeedback.findAllByOrderByCreatedAtDesc();
        Map<String, Long> byRating = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            int fixedRating = rating;
            byRating.put(String.valueOf(rating), feedback.stream().filter(item -> item.getRating() == fixedRating).count());
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("totalFeedback", feedback.size());
        output.put("newFeedback", feedback.stream().filter(item -> item.getStatus() == FeedbackStatus.NEW).count());
        output.put("reviewedFeedback", feedback.stream().filter(item -> item.getStatus() == FeedbackStatus.REVIEWED).count());
        output.put("averageRating", feedbackRatingAverage(feedback));
        output.put("ratingChart", countChart("Feedback theo rating", byRating).get("series"));
        output.put("recentFeedback", feedback.stream().limit(10).map(this::toBusinessFeedbackView).toList());
        return output;
    }

    public BusinessFeedbackView submitBusinessFeedback(BusinessFeedbackRequest request) {
        AuthenticatedUser user = currentUser();
        if (isPlatformAdminRole(user.role())) throw new IllegalArgumentException("System Admin không gửi feedback thay workspace.");
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

    public List<AuditLogView> adminAuditLogs() {
        requireSystemAdmin();
        return auditLogs.findAllByOrderByCreatedAtDesc().stream().map(this::toAuditLogView).toList();
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

    private Map<String, Object> taskDomainAnalysisPayload(TaskDomainAnalysisRequest request, UUID workspaceId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspaceId", workspaceId.toString());
        payload.put("taskTitle", request.taskTitle());
        payload.put("taskDescription", request.taskDescription());
        payload.put("projectDescription", request.projectDescription());
        payload.put("departmentName", request.departmentName());
        payload.put("availableTaskTypes", availableTaskTypes(workspaceId));
        payload.put("availableJobPositions", availableBusinessPositionNames(workspaceId));
        payload.put("availableSkills", availableWorkspaceSkills(workspaceId));
        payload.put("availableDepartments", availableDepartmentNames(workspaceId));
        payload.put("startDate", request.startDate() == null ? null : request.startDate().toString());
        payload.put("deadline", request.deadline() == null ? null : request.deadline().toString());
        return payload;
    }

    private RecommendAssigneeRequest enrichRecommendationRequestWithDomainAnalysis(RecommendAssigneeRequest request) {
        if (request == null || (request.departmentId() != null && request.requiredJobPositionId() != null && hasText(request.requiredSkills()) && hasText(request.taskDomain()))) {
            return request;
        }
        UUID workspaceId = currentUser().workspaceId();
        TaskDomainAnalysisRequest analysisRequest = new TaskDomainAnalysisRequest(
                request.title(),
                request.requirements(),
                request.taskDomain(),
                null,
                null,
                request.deadline()
        );
        Map<String, Object> analysis;
        try {
            enforceAiUsageLimit();
            analysis = aiServiceClient.analyzeTaskDescription(taskDomainAnalysisPayload(analysisRequest, workspaceId));
            saveAiHistory("TASK_DOMAIN_ANALYSIS_FOR_RECOMMENDATION", AiHistoryStatus.SUCCESS);
        } catch (AiProviderException exception) {
            saveAiHistory("TASK_DOMAIN_ANALYSIS_FOR_RECOMMENDATION", AiHistoryStatus.FAILED);
            log.warn("AI task domain analysis before recommendation failed; using request as-is. message={}", fallbackReason(exception));
            analysis = fallbackTaskDomainAnalysis(analysisRequest, workspaceId, exception);
        }
        UUID departmentId = request.departmentId() == null ? resolveDepartmentId(workspaceId, stringValue(analysis.get("relatedDepartment"))) : request.departmentId();
        UUID jobPositionId = request.requiredJobPositionId() == null ? resolveBusinessPositionId(workspaceId, stringList(analysis.get("requiredJobPositions"))) : request.requiredJobPositionId();
        String requiredSkills = hasText(request.requiredSkills()) ? request.requiredSkills() : String.join(", ", stringList(analysis.get("requiredSkills")));
        String taskDomain = hasText(request.taskDomain()) ? request.taskDomain() : stringValue(analysis.get("taskDomain"));
        return new RecommendAssigneeRequest(
                request.title(),
                request.requirements(),
                request.deadline(),
                request.estimatedHours(),
                taskDomain,
                departmentId,
                jobPositionId,
                requiredSkills
        );
    }

    private UUID resolveDepartmentId(UUID workspaceId, String departmentName) {
        if (!hasText(departmentName)) return null;
        String normalized = normalizedSearchText(departmentName);
        return departments.findByWorkspaceIdAndStatusOrderByNameAsc(workspaceId, DepartmentStatus.ACTIVE).stream()
                .filter(department -> normalizedSearchText(department.getName()).equals(normalized))
                .map(DepartmentEntity::getId)
                .findFirst()
                .orElseGet(() -> departments.findByWorkspaceIdAndStatusOrderByNameAsc(workspaceId, DepartmentStatus.ACTIVE).stream()
                        .filter(department -> textOverlapCount(normalized, normalizedSearchText(department.getName())) > 0)
                        .map(DepartmentEntity::getId)
                        .findFirst()
                        .orElse(null));
    }

    private UUID resolveBusinessPositionId(UUID workspaceId, List<String> positionNames) {
        if (positionNames == null || positionNames.isEmpty()) return null;
        List<JobPositionEntity> activePositions = jobPositions.findByWorkspaceIdOrderByNameAsc(workspaceId).stream()
                .filter(position -> position.getStatus() == JobPositionStatus.ACTIVE)
                .toList();
        for (String name : positionNames) {
            String normalized = normalizedSearchText(name);
            UUID exactMatch = activePositions.stream()
                    .filter(position -> normalizedSearchText(position.getTitle()).equals(normalized))
                    .map(JobPositionEntity::getId)
                    .findFirst()
                    .orElse(null);
            if (exactMatch != null) return exactMatch;
        }
        for (String name : positionNames) {
            String normalized = normalizedSearchText(name);
            UUID fuzzyMatch = activePositions.stream()
                    .filter(position -> textOverlapCount(normalized, normalizedSearchText(position.getTitle())) > 0)
                    .map(JobPositionEntity::getId)
                    .findFirst()
                    .orElse(null);
            if (fuzzyMatch != null) return fuzzyMatch;
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(this::hasText).toList();
        }
        return hasText(stringValue(value)) ? List.of(stringValue(value)) : List.of();
    }

    private List<String> availableTaskTypes(UUID workspaceId) {
        return tasks.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .map(TaskEntity::getTaskDomain)
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .limit(50)
                .toList();
    }

    private List<String> availableBusinessPositionNames(UUID workspaceId) {
        return jobPositions.findByWorkspaceIdOrderByNameAsc(workspaceId).stream()
                .filter(position -> position.getStatus() == JobPositionStatus.ACTIVE)
                .map(JobPositionEntity::getTitle)
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .limit(100)
                .toList();
    }

    private List<String> availableDepartmentNames(UUID workspaceId) {
        return departments.findByWorkspaceIdAndStatusOrderByNameAsc(workspaceId, DepartmentStatus.ACTIVE).stream()
                .map(DepartmentEntity::getName)
                .limit(100)
                .toList();
    }

    private List<String> availableWorkspaceSkills(UUID workspaceId) {
        Set<String> skills = new LinkedHashSet<>();
        workspaceEmployees(workspaceId).forEach(employee -> addDelimitedValues(skills, employee.getSkills()));
        jobPositions.findByWorkspaceIdOrderByNameAsc(workspaceId).forEach(position -> addDelimitedValues(skills, position.getRequiredSkills()));
        return skills.stream().limit(150).toList();
    }

    private void addDelimitedValues(Set<String> values, String rawValue) {
        if (!hasText(rawValue)) return;
        Arrays.stream(rawValue.split("[,;\\n]"))
                .map(String::trim)
                .filter(this::hasText)
                .forEach(values::add);
    }

    private Map<String, Object> fallbackTaskDomainAnalysis(TaskDomainAnalysisRequest request, UUID workspaceId, AiProviderException exception) {
        List<String> positions = availableBusinessPositionNames(workspaceId);
        List<String> departments = availableDepartmentNames(workspaceId);
        List<String> skills = availableWorkspaceSkills(workspaceId);
        String text = normalizedSearchText(request.taskTitle() + " " + request.taskDescription() + " " + (request.projectDescription() == null ? "" : request.projectDescription()));
        List<String> matchingPositions = positions.stream()
                .filter(position -> textOverlapCount(text, normalizedSearchText(position)) > 0)
                .limit(8)
                .toList();
        List<String> matchingSkills = skills.stream()
                .filter(skill -> textOverlapCount(text, normalizedSearchText(skill)) > 0)
                .limit(12)
                .toList();
        String relatedDepartment = departments.stream()
                .filter(department -> textOverlapCount(text, normalizedSearchText(department)) > 0)
                .findFirst()
                .orElse(departments.isEmpty() ? "UNKNOWN" : departments.getFirst());
        Map<String, Object> hours = new LinkedHashMap<>();
        hours.put("value", null);
        hours.put("reason", "AI service unavailable; estimated hours require manual confirmation.");
        hours.put("confidence", 0);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("taskType", "UNKNOWN");
        output.put("taskDomain", hasText(request.departmentName()) ? request.departmentName() : relatedDepartment);
        output.put("suggestedDifficulty", "MEDIUM");
        output.put("suggestedEmployeeLevel", "MIDDLE");
        output.put("requiredSkills", matchingSkills);
        output.put("requiredJobPositions", matchingPositions);
        output.put("relatedDepartment", relatedDepartment);
        output.put("estimatedWorkingHoursSuggestion", hours);
        output.put("missingInformation", request.deadline() == null ? List.of("deadline") : List.of());
        output.put("clarifyingQuestions", List.of("Please confirm task domain, required business positions, required skills, and estimated hours."));
        output.put("summary", "Fallback analysis based only on existing workspace departments, business positions, and skills. Reason: " + fallbackReason(exception));
        return output;
    }

    private List<AiEmployeeWorkload> assigneeCandidates(RecommendAssigneeRequest request) {
        UUID workspaceId = currentUser().workspaceId();
        List<TaskEntity> scopedTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        return workspaceEmployees(workspaceId).stream()
                .filter(employee -> employee.getStatus() == UserStatus.ACTIVE)
                .map(employee -> {
                    WorkloadView workload = workloadForEmployee(employee, scopedTasks);
                    Map<String, Object> scoreComponents = scoreComponents(workload, employee, request);
                    int leadershipScore = leadershipScore(employee, scopedTasks, request, scoreComponents);
                    int teamMemberScore = teamMemberScore(employee, scopedTasks, request, scoreComponents);
                    long leadTaskCount = leadTasks(employee, scopedTasks).size();
                    long leadCompletedTasks = leadTasks(employee, scopedTasks).stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
                    double leadCompletionRate = leadTaskCount == 0 ? 0 : leadCompletedTasks * 1.0 / leadTaskCount;
                    int similarTaskCount = similarTaskCount(employee, scopedTasks, request);
                    int domainMatchScore = domainMatchScore(request, employee);
                    JobPositionEntity businessPosition = employee.getJobPositionId() == null ? null : jobPositions.findById(employee.getJobPositionId())
                            .filter(position -> position.getWorkspaceId().equals(workspaceId))
                            .orElse(null);
                    return new AiEmployeeWorkload(
                            workload.employeeId().toString(),
                            workload.fullName(),
                            workload.openTasks(),
                            workload.completedTasks(),
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
                            employee.getDepartmentId(),
                            employee.getJobPositionId(),
                            businessPosition == null ? null : businessPosition.getTitle(),
                            businessPosition == null ? null : businessPosition.getPermissionGroup(),
                            (int) scoreComponents.get("candidateScore"),
                            leadershipScore,
                            teamMemberScore,
                            leadTaskCount,
                            leadCompletedTasks,
                            leadCompletionRate,
                            similarTaskCount,
                            domainMatchScore,
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
        int departmentMatchScore = departmentMatchScore(request, employee);
        int jobPositionMatchScore = jobPositionMatchScore(request, employee);
        int taskProfileMatchScore = taskProfileMatchScore(request, employee);
        int departmentSuitabilityScore = departmentSuitabilityScore(request, employee);
        int businessPositionSuitabilityScore = businessPositionSuitabilityScore(request, employee);
        int skillMatchScore = clampScore((int) Math.round(taskProfileMatchScore * 100.0 / 30.0));
        int workloadAvailabilityScore = workloadAvailabilityScore(workload);
        int performanceScore = performanceScore(workload);
        int domainExperienceScore = clampScore(domainMatchScore(request, employee) * 5);
        boolean taskNeedsProfileFit = request != null && hasText(request.title() + " " + request.requirements());
        boolean employeeHasProfile = hasText(employee.getJobTitle()) || hasText(employee.getSkills());
        int profileMismatchPenalty = taskNeedsProfileFit && employeeHasProfile && taskProfileMatchScore == 0 ? 55 : 0;
        int missingProfilePenalty = taskNeedsProfileFit && !employeeHasProfile ? 35 : 0;
        int departmentMismatchPenalty = request != null && request.departmentId() != null && departmentMatchScore == 0 ? 30 : 0;
        int jobPositionMismatchPenalty = request != null && request.requiredJobPositionId() != null && jobPositionMatchScore == 0 ? 45 : 0;
        double openPenalty = workload.openTasks() * 6.0;
        double overduePenalty = workload.overdueTasks() * 18.0;
        double blockedPenalty = workload.blockedTasks() * 12.0;
        double workloadPenalty = workload.estimatedWorkload().doubleValue() / 2.0;
        int structuralFitBonus = departmentMatchScore + jobPositionMatchScore;
        int candidateScore = (int) Math.max(0, Math.min(100, Math.round(100 - openPenalty - overduePenalty - blockedPenalty - workloadPenalty - levelPenalty - profilePenalty - profileMismatchPenalty - missingProfilePenalty - departmentMismatchPenalty - jobPositionMismatchPenalty + taskProfileMatchScore + structuralFitBonus)));
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
        components.put("departmentMatchScore", departmentMatchScore);
        components.put("jobPositionMatchScore", jobPositionMatchScore);
        components.put("departmentMismatchPenalty", departmentMismatchPenalty);
        components.put("jobPositionMismatchPenalty", jobPositionMismatchPenalty);
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
        components.put("departmentSuitabilityScore", departmentSuitabilityScore);
        components.put("businessPositionSuitabilityScore", businessPositionSuitabilityScore);
        components.put("skillMatchScore", skillMatchScore);
        components.put("workloadAvailabilityScore", workloadAvailabilityScore);
        components.put("performanceScore", performanceScore);
        components.put("domainExperienceScore", domainExperienceScore);
        return components;
    }

    private int leadershipScore(UserEntity employee, List<TaskEntity> scopedTasks, RecommendAssigneeRequest request, Map<String, Object> scoreComponents) {
        List<TaskEntity> ledTasks = leadTasks(employee, scopedTasks);
        long completedLedTasks = ledTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        long overdueLedTasks = ledTasks.stream().filter(this::isOverdue).count();
        double completionRate = ledTasks.isEmpty() ? 0 : completedLedTasks * 1.0 / ledTasks.size();
        int leadExperienceScore = clampScore((int) Math.min(100, ledTasks.size() * 12 + Math.round(completionRate * 50) - overdueLedTasks * 10));
        scoreComponents.put("leadExperienceScore", leadExperienceScore);
        return weightedScore(
                numberComponent(scoreComponents, "departmentSuitabilityScore"), 25,
                numberComponent(scoreComponents, "businessPositionSuitabilityScore"), 25,
                leadExperienceScore, 15,
                numberComponent(scoreComponents, "domainExperienceScore"), 15,
                numberComponent(scoreComponents, "skillMatchScore"), 10,
                numberComponent(scoreComponents, "workloadAvailabilityScore"), 5,
                numberComponent(scoreComponents, "performanceScore"), 5
        );
    }

    private int teamMemberScore(UserEntity employee, List<TaskEntity> scopedTasks, RecommendAssigneeRequest request, Map<String, Object> scoreComponents) {
        int similarTaskExperienceScore = clampScore(Math.min(100, similarTaskCount(employee, scopedTasks, request) * 20));
        scoreComponents.put("similarTaskExperienceScore", similarTaskExperienceScore);
        return weightedScore(
                numberComponent(scoreComponents, "departmentSuitabilityScore"), 25,
                numberComponent(scoreComponents, "businessPositionSuitabilityScore"), 25,
                numberComponent(scoreComponents, "skillMatchScore"), 20,
                similarTaskExperienceScore, 10,
                numberComponent(scoreComponents, "workloadAvailabilityScore"), 10,
                numberComponent(scoreComponents, "performanceScore"), 10
        );
    }

    private int departmentMatchScore(RecommendAssigneeRequest request, UserEntity employee) {
        if (request == null || request.departmentId() == null) {
            return 0;
        }
        return request.departmentId().equals(employee.getDepartmentId()) ? 20 : 0;
    }

    private int jobPositionMatchScore(RecommendAssigneeRequest request, UserEntity employee) {
        if (request == null || request.requiredJobPositionId() == null) {
            return 0;
        }
        return request.requiredJobPositionId().equals(employee.getJobPositionId()) ? 30 : 0;
    }

    private int departmentSuitabilityScore(RecommendAssigneeRequest request, UserEntity employee) {
        if (request == null || request.departmentId() == null) return 50;
        return request.departmentId().equals(employee.getDepartmentId()) ? 100 : 0;
    }

    private int businessPositionSuitabilityScore(RecommendAssigneeRequest request, UserEntity employee) {
        if (request == null || request.requiredJobPositionId() == null) return 50;
        return request.requiredJobPositionId().equals(employee.getJobPositionId()) ? 100 : 0;
    }

    private int workloadAvailabilityScore(WorkloadView workload) {
        int base = switch (workload.workloadLevel()) {
            case NO_WORK -> 100;
            case LOW -> 85;
            case NORMAL -> 65;
            case HIGH -> 35;
            case OVERLOADED -> 0;
        };
        return clampScore((int) Math.round(base - workload.overdueTasks() * 12 - workload.blockedTasks() * 8));
    }

    private int performanceScore(WorkloadView workload) {
        long total = workload.completedTasks() + workload.overdueTasks() + workload.blockedTasks();
        if (total == 0) return 50;
        double completionRate = workload.completedTasks() * 1.0 / total;
        double riskRate = (workload.overdueTasks() + workload.blockedTasks()) * 1.0 / total;
        return clampScore((int) Math.round(completionRate * 100 - riskRate * 40));
    }

    private List<TaskEntity> leadTasks(UserEntity employee, List<TaskEntity> scopedTasks) {
        Set<UUID> ledTaskIds = taskAssignees.findByWorkspaceIdAndEmployeeId(employee.getWorkspaceId(), employee.getId()).stream()
                .filter(TaskAssigneeEntity::isLeader)
                .map(TaskAssigneeEntity::getTaskId)
                .collect(java.util.stream.Collectors.toSet());
        return scopedTasks.stream().filter(task -> ledTaskIds.contains(task.getId())).toList();
    }

    private int similarTaskCount(UserEntity employee, List<TaskEntity> scopedTasks, RecommendAssigneeRequest request) {
        String taskText = normalizedRecommendationText(request);
        if (taskText.isBlank()) return 0;
        return (int) scopedTasks.stream()
                .filter(task -> taskParticipants(task).contains(employee.getId()))
                .filter(task -> textOverlapCount(taskText, normalizedSearchText(task.getTitle() + " " + task.getRequirements() + " " + (task.getTaskDomain() == null ? "" : task.getTaskDomain()))) >= 2)
                .count();
    }

    private int domainMatchScore(RecommendAssigneeRequest request, UserEntity employee) {
        if (request == null) return 0;
        String taskText = normalizedRecommendationText(request);
        String profileText = normalizedSearchText((employee.getJobTitle() == null ? "" : employee.getJobTitle()) + " " + (employee.getSkills() == null ? "" : employee.getSkills()) + " " + (employee.getMainExpertise() == null ? "" : employee.getMainExpertise()) + " " + (employee.getSecondaryExpertise() == null ? "" : employee.getSecondaryExpertise()));
        if (taskText.isBlank() || profileText.isBlank()) return 0;
        long matches = Arrays.stream(taskText.split("\\s+"))
                .filter(term -> term.length() >= 3)
                .distinct()
                .filter(profileText::contains)
                .limit(4)
                .count();
        return (int) Math.min(20, matches * 5);
    }

    private String normalizedRecommendationText(RecommendAssigneeRequest request) {
        if (request == null) return "";
        return normalizedSearchText((request.title() == null ? "" : request.title()) + " " + (request.requirements() == null ? "" : request.requirements()));
    }

    private long textOverlapCount(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) return 0;
        return Arrays.stream(left.split("\\s+"))
                .filter(term -> term.length() >= 3)
                .distinct()
                .filter(right::contains)
                .limit(6)
                .count();
    }

    private int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int weightedScore(int scoreA, int weightA, int scoreB, int weightB, int scoreC, int weightC, int scoreD, int weightD, int scoreE, int weightE, int scoreF, int weightF, int scoreG, int weightG) {
        return clampScore((int) Math.round((
                scoreA * weightA
                        + scoreB * weightB
                        + scoreC * weightC
                        + scoreD * weightD
                        + scoreE * weightE
                        + scoreF * weightF
                        + scoreG * weightG
        ) / 100.0));
    }

    private int weightedScore(int scoreA, int weightA, int scoreB, int weightB, int scoreC, int weightC, int scoreD, int weightD, int scoreE, int weightE, int scoreF, int weightF) {
        return weightedScore(scoreA, weightA, scoreB, weightB, scoreC, weightC, scoreD, weightD, scoreE, weightE, scoreF, weightF, 0, 0);
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
                .map(candidate -> recommendationView(candidate, candidate.candidateScore(), null, roleFitLabel(candidate), fallbackRoleFitReason(candidate), fallbackAssigneeReason(candidate), fallbackAssigneeRisk(candidate)))
                .toList();
    }

    private AssigneeRecommendationView teamLeaderRecommendation(AiEmployeeWorkload candidate) {
        int departmentScore = numberComponent(candidate, "departmentMatchScore");
        int jobPositionScore = numberComponent(candidate, "jobPositionMatchScore");
        String reason = displayText(candidate.fullName()) + " đạt " + candidate.leadershipScore()
            + " điểm lead: khớp phòng ban +" + departmentScore
            + ", khớp business position +" + jobPositionScore
            + ", từng làm leader " + candidate.leadTaskCount()
                + " task, hoàn thành " + candidate.leadCompletedTasks()
                + " task lead, tỉ lệ hoàn thành lead " + Math.round(candidate.leadCompletionRate() * 100)
                + "%, domain match +" + candidate.domainMatchScore()
                + ", similar tasks " + candidate.similarTaskCount() + ".";
        String risk = candidate.workloadLevel() == WorkloadLevel.OVERLOADED
                ? "Rủi ro cao: ứng viên leader đang quá tải."
                : candidate.overdueTasks() > 0
                ? "Rủi ro trung bình: ứng viên leader còn task quá hạn."
                : "Không có rủi ro lớn cho vai trò leader.";
        return recommendationView(candidate, candidate.leadershipScore(), "TEAM_LEADER", roleFitLabel(candidate), fallbackRoleFitReason(candidate), reason, risk);
    }

    private AssigneeRecommendationView teamMemberRecommendation(AiEmployeeWorkload candidate) {
        int departmentScore = numberComponent(candidate, "departmentMatchScore");
        int jobPositionScore = numberComponent(candidate, "jobPositionMatchScore");
        String reason = displayText(candidate.fullName()) + " đạt " + candidate.teamMemberScore()
            + " điểm member: khớp phòng ban +" + departmentScore
            + ", khớp business position +" + jobPositionScore
            + ", score cá nhân " + candidate.candidateScore()
                + ", domain match +" + candidate.domainMatchScore()
                + ", similar tasks " + candidate.similarTaskCount()
                + ", mức tải " + vietnameseWorkloadLevel(candidate.workloadLevel()) + ".";
        String risk = candidate.workloadLevel() == WorkloadLevel.OVERLOADED
                ? "Rủi ro cao: thành viên đang quá tải."
                : candidate.blockedTasks() > 0
                ? "Rủi ro trung bình: thành viên đang có task bị vướng mắc."
                : candidate.overdueTasks() > 0
                ? "Rủi ro trung bình: thành viên còn task quá hạn."
                : "Không có rủi ro lớn cho vai trò member.";
        return recommendationView(candidate, candidate.teamMemberScore(), "TEAM_MEMBER", roleFitLabel(candidate), fallbackRoleFitReason(candidate), reason, risk);
    }

    private AssigneeRecommendationView recommendationView(AiEmployeeWorkload candidate, int score, String requiredRole, String roleFit, String roleFitReason, String reason, String risk) {
        return new AssigneeRecommendationView(
                UUID.fromString(candidate.employeeId()),
                displayText(candidate.fullName()),
                score,
                candidate.workloadLevel(),
                requiredRole,
                roleFit,
                roleFitReason,
                reason,
                risk,
                candidate.departmentId(),
                candidate.businessPositionId(),
                candidate.businessPositionName(),
                candidate.permissionGroup(),
                numberComponent(candidate, "departmentSuitabilityScore"),
                numberComponent(candidate, "businessPositionSuitabilityScore"),
                numberComponent(candidate, "leadExperienceScore"),
                numberComponent(candidate, "domainExperienceScore"),
                numberComponent(candidate, "skillMatchScore"),
                numberComponent(candidate, "similarTaskExperienceScore"),
                numberComponent(candidate, "workloadAvailabilityScore"),
                numberComponent(candidate, "performanceScore"),
                candidate.leadTaskCount(),
                candidate.leadCompletionRate(),
                candidate.similarTaskCount(),
                candidate.scoreComponents()
        );
    }

    private String fallbackAssigneeReason(AiEmployeeWorkload candidate) {
        Object taskProfileMatchScore = candidate.scoreComponents().getOrDefault("taskProfileMatchScore", 0);
        Object departmentMatchScore = candidate.scoreComponents().getOrDefault("departmentMatchScore", 0);
        Object jobPositionMatchScore = candidate.scoreComponents().getOrDefault("jobPositionMatchScore", 0);
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
                + vietnameseWorkloadLevel(candidate.workloadLevel()) + ", khớp phòng ban +" + departmentMatchScore
                + ", khớp business position +" + jobPositionMatchScore + ", độ khớp hồ sơ +" + taskProfileMatchScore + "."
                + profileNote;
    }

    private String roleFitLabel(AiEmployeeWorkload candidate) {
        if (numberComponent(candidate, "departmentMatchScore") > 0 || numberComponent(candidate, "jobPositionMatchScore") > 0) {
            return "STRONG";
        }
        if (numberComponent(candidate, "profileMismatchPenalty") > 0) {
            return "UNCERTAIN";
        }
        if (numberComponent(candidate, "missingProfilePenalty") > 0) {
            return "UNCERTAIN";
        }
        return numberComponent(candidate, "taskProfileMatchScore") >= 10 ? "PARTIAL" : "UNCERTAIN";
    }

    private String fallbackRoleFitReason(AiEmployeeWorkload candidate) {
        if (numberComponent(candidate, "departmentMatchScore") > 0 || numberComponent(candidate, "jobPositionMatchScore") > 0) {
            return "Ứng viên khớp trực tiếp phòng ban hoặc business position được yêu cầu nên hệ thống ưu tiên cao hơn các tín hiệu khác.";
        }
        if (numberComponent(candidate, "profileMismatchPenalty") > 0) {
            return "Hồ sơ hiện chưa có tín hiệu chuyên môn khớp với nội dung task; cần owner kiểm tra lại trước khi giao.";
        }
        if (numberComponent(candidate, "missingProfilePenalty") > 0) {
            return "Hồ sơ chuyên môn còn thiếu job title hoặc skills nên hệ thống chưa đủ dữ liệu để xác nhận vai trò phù hợp.";
        }
        return "Hồ sơ có tín hiệu chuyên môn khớp một phần với nội dung task theo dữ liệu job title và skills hiện có.";
    }

    private int numberComponent(AiEmployeeWorkload candidate, String key) {
        return numberComponent(candidate.scoreComponents(), key);
    }

    private int numberComponent(Map<String, Object> components, String key) {
        Object value = components.get(key);
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
                    return recommendationView(candidate, candidate.candidateScore(), item.requiredRole(), item.roleFit(), item.roleFitReason(), item.reason(), item.risk());
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
        output.put("summary", "Đang theo dõi " + currentWorkload.size()
                + " nhân viên: " + overloadedEmployees.size()
                + " quá tải, " + idleEmployees.size()
                + " chưa có task mở, " + overdueEmployees.size()
                + " có task quá hạn.");
        output.put("workloadInsights", fallbackWorkloadInsights(currentWorkload, overloadedEmployees, idleEmployees, overdueEmployees));
        output.put("recommendedActions", fallbackWorkloadActions(overloadedEmployees, idleEmployees, overdueEmployees));
        return withFallbackMetadata(output, exception);
    }

    private List<String> fallbackWorkloadInsights(List<WorkloadView> currentWorkload, List<String> overloadedEmployees, List<String> idleEmployees, List<String> overdueEmployees) {
        List<String> insights = new ArrayList<>();
        if (currentWorkload.isEmpty()) {
            insights.add("Chưa có dữ liệu nhân viên để đánh giá mức tải.");
            return insights;
        }
        insights.add("Tổng quan workload dựa trên task đang mở, task quá hạn, task bị blocker và tổng giờ ước tính.");
        if (!overloadedEmployees.isEmpty()) {
            insights.add("Nhóm quá tải cần được rà soát trước khi giao thêm việc: " + String.join(", ", overloadedEmployees) + ".");
        }
        if (!idleEmployees.isEmpty()) {
            insights.add("Có nhân sự chưa có task mở, có thể cân nhắc phân bổ việc mới nếu kỹ năng phù hợp: " + String.join(", ", idleEmployees) + ".");
        }
        if (!overdueEmployees.isEmpty()) {
            insights.add("Có nhân sự gắn với task quá hạn, owner nên kiểm tra ETA và rào cản thực tế: " + String.join(", ", overdueEmployees) + ".");
        }
        if (insights.size() == 1) {
            insights.add("Chưa phát hiện tín hiệu quá tải hoặc quá hạn nổi bật trong dữ liệu hiện tại.");
        }
        return insights;
    }

    private List<String> fallbackWorkloadActions(List<String> overloadedEmployees, List<String> idleEmployees, List<String> overdueEmployees) {
        List<String> actions = new ArrayList<>();
        if (!overloadedEmployees.isEmpty()) {
            actions.add("Không giao thêm task cho nhóm quá tải cho tới khi rà soát lại deadline và phân bổ.");
        }
        if (!overdueEmployees.isEmpty()) {
            actions.add("Ưu tiên follow-up task quá hạn và yêu cầu cập nhật ETA trong ngày.");
        }
        if (!idleEmployees.isEmpty()) {
            actions.add("Dùng danh sách nhân sự tải thấp để cân bằng lại task mới hoặc task cần hỗ trợ.");
        }
        if (actions.isEmpty()) {
            actions.add("Tiếp tục theo dõi workload định kỳ và yêu cầu nhân viên cập nhật tiến độ/report đầy đủ.");
        }
        return actions;
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
        output.put("summary", fallbackDelayRiskSummary(risks));
        output.put("risks", risks);
        output.put("recommendedActions", fallbackDelayRecommendedActions(risks));
        return withFallbackMetadata(output, exception);
    }

    private String fallbackDelayRiskSummary(List<Map<String, Object>> risks) {
        long high = risks.stream().filter(item -> "HIGH".equals(item.get("riskLevel"))).count();
        long medium = risks.stream().filter(item -> "MEDIUM".equals(item.get("riskLevel"))).count();
        if (risks.isEmpty()) {
            return "Chưa phát hiện task có tín hiệu trễ hạn rõ ràng từ dữ liệu hiện tại.";
        }
        return "Có " + risks.size() + " task cần owner kiểm tra, gồm " + high + " rủi ro cao và " + medium + " rủi ro trung bình.";
    }

    private List<String> fallbackDelayRecommendedActions(List<Map<String, Object>> risks) {
        if (risks.isEmpty()) {
            return List.of("Tiếp tục theo dõi deadline, blocker và cập nhật tiến độ hằng ngày.");
        }
        return risks.stream()
                .limit(3)
                .map(item -> displayText(String.valueOf(item.get("recommendedAction"))))
                .toList();
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
        item.put("reason", fallbackDelayReasonClean(task));
        item.put("recommendedAction", fallbackDelayActionClean(task, employeeNames));
        return item;
    }

    private String fallbackDelayReasonClean(TaskEntity task) {
        long hoursUntilDeadline = Duration.between(OffsetDateTime.now(), task.getDeadline()).toHours();
        if (isOverdue(task)) {
            return "Task đã quá deadline và chưa hoàn thành.";
        }
        if (task.getStatus() == TaskStatus.BLOCKED) {
            return "Task đang có vướng mắc cần owner kiểm tra.";
        }
        if (hoursUntilDeadline <= 48 && task.getProgressPercent() < 30) {
            return "Tiến độ dưới 30% trong khi deadline còn dưới 48 giờ.";
        }
        if (hoursUntilDeadline <= 48) {
            return "Deadline sắp đến trong vòng 48 giờ.";
        }
        if (task.getProgressPercent() < 50) {
            return "Tiến độ task dưới 50%, cần cập nhật tình hình.";
        }
        if (task.getPriority() == TaskPriority.CRITICAL || task.getPriority() == TaskPriority.HIGH) {
            return "Task ưu tiên cao cần được theo dõi sát.";
        }
        return "Task có tín hiệu cần theo dõi thêm.";
    }

    private String fallbackDelayActionClean(TaskEntity task, Map<UUID, String> employeeNames) {
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
        for (UserEntity employee : workspaceEmployees(workspaceId).stream()
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
        output.put("summary", fallbackActionSummary(suggestions));
        output.put("suggestions", suggestions);
        return withFallbackMetadata(output, exception);
    }

    private String fallbackActionSummary(List<Map<String, Object>> suggestions) {
        if (suggestions.isEmpty()) {
            return "Chưa có hành động khẩn cấp. Owner nên tiếp tục theo dõi task quá hạn, blocker, workload và daily report.";
        }
        return "Có " + suggestions.size() + " khuyến nghị hành động. Ưu tiên xử lý: " + displayText(String.valueOf(suggestions.getFirst().get("title"))) + ".";
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

    private Map<String, Object> fallbackEstimateHours(Map<String, Object> payload, AiProviderException exception) {
        long workingDays = Math.max(1, longValue(payload, "backendWorkingDays"));
        Number configured = numberFrom(payload.get("backendDefaultHours"));
        double suggestedHours = configured.doubleValue() > 0 ? configured.doubleValue() : workingDays * 8.0;
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("suggestedHours", suggestedHours);
        output.put("workingDays", workingDays);
        output.put("calculationBasis", "Backend default based on working days; user confirmation is required.");
        output.put("confidence", 0.35);
        output.put("userConfirmationRequired", true);
        return withFallbackMetadata(output, exception);
    }

    private Map<String, Object> fallbackRecommendationExplanation(String recommendationType, RecommendationExplanationRequest request, AiProviderException exception) {
        List<Map<String, Object>> ranked = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> candidate : request.candidates()) {
            Map<String, Object> item = new LinkedHashMap<>(candidate);
            item.putIfAbsent("rank", rank++);
            item.putIfAbsent("summaryReason", "The order is computed by backend ranking data. AI narrative is temporarily unavailable.");
            item.putIfAbsent("strengths", List.of());
            item.putIfAbsent("risks", candidate.get("riskFlags") instanceof List<?> risks ? risks : List.of());
            ranked.add(item);
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("recommendationType", recommendationType);
        output.put("taskSummary", stringValue(request.task(), "title"));
        if ("TEAM_LEADER".equals(recommendationType)) {
            output.put("taskOrProjectDomain", stringValue(request.task(), "taskDomain"));
            output.put("leaderCandidates", ranked);
        } else if ("TEAM_MEMBER".equals(recommendationType)) {
            output.put("memberCandidates", ranked);
            output.put("teamCompositionAdvice", "Use the backend ranking and verify role coverage before confirming the team.");
        } else {
            output.put("rankedCandidates", ranked);
        }
        output.put("finalNote", "Candidate ranking remains unchanged; only the optional AI narrative is unavailable.");
        return withFallbackMetadata(output, exception);
    }

    private Map<String, Object> fallbackRecommendationResult(RecommendationResultExplanationRequest request, AiProviderException exception) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("explanationTitle", "Backend recommendation result");
        output.put("shortExplanation", "The selection is preserved from backend-computed ranking data.");
        output.put("detailedExplanation", "AI narrative is temporarily unavailable. Review the ranking, workload and performance figures returned by the backend.");
        output.put("keyReasons", List.of("Backend ranking order", "Workload data", "Role and skill suitability"));
        output.put("riskWarnings", List.of());
        output.put("dataUsed", List.of("rankingData", "workloadData", "performanceData"));
        return withFallbackMetadata(output, exception);
    }

    private Map<String, Object> fallbackWorkloadRisk(WorkloadRiskExplanationRequest request, AiProviderException exception) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("overallRisk", hasText(request.backendOverallRisk()) ? request.backendOverallRisk().toUpperCase(Locale.ROOT) : "LOW");
        output.put("monthlyWarnings", request.monthlyWorkloadEvaluation());
        output.put("recommendation", "Use backend workload thresholds and review any month above capacity before assignment.");
        return withFallbackMetadata(output, exception);
    }

    private Map<String, Object> fallbackEmployeeReport(EmployeeReportAiRequest request, AiProviderException exception) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("reportType", stringValue(request.period(), "reportType"));
        output.put("employeeName", stringValue(request.employee(), "fullName"));
        output.put("periodSummary", "Report generated from backend metrics without optional AI narrative.");
        output.put("performanceEvaluation", "STABLE");
        output.put("keyMetrics", request.metrics());
        output.put("strengths", List.of());
        output.put("issues", request.risks() == null ? List.of() : request.risks());
        output.put("recommendations", List.of("Review the backend metrics and notable tasks with the employee."));
        return withFallbackMetadata(output, exception);
    }

    private Map<String, Object> fallbackOwnerOperationalSummary(Map<String, Object> payload, AiProviderException exception) {
        long overdue = longValue(payload, "overdueTasks");
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("summaryTitle", "Operational overview");
        output.put("businessHealthLabel", overdue > 0 ? "NEEDS_ATTENTION" : "STABLE");
        output.put("summary", "Backend metrics remain available while AI narrative is temporarily unavailable.");
        output.put("keyNumbers", Map.of("totalEmployees", longValue(payload, "totalEmployees"), "activeEmployees", longValue(payload, "activeEmployees"), "totalTasks", longValue(payload, "totalTasks"), "completedTasks", longValue(payload, "completedTasks"), "overdueTasks", overdue));
        output.put("workloadInsights", List.of("Review workload distribution returned by the backend dashboard."));
        output.put("subscriptionInsights", List.of("Subscription status and limits remain backend-computed."));
        output.put("risks", overdue > 0 ? List.of(overdue + " overdue task(s) require attention.") : List.of());
        output.put("recommendedActions", overdue > 0 ? List.of("Review overdue tasks and owners.") : List.of("Continue monitoring workload and deadlines."));
        return withFallbackMetadata(output, exception);
    }

    private Map<String, Object> fallbackPlatformSystemSummary(Map<String, Object> payload, AiProviderException exception) {
        long failedPayments = longValue(payload, "failedPayments");
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("summaryTitle", "Platform overview");
        output.put("platformStatusLabel", failedPayments > 0 ? "NEEDS_ATTENTION" : "STABLE");
        output.put("summary", "Platform figures are backend-computed; optional AI narrative is temporarily unavailable.");
        output.put("revenueInsights", List.of("Use backend revenue charts for authoritative totals."));
        output.put("workspaceInsights", List.of("Active workspaces: " + longValue(payload, "activeWorkspaces")));
        output.put("paymentInsights", List.of("Failed payments: " + failedPayments));
        output.put("feedbackInsights", List.of("Feedback aggregates remain available in the dashboard."));
        output.put("risks", failedPayments > 0 ? List.of("Failed payments require review.") : List.of());
        output.put("recommendedActions", failedPayments > 0 ? List.of("Review failed and manual-review payments.") : List.of("Continue monitoring platform health."));
        return withFallbackMetadata(output, exception);
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

    private double percentage(long numerator, long denominator) {
        return denominator <= 0 ? 0 : Math.round((numerator * 10000.0 / denominator)) / 100.0;
    }

    private Number numberFrom(Object value) {
        if (value instanceof Number number) return number;
        if (value instanceof String text && !text.isBlank()) {
            try {
                return new BigDecimal(text);
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
        long activeTasks = periodTasks.stream().filter(task -> openTaskStatuses().contains(task.getStatus())).count();
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

    private Map<String, Object> businessOwnerOperationalSummaryPayload() {
        UUID workspaceId = currentUser().workspaceId();
        WorkspaceEntity workspace = requireWorkspace(workspaceId);
        List<UserEntity> workforce = workspaceEmployees(workspaceId);
        List<TaskEntity> scopedTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        List<WorkloadView> currentWorkload = workforce.stream()
                .map(employee -> workloadForEmployee(employee, scopedTasks))
                .toList();
        long completedTasks = scopedTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        long overdueTasks = scopedTasks.stream().filter(this::isOverdue).count();
        long totalTasks = scopedTasks.size();
        Map<String, Object> workloadDistribution = new LinkedHashMap<>();
        Arrays.stream(WorkloadLevel.values()).forEach(level -> workloadDistribution.put(level.name(), currentWorkload.stream().filter(item -> item.workloadLevel() == level).count()));
        Map<UUID, DepartmentEntity> departmentById = departments.findByWorkspaceIdOrderByNameAsc(workspaceId).stream()
                .collect(java.util.stream.Collectors.toMap(DepartmentEntity::getId, item -> item, (left, right) -> left));
        List<Map<String, Object>> departmentWorkload = departmentById.values().stream()
                .map(department -> {
                    List<UserEntity> departmentEmployees = workforce.stream().filter(employee -> department.getId().equals(employee.getDepartmentId())).toList();
                    Set<UUID> departmentEmployeeIds = departmentEmployees.stream().map(UserEntity::getId).collect(java.util.stream.Collectors.toSet());
                    List<WorkloadView> departmentLoads = currentWorkload.stream().filter(load -> departmentEmployeeIds.contains(load.employeeId())).toList();
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("departmentId", department.getId().toString());
                    item.put("departmentName", department.getName());
                    item.put("employeeCount", departmentEmployees.size());
                    item.put("overloadedEmployees", departmentLoads.stream().filter(load -> load.workloadLevel() == WorkloadLevel.OVERLOADED).count());
                    item.put("allocatedHours", departmentLoads.stream().map(WorkloadView::estimatedWorkload).reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue());
                    return item;
                })
                .toList();
        Map<String, Object> planLimitUsage = new LinkedHashMap<>();
        planLimitUsage.put("maxUsers", workspace.getMaxUsers());
        planLimitUsage.put("currentUsers", currentWorkspaceUserCount(workspaceId));
        planLimitUsage.put("maxOwnerAccounts", workspace.getMaxOwnerAccounts());
        planLimitUsage.put("ownerAccountCount", workspace.getOwnerAccountCount());
        planLimitUsage.put("maxEmployeeAccounts", workspace.getMaxEmployeeAccounts());
        planLimitUsage.put("employeeAccountCount", workforce.size());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspaceId", workspaceId.toString());
        payload.put("totalEmployees", workforce.size());
        payload.put("activeEmployees", workforce.stream().filter(employee -> employee.getStatus() == UserStatus.ACTIVE).count());
        payload.put("totalTasks", totalTasks);
        payload.put("completedTasks", completedTasks);
        payload.put("overdueTasks", overdueTasks);
        payload.put("completionRate", totalTasks == 0 ? 0 : completedTasks * 1.0 / totalTasks);
        payload.put("overdueRate", totalTasks == 0 ? 0 : overdueTasks * 1.0 / totalTasks);
        payload.put("workloadDistribution", workloadDistribution);
        payload.put("departmentWorkload", departmentWorkload);
        payload.put("aiRecommendationEffectiveness", Map.of("generatedSuggestions", aiSuggestions.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).size()));
        payload.put("subscriptionStatus", workspace.getPaymentStatus().name() + "/" + workspace.getStatus().name());
        payload.put("planLimitUsage", planLimitUsage);
        payload.put("expirationDate", workspace.getExpiresAt() == null ? null : workspace.getExpiresAt().toString());
        payload.put("upgradeOptions", List.of("Increase max users", "Increase owner accounts", "Enable more AI usage"));
        return payload;
    }

    private Map<String, Object> platformAdminSystemSummaryPayload() {
        List<WorkspaceEntity> allWorkspaces = workspaces.findAll();
        List<PaymentTransactionEntity> allPayments = paymentTransactions.findAll();
        OffsetDateTime monthStart = OffsetDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        Map<String, Float> revenueByMonth = new LinkedHashMap<>();
        Map<String, Float> revenueByQuarter = new LinkedHashMap<>();
        Map<String, Float> revenueByYear = new LinkedHashMap<>();
        Map<String, Float> revenueByPlan = new LinkedHashMap<>();
        allPayments.stream()
                .filter(payment -> payment.getStatus() == PaymentTransactionStatus.SUCCESS)
                .forEach(payment -> {
                    OffsetDateTime paidAt = payment.getPaidAt() == null ? payment.getCreatedAt() : payment.getPaidAt();
                    String monthKey = paidAt.getYear() + "-" + String.format("%02d", paidAt.getMonthValue());
                    String quarterKey = paidAt.getYear() + "-Q" + ((paidAt.getMonthValue() - 1) / 3 + 1);
                    String yearKey = String.valueOf(paidAt.getYear());
                    String planKey = payment.getSubscriptionPlanId().toString();
                    float amount = payment.getAmount().floatValue();
                    revenueByMonth.merge(monthKey, amount, Float::sum);
                    revenueByQuarter.merge(quarterKey, amount, Float::sum);
                    revenueByYear.merge(yearKey, amount, Float::sum);
                    revenueByPlan.merge(planKey, amount, Float::sum);
                });
        long successfulPayments = allPayments.stream().filter(payment -> payment.getStatus() == PaymentTransactionStatus.SUCCESS).count();
        long failedPayments = allPayments.stream().filter(payment -> List.of(PaymentTransactionStatus.FAILED, PaymentTransactionStatus.EXPIRED, PaymentTransactionStatus.CANCELLED).contains(payment.getStatus())).count();
        long pendingManualPayments = allPayments.stream().filter(payment -> List.of(PaymentTransactionStatus.PENDING, PaymentTransactionStatus.PROCESSING, PaymentTransactionStatus.MANUAL_REVIEW).contains(payment.getStatus())).count();
        long totalPaymentDecisions = Math.max(1, successfulPayments + failedPayments);
        Map<String, Object> feedbackSummary = new LinkedHashMap<>();
        feedbackSummary.put("totalFeedback", businessFeedback.count());
        feedbackSummary.put("newFeedback", businessFeedback.findAllByOrderByCreatedAtDesc().stream().filter(item -> item.getStatus() == FeedbackStatus.NEW).count());
        Map<String, Object> aiUsageStatistics = new LinkedHashMap<>();
        aiUsageStatistics.put("totalSuggestions", aiSuggestions.findAll().size());
        aiUsageStatistics.put("generatedSuggestions", aiSuggestions.findAll().stream().filter(item -> item.getStatus() == AiSuggestionStatus.GENERATED).count());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalWorkspaces", allWorkspaces.size());
        payload.put("activeWorkspaces", allWorkspaces.stream().filter(workspace -> workspace.getStatus() == WorkspaceStatus.ACTIVE).count());
        payload.put("suspendedWorkspaces", allWorkspaces.stream().filter(workspace -> workspace.getStatus() == WorkspaceStatus.SUSPENDED).count());
        payload.put("expiredWorkspaces", allWorkspaces.stream().filter(workspace -> workspace.getStatus() == WorkspaceStatus.EXPIRED).count());
        payload.put("newWorkspacesThisMonth", allWorkspaces.stream().filter(workspace -> workspace.getCreatedAt() != null && !workspace.getCreatedAt().isBefore(monthStart)).count());
        payload.put("revenueByMonth", revenueByMonth);
        payload.put("revenueByQuarter", revenueByQuarter);
        payload.put("revenueByYear", revenueByYear);
        payload.put("revenueByPlan", revenueByPlan);
        payload.put("paymentSuccessRate", successfulPayments * 1.0 / totalPaymentDecisions);
        payload.put("failedPayments", failedPayments);
        payload.put("pendingManualPayments", pendingManualPayments);
        payload.put("businessFeedbackSummary", feedbackSummary);
        payload.put("aiUsageStatistics", aiUsageStatistics);
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
        return workspaceEmployees(currentUser().workspaceId()).stream()
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

    private boolean isTaskInPeriod(TaskEntity task, LocalDate start, LocalDate end) {
        return isDateInPeriod(task.getCreatedAt().toLocalDate(), start, end)
                || isDateInPeriod(task.getUpdatedAt().toLocalDate(), start, end)
                || (task.getCompletedAt() != null && isDateInPeriod(task.getCompletedAt().toLocalDate(), start, end))
                || isDateInPeriod(task.getDeadline().toLocalDate(), start, end)
                || (isOpenTask(task) && task.getCreatedAt().toLocalDate().isBefore(start));
    }

    private boolean isDateInPeriod(LocalDate value, LocalDate start, LocalDate end) {
        return !value.isBefore(start) && !value.isAfter(end);
    }

    private void applyTaskStatus(TaskEntity task, TaskStatus status, int progressPercent) {
        task.setStatus(status);
        task.setProgressPercent(status == TaskStatus.COMPLETED ? 100 : Math.max(0, Math.min(100, progressPercent)));
        task.setCompletedAt(status == TaskStatus.COMPLETED ? OffsetDateTime.now() : null);
        task.setUpdatedAt(OffsetDateTime.now());
    }

    private WorkloadView workloadForEmployee(UserEntity employee, List<TaskEntity> scopedTasks) {
        List<TaskEntity> assigned = scopedTasks.stream().filter(task -> taskParticipants(task).contains(employee.getId())).toList();
        long open = assigned.stream().filter(task -> !List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED).contains(task.getStatus())).count();
        long inProgress = assigned.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS).count();
        long blocked = assigned.stream().filter(task -> task.getStatus() == TaskStatus.BLOCKED).count();
        long completed = assigned.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        long overdue = assigned.stream().filter(this::isOverdue).count();
        BigDecimal estimated = assigned.stream()
                .filter(task -> !List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED).contains(task.getStatus()))
                .map(task -> allocatedHoursForParticipant(task, Math.max(1, taskParticipants(task).size())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
    private List<TaskStatus> openTaskStatuses() {
        return List.of(TaskStatus.ASSIGNED, TaskStatus.ACCEPTED, TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED, TaskStatus.SUBMITTED, TaskStatus.RETURNED);
    }

    private List<TaskEntity> visibleTasksForEmployee(AuthenticatedUser user) {
        Set<UUID> participantTaskIds = taskAssignees.findByWorkspaceIdAndEmployeeId(user.workspaceId(), user.userId()).stream()
                .map(TaskAssigneeEntity::getTaskId)
                .collect(java.util.stream.Collectors.toSet());
        return tasks.findByWorkspaceIdOrderByCreatedAtDesc(user.workspaceId()).stream()
                .filter(task -> user.userId().equals(task.getAssigneeId()) || participantTaskIds.contains(task.getId()))
                .toList();
    }

    private AssignmentPlan assignmentPlan(AssignmentType requestedType, UUID assigneeId, UUID teamLeaderId, List<UUID> teamMemberIds) {
        AssignmentType assignmentType = requestedType == null ? AssignmentType.INDIVIDUAL : requestedType;
        if (assignmentType == AssignmentType.INDIVIDUAL) {
            if (assigneeId == null) throw new IllegalArgumentException("Task cá nhân phải có assigneeId.");
            requireActiveEmployee(assigneeId);
            return new AssignmentPlan(AssignmentType.INDIVIDUAL, assigneeId, assigneeId, List.of(assigneeId));
        }
        if (teamLeaderId == null) throw new IllegalArgumentException("Task nhóm phải có teamLeaderId.");
        requireActiveEmployee(teamLeaderId);
        LinkedHashSet<UUID> participantIds = new LinkedHashSet<>();
        participantIds.add(teamLeaderId);
        if (teamMemberIds != null) {
            participantIds.addAll(teamMemberIds);
        }
        if (participantIds.isEmpty()) throw new IllegalArgumentException("Task nhóm phải có ít nhất một người tham gia.");
        participantIds.forEach(this::requireActiveEmployee);
        return new AssignmentPlan(AssignmentType.TEAM, teamLeaderId, teamLeaderId, new ArrayList<>(participantIds));
    }

    private void saveTaskParticipants(TaskEntity task, AssignmentPlan assignment) {
        taskAssignees.deleteByTaskId(task.getId());
        OffsetDateTime now = OffsetDateTime.now();
        for (UUID employeeId : assignment.participantIds()) {
            TaskAssigneeEntity item = new TaskAssigneeEntity();
            item.setWorkspaceId(task.getWorkspaceId());
            item.setTaskId(task.getId());
            item.setEmployeeId(employeeId);
            boolean isLeader = assignment.assignmentType() == AssignmentType.TEAM && employeeId.equals(assignment.teamLeaderId());
            item.setLeader(isLeader);
            item.setParticipantRole(assignment.assignmentType() == AssignmentType.INDIVIDUAL ? TaskParticipantRole.ASSIGNEE : isLeader ? TaskParticipantRole.LEADER : TaskParticipantRole.MEMBER);
            item.setAllocatedHours(allocatedHoursForParticipant(task, assignment.participantIds().size()));
            item.setCreatedAt(now);
            taskAssignees.save(item);
        }
    }

    private BigDecimal allocatedHoursForParticipant(TaskEntity task, int participantCount) {
        if (task.getEstimatedHours() == null || participantCount <= 0) return BigDecimal.ZERO;
        return task.getEstimatedHours().divide(BigDecimal.valueOf(participantCount), 2, java.math.RoundingMode.HALF_UP);
    }

    private void notifyParticipants(TaskEntity task, String type, String title, String message) {
        List<TaskAssigneeEntity> participants = taskAssignees.findByTaskIdOrderByCreatedAtAsc(task.getId());
        if (participants.isEmpty()) {
            createNotification(task.getWorkspaceId(), task.getAssigneeId(), type, title, message, "TASK", task.getId());
            return;
        }
        participants.forEach(participant -> createNotification(task.getWorkspaceId(), participant.getEmployeeId(), type, title, message, "TASK", task.getId()));
    }

    private void saveTaskAttachments(TaskEntity task, List<TaskAttachmentRequest> attachments) {
        if (attachments == null) return;
        attachments.forEach(attachment -> saveTaskAttachment(task, attachment));
    }

    private void replaceTaskAttachments(TaskEntity task, List<TaskAttachmentRequest> attachments) {
        if (attachments == null) return;
        taskAttachments.findByTaskIdOrderByCreatedAtAsc(task.getId()).forEach(taskAttachments::delete);
        saveTaskAttachments(task, attachments);
    }

    private TaskAttachmentEntity saveTaskAttachment(TaskEntity task, TaskAttachmentRequest request) {
        TaskAttachmentEntity item = new TaskAttachmentEntity();
        item.setWorkspaceId(task.getWorkspaceId());
        item.setTaskId(task.getId());
        item.setFileName(request.fileName());
        item.setFileUrl(request.fileUrl());
        item.setContentType(request.contentType());
        item.setFileSize(request.fileSize());
        item.setAttachmentType(request.attachmentType() == null ? AttachmentType.OTHER : request.attachmentType());
        item.setUploadedBy(currentUser().userId());
        item.setCreatedAt(OffsetDateTime.now());
        return taskAttachments.save(item);
    }

    private TaskUpdateEntity saveTaskUpdate(TaskEntity task, int progressPercent, String content, String attachment, UpdateType updateType) {
        TaskUpdateEntity update = new TaskUpdateEntity();
        update.setTaskId(task.getId());
        update.setUserId(currentUser().userId());
        update.setProgressPercent(progressPercent);
        update.setContent(content);
        update.setAttachment(attachment);
        update.setUpdateType(updateType);
        update.setCreatedAt(OffsetDateTime.now());
        return taskUpdates.save(update);
    }

    private BigDecimal validEstimatedHours(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("estimatedHours phải lớn hơn 0.");
        }
        return value;
    }

    private void applyEmployeeProfile(UserEntity employee, UUID departmentId, UUID jobPositionId, LocalDate dateOfBirth, String gender, String address, String personalSummary, EmploymentType employmentType, WorkingStatus workingStatus, EmployeeLevel employeeLevel, Integer monthlyWorkingCapacityHours, String mainExpertise, String secondaryExpertise) {
        if (jobPositionId != null) {
            JobPositionEntity position = requireActiveBusinessPosition(jobPositionId);
            if (departmentId != null && !departmentId.equals(position.getDepartmentId())) {
                throw new IllegalArgumentException("Phòng ban phải khớp với business position.");
            }
            employee.setDepartmentId(position.getDepartmentId());
            employee.setJobPositionId(position.getId());
        } else {
            if (departmentId != null) requireActiveDepartment(departmentId);
            employee.setDepartmentId(departmentId);
            employee.setJobPositionId(null);
        }
        employee.setDateOfBirth(dateOfBirth);
        employee.setGender(gender);
        employee.setAddress(address);
        employee.setPersonalSummary(personalSummary);
        employee.setEmploymentType(employmentType);
        employee.setWorkingStatus(workingStatus == null ? WorkingStatus.WORKING : workingStatus);
        employee.setEmployeeLevel(employeeLevel);
        employee.setMonthlyWorkingCapacityHours(monthlyWorkingCapacityHours == null ? 168 : monthlyWorkingCapacityHours);
        employee.setMainExpertise(mainExpertise);
        employee.setSecondaryExpertise(secondaryExpertise);
    }

    private void applyTaskRequirementContext(TaskEntity task, UUID departmentId, UUID requiredJobPositionId) {
        if (requiredJobPositionId != null) {
            JobPositionEntity position = requireActiveBusinessPosition(requiredJobPositionId);
            if (departmentId != null && !departmentId.equals(position.getDepartmentId())) {
                throw new IllegalArgumentException("Task department phải khớp với required business position.");
            }
            task.setDepartmentId(position.getDepartmentId());
            task.setRequiredJobPositionId(position.getId());
        } else {
            if (departmentId != null) requireActiveDepartment(departmentId);
            task.setDepartmentId(departmentId);
            task.setRequiredJobPositionId(null);
        }
    }

    private MonthlyWorkloadView monthlyWorkloadForEmployee(UserEntity employee, List<TaskEntity> scopedTasks, YearMonth month) {
        BigDecimal allocated = scopedTasks.stream()
                .filter(this::isOpenTask)
                .filter(task -> taskParticipants(task).contains(employee.getId()))
                .map(task -> allocatedHoursInMonth(task, month, taskParticipants(task).size()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal capacity = BigDecimal.valueOf(employee.getMonthlyWorkingCapacityHours() == null ? 168 : employee.getMonthlyWorkingCapacityHours());
        double ratio = capacity.signum() == 0 ? 0 : allocated.doubleValue() / capacity.doubleValue();
        String level = ratio < 0.25 ? "IDLE" : ratio < 0.60 ? "LIGHT" : ratio <= 1.0 ? "FULL" : "OVERLOADED";
        String label = switch (level) {
            case "IDLE" -> "Rảnh rỗi";
            case "LIGHT" -> "Thong thả";
            case "FULL" -> "Đủ việc";
            default -> "Quá tải";
        };
        return new MonthlyWorkloadView(employee.getId(), employee.getFullName(), month.getYear(), month.getMonthValue(), allocated, capacity, ratio, level, label);
    }

    private Set<UUID> taskParticipants(TaskEntity task) {
        List<TaskAssigneeEntity> participants = taskAssignees.findByTaskIdOrderByCreatedAtAsc(task.getId());
        if (participants.isEmpty()) return Set.of(task.getAssigneeId());
        return participants.stream().map(TaskAssigneeEntity::getEmployeeId).collect(java.util.stream.Collectors.toSet());
    }

    private boolean isTaskLeader(TaskEntity task, UUID userId) {
        return taskAssignees.findByTaskIdOrderByCreatedAtAsc(task.getId()).stream()
                .anyMatch(participant -> participant.isLeader() && participant.getEmployeeId().equals(userId));
    }

    private void requireCanUpdateTaskCustomerInfo(TaskEntity task) {
        AuthenticatedUser user = currentUser();
        if (isBusinessOwnerRole(user.role()) || isManagerOrExecutiveRole(user.role())) {
            return;
        }
        if (task.getAssignmentType() == AssignmentType.INDIVIDUAL && user.userId().equals(task.getAssigneeId())) {
            return;
        }
        if (task.getAssignmentType() == AssignmentType.TEAM && isTaskLeader(task, user.userId())) {
            return;
        }
        throw new IllegalArgumentException("Bạn không có quyền cập nhật thông tin khách hàng của task này.");
    }

    private BigDecimal allocatedHoursInMonth(TaskEntity task, YearMonth month, int participantCount) {
        if (task.getEstimatedHours() == null || participantCount <= 0) return BigDecimal.ZERO;
        LocalDate start = task.getStartDate() == null ? task.getCreatedAt().toLocalDate() : task.getStartDate().toLocalDate();
        LocalDate end = task.getDeadline().toLocalDate();
        if (end.isBefore(start)) end = start;
        List<LocalDate> workingDays = workingDays(start, end);
        long workingDaysInMonth = workingDays.stream().filter(day -> YearMonth.from(day).equals(month)).count();
        if (workingDaysInMonth == 0 || workingDays.isEmpty()) return BigDecimal.ZERO;
        BigDecimal totalForParticipant = allocatedHoursForParticipant(task, participantCount);
        return totalForParticipant.multiply(BigDecimal.valueOf(workingDaysInMonth))
                .divide(BigDecimal.valueOf(workingDays.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private List<LocalDate> workingDays(LocalDate start, LocalDate end) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek().getValue() <= 5) {
                days.add(current);
            }
            current = current.plusDays(1);
        }
        return days.isEmpty() ? List.of(start) : days;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private byte[] validatedQrImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("QR image file is required.");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new IllegalArgumentException("QR image must not exceed 5 MB.");
        }
        String contentType = normalizedImageContentType(file.getContentType());
        try {
            byte[] content = file.getBytes();
            if (!matchesImageSignature(contentType, content)) {
                throw new IllegalArgumentException("QR image content does not match its declared MIME type.");
            }
            var image = ImageIO.read(new ByteArrayInputStream(content));
            if (!"image/webp".equals(contentType) && image == null) {
                throw new IllegalArgumentException("QR image is corrupt or unsupported.");
            }
            if (image != null && (image.getWidth() < 128 || image.getHeight() < 128 || image.getWidth() > 4096 || image.getHeight() > 4096)) {
                throw new IllegalArgumentException("QR image dimensions must be between 128 and 4096 pixels.");
            }
            return content;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read QR image.", exception);
        }
    }

    private String normalizedImageContentType(String contentType) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(normalized)) normalized = "image/jpeg";
        if (!List.of("image/png", "image/jpeg", "image/webp").contains(normalized)) {
            throw new IllegalArgumentException("Only PNG, JPG, JPEG and WEBP QR images are accepted.");
        }
        return normalized;
    }

    private boolean matchesImageSignature(String contentType, byte[] content) {
        if (content.length < 12) return false;
        if ("image/png".equals(contentType)) {
            return (content[0] & 0xff) == 0x89 && content[1] == 0x50 && content[2] == 0x4e && content[3] == 0x47;
        }
        if ("image/jpeg".equals(contentType)) {
            return (content[0] & 0xff) == 0xff && (content[1] & 0xff) == 0xd8 && (content[content.length - 2] & 0xff) == 0xff && (content[content.length - 1] & 0xff) == 0xd9;
        }
        return content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P';
    }

    private String uniqueQrFileName(String contentType) {
        String extension = switch (normalizedImageContentType(contentType)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        return "bank-qr-" + UUID.randomUUID() + extension;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize payment payload.", exception);
        }
    }

    private PaymentQrSettingEntity requireEnabledPaymentQrSetting(PaymentMethod paymentMethod) {
        PaymentQrSettingEntity setting = paymentQrSettings.findByPaymentMethod(paymentMethod)
                .orElseThrow(() -> new IllegalArgumentException("Payment method is not configured."));
        if (!setting.isEnabled()) {
            throw new IllegalArgumentException("Payment method is disabled.");
        }
        if (paymentMethod == PaymentMethod.BANK_TRANSFER
                && (!hasText(setting.getBankCode()) || !hasText(setting.getBankAccountNumber()) || !hasText(setting.getBankAccountName()))) {
            throw new IllegalArgumentException("Thông tin tài khoản ngân hàng chưa đầy đủ. Vui lòng đợi quản trị viên cập nhật.");
        }
        return setting;
    }

    private String transferContent(PaymentQrSettingEntity setting, WorkspaceRegistrationEntity registration, PaymentTransactionEntity payment) {
        String prefix = hasText(setting.getTransferContentPrefix()) ? setting.getTransferContentPrefix().trim() : "FOREP";
        return prefix + " " + registration.getWorkspaceIdentifier() + " " + payment.getOrderCode();
    }

    private String paymentConfigurationSnapshot(PaymentQrSettingEntity setting) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("configurationId", setting.getId());
        snapshot.put("paymentMethod", setting.getPaymentMethod());
        snapshot.put("bankCode", setting.getBankCode());
        snapshot.put("bankName", setting.getBankName());
        snapshot.put("accountNumber", setting.getBankAccountNumber());
        snapshot.put("accountName", setting.getBankAccountName());
        snapshot.put("transferContentPrefix", setting.getTransferContentPrefix());
        snapshot.put("qrFileId", setting.getQrFileId());
        snapshot.put("configuredAt", setting.getUpdatedAt());
        return toJson(snapshot);
    }

    private ProviderPaymentResult withUploadedBankQr(ProviderPaymentResult providerResult, UUID qrFileId) {
        return new ProviderPaymentResult(
                providerResult.paymentUrl(),
                providerResult.deeplink(),
                "/api/public/payment-files/" + qrFileId,
                providerResult.bankCode(),
                providerResult.bankName(),
                providerResult.bankAccountNumber(),
                providerResult.bankAccountName(),
                providerResult.rawRequest(),
                providerResult.rawResponse()
        );
    }

    @Scheduled(fixedDelayString = "${forep.payments.expiration-scan-delay-ms:60000}", initialDelayString = "${forep.payments.expiration-initial-delay-ms:60000}")
    public void expireStalePaymentAndRegistrationFlows() {
        OffsetDateTime now = OffsetDateTime.now();
        List<PaymentTransactionEntity> expiredPayments = paymentTransactions.findByStatusInAndExpiredAtBefore(
                List.of(PaymentTransactionStatus.PENDING, PaymentTransactionStatus.PROCESSING),
                now
        );
        expiredPayments.forEach(payment -> {
            payment.setStatus(PaymentTransactionStatus.EXPIRED);
            payment.setUpdatedAt(now);
        });
        if (!expiredPayments.isEmpty()) {
            paymentTransactions.saveAll(expiredPayments);
        }

        List<WorkspaceRegistrationEntity> expiredRegistrations = workspaceRegistrations.findByRegistrationStatusInAndExpiredAtBefore(
                List.of(
                        RegistrationStatus.SUBMITTED,
                        RegistrationStatus.DRAFT,
                        RegistrationStatus.PAYMENT_PENDING,
                        RegistrationStatus.PAYMENT_SUBMITTED,
                        RegistrationStatus.PENDING_PLAN_SELECTION,
                        RegistrationStatus.PENDING_PAYMENT
                ),
                now
        ).stream()
                .filter(registration -> registration.getWorkspaceId() == null)
                .toList();
        expiredRegistrations.forEach(registration -> {
            registration.setRegistrationStatus(RegistrationStatus.EXPIRED);
            registration.setUpdatedAt(now);
        });
        if (!expiredRegistrations.isEmpty()) {
            workspaceRegistrations.saveAll(expiredRegistrations);
        }
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
        workspaceEmployees(workspaceId).forEach(employee -> {
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
            saveAiHistory(type.name(), AiHistoryStatus.SUCCESS);
        } catch (Exception exception) {
            throw new IllegalStateException("Không lưu được AI suggestion.", exception);
        }
    }

    private void audit(UUID workspaceId, String action, String entityType, UUID entityId, Object oldValue, Object newValue) {
        try {
            AuditLogEntity logItem = new AuditLogEntity();
            AuthenticatedUser actor = null;
            try { actor = currentUser(); } catch (Exception ignored) { }
            UserEntity actorEntity = actor == null ? null : users.findById(actor.userId()).orElse(null);
            logItem.setWorkspaceId(workspaceId == null && actor != null ? actor.workspaceId() : workspaceId);
            logItem.setActorId(actor == null ? null : actor.userId());
            logItem.setActorNameSnapshot(actorEntity == null ? "SYSTEM" : actorEntity.getFullName());
            logItem.setActorRoleSnapshot(actor == null ? "SYSTEM" : actor.role().name());
            logItem.setAction(action);
            logItem.setEntityType(entityType);
            logItem.setEntityId(entityId);
            logItem.setResult("SUCCESS");
            logItem.setOldValue(oldValue == null ? null : objectMapper.writeValueAsString(oldValue));
            logItem.setNewValue(newValue == null ? null : objectMapper.writeValueAsString(newValue));
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                var request = attributes.getRequest();
                logItem.setIpAddress(clientIp(request.getHeader("X-Forwarded-For"), request.getRemoteAddr()));
                logItem.setUserAgent(request.getHeader("User-Agent"));
                Object requestId = request.getAttribute("requestId");
                logItem.setRequestId(requestId == null ? null : requestId.toString());
            }
            logItem.setCreatedAt(OffsetDateTime.now());
            auditLogs.save(logItem);
        } catch (Exception exception) {
            log.warn("Could not write audit log action={} entityType={} entityId={}", action, entityType, entityId, exception);
        }
    }

    private void enforceAiUsageLimit() {
        if (isAiUsageLimitReached()) {
            throw new IllegalArgumentException("Workspace đã sử dụng hết quota AI của gói subscription hiện tại.");
        }
    }

    private boolean isAiUsageLimitReached() {
        WorkspaceEntity workspace = requireWorkspace(currentUser().workspaceId());
        if (workspace.getSubscriptionPlanId() == null) {
            return false;
        }
        SubscriptionPlanEntity plan = requireSubscriptionPlan(workspace.getSubscriptionPlanId());
        if (plan.getAiUsageLimit() == null || plan.getAiUsageLimit() <= 0) {
            return false;
        }
        OffsetDateTime periodStart = workspace.getActivatedAt() == null ? workspace.getCreatedAt() : workspace.getActivatedAt();
        long used = aiSuggestions.findByWorkspaceIdOrderByCreatedAtDesc(workspace.getId()).stream()
                .filter(suggestion -> periodStart == null || !suggestion.getCreatedAt().isBefore(periodStart))
                .filter(suggestion -> workspace.getExpiresAt() == null || suggestion.getCreatedAt().isBefore(workspace.getExpiresAt()))
                .count();
        return used >= plan.getAiUsageLimit();
    }

    private PaymentTransactionView confirmPayment(UUID paymentId, boolean adminOverride, String rawPayloadOrNote) {
        PaymentTransactionEntity payment = paymentTransactions.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found."));
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
        WorkspaceRegistrationEntity registration = workspaceRegistrations.findByIdForUpdate(payment.getWorkspaceRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace registration not found."));
        SubscriptionPlanEntity plan = requireSubscriptionPlan(payment.getSubscriptionPlanId());
        if (payment.getAmount().compareTo(plan.getPrice()) != 0) {
            throw new IllegalArgumentException("Payment amount does not match the selected subscription plan.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        payment.setStatus(PaymentTransactionStatus.SUCCESS);
        payment.setPaidAt(now);
        payment.setConfirmedAt(now);
        payment.setConfirmedBy(adminOverride ? safeCurrentUserId() : null);
        payment.setFailureReason(null);
        payment.setUpdatedAt(now);
        if (hasText(rawPayloadOrNote)) {
            payment.setRawProviderResponse(rawPayloadOrNote);
        }
        payment = paymentTransactions.save(payment);

        registration.setPaymentStatus(PaymentStatus.CONFIRMED);
        registration.setRegistrationStatus(RegistrationStatus.PAYMENT_CONFIRMED);
        registration.setUpdatedAt(now);
        workspaceRegistrations.save(registration);

        activateWorkspaceForRegistration(registration, adminOverride ? rawPayloadOrNote : null, payment.getId());
        return toPaymentTransactionView(payment);
    }

    private PaymentTransactionView failPayment(UUID paymentId, String rawPayloadOrNote) {
        PaymentTransactionEntity payment = paymentTransactions.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found."));
        if (payment.getStatus() == PaymentTransactionStatus.SUCCESS) {
            throw new IllegalArgumentException("Successful payment transactions cannot be rejected.");
        }
        payment.setStatus(PaymentTransactionStatus.FAILED);
        payment.setFailureReason(hasText(rawPayloadOrNote) ? rawPayloadOrNote : "Payment provider rejected the transaction.");
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

    private List<GeneratedOwnerAccountView> activateWorkspaceForRegistration(WorkspaceRegistrationEntity registration, String reviewNote, UUID paymentTransactionId) {
        registration = workspaceRegistrations.findByIdForUpdate(registration.getId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace registration not found."));
        if (registration.getWorkspaceId() != null) {
            return List.of();
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
        workspace.setOrganizationAbbreviation(registration.getWorkspaceIdentifier());
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

        createWorkspaceSubscription(workspace, plan, now, paymentTransactionId);

        List<GeneratedOwnerAccountView> owners = provisionOwnerAccounts(workspace, plan, now);
        workspace.setOwnerAccountProvisionedAt(now);
        workspace.setOwnerAccountCount(owners.size());
        if (!owners.isEmpty()) {
            workspace.setOwnerId(owners.getFirst().userId());
        }
        workspace = workspaces.save(workspace);
        registration.setWorkspaceId(workspace.getId());
        registration.setPaymentStatus(PaymentStatus.CONFIRMED);
        registration.setRegistrationStatus(RegistrationStatus.ACTIVATED);
        registration.setReviewedBy(safeCurrentUserId());
        registration.setReviewedAt(now);
        registration.setReviewNote(reviewNote);
        registration.setUpdatedAt(now);
        workspaceRegistrations.save(registration);
        audit(workspace.getId(), "ACTIVATE_WORKSPACE_AFTER_PAYMENT", "WORKSPACE_REGISTRATION", registration.getId(), null, toWorkspaceRegistrationView(registration));
        return owners;
    }

    private WorkspaceSubscriptionEntity createWorkspaceSubscription(WorkspaceEntity workspace, SubscriptionPlanEntity plan, OffsetDateTime startDate, UUID paymentTransactionId) {
        if (paymentTransactionId != null && workspaceSubscriptions.existsByPaymentTransactionId(paymentTransactionId)) {
            return workspaceSubscriptions.findFirstByWorkspaceIdAndStatusOrderByCreatedAtDesc(workspace.getId(), WorkspaceSubscriptionStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Workspace subscription already exists for payment but active subscription was not found."));
        }
        WorkspaceSubscriptionEntity subscription = new WorkspaceSubscriptionEntity();
        subscription.setWorkspaceId(workspace.getId());
        subscription.setSubscriptionPlanId(plan.getId());
        subscription.setStatus(WorkspaceSubscriptionStatus.ACTIVE);
        subscription.setStartDate(startDate);
        subscription.setEndDate(workspace.getExpiresAt() == null ? startDate.plusMonths(plan.getDurationInMonths()) : workspace.getExpiresAt());
        subscription.setRenewalDate(subscription.getEndDate());
        subscription.setPrice(plan.getPrice());
        subscription.setMaxOwnerAccounts(plan.getMaxOwnerAccounts());
        subscription.setMaxEmployeeAccounts(plan.getMaxEmployeeAccounts());
        subscription.setPaymentTransactionId(paymentTransactionId);
        subscription.setCreatedAt(startDate);
        subscription.setUpdatedAt(startDate);
        return workspaceSubscriptions.save(subscription);
    }

    private WorkspaceSubscriptionEntity ensureActiveWorkspaceSubscription(WorkspaceEntity workspace, SubscriptionPlanEntity plan, OffsetDateTime startDate) {
        WorkspaceSubscriptionEntity active = workspaceSubscriptions.findFirstByWorkspaceIdAndStatusOrderByCreatedAtDesc(workspace.getId(), WorkspaceSubscriptionStatus.ACTIVE)
                .orElse(null);
        if (active == null) {
            return createWorkspaceSubscription(workspace, plan, startDate, null);
        }
        if (!active.getSubscriptionPlanId().equals(plan.getId())) {
            return replaceActiveWorkspaceSubscription(workspace, plan, startDate);
        }
        return active;
    }

    private WorkspaceSubscriptionEntity replaceActiveWorkspaceSubscription(WorkspaceEntity workspace, SubscriptionPlanEntity plan, OffsetDateTime startDate) {
        workspaceSubscriptions.findFirstByWorkspaceIdAndStatusOrderByCreatedAtDesc(workspace.getId(), WorkspaceSubscriptionStatus.ACTIVE)
                .ifPresent(active -> {
                    active.setStatus(subscriptionTransitionStatus(active, plan));
                    active.setEndDate(startDate);
                    active.setRenewalDate(startDate);
                    active.setUpdatedAt(startDate);
                    workspaceSubscriptions.saveAndFlush(active);
                });
        return createWorkspaceSubscription(workspace, plan, startDate, null);
    }

    private WorkspaceSubscriptionStatus subscriptionTransitionStatus(WorkspaceSubscriptionEntity current, SubscriptionPlanEntity nextPlan) {
        int priceComparison = nextPlan.getPrice().compareTo(current.getPrice());
        if (priceComparison > 0 || nextPlan.getMaxEmployeeAccounts() > current.getMaxEmployeeAccounts() || nextPlan.getMaxOwnerAccounts() > current.getMaxOwnerAccounts()) {
            return WorkspaceSubscriptionStatus.UPGRADED;
        }
        if (priceComparison < 0 || nextPlan.getMaxEmployeeAccounts() < current.getMaxEmployeeAccounts() || nextPlan.getMaxOwnerAccounts() < current.getMaxOwnerAccounts()) {
            return WorkspaceSubscriptionStatus.DOWNGRADED;
        }
        return WorkspaceSubscriptionStatus.CANCELLED;
    }

    public List<GeneratedOwnerAccountView> provisionOwnerAccounts(UUID workspaceId) {
        requireSystemAdmin();
        WorkspaceEntity workspace = requireWorkspace(workspaceId);
        SubscriptionPlanEntity plan = requireSubscriptionPlan(workspace.getSubscriptionPlanId());
        OffsetDateTime now = OffsetDateTime.now();
        List<GeneratedOwnerAccountView> generated = provisionOwnerAccounts(workspace, plan, now);
        workspace.setOwnerAccountProvisionedAt(now);
        workspace.setOwnerAccountCount(ownerAccounts(workspace.getId()).size());
        if (!generated.isEmpty() && workspace.getOwnerId() == null) {
            workspace.setOwnerId(generated.getFirst().userId());
        }
        workspaces.save(workspace);
        return generated;
    }

    private List<GeneratedOwnerAccountView> provisionOwnerAccounts(WorkspaceEntity workspace, SubscriptionPlanEntity plan, OffsetDateTime now) {
        List<GeneratedOwnerAccountView> generated = new ArrayList<>();
        List<UserEntity> existingOwners = new ArrayList<>(ownerAccounts(workspace.getId()));
        int baseOwnerCount = existingOwners.size();
        int missingOwnerAccounts = Math.max(0, plan.getMaxOwnerAccounts() - existingOwners.size());
        for (int index = 1; index <= missingOwnerAccounts; index++) {
            String username = nextOwnerUsername(workspace);
            String password = defaultOwnerPassword();
            UserEntity owner = new UserEntity();
            owner.setWorkspaceId(workspace.getId());
            owner.setFullName((hasText(workspace.getBusinessName()) ? workspace.getBusinessName() : workspace.getName()) + " Owner " + (baseOwnerCount + index));
            owner.setEmail(username + "@workspace.local");
            owner.setPhone(workspace.getContactPhone());
            owner.setUsername(username);
            owner.setPasswordHash(passwordEncoder.encode(password));
            owner.setMustChangePassword(true);
            owner.setInitialAccountGenerated(true);
            owner.setRole(Role.BUSINESS_OWNER);
            owner.setStatus(UserStatus.ACTIVE);
            owner.setCreatedAt(now);
            owner.setUpdatedAt(now);
            owner = users.save(owner);
            existingOwners.add(owner);
            generated.add(new GeneratedOwnerAccountView(owner.getId(), owner.getUsername(), password, owner.getFullName()));
        }
        return generated;
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

    private WorkspaceRegistrationEntity requireWorkspaceRegistrationWithToken(UUID registrationId, String token) {
        if (!hasText(token)) {
            throw new IllegalArgumentException("Registration token is required.");
        }
        WorkspaceRegistrationEntity registration = requireWorkspaceRegistration(registrationId);
        if (!token.equals(registration.getRegistrationToken())) {
            throw new IllegalArgumentException("Registration token is invalid.");
        }
        if (registration.getExpiredAt() != null && registration.getExpiredAt().isBefore(OffsetDateTime.now())
                && registration.getRegistrationStatus() != RegistrationStatus.APPROVED) {
            registration.setRegistrationStatus(RegistrationStatus.EXPIRED);
            registration.setUpdatedAt(OffsetDateTime.now());
            workspaceRegistrations.save(registration);
            throw new IllegalArgumentException("Workspace registration has expired.");
        }
        return registration;
    }

    private PaymentTransactionEntity requirePayment(UUID paymentId) {
        return paymentTransactions.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("Payment transaction not found."));
    }

    private PaymentTransactionEntity reusablePendingPayment(UUID registrationId, OffsetDateTime now) {
        return paymentTransactions.findByWorkspaceRegistrationIdOrderByCreatedAtDesc(registrationId).stream()
                .filter(payment -> payment.getStatus() == PaymentTransactionStatus.PENDING || payment.getStatus() == PaymentTransactionStatus.PROCESSING)
                .filter(payment -> payment.getExpiredAt() == null || payment.getExpiredAt().isAfter(now))
                .findFirst()
                .orElse(null);
    }

    private void expireStalePaymentsForRegistration(UUID registrationId, OffsetDateTime now) {
        List<PaymentTransactionEntity> expiredPayments = paymentTransactions.findByWorkspaceRegistrationIdOrderByCreatedAtDesc(registrationId).stream()
                .filter(payment -> payment.getStatus() == PaymentTransactionStatus.PENDING || payment.getStatus() == PaymentTransactionStatus.PROCESSING)
                .filter(payment -> payment.getExpiredAt() != null && !payment.getExpiredAt().isAfter(now))
                .toList();
        expiredPayments.forEach(payment -> {
            payment.setStatus(PaymentTransactionStatus.EXPIRED);
            payment.setUpdatedAt(now);
        });
        if (!expiredPayments.isEmpty()) {
            paymentTransactions.saveAll(expiredPayments);
        }
    }

    private PaymentTransactionEntity refreshExpiredPayment(PaymentTransactionEntity payment, OffsetDateTime now) {
        if ((payment.getStatus() == PaymentTransactionStatus.PENDING || payment.getStatus() == PaymentTransactionStatus.PROCESSING)
                && payment.getExpiredAt() != null
                && !payment.getExpiredAt().isAfter(now)) {
            payment.setStatus(PaymentTransactionStatus.EXPIRED);
            payment.setUpdatedAt(now);
            return paymentTransactions.save(payment);
        }
        return payment;
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

    private void assertCallbackAmountMatches(PaymentTransactionEntity payment, PaymentCallbackRequest request) {
        if (request.amount() == null) {
            throw new IllegalArgumentException("Callback amount is required.");
        }
        if (payment.getAmount().compareTo(request.amount()) != 0) {
            throw new IllegalArgumentException("Callback amount does not match the payment transaction.");
        }
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

    private String uniqueRegistrationToken() {
        String value;
        do {
            byte[] bytes = new byte[36];
            SECURE_RANDOM.nextBytes(bytes);
            value = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (workspaceRegistrations.findByRegistrationToken(value).isPresent());
        return value;
    }

    private String uniquePaymentCode() {
        String value;
        do {
            value = "PAY-" + Long.toString(Math.abs(SECURE_RANDOM.nextLong()), 36).toUpperCase(Locale.ROOT);
        } while (paymentTransactions.findByPaymentCode(value).isPresent());
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
    private void requireOwner() { authorizationService.require(Permission.WORKSPACE_UPDATE); }
    private void requireOwnerOrHr() { authorizationService.requireAny(Permission.EMPLOYEE_VIEW, Permission.DEPARTMENT_VIEW, Permission.POSITION_VIEW); }
    private void requireHr() { authorizationService.requireAny(Permission.EMPLOYEE_CREATE, Permission.DEPARTMENT_MANAGE, Permission.POSITION_MANAGE); }
    private void requireOwnerOrHrOrManager() { authorizationService.requireAny(Permission.EMPLOYEE_VIEW, Permission.AI_ANALYZE, Permission.TASK_ASSIGN); }
    private void requireTaskManager() { authorizationService.requireAny(Permission.TASK_ASSIGN, Permission.TASK_APPROVE, Permission.AI_RECOMMENDATION); }
    private void requireSystemAdmin() { authorizationService.require(Permission.SYSTEM_CONFIGURATION); }
    private boolean isPlatformAdminRole(Role role) { return role == Role.PLATFORM_ADMIN || role == Role.SYSTEM_ADMIN || role == Role.SYSTEM; }
    private boolean isBusinessOwnerRole(Role role) { return role == Role.BUSINESS_OWNER || role == Role.OWNER; }
    private boolean isManagerOrExecutiveRole(Role role) { return role == Role.MANAGER || role == Role.EXECUTIVE; }
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
    private String defaultOwnerPassword() {
        return secureTemporaryPassword();
    }
    private List<Role> workforceRoles() {
        return List.of(Role.EMPLOYEE, Role.MANAGER, Role.EXECUTIVE);
    }
    private List<UserEntity> workspaceEmployees(UUID workspaceId) {
        return users.findByWorkspaceIdAndRoleInOrderByFullNameAsc(workspaceId, workforceRoles());
    }
    private boolean isWorkforceRole(Role role) {
        return workforceRoles().contains(role);
    }
    private Role roleForBusinessPosition(UUID jobPositionId) {
        if (jobPositionId == null) return Role.EMPLOYEE;
        JobPositionEntity position = requireActiveBusinessPosition(jobPositionId);
        return switch (position.getPermissionGroup()) {
            case MANAGER -> Role.MANAGER;
            case EXECUTIVE -> Role.EXECUTIVE;
            case EMPLOYEE -> Role.EMPLOYEE;
        };
    }
    private UserEntity requireEmployee(UUID employeeId) {
        UserEntity user = users.findById(employeeId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));
        if (!user.getWorkspaceId().equals(currentUser().workspaceId()) || !isWorkforceRole(user.getRole())) throw new IllegalArgumentException("Không tìm thấy nhân viên trong workspace.");
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
    private JobPositionEntity requireJobPosition(UUID id) {
        JobPositionEntity item = jobPositions.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vị trí công việc."));
        if (!item.getWorkspaceId().equals(currentUser().workspaceId())) throw new IllegalArgumentException("Vị trí công việc không thuộc workspace hiện tại.");
        return item;
    }
    private DepartmentEntity requireDepartment(UUID id) {
        DepartmentEntity department = departments.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng ban."));
        if (!department.getWorkspaceId().equals(currentUser().workspaceId())) throw new IllegalArgumentException("Phòng ban không thuộc workspace hiện tại.");
        return department;
    }
    private JobPositionEntity requireActiveBusinessPosition(UUID id) {
        JobPositionEntity position = requireJobPosition(id);
        if (position.getStatus() != JobPositionStatus.ACTIVE) throw new IllegalArgumentException("Business position không hoạt động.");
        return position;
    }
    private DepartmentEntity requireActiveDepartment(UUID id) {
        DepartmentEntity department = requireDepartment(id);
        if (department.getStatus() != DepartmentStatus.ACTIVE) throw new IllegalArgumentException("Phòng ban không hoạt động.");
        return department;
    }
    private DailyReportEntity requireReport(UUID reportId) {
        DailyReportEntity report = reports.findById(reportId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy báo cáo."));
        if (!report.getWorkspaceId().equals(currentUser().workspaceId())) throw new IllegalArgumentException("Báo cáo không thuộc workspace hiện tại.");
        return report;
    }
    private void requireTaskVisible(TaskEntity task) {
        if (!isBusinessOwnerRole(currentUser().role()) && !isManagerOrExecutiveRole(currentUser().role()) && currentUser().role() != Role.HR && !taskParticipants(task).contains(currentUser().userId())) throw new IllegalArgumentException("Bạn không có quyền xem task này.");
    }
    private void requireOwnerOrAssignee(TaskEntity task) {
        if (!isBusinessOwnerRole(currentUser().role()) && !isManagerOrExecutiveRole(currentUser().role()) && !taskParticipants(task).contains(currentUser().userId())) throw new IllegalArgumentException("Bạn không có quyền cập nhật task này.");
    }
    private void requireTaskParticipant(TaskEntity task) {
        if (!taskParticipants(task).contains(currentUser().userId())) throw new IllegalArgumentException("Chỉ nhân viên được giao task mới được thực hiện thao tác này.");
    }
    private void requireCompletionSubmitter(TaskEntity task) {
        if (task.getAssignmentType() == AssignmentType.INDIVIDUAL && currentUser().userId().equals(task.getAssigneeId())) return;
        boolean isTeamLeader = taskAssignees.findByTaskIdOrderByCreatedAtAsc(task.getId()).stream()
                .anyMatch(item -> currentUser().userId().equals(item.getEmployeeId()) && (item.isLeader() || item.getParticipantRole() == TaskParticipantRole.LEADER));
        if (!isTeamLeader) throw new IllegalArgumentException("Chỉ assignee hoặc team leader được submit completion.");
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

    private String workspaceOwnerAbbreviation(WorkspaceEntity workspace) {
        if (hasText(workspace.getOrganizationAbbreviation())) {
            return normalizeOwnerAbbreviation(workspace.getOrganizationAbbreviation());
        }
        if (hasText(workspace.getShortCode())) {
            return normalizeOwnerAbbreviation(workspace.getShortCode());
        }
        return normalizeOwnerAbbreviation(nextAvailableShortCode(workspace.getName()));
    }

    private String normalizeOwnerAbbreviation(String value) {
        String normalized = normalizeAccountText(value).replaceAll("[^a-z0-9]", "").toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "XX";
        }
        if (normalized.length() == 1) {
            return (normalized + normalized).substring(0, 2);
        }
        return normalized.substring(0, 2);
    }

    private String nextOwnerUsername(WorkspaceEntity workspace) {
        String abbreviation = workspaceOwnerAbbreviation(workspace);
        int candidateSequence = 1;
        while (candidateSequence <= 702) {
            String username = abbreviation + "0000" + ownerAccountSuffix(candidateSequence);
            if (!users.existsByUsernameIgnoreCase(username)) {
                return username;
            }
            candidateSequence++;
        }
        throw new IllegalArgumentException("Không thể tạo username Business Owner duy nhất.");
    }

    private String nextHrUsername(WorkspaceEntity workspace) {
        String prefix = "hr" + workspaceOwnerAbbreviation(workspace).toLowerCase(Locale.ROOT);
        for (int sequence = 1; sequence <= 9999; sequence++) {
            String username = prefix + String.format("%04d", sequence);
            if (!users.existsByUsernameIgnoreCase(username)) return username;
        }
        throw new IllegalArgumentException("Could not allocate a unique HR username.");
    }

    private String secureTemporaryPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
        StringBuilder value = new StringBuilder(16);
        for (int index = 0; index < 16; index++) {
            value.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
        }
        return value.toString();
    }

    private String ownerAccountSuffix(int sequence) {
        int number = sequence;
        StringBuilder suffix = new StringBuilder();
        while (number > 0) {
            number--;
            suffix.insert(0, (char) ('A' + (number % 26)));
            number /= 26;
        }
        return suffix.toString();
    }

    private List<UserEntity> ownerAccounts(UUID workspaceId) {
        return users.findByWorkspaceIdAndRoleInOrderByFullNameAsc(workspaceId, List.of(Role.BUSINESS_OWNER, Role.OWNER));
    }

    private void applyDepartmentRequest(DepartmentEntity department, DepartmentRequest request) {
        UUID workspaceId = currentUser().workspaceId();
        String normalizedName = request.name().trim();
        String normalizedCode = hasText(request.code()) ? request.code().trim().toUpperCase(Locale.ROOT) : null;
        departments.findByWorkspaceIdAndNameIgnoreCase(workspaceId, normalizedName)
                .filter(existing -> !existing.getId().equals(department.getId()))
                .ifPresent(existing -> { throw new IllegalArgumentException("Tên phòng ban đã tồn tại trong workspace."); });
        if (hasText(normalizedCode)) {
            departments.findByWorkspaceIdAndCodeIgnoreCase(workspaceId, normalizedCode)
                    .filter(existing -> !existing.getId().equals(department.getId()))
                    .ifPresent(existing -> { throw new IllegalArgumentException("Mã phòng ban đã tồn tại trong workspace."); });
        }
        department.setWorkspaceId(workspaceId);
        department.setName(normalizedName);
        department.setCode(normalizedCode);
        department.setDescription(request.description());
        department.setStatus(request.status() == null ? DepartmentStatus.ACTIVE : request.status());
    }

    private JobPositionEntity legacyBusinessPosition(JobPositionEntity item, String title, PermissionGroup permissionGroup, UUID departmentId, String departmentName, String description, String requiredSkills, JobPositionStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        DepartmentEntity department = requireActiveDepartment(departmentId);
        if (item == null) {
            item = new JobPositionEntity();
            item.setWorkspaceId(currentUser().workspaceId());
            item.setCreatedAt(now);
        }
        String normalizedTitle = title.trim();
        UUID workspaceId = currentUser().workspaceId();
        if (jobPositions.existsByWorkspaceIdAndNameIgnoreCaseAndDepartmentId(workspaceId, normalizedTitle, departmentId) && !normalizedTitle.equalsIgnoreCase(item.getTitle())) {
            throw new IllegalArgumentException("Position name already exists in this workspace and department.");
        }
        item.setTitle(normalizedTitle);
        item.setPermissionGroup(permissionGroup);
        item.setDepartmentId(department.getId());
        item.setDepartmentName(department.getName());
        item.setDescription(description);
        item.setRequiredSkills(requiredSkills);
        if (status != null) {
            item.setStatus(status);
        } else if (item.getStatus() == null) {
            item.setStatus(JobPositionStatus.ACTIVE);
        }
        item.setUpdatedAt(now);
        return item;
    }

    private void applyBusinessPositionRequest(JobPositionEntity entity, BusinessPositionRequest request) {
        UUID workspaceId = currentUser().workspaceId();
        DepartmentEntity department = requireActiveDepartment(request.departmentId());
        String normalizedName = request.name().trim();
        String normalizedCode = hasText(request.code()) ? request.code().trim().toUpperCase(Locale.ROOT) : null;
        if (jobPositions.existsByWorkspaceIdAndNameIgnoreCaseAndDepartmentId(workspaceId, normalizedName, request.departmentId()) && !normalizedName.equalsIgnoreCase(entity.getTitle())) {
            throw new IllegalArgumentException("Tên vị trí đã tồn tại trong workspace và phòng ban.");
        }
        if (hasText(normalizedCode) && jobPositions.existsByWorkspaceIdAndCodeIgnoreCase(workspaceId, normalizedCode) && !normalizedCode.equalsIgnoreCase(entity.getCode())) {
            throw new IllegalArgumentException("Mã vị trí đã tồn tại trong workspace.");
        }
        entity.setWorkspaceId(workspaceId);
        entity.setTitle(normalizedName);
        entity.setCode(normalizedCode);
        entity.setPermissionGroup(request.permissionGroup());
        entity.setDepartmentId(department.getId());
        entity.setDepartmentName(department.getName());
        entity.setDescription(request.description());
        entity.setStatus(request.status() == null ? JobPositionStatus.ACTIVE : request.status());
    }

    private void saveAiHistory(String functionName, AiHistoryStatus status) {
        AiHistoryEntity history = new AiHistoryEntity();
        history.setWorkspaceId(currentUser().workspaceId());
        history.setCallerId(currentUser().userId());
        history.setCallerName(currentUserEntity().getFullName());
        history.setCallerRole(currentUser().role().name());
        history.setFunctionName(functionName);
        history.setStatus(status);
        history.setCalledAt(OffsetDateTime.now());
        history.setCreatedAt(OffsetDateTime.now());
        aiHistory.save(history);
    }

    private Map<String, Object> invokeAiMap(String functionName, AiSuggestionType suggestionType, Object inputData, Supplier<Map<String, Object>> call) {
        enforceAiUsageLimit();
        try {
            Map<String, Object> output = call.get();
            saveAiSuggestion(suggestionType, inputData, output);
            saveAiHistory(functionName, AiHistoryStatus.SUCCESS);
            return output;
        } catch (AiProviderException exception) {
            saveAiHistory(functionName, AiHistoryStatus.FAILED);
            throw exception;
        }
    }

    private String clientIp(String forwardedFor, String remoteAddress) {
        if (!hasText(forwardedFor)) return remoteAddress;
        return forwardedFor.split(",", 2)[0].trim();
    }

    private Map<String, Object> invokeAiMapWithFallback(String functionName,
                                                        AiSuggestionType suggestionType,
                                                        Object inputData,
                                                        Supplier<Map<String, Object>> call,
                                                        Function<AiProviderException, Map<String, Object>> fallback) {
        AiProviderException failure = null;
        Map<String, Object> output;
        if (isAiUsageLimitReached()) {
            failure = new AiProviderException("AI usage limit reached.");
            output = fallback.apply(failure);
        } else {
            try {
                output = call.get();
            } catch (AiProviderException exception) {
                failure = exception;
                log.warn("AI function {} failed; using backend fallback. message={}", functionName, fallbackReason(exception));
                output = fallback.apply(exception);
            }
        }
        saveAiSuggestion(suggestionType, inputData, output);
        saveAiHistory(functionName, failure == null ? AiHistoryStatus.SUCCESS : AiHistoryStatus.FALLBACK);
        return output;
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
    private UserView toUserView(UserEntity item) { return new UserView(item.getId(), item.getWorkspaceId(), item.getFullName(), item.getEmail(), item.getPhone(), item.getUsername(), item.getEmployeeCode(), item.getRole(), authorizationService.permissionNamesFor(item.getRole()), item.getAvatar(), item.getAvatarFileId(), item.getStatus(), item.getJobTitle(), item.getSeniorityLevel(), item.getSkillRating(), item.getYearsOfExperience(), item.getSkills(), item.getDepartmentId(), item.getJobPositionId(), item.getDateOfBirth(), item.getGender(), item.getAddress(), item.getPersonalSummary(), item.getEmploymentType(), item.getWorkingStatus(), item.getEmployeeLevel(), item.getMonthlyWorkingCapacityHours(), item.getMainExpertise(), item.getSecondaryExpertise(), item.isMustChangePassword(), item.isInitialAccountGenerated(), item.getCreatedAt(), item.getUpdatedAt()); }
    private TaskView toTaskView(TaskEntity item) { return new TaskView(item.getId(), item.getWorkspaceId(), item.getTitle(), item.getRequirements(), item.getDescription(), item.getCustomerPhone(), item.getCustomerEmail(), item.getCustomerDescription(), item.getAssignmentType(), item.getAssigneeId(), item.getCreatorId(), item.getPriority(), item.getDeadline(), item.getStartDate(), item.getEstimatedHours(), item.getDifficulty(), item.getRequiredSkills(), item.getRequiredJobPositionId(), item.getTaskDomain(), item.getProjectId(), item.getDepartmentId(), taskAssignees.findByTaskIdOrderByCreatedAtAsc(item.getId()).stream().map(this::toTaskAssigneeView).toList(), taskAttachments.findByTaskIdOrderByCreatedAtAsc(item.getId()).stream().map(this::toTaskAttachmentView).toList(), item.getProgressPercent(), item.getStatus(), item.getCreatedAt(), item.getUpdatedAt(), item.getCompletedAt()); }
    private TaskAssigneeView toTaskAssigneeView(TaskAssigneeEntity item) { return new TaskAssigneeView(item.getId(), item.getTaskId(), item.getEmployeeId(), item.getParticipantRole(), item.isLeader(), item.getAllocatedHours(), item.getCreatedAt()); }
    private TaskAttachmentView toTaskAttachmentView(TaskAttachmentEntity item) { return new TaskAttachmentView(item.getId(), item.getTaskId(), item.getFileName(), item.getFileUrl(), item.getContentType(), item.getFileSize(), item.getAttachmentType(), item.getUploadedBy(), item.getCreatedAt()); }
    private DepartmentView toDepartmentView(DepartmentEntity item) { return new DepartmentView(item.getId(), item.getWorkspaceId(), item.getName(), item.getCode(), item.getDescription(), item.getStatus(), item.getCreatedAt(), item.getUpdatedAt()); }
    private JobPositionView toJobPositionView(JobPositionEntity item) { return new JobPositionView(item.getId(), item.getWorkspaceId(), item.getTitle(), item.getPermissionGroup(), item.getDepartmentId(), item.getDepartmentName(), item.getDescription(), item.getRequiredSkills(), item.getStatus(), item.getCreatedAt(), item.getUpdatedAt()); }
    private BusinessPositionView toBusinessPositionView(JobPositionEntity item) { return new BusinessPositionView(item.getId(), item.getWorkspaceId(), item.getTitle(), item.getCode(), item.getPermissionGroup(), item.getDepartmentId(), item.getDepartmentName(), item.getDescription(), item.getStatus(), item.getCreatedAt(), item.getUpdatedAt()); }
    private AiHistoryView toAiHistoryView(AiHistoryEntity item) { return new AiHistoryView(item.getId(), item.getCallerName(), item.getCallerRole(), item.getFunctionName(), item.getStatus(), item.getCalledAt()); }
    private TaskUpdateView toTaskUpdateView(TaskUpdateEntity item) { return new TaskUpdateView(item.getId(), item.getTaskId(), item.getUserId(), item.getProgressPercent(), item.getContent(), item.getAttachment(), item.getUpdateType(), item.getCreatedAt()); }
    private DailyReportView toDailyReportView(DailyReportEntity item) { return new DailyReportView(item.getId(), item.getWorkspaceId(), item.getUserId(), item.getReportDate(), item.getTodayCompleted(), item.getCurrentWork(), item.getBlockers(), item.getTomorrowPlan(), item.getReviewedAt(), item.getCreatedAt(), item.getUpdatedAt()); }
    private NotificationView toNotificationView(NotificationEntity item) { return new NotificationView(item.getId(), item.getWorkspaceId(), item.getUserId(), item.getType(), item.getTitle(), item.getMessage(), item.getRelatedEntityType(), item.getRelatedEntityId(), item.isRead(), item.getCreatedAt()); }
    private AiSuggestionView toAiSuggestionView(AiSuggestionEntity item) { return new AiSuggestionView(item.getId(), item.getWorkspaceId(), item.getType(), item.getInputData(), item.getOutputData(), item.getStatus(), item.getCreatedBy(), item.getCreatedAt()); }
    private SubscriptionPlanView toSubscriptionPlanView(SubscriptionPlanEntity item) { return new SubscriptionPlanView(item.getId(), item.getName(), item.getDescription(), item.getPrice(), item.getDurationDays(), item.getDurationInMonths(), item.getMaxUsers(), item.getMaxOwnerAccounts(), item.getMaxEmployeeAccounts(), item.isHasFullFeatures(), item.getMaxWorkspaces(), item.getAiUsageLimit(), item.getFeatures(), item.getStatus(), item.getCreatedAt(), item.getUpdatedAt()); }
    private PaymentQrSettingView toPaymentQrSettingView(PaymentQrSettingEntity item) { return new PaymentQrSettingView(item.getId(), item.getPaymentMethod(), item.getQrCodeUrl(), item.getQrFileId(), item.getQrFileId() == null ? null : "/api/public/payment-files/" + item.getQrFileId(), item.getPaymentUrl(), item.getDeeplink(), item.getBankCode(), item.getBankName(), item.getBankAccountNumber(), item.getBankAccountName(), item.getTransferContentPrefix(), item.isEnabled(), item.getUpdatedBy(), item.getCreatedAt(), item.getUpdatedAt()); }
    private PaymentQrFileView toPaymentQrFileView(PaymentQrFileEntity item) { return new PaymentQrFileView(item.getId(), item.getFileName(), item.getContentType(), item.getFileSize(), "/api/public/payment-files/" + item.getId(), item.getCreatedAt()); }
    private WorkspaceSubscriptionView currentWorkspaceSubscription(UUID workspaceId) { return workspaceSubscriptions.findFirstByWorkspaceIdAndStatusOrderByCreatedAtDesc(workspaceId, WorkspaceSubscriptionStatus.ACTIVE).map(this::toWorkspaceSubscriptionView).orElse(null); }
    private WorkspaceSubscriptionView toWorkspaceSubscriptionView(WorkspaceSubscriptionEntity item) { return new WorkspaceSubscriptionView(item.getId(), item.getWorkspaceId(), item.getSubscriptionPlanId(), item.getStatus(), item.getStartDate(), item.getEndDate(), item.getRenewalDate(), item.getPrice(), item.getMaxOwnerAccounts(), item.getMaxEmployeeAccounts(), item.getPaymentTransactionId(), item.getCreatedAt(), item.getUpdatedAt()); }
    private PlatformWorkspaceView toPlatformWorkspaceView(WorkspaceEntity item) { return toPlatformWorkspaceView(item, List.of()); }
    private PlatformWorkspaceView toPlatformWorkspaceView(WorkspaceEntity item, List<GeneratedOwnerAccountView> generatedOwnerAccounts) { List<UserView> ownerAccountViews = ownerAccounts(item.getId()).stream().map(this::toUserView).toList(); return new PlatformWorkspaceView(item.getId(), item.getBusinessName(), item.getName(), item.getShortCode(), item.getOrganizationAbbreviation(), item.getContactEmail(), item.getContactPhone(), item.getAddress(), item.getSubscriptionPlanId(), currentWorkspaceSubscription(item.getId()), item.getMaxUsers(), item.getMaxOwnerAccounts(), item.getMaxEmployeeAccounts(), ownerAccountViews.size(), currentWorkspaceUserCount(item.getId()), item.getStatus(), item.getPaymentStatus(), item.getOwnerId(), item.getOwnerAccountProvisionedAt(), item.getActivatedAt(), item.getExpiresAt(), item.getLastActivityAt(), ownerAccountViews, generatedOwnerAccounts, item.getCreatedAt()); }
    private WorkspaceRegistrationView toWorkspaceRegistrationView(WorkspaceRegistrationEntity item) { return toWorkspaceRegistrationView(item, List.of()); }
    private WorkspaceRegistrationView toWorkspaceRegistrationView(WorkspaceRegistrationEntity item, List<GeneratedOwnerAccountView> generatedOwnerAccounts) { return new WorkspaceRegistrationView(item.getId(), item.getBusinessName(), item.getWorkspaceName(), item.getWorkspaceIdentifier(), item.getContactEmail(), item.getContactPhone(), item.getBusinessAddress(), item.getRepresentativeFullName(), item.getRepresentativeEmail(), item.getRepresentativePhone(), item.getRegistrationToken(), item.getSubscriptionPlanId(), item.getMaxUsers(), item.getMaxOwnerAccounts(), item.getMaxEmployeeAccounts(), item.getOwnerFullName(), item.getOwnerEmail(), item.getOwnerPhone(), item.getPaymentProofUrl(), item.getPaymentStatus(), item.getRegistrationStatus(), item.getWorkspaceId(), item.getReviewedBy(), item.getReviewedAt(), item.getReviewNote(), item.getExpiredAt(), generatedOwnerAccounts, item.getCreatedAt(), item.getUpdatedAt()); }
    private PaymentTransactionView toPaymentTransactionView(PaymentTransactionEntity item) { UUID workspaceId = workspaceRegistrations.findById(item.getWorkspaceRegistrationId()).map(WorkspaceRegistrationEntity::getWorkspaceId).orElse(null); return new PaymentTransactionView(item.getId(), item.getWorkspaceRegistrationId(), workspaceId, item.getSubscriptionPlanId(), item.getPaymentMethod(), item.getAmount(), item.getCurrency(), item.getPaymentCode(), item.getOrderCode(), item.getRequestId(), item.getProviderName(), item.getProviderTransactionId(), item.getProviderPaymentUrl(), item.getProviderDeeplink(), item.getProviderQrCodeUrl(), item.getQrDisplayData(), item.getBankCode(), item.getBankName(), item.getBankAccountNumber(), item.getBankAccountName(), item.getTransferContent(), item.getPaymentConfigurationSnapshot(), item.getRawProviderResponse(), item.getStatus(), item.getPaidAt(), item.getConfirmedAt(), item.getConfirmedBy(), item.getExpiredAt(), item.getFailureReason(), item.getCreatedAt(), item.getUpdatedAt()); }
    private PublicPaymentStatusView toPublicPaymentStatusView(PaymentTransactionEntity item) { return toPublicPaymentStatusView(item, requireWorkspaceRegistration(item.getWorkspaceRegistrationId())); }
    private PublicPaymentStatusView toPublicPaymentStatusView(PaymentTransactionEntity item, WorkspaceRegistrationEntity registration) { return new PublicPaymentStatusView(item.getWorkspaceRegistrationId(), registration.getWorkspaceId(), registration.getPaymentStatus(), registration.getRegistrationStatus(), item.getPaymentMethod(), item.getAmount(), item.getCurrency(), item.getPaymentCode(), item.getProviderPaymentUrl(), item.getProviderDeeplink(), item.getProviderQrCodeUrl(), item.getBankCode(), item.getBankName(), item.getBankAccountNumber(), item.getBankAccountName(), item.getTransferContent(), item.getStatus(), item.getPaidAt(), item.getExpiredAt(), item.getCreatedAt(), item.getUpdatedAt()); }
    private BusinessFeedbackView toBusinessFeedbackView(BusinessFeedbackEntity item) { return new BusinessFeedbackView(item.getId(), item.getWorkspaceId(), item.getRating(), item.getContent(), item.getSupportNote(), item.getStatus(), item.getReviewedBy(), item.getReviewedAt(), item.getCreatedAt(), item.getUpdatedAt()); }
    private AuditLogView toAuditLogView(AuditLogEntity item) { return new AuditLogView(item.getId(), item.getWorkspaceId(), item.getActorId(), item.getActorNameSnapshot(), item.getActorRoleSnapshot(), item.getAction(), item.getEntityType(), item.getEntityId(), item.getResult(), item.getIpAddress(), item.getUserAgent(), item.getRequestId(), item.getMetadata(), item.getOldValue(), item.getNewValue(), item.getCreatedAt()); }

    public record WorkspaceView(UUID id, String name, String shortCode, String logo, String address, UUID ownerId, OffsetDateTime createdAt) {}
    public record UserView(UUID id, UUID workspaceId, String fullName, String email, String phone, String username, String employeeCode, Role role, List<String> permissions, String avatar, String avatarFileId, UserStatus status, String jobTitle, SeniorityLevel seniorityLevel, Integer skillRating, Integer yearsOfExperience, String skills, UUID departmentId, UUID jobPositionId, LocalDate dateOfBirth, String gender, String address, String personalSummary, EmploymentType employmentType, WorkingStatus workingStatus, EmployeeLevel employeeLevel, Integer monthlyWorkingCapacityHours, String mainExpertise, String secondaryExpertise, boolean mustChangePassword, boolean initialAccountGenerated, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record CreatedUserAccountView(UserView user, String username, String initialPassword, boolean credentialsVisibleOnce) {}
    public record TaskView(UUID id, UUID workspaceId, String title, String requirements, String description, String customerPhone, String customerEmail, String customerDescription, AssignmentType assignmentType, UUID assigneeId, UUID creatorId, TaskPriority priority, OffsetDateTime deadline, OffsetDateTime startDate, BigDecimal estimatedHours, Integer difficulty, String requiredSkills, UUID requiredJobPositionId, String taskDomain, UUID projectId, UUID departmentId, List<TaskAssigneeView> participants, List<TaskAttachmentView> attachments, int progressPercent, TaskStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime completedAt) {}
    public record TaskAssigneeView(UUID id, UUID taskId, UUID employeeId, TaskParticipantRole participantRole, boolean leader, BigDecimal allocatedHours, OffsetDateTime createdAt) {}
    public record TaskAttachmentView(UUID id, UUID taskId, String fileName, String fileUrl, String contentType, Long fileSize, AttachmentType attachmentType, UUID uploadedBy, OffsetDateTime createdAt) {}
    public record DepartmentView(UUID id, UUID workspaceId, String name, String code, String description, DepartmentStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record JobPositionView(UUID id, UUID workspaceId, String title, PermissionGroup permissionGroup, UUID departmentId, String departmentName, String description, String requiredSkills, JobPositionStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record BusinessPositionView(UUID id, UUID workspaceId, String name, String code, PermissionGroup permissionGroup, UUID departmentId, String departmentName, String description, JobPositionStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record GeneratedOwnerAccountView(UUID userId, String username, String initialPassword, String fullName) {}
    public record GeneratedHrAccountView(UUID userId, String username, String initialPassword, String fullName, boolean credentialsVisibleOnce) {}
    public record AiHistoryView(UUID id, String callerName, String callerRole, String calledFunction, AiHistoryStatus status, OffsetDateTime calledAt) {}
    public record TaskUpdateView(UUID id, UUID taskId, UUID userId, int progressPercent, String content, String attachment, UpdateType updateType, OffsetDateTime createdAt) {}
    public record DailyReportView(UUID id, UUID workspaceId, UUID userId, LocalDate reportDate, String todayCompleted, String currentWork, String blockers, String tomorrowPlan, OffsetDateTime reviewedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record NotificationView(UUID id, UUID workspaceId, UUID userId, String type, String title, String message, String relatedEntityType, UUID relatedEntityId, boolean isRead, OffsetDateTime createdAt) {}
    public record WorkloadView(UUID employeeId, String fullName, long openTasks, long inProgressTasks, long blockedTasks, long completedTasks, long overdueTasks, BigDecimal estimatedWorkload, double workloadScore, WorkloadLevel workloadLevel) {}
    public record MonthlyWorkloadView(UUID employeeId, String fullName, int year, int month, BigDecimal allocatedHours, BigDecimal capacityHours, double utilizationRatio, String workloadLevel, String workloadLabel) {}
    public record AssigneeRecommendationView(UUID employeeId, String fullName, int score, WorkloadLevel workloadLevel, String requiredRole, String roleFit, String roleFitReason, String reason, String risk, UUID departmentId, UUID businessPositionId, String businessPositionName, PermissionGroup permissionGroup, int departmentSuitabilityScore, int businessPositionSuitabilityScore, int leadExperienceScore, int domainExperienceScore, int skillMatchScore, int similarTaskExperienceScore, int workloadAvailabilityScore, int performanceScore, long previousLeaderCount, double leadCompletionRate, int similarTaskCount, Map<String, Object> scoreComponents) {
        public AssigneeRecommendationView(UUID employeeId, String fullName, int score, WorkloadLevel workloadLevel, String requiredRole, String roleFit, String roleFitReason, String reason, String risk) {
            this(employeeId, fullName, score, workloadLevel, requiredRole, roleFit, roleFitReason, reason, risk, null, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Map.of());
        }
    }
    public record OwnerDashboardView(long totalTasks, long activeTasks, long completedTasks, long overdueTasks, List<WorkloadView> employeeWorkload, List<TaskView> recentlyUpdatedTasks, List<DashboardAiRecommendationView> aiRecommendations) {}
    public record DashboardAiRecommendationView(UUID suggestionId, AiSuggestionType type, String source, String outputData, OffsetDateTime createdAt) {}
    public record BusinessSummaryView(long completedTasks, long overdueTasks, long overloadedEmployees, long idleEmployees, String summary) {}
    public record LoginView(String token, UserView user, List<String> permissions) {}
    public record AiSuggestionView(UUID id, UUID workspaceId, AiSuggestionType type, String inputData, String outputData, AiSuggestionStatus status, UUID createdBy, OffsetDateTime createdAt) {}
    public record SubscriptionPlanView(UUID id, String name, String description, BigDecimal price, int durationDays, int durationInMonths, int maxUsers, int maxOwnerAccounts, int maxEmployeeAccounts, boolean hasFullFeatures, Integer maxWorkspaces, Integer aiUsageLimit, String features, SubscriptionPlanStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record PaymentQrSettingView(UUID id, PaymentMethod paymentMethod, String qrCodeUrl, UUID qrFileId, String qrDisplayUrl, String paymentUrl, String deeplink, String bankCode, String bankName, String bankAccountNumber, String bankAccountName, String transferContentPrefix, boolean enabled, UUID updatedBy, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record PaymentQrFileView(UUID fileId, String fileName, String contentType, long size, String displayUrl, OffsetDateTime createdAt) {}
    public record PaymentQrFileContent(String contentType, byte[] content) {}
    public record WorkspaceSubscriptionView(UUID id, UUID workspaceId, UUID subscriptionPlanId, WorkspaceSubscriptionStatus status, OffsetDateTime startDate, OffsetDateTime endDate, OffsetDateTime renewalDate, BigDecimal price, int maxOwnerAccounts, int maxEmployeeAccounts, UUID paymentTransactionId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record PlatformWorkspaceView(UUID id, String businessName, String workspaceName, String workspaceIdentifier, String organizationAbbreviation, String contactEmail, String contactPhone, String businessAddress, UUID subscriptionPlanId, WorkspaceSubscriptionView activeSubscription, int maxUsers, int maxOwnerAccounts, int maxEmployeeAccounts, int ownerAccountCount, int currentUsers, WorkspaceStatus status, PaymentStatus paymentStatus, UUID ownerId, OffsetDateTime ownerAccountProvisionedAt, OffsetDateTime activatedAt, OffsetDateTime expiresAt, OffsetDateTime lastActivityAt, List<UserView> ownerAccounts, List<GeneratedOwnerAccountView> generatedOwnerAccounts, OffsetDateTime createdAt) {}
    public record WorkspaceRegistrationView(UUID id, String businessName, String workspaceName, String workspaceIdentifier, String contactEmail, String contactPhone, String businessAddress, String representativeFullName, String representativeEmail, String representativePhone, String registrationToken, UUID subscriptionPlanId, int maxUsers, int maxOwnerAccounts, int maxEmployeeAccounts, String ownerFullName, String ownerEmail, String ownerPhone, String paymentProofUrl, PaymentStatus paymentStatus, RegistrationStatus registrationStatus, UUID workspaceId, UUID reviewedBy, OffsetDateTime reviewedAt, String reviewNote, OffsetDateTime expiredAt, List<GeneratedOwnerAccountView> generatedOwnerAccounts, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record PaymentTransactionView(UUID id, UUID workspaceRegistrationId, UUID workspaceId, UUID subscriptionPlanId, PaymentMethod paymentMethod, BigDecimal amount, String currency, String paymentCode, String orderCode, String requestId, String providerName, String providerTransactionId, String providerPaymentUrl, String providerDeeplink, String providerQrCodeUrl, String qrDisplayData, String bankCode, String bankName, String bankAccountNumber, String bankAccountName, String transferContent, String paymentConfigurationSnapshot, String providerResponseSnapshot, PaymentTransactionStatus status, OffsetDateTime paidAt, OffsetDateTime confirmedAt, UUID confirmedBy, OffsetDateTime expiredAt, String failureReason, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record PublicPaymentStatusView(UUID workspaceRegistrationId, UUID workspaceId, PaymentStatus registrationPaymentStatus, RegistrationStatus registrationStatus, PaymentMethod paymentMethod, BigDecimal amount, String currency, String paymentCode, String providerPaymentUrl, String providerDeeplink, String providerQrCodeUrl, String bankCode, String bankName, String bankAccountNumber, String bankAccountName, String transferContent, PaymentTransactionStatus status, OffsetDateTime paidAt, OffsetDateTime expiredAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record BusinessFeedbackView(UUID id, UUID workspaceId, int rating, String content, String supportNote, FeedbackStatus status, UUID reviewedBy, OffsetDateTime reviewedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record AuditLogView(UUID id, UUID workspaceId, UUID actorId, String actorName, String actorRole, String action, String entityType, UUID entityId, String result, String ipAddress, String userAgent, String requestId, String metadata, String oldValue, String newValue, OffsetDateTime createdAt) {}
    private record AssignmentPlan(AssignmentType assignmentType, UUID primaryAssigneeId, UUID teamLeaderId, List<UUID> participantIds) {}
}

