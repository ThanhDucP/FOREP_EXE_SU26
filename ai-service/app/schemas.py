from __future__ import annotations

from typing import Any, Optional

from pydantic import BaseModel, ConfigDict, Field


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid", populate_by_name=True)


class EmployeeWorkload(StrictModel):
    employee_id: str = Field(alias="employeeId")
    full_name: str = Field(alias="fullName")
    open_tasks: int = Field(alias="openTasks")
    overdue_tasks: int = Field(alias="overdueTasks")
    blocked_tasks: int = Field(default=0, alias="blockedTasks")
    estimated_workload: float = Field(alias="estimatedWorkload")
    workload_level: str = Field(alias="workloadLevel")
    status: str = "ACTIVE"
    job_title: Optional[str] = Field(default=None, alias="jobTitle")
    seniority_level: Optional[str] = Field(default=None, alias="seniorityLevel")
    skill_rating: Optional[int] = Field(default=None, alias="skillRating", ge=1, le=5)
    years_of_experience: Optional[int] = Field(default=None, alias="yearsOfExperience", ge=0)
    skills: Optional[str] = None
    candidate_score: Optional[int] = Field(default=None, alias="candidateScore")
    score_components: dict[str, Any] = Field(default_factory=dict, alias="scoreComponents")


class RecommendAssigneeRequest(StrictModel):
    title: str
    requirements: str
    deadline: str
    estimated_hours: float = Field(default=0, alias="estimatedHours")
    employees: list[EmployeeWorkload]


class AssigneeRecommendation(StrictModel):
    employee_id: str = Field(alias="employeeId")
    full_name: str = Field(alias="fullName")
    score: int
    workload_level: str = Field(alias="workloadLevel")
    reason: str
    risk: str


class WorkloadSummaryRequest(StrictModel):
    employees: list[EmployeeWorkload]


class WorkloadSummaryResponse(StrictModel):
    summary: str
    overloaded_employees: list[str] = Field(alias="overloadedEmployees")
    idle_employees: list[str] = Field(alias="idleEmployees")
    overdue_employees: list[str] = Field(alias="overdueEmployees")


class DelayRiskTask(StrictModel):
    task_id: str = Field(alias="taskId")
    title: str
    assignee_name: str = Field(alias="assigneeName")
    deadline: str
    progress_percent: int = Field(alias="progressPercent")
    overdue: bool


class DelayRiskRequest(StrictModel):
    tasks: list[DelayRiskTask]


class DelayRisk(StrictModel):
    task_id: str = Field(alias="taskId")
    title: str
    risk_level: str = Field(alias="riskLevel")
    reason: str
    recommended_action: str = Field(alias="recommendedAction")


class DailySummaryRequest(StrictModel):
    completed_tasks: int = Field(alias="completedTasks")
    overdue_tasks: int = Field(alias="overdueTasks")
    overloaded_employees: int = Field(alias="overloadedEmployees")
    idle_employees: int = Field(alias="idleEmployees")


class DailySummaryResponse(StrictModel):
    summary: str


class BusinessSummaryTask(StrictModel):
    task_id: str = Field(alias="taskId")
    title: str
    requirements: Optional[str] = None
    description: Optional[str] = None
    assignee_name: str = Field(alias="assigneeName")
    priority: str
    status: str
    deadline: str
    progress_percent: int = Field(alias="progressPercent")
    estimated_hours: float = Field(alias="estimatedHours")
    overdue: bool


class BusinessSummaryReport(StrictModel):
    report_id: str = Field(alias="reportId")
    employee_id: Optional[str] = Field(default=None, alias="employeeId")
    user_name: str = Field(alias="userName")
    report_date: str = Field(alias="reportDate")
    today_completed: str = Field(alias="todayCompleted")
    current_work: str = Field(alias="currentWork")
    blockers: Optional[str] = None
    tomorrow_plan: Optional[str] = Field(default=None, alias="tomorrowPlan")
    reviewed: bool


class BusinessSummaryRequest(StrictModel):
    period: str
    period_type: Optional[str] = Field(default=None, alias="periodType")
    period_start: Optional[str] = Field(default=None, alias="periodStart")
    period_end: Optional[str] = Field(default=None, alias="periodEnd")
    completed_tasks: int = Field(alias="completedTasks")
    active_tasks: int = Field(default=0, alias="activeTasks")
    overdue_tasks: int = Field(alias="overdueTasks")
    blocked_tasks: int = Field(default=0, alias="blockedTasks")
    completion_rate: float = Field(default=0, alias="completionRate")
    missing_daily_reports: int = Field(default=0, alias="missingDailyReports")
    overloaded_employees: int = Field(alias="overloadedEmployees")
    idle_employees: int = Field(alias="idleEmployees")
    tasks: list[BusinessSummaryTask]
    reports: list[BusinessSummaryReport] = Field(default_factory=list)
    workload: list[EmployeeWorkload] = Field(default_factory=list)


class BusinessSummaryResponse(StrictModel):
    period_type: Optional[str] = Field(default=None, alias="periodType")
    period_start: Optional[str] = Field(default=None, alias="periodStart")
    period_end: Optional[str] = Field(default=None, alias="periodEnd")
    summary: str
    highlights: list[str]
    risks: list[str]
    action_suggestions: list["ActionSuggestion"] = Field(default_factory=list, alias="actionSuggestions")


class DailyReportInsightReport(StrictModel):
    report_id: str = Field(alias="reportId")
    employee_id: str = Field(alias="employeeId")
    employee_name: str = Field(alias="employeeName")
    report_date: str = Field(alias="reportDate")
    today_completed: str = Field(alias="todayCompleted")
    current_work: str = Field(alias="currentWork")
    blockers: Optional[str] = None
    tomorrow_plan: Optional[str] = Field(default=None, alias="tomorrowPlan")


class DailyReportInsightsRequest(StrictModel):
    reports: list[DailyReportInsightReport]


class DailyReportBlocker(StrictModel):
    severity: str
    description: str


class DailyReportInsightsResponse(StrictModel):
    summary: str
    blockers: list[DailyReportBlocker]
    action_suggestions: list["ActionSuggestion"] = Field(alias="actionSuggestions")


class ExtractTasksRequest(StrictModel):
    text: str
    default_deadline: Optional[str] = Field(default=None, alias="defaultDeadline")
    employees: list[EmployeeWorkload] = Field(default_factory=list)


class ExtractedTask(StrictModel):
    title: str
    requirements: str
    description: Optional[str] = None
    priority: str
    suggested_assignee_id: Optional[str] = Field(default=None, alias="suggestedAssigneeId")
    deadline_suggestion: Optional[str] = Field(default=None, alias="deadlineSuggestion")
    estimated_hours: Optional[float] = Field(default=None, alias="estimatedHours")
    confidence: float
    missing_information: list[str] = Field(default_factory=list, alias="missingInformation")


class ExtractTasksResponse(StrictModel):
    tasks: list[ExtractedTask]


class TaskContext(StrictModel):
    task_id: Optional[str] = Field(default=None, alias="taskId")
    title: str
    requirements: str
    description: Optional[str] = None
    assignee_name: Optional[str] = Field(default=None, alias="assigneeName")
    priority: str
    status: Optional[str] = None
    deadline: str
    progress_percent: int = Field(default=0, alias="progressPercent")
    estimated_hours: Optional[float] = Field(default=None, alias="estimatedHours")
    overdue: bool = False


class SplitTaskRequest(StrictModel):
    task: TaskContext


class SubtaskSuggestion(StrictModel):
    title: str
    requirements: str
    estimated_hours: Optional[float] = Field(default=None, alias="estimatedHours")
    suggested_order: int = Field(alias="suggestedOrder")
    dependency_note: Optional[str] = Field(default=None, alias="dependencyNote")
    confidence: float


class SplitTaskResponse(StrictModel):
    parent_task_id: str = Field(alias="parentTaskId")
    subtasks: list[SubtaskSuggestion]


class TaskAdjustmentRequest(StrictModel):
    task: TaskContext


class TaskAdjustmentSuggestion(StrictModel):
    action_type: str = Field(alias="actionType")
    target_entity_id: str = Field(alias="targetEntityId")
    suggested_deadline: Optional[str] = Field(default=None, alias="suggestedDeadline")
    suggested_priority: Optional[str] = Field(default=None, alias="suggestedPriority")
    reason: str
    risk_if_ignored: str = Field(alias="riskIfIgnored")
    confidence: float


class TaskAdjustmentResponse(StrictModel):
    task_id: str = Field(alias="taskId")
    suggestions: list[TaskAdjustmentSuggestion]


class MissingReportEmployee(StrictModel):
    employee_id: str = Field(alias="employeeId")
    full_name: str = Field(alias="fullName")
    status: str


class MissingReportReport(StrictModel):
    report_id: str = Field(alias="reportId")
    user_id: str = Field(alias="userId")
    user_name: str = Field(alias="userName")
    report_date: str = Field(alias="reportDate")


class MissingReportsRequest(StrictModel):
    report_date: str = Field(alias="reportDate")
    employees: list[MissingReportEmployee]
    reports: list[MissingReportReport] = Field(default_factory=list)


class MissingReportSuggestion(StrictModel):
    employee_id: str = Field(alias="employeeId")
    employee_name: str = Field(alias="employeeName")
    report_date: str = Field(alias="reportDate")
    days_missing: int = Field(alias="daysMissing")
    recommended_action: str = Field(alias="recommendedAction")
    confidence: float


class MissingReportsResponse(StrictModel):
    missing_reports: list[MissingReportSuggestion] = Field(alias="missingReports")


class ActionSuggestionsRequest(StrictModel):
    tasks: list[BusinessSummaryTask] = Field(default_factory=list)
    reports: list[BusinessSummaryReport] = Field(default_factory=list)
    workload: list[EmployeeWorkload] = Field(default_factory=list)


class ActionSuggestion(StrictModel):
    action_type: str = Field(alias="actionType")
    target_entity_type: str = Field(alias="targetEntityType")
    target_entity_id: str = Field(alias="targetEntityId")
    title: str
    reason: str
    confidence: float


class ActionSuggestionsResponse(StrictModel):
    suggestions: list[ActionSuggestion]

