package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.domain.Enums.Role;
import com.forep.exe.domain.Enums.UserStatus;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.TaskEntity;
import com.forep.exe.persistence.TaskRepository;
import com.forep.exe.persistence.UserEntity;
import com.forep.exe.persistence.UserRepository;
import com.forep.exe.persistence.WorkspaceRegistrationRepository;
import com.forep.exe.security.AuthenticatedUser;
import com.forep.exe.security.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class WorkspaceDashboardKpiService {
    private static final List<Role> WORKFORCE_ROLES = List.of(Role.EMPLOYEE, Role.MANAGER, Role.EXECUTIVE);

    private final SecurityContext securityContext;
    private final UserRepository users;
    private final TaskRepository tasks;
    private final WorkspaceRegistrationRepository workspaceRegistrations;
    private final PaymentTransactionRepository paymentTransactions;

    public WorkspaceDashboardKpiService(SecurityContext securityContext,
                                        UserRepository users,
                                        TaskRepository tasks,
                                        WorkspaceRegistrationRepository workspaceRegistrations,
                                        PaymentTransactionRepository paymentTransactions) {
        this.securityContext = securityContext;
        this.users = users;
        this.tasks = tasks;
        this.workspaceRegistrations = workspaceRegistrations;
        this.paymentTransactions = paymentTransactions;
    }

    public Map<String, Object> ownerDashboardKpis() {
        AuthenticatedUser currentUser = securityContext.currentUser();
        UUID workspaceId = currentUser.workspaceId();
        if (workspaceId == null) {
            throw new IllegalArgumentException("Tài khoản hiện tại không thuộc workspace.");
        }

        List<UserEntity> workforce = users.findByWorkspaceIdAndRoleInOrderByFullNameAsc(workspaceId, WORKFORCE_ROLES);
        long employeeCount = workforce.stream()
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .count();

        List<TaskEntity> scopedTasks = tasks.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        long taskCount = scopedTasks.size();
        long projectCount = scopedTasks.stream()
                .map(TaskEntity::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        BigDecimal monthlyRevenue = workspaceRegistrations.findByWorkspaceId(workspaceId)
                .map(registration -> paidAmountThisMonth(registration.getId()))
                .orElse(BigDecimal.ZERO);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("employeeCount", employeeCount);
        summary.put("projectCount", projectCount);
        summary.put("taskCount", taskCount);
        summary.put("monthlyRevenue", monthlyRevenue);
        summary.put("currency", "VND");

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("summary", summary);

        // Compatibility aliases for existing/new frontend dashboard contracts.
        output.put("employeeCount", employeeCount);
        output.put("activeEmployeeCount", employeeCount);
        output.put("totalEmployees", employeeCount);
        output.put("personnelCount", employeeCount);

        output.put("projectCount", projectCount);
        output.put("totalProjects", projectCount);

        output.put("taskCount", taskCount);
        output.put("totalTasks", taskCount);

        output.put("monthlyRevenue", monthlyRevenue);
        output.put("revenueThisMonth", monthlyRevenue);
        output.put("monthRevenue", monthlyRevenue);
        output.put("monthlyPaidAmount", monthlyRevenue);
        output.put("currency", "VND");
        return output;
    }

    private BigDecimal paidAmountThisMonth(UUID workspaceRegistrationId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime monthStart = now
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        OffsetDateTime nextMonthStart = monthStart.plusMonths(1);

        return paymentTransactions.findByWorkspaceRegistrationIdOrderByCreatedAtDesc(workspaceRegistrationId).stream()
                .filter(payment -> payment.getStatus() == PaymentTransactionStatus.SUCCESS
                        || payment.getStatus() == PaymentTransactionStatus.PAID)
                .filter(payment -> payment.getPaidAt() != null)
                .filter(payment -> !payment.getPaidAt().isBefore(monthStart)
                        && payment.getPaidAt().isBefore(nextMonthStart))
                .map(PaymentTransactionEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
