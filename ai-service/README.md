# FOREP EXE AI Service

AI Service la service noi bo. Frontend khong duoc goi truc tiep service nay.

Luong dung:

```text
Frontend -> Backend API -> AI Service -> Gemini/Groq
```

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

Service goi Gemini truoc, neu fail thi fallback sang Groq. Neu ca hai provider deu fail, service tra:

```json
{
  "code": "AI_PROVIDER_ERROR",
  "message": "Gemini and Groq both failed",
  "details": {
    "feature": "WEEKLY_SUMMARY",
    "providersAttempted": ["GEMINI", "GROQ"]
  }
}
```

Global prompt rules:

- Chi dung input data; khong tu bia employee/task/report/date/metric.
- Output la JSON dung schema, khong markdown, khong field ngoai schema.
- Text reason/risk/action la tieng Viet nhat quan.
- Input list rong phai tra response rong hop le.
- Backend tinh employee eligibility/ranking/candidateScore; AI chi sinh reason/risk va bi validate lai theo candidate input.
- Recommendation uu tien `NO_WORK`, `LOW`, tranh `OVERLOADED`, va score phai giai thich duoc bang workload/risk.
- Action/task tool dung `confidence` trong khoang `0..1`.
