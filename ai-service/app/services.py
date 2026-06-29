import json
import os
import re
from typing import Any

import httpx

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


GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
REQUEST_TIMEOUT_SECONDS = 20


def recommend_assignee(payload: RecommendAssigneeRequest) -> list[AssigneeRecommendation]:
    llm_output = _ask_llm_json(
        task=(
            "Recommend up to 3 best assignees for this task. "
            "Return JSON only with key recommendations. Each item must include "
            "employeeId, fullName, score integer 0-100, workloadLevel, reason, risk. "
            "Do not recommend employees with workloadLevel OVERLOADED unless no other employee exists."
        ),
        data=payload.model_dump(by_alias=True),
    )
    if llm_output and isinstance(llm_output.get("recommendations"), list):
        parsed = _parse_recommendations(llm_output["recommendations"])
        if parsed:
            return parsed[:3]
    return _rule_recommend_assignee(payload)


def workload_summary(payload: WorkloadSummaryRequest) -> WorkloadSummaryResponse:
    llm_output = _ask_llm_json(
        task=(
            "Summarize employee workload in Vietnamese without accents. "
            "Return JSON only with keys summary, overloadedEmployees, idleEmployees, overdueEmployees."
        ),
        data=payload.model_dump(by_alias=True),
    )
    if llm_output:
        try:
            return WorkloadSummaryResponse(**llm_output)
        except Exception:
            pass
    return _rule_workload_summary(payload)


def delay_risks(payload: DelayRiskRequest) -> list[DelayRisk]:
    llm_output = _ask_llm_json(
        task=(
            "Detect task delay risks. Return JSON only with key risks. "
            "Each item must include taskId, title, riskLevel, reason, recommendedAction. "
            "riskLevel should be LOW, MEDIUM, or HIGH."
        ),
        data=payload.model_dump(by_alias=True),
    )
    if llm_output and isinstance(llm_output.get("risks"), list):
        parsed = _parse_delay_risks(llm_output["risks"])
        if parsed:
            return parsed
    return _rule_delay_risks(payload)


def daily_summary(payload: DailySummaryRequest) -> DailySummaryResponse:
    llm_output = _ask_llm_json(
        task=(
            "Write a short Vietnamese business daily summary without accents. "
            "Return JSON only with key summary."
        ),
        data=payload.model_dump(by_alias=True),
    )
    if llm_output:
        try:
            return DailySummaryResponse(**llm_output)
        except Exception:
            pass
    return _rule_daily_summary(payload)


def _ask_llm_json(task: str, data: dict[str, Any]) -> dict[str, Any] | None:
    prompt = (
        f"{task}\n\n"
        "Input JSON:\n"
        f"{json.dumps(data, ensure_ascii=True)}\n\n"
        "Return valid JSON only. No markdown, no explanation."
    )
    for caller in (_call_gemini, _call_groq):
        try:
            raw = caller(prompt)
            if raw:
                return _load_json(raw)
        except Exception:
            continue
    return None


def _call_gemini(prompt: str) -> str | None:
    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    if not api_key:
        return None
    model = os.getenv("GEMINI_MODEL", "gemini-3.5-flash").strip() or "gemini-3.5-flash"
    response = httpx.post(
        GEMINI_API_URL.format(model=model),
        params={"key": api_key},
        json={
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": 0.2,
                "responseMimeType": "application/json",
            },
        },
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    body = response.json()
    return body["candidates"][0]["content"]["parts"][0]["text"]


def _call_groq(prompt: str) -> str | None:
    api_key = os.getenv("GROQ_API_KEY", "").strip()
    if not api_key:
        return None
    model = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile").strip() or "llama-3.3-70b-versatile"
    response = httpx.post(
        GROQ_API_URL,
        headers={"Authorization": f"Bearer {api_key}"},
        json={
            "model": model,
            "messages": [
                {
                    "role": "system",
                    "content": "You are a JSON-only API. Return valid JSON matching the requested schema.",
                },
                {"role": "user", "content": prompt},
            ],
            "temperature": 0.2,
            "response_format": {"type": "json_object"},
        },
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    body = response.json()
    return body["choices"][0]["message"]["content"]


def _load_json(raw: str) -> dict[str, Any]:
    try:
        loaded = json.loads(raw)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", raw, re.DOTALL)
        if not match:
            raise
        loaded = json.loads(match.group(0))
    if not isinstance(loaded, dict):
        raise ValueError("LLM output must be a JSON object")
    return loaded


def _parse_recommendations(items: list[Any]) -> list[AssigneeRecommendation]:
    parsed: list[AssigneeRecommendation] = []
    for item in items:
        if not isinstance(item, dict):
            continue
        try:
            parsed.append(AssigneeRecommendation(**item))
        except Exception:
            continue
    return sorted(parsed, key=lambda item: item.score, reverse=True)


def _parse_delay_risks(items: list[Any]) -> list[DelayRisk]:
    parsed: list[DelayRisk] = []
    for item in items:
        if not isinstance(item, dict):
            continue
        try:
            parsed.append(DelayRisk(**item))
        except Exception:
            continue
    return parsed


def _rule_recommend_assignee(payload: RecommendAssigneeRequest) -> list[AssigneeRecommendation]:
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
            "Dang ranh, phu hop de nhan task tiep theo."
            if employee.workload_level == "NO_WORK"
            else f"Hien co {employee.open_tasks} task dang mo va workload o muc {employee.workload_level}."
        )
        risk = "Co task qua han, can can nhac." if employee.overdue_tasks else "Khong co"
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


def _rule_workload_summary(payload: WorkloadSummaryRequest) -> WorkloadSummaryResponse:
    overloaded = [item.full_name for item in payload.employees if item.workload_level == "OVERLOADED"]
    idle = [item.full_name for item in payload.employees if item.workload_level == "NO_WORK"]
    overdue = [item.full_name for item in payload.employees if item.overdue_tasks > 0]
    summary = (
        f"Co {len(overloaded)} nhan vien qua tai, "
        f"{len(idle)} nhan vien dang ranh va "
        f"{len(overdue)} nhan vien co task qua han."
    )
    return WorkloadSummaryResponse(
        summary=summary,
        overloadedEmployees=overloaded,
        idleEmployees=idle,
        overdueEmployees=overdue,
    )


def _rule_delay_risks(payload: DelayRiskRequest) -> list[DelayRisk]:
    risks: list[DelayRisk] = []
    for task in payload.tasks:
        if task.overdue:
            risks.append(
                DelayRisk(
                    taskId=task.task_id,
                    title=task.title,
                    riskLevel="HIGH",
                    reason="Task da qua han.",
                    recommendedAction=f"Lien he {task.assignee_name} de cap nhat trang thai ngay.",
                )
            )
        elif task.progress_percent < 30:
            risks.append(
                DelayRisk(
                    taskId=task.task_id,
                    title=task.title,
                    riskLevel="MEDIUM",
                    reason="Tien do thap so voi deadline.",
                    recommendedAction="Yeu cau cap nhat tien do hoac chia nho task.",
                )
            )
    return risks


def _rule_daily_summary(payload: DailySummaryRequest) -> DailySummaryResponse:
    return DailySummaryResponse(
        summary=(
            f"Hom nay co {payload.completed_tasks} task hoan thanh, "
            f"{payload.overdue_tasks} task qua han, "
            f"{payload.overloaded_employees} nhan vien qua tai va "
            f"{payload.idle_employees} nhan vien dang ranh."
        )
    )
