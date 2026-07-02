# FOREP EXE Backend API Implementation

Base path: `/api/v1`

Backend expose API MVP theo mo hinh Workspace + OWNER + EMPLOYEE. Du lieu duoc luu bang Spring Data JPA/PostgreSQL repository.

## Auth

- POST `/auth/login`
- POST `/auth/logout`
- GET `/auth/me`

Login chap nhan email hoac username. Employee username duoc tao tu ten nhan vien + chu cai dau ho/ten dem + employeeCode; password ban dau bang employeeCode.

## Workspace

- POST `/workspace-registrations`
- PATCH `/workspace-registrations/{id}/payment`
- GET `/workspaces/current`
- PUT `/workspaces/current`

Workspace register bat buoc co `shortCode` gom 2 ky tu chu/so, vi day la tien to tao ma nhan vien.

## Employees

- GET `/employees`
- POST `/employees`
- GET `/employees/{id}`
- PUT `/employees/{id}`
- PATCH `/employees/{id}/status?status=ACTIVE|INACTIVE|INVITED`

Khi tao employee, backend tu sinh `employeeCode` dang `{shortCode}{0001..1000}`, `username`, `initialPassword`; moi workspace gioi han 1000 employee.

## Tasks

- GET `/tasks`
- POST `/tasks`
- GET `/tasks/{id}`
- PUT `/tasks/{id}`
- PATCH `/tasks/{id}/assign`
- PATCH `/tasks/{id}/status`
- PATCH `/tasks/{id}/progress`
- GET `/tasks/{id}/updates`
- POST `/tasks/{id}/updates`
- PATCH `/tasks/{id}/cancel`

## Daily Reports

- GET `/daily-reports`
- POST `/daily-reports`
- GET `/daily-reports/{id}`
- PATCH `/daily-reports/{id}/review`

## Analytics

- GET `/analytics/owner-dashboard`
- GET `/analytics/workload`
- GET `/analytics/employees/{id}/workload`

`/analytics/owner-dashboard` tra `aiRecommendations` tu cache `ai_suggestions` moi nhat co status `GENERATED` trong dung workspace. Dashboard khong goi LLM moi lan refresh, nen van tra KPI/workload nhanh va khong spam provider. Moi item dashboard co `{ suggestionId, type, source: "CACHE", outputData, createdAt }`.

## AI Integration

Backend-only integration. Frontend calls these backend endpoints; backend calls AI Service internally.

- POST `/ai/recommend-assignee`
- GET `/ai/workload-summary`
- GET `/ai/delay-risks`
- GET `/ai/daily-reports/insights`
- GET `/ai/daily-reports/missing`
- POST `/ai/tasks/extract`
- POST `/ai/tasks/{id}/split`
- POST `/ai/tasks/{id}/adjust`
- GET `/ai/action-suggestions`
- GET `/ai/suggestions`
- PATCH `/ai/suggestions/{id}/status?status=ACCEPTED|REJECTED`
- GET `/ai/business-summary/daily`
- GET `/ai/business-summary/weekly`
- GET `/ai/business-summary/monthly`

Current behavior:

- If `AI_SERVICE_URL` and `AI_SERVICE_TOKEN` are configured and AI service is reachable, backend calls AI service.
- Weekly/monthly business summary call LLM through `/internal/ai/business-summary`; they are not internal rule summaries.
- Assignee recommendation ranking/eligibility do backend tinh: chi employee ACTIVE, workspace-scoped, co `candidateScore` va `scoreComponents`; AI tu nhan dien `requiredRole`, `roleFit`, `roleFitReason` tu title/requirements va profile ung vien, sau do backend validate lai employeeId/fullName/workload/score theo candidate input.
- Backend enforce `aiUsageLimit` theo goi subscription hien tai bang so luong record `ai_suggestions` trong ky kich hoat workspace. Khi het quota, backend chan truoc khi goi AI.
- Backend protects AI providers with in-flight request dedupe, a global concurrency limiter, and a circuit breaker. Tunable env vars: `AI_SERVICE_MAX_CONCURRENT_REQUESTS`, `AI_SERVICE_ACQUIRE_TIMEOUT_MILLIS`, `AI_SERVICE_DEDUPE_WAIT_MILLIS`, `AI_SERVICE_RETRY_AFTER_SECONDS`, `AI_SERVICE_CIRCUIT_BREAKER_FAILURE_THRESHOLD`, `AI_SERVICE_CIRCUIT_BREAKER_OPEN_MILLIS`.
- `POST /ai/recommend-assignee` returns deterministic top-3 fallback from backend `candidateScore` if AI providers timeout/fail. Response shape stays the same; fallback is marked in each item's `reason`/`risk`.
- The AI cards `GET /ai/workload-summary`, `GET /ai/delay-risks`, `GET /ai/action-suggestions`, `GET /ai/daily-reports/insights`, and `GET /ai/daily-reports/missing` return deterministic rule-based fallback when AI providers fail. Fallback payload keeps the normal response shape and adds `source: "RULE_BASED_FALLBACK"`, `aiProviderFailed: true`, and `fallbackReason`.
- Other AI endpoints do not return mock/rule-based fallback. If AI capacity is exhausted, backend returns HTTP 429:

```json
{
  "data": null,
  "meta": {},
  "errors": [
    {
      "code": "AI_RATE_LIMITED",
      "message": "AI dang xu ly qua nhieu yeu cau. Vui long thu lai sau 15 giay.",
      "field": null
    }
  ]
}
```

- If AI providers fail, backend returns JSON error with HTTP 502:

```json
{
  "data": null,
  "meta": {},
  "errors": [
    {
      "code": "AI_PROVIDER_ERROR",
      "message": "Không thể tạo phân tích AI ở thời điểm này. Vui lòng thử lại sau.",
      "field": null
    }
  ]
}
```

## Notifications

- GET `/notifications`
- PATCH `/notifications/{id}/read`
- PATCH `/notifications/read-all`

Notifications are created for:

- Task assigned.
- Task reassigned.
- Task cancelled.
- Task blocked.
- Task overdue.
- Deadline soon.
- Missing daily report.

## Health

- GET `/health`

## Backend Environment Variables

- `SERVER_PORT`
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `AI_SERVICE_URL`
- `AI_SERVICE_TOKEN`
- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`
- `SUPABASE_STORAGE_BUCKET`
- `CORS_ALLOWED_ORIGINS`

## Remaining Backend Work

1. Add audit logs.
2. Add Supabase Storage file upload integration.
3. Add scheduled notification jobs for overdue/missing updates/missing reports.
4. Add automated tests for AI endpoint contracts.
