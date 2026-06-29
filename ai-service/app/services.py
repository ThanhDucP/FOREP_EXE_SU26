from app.schemas import (
    AssigneeRecommendation,
    DailySummaryRequest,
    DailySummaryResponse,
    DelayRisk,
    DelayRiskRequest,
    RecommendAssigneeRequest,
    WorkloadSummaryRequest,
    WorkloadSummaryResponse,
)


def recommend_assignee(payload: RecommendAssigneeRequest) -> list[AssigneeRecommendation]:
    recommendations: list[AssigneeRecommendation] = []
    for employee in payload.employees:
        score = int(
            100
            - employee.open_tasks * 6
            - employee.overdue_tasks * 12
            - employee.blocked_tasks * 8
            - employee.estimated_workload / 2
        )
        score = max(35, min(98, score))
        if employee.workload_level == "OVERLOADED":
            continue
        reason = (
            "Đang rảnh, phù hợp để nhận task tiếp theo."
            if employee.workload_level == "NO_WORK"
            else f"Hiện có {employee.open_tasks} task đang mở và workload ở mức {employee.workload_level}."
        )
        risk = "Có task quá hạn, cần cân nhắc." if employee.overdue_tasks else "Không có"
        recommendations.append(
            AssigneeRecommendation(
                employeeId=employee.employee_id,
                fullName=employee.full_name,
                score=score,
                workloadLevel=employee.workload_level,
                reason=reason,
                risk=risk,
            )
        )
    return sorted(recommendations, key=lambda item: item.score, reverse=True)[:3]


def workload_summary(payload: WorkloadSummaryRequest) -> WorkloadSummaryResponse:
    overloaded = [item.full_name for item in payload.employees if item.workload_level == "OVERLOADED"]
    idle = [item.full_name for item in payload.employees if item.workload_level == "NO_WORK"]
    overdue = [item.full_name for item in payload.employees if item.overdue_tasks > 0]
    summary = f"Có {len(overloaded)} nhân viên quá tải, {len(idle)} nhân viên đang rảnh và {len(overdue)} nhân viên có task quá hạn."
    return WorkloadSummaryResponse(
        summary=summary,
        overloadedEmployees=overloaded,
        idleEmployees=idle,
        overdueEmployees=overdue,
    )


def delay_risks(payload: DelayRiskRequest) -> list[DelayRisk]:
    risks: list[DelayRisk] = []
    for task in payload.tasks:
        if task.overdue:
            risks.append(
                DelayRisk(
                    taskId=task.task_id,
                    title=task.title,
                    riskLevel="HIGH",
                    reason="Task đã quá hạn.",
                    recommendedAction=f"Liên hệ {task.assignee_name} để cập nhật trạng thái ngay.",
                )
            )
        elif task.progress_percent < 30:
            risks.append(
                DelayRisk(
                    taskId=task.task_id,
                    title=task.title,
                    riskLevel="MEDIUM",
                    reason="Tiến độ thấp so với deadline.",
                    recommendedAction="Yêu cầu cập nhật tiến độ hoặc chia nhỏ task.",
                )
            )
    return risks


def daily_summary(payload: DailySummaryRequest) -> DailySummaryResponse:
    return DailySummaryResponse(
        summary=(
            f"Hôm nay có {payload.completed_tasks} task hoàn thành, "
            f"{payload.overdue_tasks} task quá hạn, "
            f"{payload.overloaded_employees} nhân viên quá tải và "
            f"{payload.idle_employees} nhân viên đang rảnh."
        )
    )

