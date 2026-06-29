import json
import os
import re
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

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
    if not isinstance(llm_output.get("recommendations"), list):
        raise RuntimeError("AI response missing recommendations.")
    parsed = _parse_recommendations(llm_output["recommendations"])
    if not parsed:
        raise RuntimeError("AI response contains no valid recommendations.")
    return parsed[:3]


def workload_summary(payload: WorkloadSummaryRequest) -> WorkloadSummaryResponse:
    llm_output = _ask_llm_json(
        task=(
            "Summarize employee workload in Vietnamese without accents. "
            "Return JSON only with keys summary, overloadedEmployees, idleEmployees, overdueEmployees."
        ),
        data=payload.model_dump(by_alias=True),
    )
    return WorkloadSummaryResponse(**llm_output)


def delay_risks(payload: DelayRiskRequest) -> list[DelayRisk]:
    llm_output = _ask_llm_json(
        task=(
            "Detect task delay risks. Return JSON only with key risks. "
            "Each item must include taskId, title, riskLevel, reason, recommendedAction. "
            "riskLevel should be LOW, MEDIUM, or HIGH."
        ),
        data=payload.model_dump(by_alias=True),
    )
    if not isinstance(llm_output.get("risks"), list):
        raise RuntimeError("AI response missing risks.")
    return _parse_delay_risks(llm_output["risks"])


def daily_summary(payload: DailySummaryRequest) -> DailySummaryResponse:
    llm_output = _ask_llm_json(
        task=(
            "Write a short Vietnamese business daily summary without accents. "
            "Return JSON only with key summary."
        ),
        data=payload.model_dump(by_alias=True),
    )
    return DailySummaryResponse(**llm_output)


def _ask_llm_json(task: str, data: dict[str, Any]) -> dict[str, Any]:
    prompt = (
        f"{task}\n\n"
        "Input JSON:\n"
        f"{json.dumps(data, ensure_ascii=True)}\n\n"
        "Return valid JSON only. No markdown, no explanation."
    )
    errors: list[str] = []
    for caller in (_call_gemini, _call_groq):
        try:
            raw = caller(prompt)
            if raw:
                return _load_json(raw)
        except Exception as exception:
            errors.append(f"{caller.__name__}: {exception}")
            continue
    raise RuntimeError("All AI providers failed. " + " | ".join(errors))


def _call_gemini(prompt: str) -> str | None:
    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    if not api_key:
        raise RuntimeError("GEMINI_API_KEY is missing.")
    model = os.getenv("GEMINI_MODEL", "gemini-3.5-flash").strip() or "gemini-3.5-flash"
    body = _post_json(
        GEMINI_API_URL.format(model=model),
        payload={
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": 0.2,
                "responseMimeType": "application/json",
            },
        },
        query={"key": api_key},
    )
    return body["candidates"][0]["content"]["parts"][0]["text"]


def _call_groq(prompt: str) -> str | None:
    api_key = os.getenv("GROQ_API_KEY", "").strip()
    if not api_key:
        raise RuntimeError("GROQ_API_KEY is missing.")
    model = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile").strip() or "llama-3.3-70b-versatile"
    body = _post_json(
        GROQ_API_URL,
        headers={"Authorization": f"Bearer {api_key}"},
        payload={
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
    )
    return body["choices"][0]["message"]["content"]


def _post_json(
    url: str,
    payload: dict[str, Any],
    headers: dict[str, str] | None = None,
    query: dict[str, str] | None = None,
) -> dict[str, Any]:
    if query:
        url = f"{url}?{urlencode(query)}"
    request_headers = {"Content-Type": "application/json", **(headers or {})}
    request = Request(
        url=url,
        data=json.dumps(payload).encode("utf-8"),
        headers=request_headers,
        method="POST",
    )
    try:
        with urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
            return json.loads(response.read().decode("utf-8"))
    except HTTPError as exception:
        error_body = exception.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exception.code}: {error_body}") from exception
    except URLError as exception:
        raise RuntimeError(f"Network error: {exception.reason}") from exception


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
