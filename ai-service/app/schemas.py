from pydantic import BaseModel, Field


class EmployeeWorkload(BaseModel):
    employee_id: str = Field(alias="employeeId")
    full_name: str = Field(alias="fullName")
    open_tasks: int = Field(alias="openTasks")
    overdue_tasks: int = Field(alias="overdueTasks")
    blocked_tasks: int = Field(default=0, alias="blockedTasks")
    estimated_workload: float = Field(alias="estimatedWorkload")
    workload_level: str = Field(alias="workloadLevel")


class RecommendAssigneeRequest(BaseModel):
    title: str
    requirements: str
    deadline: str
    estimated_hours: float = Field(default=0, alias="estimatedHours")
    employees: list[EmployeeWorkload]


class AssigneeRecommendation(BaseModel):
    employee_id: str = Field(alias="employeeId")
    full_name: str = Field(alias="fullName")
    score: int
    workload_level: str = Field(alias="workloadLevel")
    reason: str
    risk: str


class WorkloadSummaryRequest(BaseModel):
    employees: list[EmployeeWorkload]


class WorkloadSummaryResponse(BaseModel):
    summary: str
    overloaded_employees: list[str] = Field(alias="overloadedEmployees")
    idle_employees: list[str] = Field(alias="idleEmployees")
    overdue_employees: list[str] = Field(alias="overdueEmployees")


class DelayRiskTask(BaseModel):
    task_id: str = Field(alias="taskId")
    title: str
    assignee_name: str = Field(alias="assigneeName")
    deadline: str
    progress_percent: int = Field(alias="progressPercent")
    overdue: bool


class DelayRiskRequest(BaseModel):
    tasks: list[DelayRiskTask]


class DelayRisk(BaseModel):
    task_id: str = Field(alias="taskId")
    title: str
    risk_level: str = Field(alias="riskLevel")
    reason: str
    recommended_action: str = Field(alias="recommendedAction")


class DailySummaryRequest(BaseModel):
    completed_tasks: int = Field(alias="completedTasks")
    overdue_tasks: int = Field(alias="overdueTasks")
    overloaded_employees: int = Field(alias="overloadedEmployees")
    idle_employees: int = Field(alias="idleEmployees")


class DailySummaryResponse(BaseModel):
    summary: str


class BusinessSummaryTask(BaseModel):
    task_id: str = Field(alias="taskId")
    title: str
    assignee_name: str = Field(alias="assigneeName")
    priority: str
    status: str
    deadline: str
    progress_percent: int = Field(alias="progressPercent")
    estimated_hours: float = Field(alias="estimatedHours")
    overdue: bool


class BusinessSummaryReport(BaseModel):
    report_id: str = Field(alias="reportId")
    user_name: str = Field(alias="userName")
    report_date: str = Field(alias="reportDate")
    today_completed: str = Field(alias="todayCompleted")
    current_work: str = Field(alias="currentWork")
    blockers: str | None = None
    tomorrow_plan: str | None = Field(default=None, alias="tomorrowPlan")
    reviewed: bool


class BusinessSummaryRequest(BaseModel):
    period: str
    completed_tasks: int = Field(alias="completedTasks")
    overdue_tasks: int = Field(alias="overdueTasks")
    overloaded_employees: int = Field(alias="overloadedEmployees")
    idle_employees: int = Field(alias="idleEmployees")
    tasks: list[BusinessSummaryTask]
    reports: list[BusinessSummaryReport] = Field(default_factory=list)
    workload: list[EmployeeWorkload] = Field(default_factory=list)


class BusinessSummaryResponse(BaseModel):
    summary: str
    highlights: list[str]
    risks: list[str]
    recommended_actions: list[str] = Field(alias="recommendedActions")


class DailyReportInsightsRequest(BaseModel):
    reports: list[BusinessSummaryReport]


class DailyReportInsightsResponse(BaseModel):
    summary: str
    blockers: list[str]
    follow_up_questions: list[str] = Field(alias="followUpQuestions")
    recommended_actions: list[str] = Field(alias="recommendedActions")


class ExtractTasksRequest(BaseModel):
    text: str
    default_deadline: str | None = Field(default=None, alias="defaultDeadline")


class ExtractedTask(BaseModel):
    title: str
    requirements: str
    description: str | None = None
    priority: str
    deadline: str | None = None
    estimated_hours: float | None = Field(default=None, alias="estimatedHours")
    confidence: int


class ExtractTasksResponse(BaseModel):
    tasks: list[ExtractedTask]
