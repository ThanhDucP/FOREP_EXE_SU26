import os
from fastapi import Depends, FastAPI, Header, HTTPException

from app.schemas import DailySummaryRequest, DelayRiskRequest, RecommendAssigneeRequest, WorkloadSummaryRequest
from app.services import daily_summary, delay_risks, recommend_assignee, workload_summary

app = FastAPI(title="FOREP EXE AI Service", version="0.1.0")


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


@app.post("/internal/ai/voice/extract-tasks", dependencies=[Depends(verify_internal_token)])
def future_voice_endpoint():
    return {"status": "not_implemented", "message": "Voice xử lý ở giai đoạn sau MVP."}
