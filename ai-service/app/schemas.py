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

