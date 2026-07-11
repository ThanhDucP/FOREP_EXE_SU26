# FOREP EXE AI Service

AI Service la service noi bo. Frontend khong duoc goi truc tiep service nay.

Luong dung:

```text
Frontend -> Backend API -> AI Service -> Gemini/Groq
```

Nguyen tac loi: Backend computes, LLM explains.

- Backend validate permission, workspace scope, subscription/payment rules, va input.
- Backend lay data that tu database va tinh deterministic metrics.
- AI Service chi nhan clean structured context tu backend.
- LLM chi phan tich text, giai thich recommendation, canh bao workload, va tom tat bao cao.
- LLM khong duoc assign task, sua data, query database truc tiep, hay quyet dinh nghiep vu cuoi cung.

## Chay local bang Docker

```bash
docker build -t forep-exe-ai:local .
docker run --rm -p 8000:8000 \
  -e AI_SERVICE_TOKEN=dev-internal-token \
  -e GEMINI_API_KEY=<key> \
  -e GROQ_API_KEY=<key> \
  forep-exe-ai:local
```

## Endpoint

Health:

- GET `/health`

Internal AI:

- POST `/internal/ai/recommend-assignee`
- POST `/internal/ai/tasks/analyze`
- POST `/internal/ai/tasks/estimate-hours`
- POST `/internal/ai/recommendations/individual/explain`
- POST `/internal/ai/recommendations/team-leader/explain`
- POST `/internal/ai/recommendations/team-member/explain`
- POST `/internal/ai/recommendations/result/explain`
- POST `/internal/ai/workload-risk`
- POST `/internal/ai/employee-report`
- POST `/internal/ai/business-owner/summary`
- POST `/internal/ai/platform-admin/summary`
- POST `/internal/ai/workload-summary`
- POST `/internal/ai/delay-risks`
- POST `/internal/ai/daily-summary`
- POST `/internal/ai/business-summary`
- POST `/internal/ai/daily-report-insights`
- POST `/internal/ai/tasks/extract`
- POST `/internal/ai/tasks/split`
- POST `/internal/ai/tasks/adjust`
- POST `/internal/ai/missing-reports`
- POST `/internal/ai/action-suggestions`
- POST `/internal/ai/voice/extract-tasks`

Moi internal endpoint yeu cau header:

```text
X-Internal-Service-Token: <AI_SERVICE_TOKEN>
```

## AI Behavior

Provider order duoc cau hinh bang `AI_PROVIDER_ORDER`, mac dinh `GEMINI,GROQ`. Service co phan loai loi provider:

- Gemini/Groq 429 hoac Gemini `RESOURCE_EXHAUSTED`: `AI_QUOTA_EXCEEDED`, co cooldown theo `Retry-After` neu parse duoc.
- Groq/Gemini 403, gom Groq code `1010`: `AI_PROVIDER_FORBIDDEN`, khong retry loop.
- Timeout that: `AI_PROVIDER_TIMEOUT`.
- JSON/provider response sai schema: `AI_INVALID_RESPONSE`.
- Network/service unavailable: `AI_PROVIDER_UNAVAILABLE`.

Neu provider bi quota/forbidden, service dat cooldown theo `provider + model + feature` de request sau khong spam provider trong thoi gian cooldown. `daily-report-insights` co cache/in-flight reuse theo payload voi TTL `AI_INSIGHT_CACHE_TTL_SECONDS`.

Neu ca hai provider deu fail, service tra loi co cau truc:

```json
{
  "code": "AI_PROVIDERS_UNAVAILABLE",
  "message": "All AI providers are currently unavailable.",
  "details": {
    "feature": "WEEKLY_SUMMARY",
    "providerErrors": [
      {
        "provider": "GEMINI",
        "model": "gemini-2.5-flash",
        "code": "AI_QUOTA_EXCEEDED",
        "statusCode": 429,
        "providerStatus": "RESOURCE_EXHAUSTED",
        "retryAfterSeconds": 15
      }
    ],
    "retryAfterSeconds": 15
  }
}
```

Config lien quan:

- `AI_PROVIDER_ORDER=GEMINI,GROQ`
- `AI_GEMINI_MODEL=gemini-2.5-flash`
- `AI_GROQ_MODEL=llama-3.3-70b-versatile`
- `AI_PROVIDER_TIMEOUT_SECONDS=10`
- `AI_PROVIDER_MAX_RETRIES=1`
- `AI_PROVIDER_COOLDOWN_SECONDS=60`
- `AI_INSIGHT_CACHE_TTL_SECONDS=600`

Global prompt rules:

- Chi dung input data; khong tu bia employee/task/report/date/metric.
- Output la JSON dung schema, khong markdown, khong field ngoai schema.
- Text reason/risk/action la tieng Viet nhat quan.
- Input list rong phai tra response rong hop le.
- Backend tinh employee eligibility/ranking/candidateScore; AI chi sinh reason/risk va bi validate lai theo candidate input.
- Recommendation uu tien `NO_WORK`, `LOW`, tranh `OVERLOADED`, va score phai giai thich duoc bang workload/risk.
- Action/task tool dung `confidence` trong khoang `0..1`.
