import os

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.responses import JSONResponse

from app.schemas import (
    ActionSuggestionsRequest,
    BusinessSummaryRequest,
    BusinessOwnerOperationalSummaryRequest,
    DailyReportInsightsRequest,
    DailySummaryRequest,
    DelayRiskRequest,
    EmployeeReportRequest,
    EstimatedHoursRequest,
    ExtractTasksRequest,
    MissingReportsRequest,
    PlatformAdminSystemSummaryRequest,
    RecommendAssigneeRequest,
    RecommendationExplanationRequest,
    RecommendationResultExplanationRequest,
    SplitTaskRequest,
    TaskAdjustmentRequest,
    TaskDescriptionAnalysisRequest,
    WorkloadRiskRequest,
    WorkloadSummaryRequest,
)
from app.services import (
    AiProviderError,
    action_suggestions,
    analyze_task_description,
    business_summary,
    daily_report_insights,
    daily_summary,
    delay_risks,
    explain_individual_recommendation,
    explain_recommendation_result,
    explain_team_leader_recommendation,
    explain_team_member_recommendation,
    explain_workload_risk,
    extract_tasks,
    generate_employee_report,
    log_provider_configuration,
    missing_reports,
    recommend_assignee,
    suggest_estimated_hours,
    summarize_business_owner_operations,
    summarize_platform_admin_system,
    split_task,
    task_adjustment,
    workload_summary,
)

app = FastAPI(title="FOREP EXE AI Service", version="0.1.0")


@app.exception_handler(AiProviderError)
def ai_provider_error_handler(_, exception: AiProviderError):
    details = {
        "feature": exception.feature,
        "providerErrors": exception.provider_errors,
    }
    if exception.retry_after_seconds is not None:
        details["retryAfterSeconds"] = exception.retry_after_seconds
    return JSONResponse(
        status_code=exception.http_status,
        content={
            "code": exception.code,
            "message": str(exception),
            "details": details,
        },
    )


@app.on_event("startup")
def startup_log_provider_configuration() -> None:
    log_provider_configuration()


def verify_internal_token(x_internal_service_token: str | None = Header(default=None)) -> None:
    expected = os.getenv("AI_SERVICE_TOKEN", "dev-internal-token")
    if x_internal_service_token != expected:
        raise HTTPException(status_code=401, detail="Invalid internal service token")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/internal/ai/recommend-assignee", dependencies=[Depends(verify_internal_token)])
def recommend_assignee_endpoint(payload: RecommendAssigneeRequest):
    return {"recommendations": [item.model_dump(by_alias=True) for item in recommend_assignee(payload)]}


@app.post("/internal/ai/tasks/analyze", dependencies=[Depends(verify_internal_token)])
def analyze_task_description_endpoint(payload: TaskDescriptionAnalysisRequest):
    return analyze_task_description(payload).model_dump(by_alias=True)


@app.post("/internal/ai/tasks/estimate-hours", dependencies=[Depends(verify_internal_token)])
def suggest_estimated_hours_endpoint(payload: EstimatedHoursRequest):
    return suggest_estimated_hours(payload).model_dump(by_alias=True)


@app.post("/internal/ai/recommendations/individual/explain", dependencies=[Depends(verify_internal_token)])
def explain_individual_recommendation_endpoint(payload: RecommendationExplanationRequest):
    return explain_individual_recommendation(payload).model_dump(by_alias=True)


@app.post("/internal/ai/recommendations/team-leader/explain", dependencies=[Depends(verify_internal_token)])
def explain_team_leader_recommendation_endpoint(payload: RecommendationExplanationRequest):
    return explain_team_leader_recommendation(payload).model_dump(by_alias=True)


@app.post("/internal/ai/recommendations/team-member/explain", dependencies=[Depends(verify_internal_token)])
def explain_team_member_recommendation_endpoint(payload: RecommendationExplanationRequest):
    return explain_team_member_recommendation(payload).model_dump(by_alias=True)


@app.post("/internal/ai/recommendations/result/explain", dependencies=[Depends(verify_internal_token)])
def explain_recommendation_result_endpoint(payload: RecommendationResultExplanationRequest):
    return explain_recommendation_result(payload).model_dump(by_alias=True)


@app.post("/internal/ai/workload-risk", dependencies=[Depends(verify_internal_token)])
def explain_workload_risk_endpoint(payload: WorkloadRiskRequest):
    return explain_workload_risk(payload).model_dump(by_alias=True)


@app.post("/internal/ai/employee-report", dependencies=[Depends(verify_internal_token)])
def generate_employee_report_endpoint(payload: EmployeeReportRequest):
    return generate_employee_report(payload).model_dump(by_alias=True)


@app.post("/internal/ai/business-owner/summary", dependencies=[Depends(verify_internal_token)])
def business_owner_operational_summary_endpoint(payload: BusinessOwnerOperationalSummaryRequest):
    return summarize_business_owner_operations(payload).model_dump(by_alias=True)


@app.post("/internal/ai/platform-admin/summary", dependencies=[Depends(verify_internal_token)])
def platform_admin_system_summary_endpoint(payload: PlatformAdminSystemSummaryRequest):
    return summarize_platform_admin_system(payload).model_dump(by_alias=True)


@app.post("/internal/ai/workload-summary", dependencies=[Depends(verify_internal_token)])
def workload_summary_endpoint(payload: WorkloadSummaryRequest):
    return workload_summary(payload).model_dump(by_alias=True)


@app.post("/internal/ai/delay-risks", dependencies=[Depends(verify_internal_token)])
def delay_risks_endpoint(payload: DelayRiskRequest):
    return delay_risks(payload).model_dump(by_alias=True)


@app.post("/internal/ai/daily-summary", dependencies=[Depends(verify_internal_token)])
def daily_summary_endpoint(payload: DailySummaryRequest):
    return daily_summary(payload).model_dump(by_alias=True)


@app.post("/internal/ai/business-summary", dependencies=[Depends(verify_internal_token)])
def business_summary_endpoint(payload: BusinessSummaryRequest):
    return business_summary(payload).model_dump(by_alias=True)


@app.post("/internal/ai/daily-report-insights", dependencies=[Depends(verify_internal_token)])
def daily_report_insights_endpoint(payload: DailyReportInsightsRequest):
    return daily_report_insights(payload).model_dump(by_alias=True)


@app.post("/internal/ai/tasks/extract", dependencies=[Depends(verify_internal_token)])
def extract_tasks_endpoint(payload: ExtractTasksRequest):
    return extract_tasks(payload).model_dump(by_alias=True)


@app.post("/internal/ai/tasks/split", dependencies=[Depends(verify_internal_token)])
def split_task_endpoint(payload: SplitTaskRequest):
    return split_task(payload).model_dump(by_alias=True)


@app.post("/internal/ai/tasks/adjust", dependencies=[Depends(verify_internal_token)])
def task_adjustment_endpoint(payload: TaskAdjustmentRequest):
    return task_adjustment(payload).model_dump(by_alias=True)


@app.post("/internal/ai/missing-reports", dependencies=[Depends(verify_internal_token)])
def missing_reports_endpoint(payload: MissingReportsRequest):
    return missing_reports(payload).model_dump(by_alias=True)


@app.post("/internal/ai/action-suggestions", dependencies=[Depends(verify_internal_token)])
def action_suggestions_endpoint(payload: ActionSuggestionsRequest):
    return action_suggestions(payload).model_dump(by_alias=True)


@app.post("/internal/ai/voice/extract-tasks", dependencies=[Depends(verify_internal_token)])
def future_voice_endpoint():
    return {"status": "not_implemented", "message": "Voice xu ly o giai doan sau MVP."}
