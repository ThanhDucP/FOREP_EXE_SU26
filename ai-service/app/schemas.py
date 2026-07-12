from __future__ import annotations

from typing import Any, Literal, Optional

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
    required_role: Optional[str] = Field(default=None, alias="requiredRole")
    role_fit: Optional[Literal["STRONG", "PARTIAL", "UNCERTAIN"]] = Field(default=None, alias="roleFit")
    role_fit_reason: Optional[str] = Field(default=None, alias="roleFitReason")
    reason: str
    risk: str


class WorkloadSummaryRequest(StrictModel):
    employees: list[EmployeeWorkload]


class WorkloadSummaryResponse(StrictModel):
    summary: str
    overloaded_employees: list[str] = Field(alias="overloadedEmployees")
    idle_employees: list[str] = Field(alias="idleEmployees")
    overdue_employees: list[str] = Field(alias="overdueEmployees")
    workload_insights: list[str] = Field(default_factory=list, alias="workloadInsights")
    recommended_actions: list[str] = Field(default_factory=list, alias="recommendedActions")


class DelayRiskTask(StrictModel):
    task_id: str = Field(alias="taskId")
    title: str
    assignee_name: str = Field(alias="assigneeName")
    deadline: str
    progress_percent: int = Field(alias="progressPercent")
    overdue: bool
    status: Optional[str] = None
    priority: Optional[str] = None


class DelayRiskRequest(StrictModel):
    tasks: list[DelayRiskTask]


class DelayRisk(StrictModel):
    task_id: str = Field(alias="taskId")
    title: str
    risk_level: str = Field(alias="riskLevel")
    reason: str
    recommended_action: str = Field(alias="recommendedAction")


class DelayRiskResponse(StrictModel):
    summary: str
    risks: list[DelayRisk]
    recommended_actions: list[str] = Field(default_factory=list, alias="recommendedActions")


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
    summary: str = ""


Difficulty = Literal["EASY", "MEDIUM", "HARD", "CRITICAL"]
EmployeeLevelLiteral = Literal["INTERN", "FRESHER", "JUNIOR", "MIDDLE", "SENIOR", "LEAD"]
RiskLevel = Literal["LOW", "MEDIUM", "HIGH"]


class WorkspaceScopedRequest(StrictModel):
    workspace_id: str = Field(alias="workspaceId")


class TaskDescriptionAnalysisRequest(WorkspaceScopedRequest):
    task_title: str = Field(alias="taskTitle")
    task_description: str = Field(alias="taskDescription")
    project_description: Optional[str] = Field(default=None, alias="projectDescription")
    department_name: Optional[str] = Field(default=None, alias="departmentName")
    available_task_types: list[str] = Field(default_factory=list, alias="availableTaskTypes")
    available_job_positions: list[str] = Field(default_factory=list, alias="availableJobPositions")
    available_skills: list[str] = Field(default_factory=list, alias="availableSkills")
    available_departments: list[str] = Field(default_factory=list, alias="availableDepartments")
    start_date: Optional[str] = Field(default=None, alias="startDate")
    deadline: Optional[str] = None


class EstimatedWorkingHoursSuggestion(StrictModel):
    value: Optional[float]
    reason: str
    confidence: float


class TaskDescriptionAnalysisResponse(StrictModel):
    task_type: str = Field(alias="taskType")
    task_domain: str = Field(alias="taskDomain")
    suggested_difficulty: Difficulty = Field(alias="suggestedDifficulty")
    suggested_employee_level: EmployeeLevelLiteral = Field(alias="suggestedEmployeeLevel")
    required_skills: list[str] = Field(alias="requiredSkills")
    required_job_positions: list[str] = Field(alias="requiredJobPositions")
    related_department: str = Field(alias="relatedDepartment")
    estimated_working_hours_suggestion: EstimatedWorkingHoursSuggestion = Field(alias="estimatedWorkingHoursSuggestion")
    missing_information: list[str] = Field(alias="missingInformation")
    clarifying_questions: list[str] = Field(alias="clarifyingQuestions")
    summary: str


class EstimatedHoursRequest(WorkspaceScopedRequest):
    task_title: str = Field(alias="taskTitle")
    task_description: Optional[str] = Field(default=None, alias="taskDescription")
    difficulty: Optional[str] = None
    task_type: Optional[str] = Field(default=None, alias="taskType")
    start_date: Optional[str] = Field(default=None, alias="startDate")
    deadline: Optional[str] = None
    backend_working_days: Optional[int] = Field(default=None, alias="backendWorkingDays")
    backend_default_hours: Optional[float] = Field(default=None, alias="backendDefaultHours")


class EstimatedHoursResponse(StrictModel):
    suggested_hours: Optional[float] = Field(alias="suggestedHours")
    working_days: Optional[int] = Field(alias="workingDays")
    calculation_basis: str = Field(alias="calculationBasis")
    confidence: float
    user_confirmation_required: bool = Field(default=True, alias="userConfirmationRequired")


class RecommendationTaskContext(StrictModel):
    title: str
    difficulty: Optional[str] = None
    task_type: Optional[str] = Field(default=None, alias="taskType")
    task_domain: Optional[str] = Field(default=None, alias="taskDomain")
    required_skills: list[str] = Field(default_factory=list, alias="requiredSkills")
    required_job_positions: list[str] = Field(default_factory=list, alias="requiredJobPositions")
    estimated_working_hours: Optional[float] = Field(default=None, alias="estimatedWorkingHours")
    start_date: Optional[str] = Field(default=None, alias="startDate")
    deadline: Optional[str] = None


class MonthlyWorkloadDetail(StrictModel):
    month: str
    existing_hours: float = Field(alias="existingHours")
    new_task_hours: float = Field(default=0, alias="newTaskHours")
    total_hours_after_assignment: float = Field(alias="totalHoursAfterAssignment")
    usage_percentage: float = Field(alias="usagePercentage")
    workload_status: str = Field(alias="workloadStatus")


class RankedCandidate(StrictModel):
    rank: int
    employee_id: str = Field(alias="employeeId")
    full_name: str = Field(alias="fullName")
    role: Optional[str] = None
    job_position: Optional[str] = Field(default=None, alias="jobPosition")
    department: Optional[str] = None
    employee_level: Optional[str] = Field(default=None, alias="employeeLevel")
    skill_match_score: float = Field(alias="skillMatchScore")
    role_suitability_score: float = Field(alias="roleSuitabilityScore")
    job_position_suitability_score: float = Field(alias="jobPositionSuitabilityScore")
    similar_task_count: int = Field(default=0, alias="similarTaskCount")
    completion_rate: float = Field(default=0, alias="completionRate")
    overdue_rate: float = Field(default=0, alias="overdueRate")
    current_monthly_hours: float = Field(default=0, alias="currentMonthlyHours")
    monthly_capacity_hours: float = Field(default=168, alias="monthlyCapacityHours")
    workload_status_after_assignment: Optional[str] = Field(default=None, alias="workloadStatusAfterAssignment")
    monthly_workload_details: list[MonthlyWorkloadDetail] = Field(default_factory=list, alias="monthlyWorkloadDetails")
    final_ranking_score: float = Field(alias="finalRankingScore")
    risk_flags: list[str] = Field(default_factory=list, alias="riskFlags")
    previous_lead_count: int = Field(default=0, alias="previousLeadCount")
    lead_completion_rate: float = Field(default=0, alias="leadCompletionRate")
    domain_match: Literal["LOW", "MEDIUM", "HIGH"] = Field(default="LOW", alias="domainMatch")
    similar_project_count: int = Field(default=0, alias="similarProjectCount")
    leadership_score: Optional[float] = Field(default=None, alias="leadershipScore")
    domain_experience_score: Optional[float] = Field(default=None, alias="domainExperienceScore")
    performance_score: Optional[float] = Field(default=None, alias="performanceScore")
    workload_availability_score: Optional[float] = Field(default=None, alias="workloadAvailabilityScore")


class RecommendationExplanationRequest(WorkspaceScopedRequest):
    recommendation_type: Literal["INDIVIDUAL", "TEAM_LEADER", "TEAM_MEMBER"] = Field(alias="recommendationType")
    task: RecommendationTaskContext
    candidates: list[RankedCandidate]


class CandidateNumbers(StrictModel):
    final_ranking_score: float = Field(alias="finalRankingScore")
    skill_match_score: Optional[float] = Field(default=None, alias="skillMatchScore")
    role_suitability_score: Optional[float] = Field(default=None, alias="roleSuitabilityScore")
    similar_task_count: Optional[int] = Field(default=None, alias="similarTaskCount")
    completion_rate: Optional[float] = Field(default=None, alias="completionRate")
    overdue_rate: Optional[float] = Field(default=None, alias="overdueRate")
    current_monthly_hours: Optional[float] = Field(default=None, alias="currentMonthlyHours")
    monthly_capacity_hours: Optional[float] = Field(default=None, alias="monthlyCapacityHours")
    job_position_suitability_score: Optional[float] = Field(default=None, alias="jobPositionSuitabilityScore")
    leadership_score: Optional[float] = Field(default=None, alias="leadershipScore")
    domain_experience_score: Optional[float] = Field(default=None, alias="domainExperienceScore")
    performance_score: Optional[float] = Field(default=None, alias="performanceScore")
    workload_availability_score: Optional[float] = Field(default=None, alias="workloadAvailabilityScore")
    workload_usage_percentage: Optional[float] = Field(default=None, alias="workloadUsagePercentage")


class IndividualCandidateExplanation(StrictModel):
    rank: int
    employee_id: str = Field(alias="employeeId")
    full_name: str = Field(alias="fullName")
    recommendation_label: Literal["HIGHLY_RECOMMENDED", "RECOMMENDED", "CONSIDER_WITH_CAUTION", "NOT_RECOMMENDED"] = Field(alias="recommendationLabel")
    summary_reason: str = Field(alias="summaryReason")
    strengths: list[str]
    risks: list[str]
    numbers: CandidateNumbers


class IndividualRecommendationExplanationResponse(StrictModel):
    recommendation_type: Literal["INDIVIDUAL"] = Field(alias="recommendationType")
    task_summary: str = Field(alias="taskSummary")
    ranked_candidates: list[IndividualCandidateExplanation] = Field(alias="rankedCandidates")
    final_note: str = Field(alias="finalNote")


class LeadershipEvidence(StrictModel):
    previous_lead_count: int = Field(default=0, alias="previousLeadCount")
    lead_completion_rate: float = Field(default=0, alias="leadCompletionRate")
    domain_match: Literal["LOW", "MEDIUM", "HIGH"] = Field(alias="domainMatch")
    similar_project_count: int = Field(default=0, alias="similarProjectCount")


class LeaderCandidateExplanation(StrictModel):
    rank: int
    employee_id: str = Field(alias="employeeId")
    full_name: str = Field(alias="fullName")
    leader_recommendation_label: Literal["STRONG_LEADER", "SUITABLE_LEADER", "POSSIBLE_LEADER", "WEAK_LEADER"] = Field(alias="leaderRecommendationLabel")
    summary_reason: str = Field(alias="summaryReason")
    leadership_evidence: LeadershipEvidence = Field(alias="leadershipEvidence")
    strengths: list[str]
    risks: list[str]
    numbers: CandidateNumbers


class TeamLeaderRecommendationExplanationResponse(StrictModel):
    recommendation_type: Literal["TEAM_LEADER"] = Field(alias="recommendationType")
    task_or_project_domain: str = Field(alias="taskOrProjectDomain")
    leader_candidates: list[LeaderCandidateExplanation] = Field(alias="leaderCandidates")
    final_note: str = Field(alias="finalNote")


class MemberCandidateExplanation(StrictModel):
    rank: int
    employee_id: str = Field(alias="employeeId")
    full_name: str = Field(alias="fullName")
    member_recommendation_label: Literal["HIGHLY_SUITABLE", "SUITABLE", "CONSIDER_WITH_CAUTION", "NOT_SUITABLE"] = Field(alias="memberRecommendationLabel")
    summary_reason: str = Field(alias="summaryReason")
    strengths: list[str]
    risks: list[str]
    numbers: CandidateNumbers


class TeamMemberRecommendationExplanationResponse(StrictModel):
    recommendation_type: Literal["TEAM_MEMBER"] = Field(alias="recommendationType")
    task_summary: str = Field(alias="taskSummary")
    member_candidates: list[MemberCandidateExplanation] = Field(alias="memberCandidates")
    team_composition_advice: str = Field(alias="teamCompositionAdvice")


class RecommendationResultExplanationRequest(WorkspaceScopedRequest):
    task: dict[str, Any]
    selected_assignee_or_team: dict[str, Any] = Field(alias="selectedAssigneeOrTeam")
    ranking_data: list[dict[str, Any]] = Field(default_factory=list, alias="rankingData")
    comparison_with_other_candidates: list[dict[str, Any]] = Field(default_factory=list, alias="comparisonWithOtherCandidates")
    workload_data: dict[str, Any] = Field(default_factory=dict, alias="workloadData")
    performance_data: dict[str, Any] = Field(default_factory=dict, alias="performanceData")


class RecommendationResultExplanationResponse(StrictModel):
    explanation_title: str = Field(alias="explanationTitle")
    short_explanation: str = Field(alias="shortExplanation")
    detailed_explanation: str = Field(alias="detailedExplanation")
    key_reasons: list[str] = Field(alias="keyReasons")
    risk_warnings: list[str] = Field(alias="riskWarnings")
    data_used: list[str] = Field(alias="dataUsed")


class WorkloadRiskRequest(WorkspaceScopedRequest):
    employee_name: str = Field(alias="employeeName")
    monthly_capacity_hours: float = Field(alias="monthlyCapacityHours")
    monthly_workload_evaluation: list[MonthlyWorkloadDetail] = Field(alias="monthlyWorkloadEvaluation")
    backend_overall_risk: Optional[RiskLevel] = Field(default=None, alias="backendOverallRisk")


class WorkloadWarningNumbers(StrictModel):
    existing_hours: float = Field(alias="existingHours")
    new_task_hours: float = Field(alias="newTaskHours")
    total_hours: float = Field(alias="totalHours")
    capacity_hours: float = Field(alias="capacityHours")
    usage_percentage: float = Field(alias="usagePercentage")


class WorkloadMonthlyWarning(StrictModel):
    month: str
    status: str
    message: str
    numbers: WorkloadWarningNumbers


class WorkloadRiskResponse(StrictModel):
    overall_risk: RiskLevel = Field(alias="overallRisk")
    monthly_warnings: list[WorkloadMonthlyWarning] = Field(alias="monthlyWarnings")
    recommendation: str


class EmployeeReportRequest(WorkspaceScopedRequest):
    employee: dict[str, Any]
    period: dict[str, Any]
    metrics: dict[str, Any]
    notable_tasks: list[dict[str, Any]] = Field(default_factory=list, alias="notableTasks")
    risks: list[str] = Field(default_factory=list)


class EmployeeReportResponse(StrictModel):
    report_type: Literal["WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"] = Field(alias="reportType")
    employee_name: str = Field(alias="employeeName")
    period_summary: str = Field(alias="periodSummary")
    performance_evaluation: Literal["EXCELLENT", "GOOD", "STABLE", "NEEDS_ATTENTION", "RISKY"] = Field(alias="performanceEvaluation")
    key_metrics: dict[str, Any] = Field(alias="keyMetrics")
    strengths: list[str]
    issues: list[str]
    recommendations: list[str]


class BusinessOwnerOperationalSummaryRequest(WorkspaceScopedRequest):
    total_employees: int = Field(alias="totalEmployees")
    active_employees: int = Field(alias="activeEmployees")
    total_tasks: int = Field(alias="totalTasks")
    completed_tasks: int = Field(alias="completedTasks")
    overdue_tasks: int = Field(alias="overdueTasks")
    completion_rate: float = Field(alias="completionRate")
    overdue_rate: float = Field(alias="overdueRate")
    workload_distribution: dict[str, Any] = Field(default_factory=dict, alias="workloadDistribution")
    department_workload: list[dict[str, Any]] = Field(default_factory=list, alias="departmentWorkload")
    ai_recommendation_effectiveness: Optional[dict[str, Any]] = Field(default=None, alias="aiRecommendationEffectiveness")
    subscription_status: Optional[str] = Field(default=None, alias="subscriptionStatus")
    plan_limit_usage: Optional[dict[str, Any]] = Field(default=None, alias="planLimitUsage")
    expiration_date: Optional[str] = Field(default=None, alias="expirationDate")
    upgrade_options: list[str] = Field(default_factory=list, alias="upgradeOptions")


class BusinessOwnerOperationalSummaryResponse(StrictModel):
    summary_title: str = Field(alias="summaryTitle")
    business_health_label: Literal["GOOD", "STABLE", "NEEDS_ATTENTION", "RISK"] = Field(alias="businessHealthLabel")
    summary: str
    key_numbers: dict[str, Any] = Field(alias="keyNumbers")
    workload_insights: list[str] = Field(alias="workloadInsights")
    subscription_insights: list[str] = Field(alias="subscriptionInsights")
    risks: list[str]
    recommended_actions: list[str] = Field(alias="recommendedActions")


class PlatformAdminSystemSummaryRequest(StrictModel):
    total_workspaces: int = Field(alias="totalWorkspaces")
    active_workspaces: int = Field(alias="activeWorkspaces")
    suspended_workspaces: int = Field(alias="suspendedWorkspaces")
    expired_workspaces: int = Field(alias="expiredWorkspaces")
    new_workspaces_this_month: int = Field(alias="newWorkspacesThisMonth")
    revenue_by_month: dict[str, float] = Field(default_factory=dict, alias="revenueByMonth")
    revenue_by_quarter: dict[str, float] = Field(default_factory=dict, alias="revenueByQuarter")
    revenue_by_year: dict[str, float] = Field(default_factory=dict, alias="revenueByYear")
    revenue_by_plan: dict[str, float] = Field(default_factory=dict, alias="revenueByPlan")
    payment_success_rate: float = Field(alias="paymentSuccessRate")
    failed_payments: int = Field(alias="failedPayments")
    pending_manual_payments: int = Field(alias="pendingManualPayments")
    business_feedback_summary: dict[str, Any] = Field(default_factory=dict, alias="businessFeedbackSummary")
    ai_usage_statistics: dict[str, Any] = Field(default_factory=dict, alias="aiUsageStatistics")


class PlatformAdminSystemSummaryResponse(StrictModel):
    summary_title: str = Field(alias="summaryTitle")
    platform_status_label: Literal["HEALTHY", "STABLE", "NEEDS_ATTENTION", "RISK"] = Field(alias="platformStatusLabel")
    summary: str
    revenue_insights: list[str] = Field(alias="revenueInsights")
    workspace_insights: list[str] = Field(alias="workspaceInsights")
    payment_insights: list[str] = Field(alias="paymentInsights")
    feedback_insights: list[str] = Field(alias="feedbackInsights")
    risks: list[str]
    recommended_actions: list[str] = Field(alias="recommendedActions")

