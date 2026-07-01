import os

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.responses import JSONResponse

from app.schemas import (
    ActionSuggestionsRequest,
    BusinessSummaryRequest,
    DailyReportInsightsRequest,
    DailySummaryRequest,
    DelayRiskRequest,
    ExtractTasksRequest,
    MissingReportsRequest,
    RecommendAssigneeRequest,
    SplitTaskRequest,
    TaskAdjustmentRequest,
    WorkloadSummaryRequest,
)
from app.services import (
    AiProviderError,
    action_suggestions,
    business_summary,
    daily_report_insights,
    daily_summary,
    delay_risks,
    extract_tasks,
    log_provider_configuration,
    missing_reports,
    recommend_assignee,
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


@app.post("/internal/ai/workload-summary", dependencies=[Depends(verify_internal_token)])
def workload_summary_endpoint(payload: WorkloadSummaryRequest):
    return workload_summary(payload).model_dump(by_alias=True)


@app.post("/internal/ai/delay-risks", dependencies=[Depends(verify_internal_token)])
def delay_risks_endpoint(payload: DelayRiskRequest):
    return {"risks": [item.model_dump(by_alias=True) for item in delay_risks(payload)]}


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
