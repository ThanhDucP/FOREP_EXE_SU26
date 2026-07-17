# FOREP EXE Backend API Implementation

Base path: `/api/v1`

Backend expose API theo mo hinh Workspace SaaS. Du lieu duoc luu bang Spring Data JPA/PostgreSQL repository.

Workspace operation screens should prefer `/api/workspace/...` aliases. `/api/v1/...` remains available for compatibility where controllers expose both.

## Authorization Model

System roles are permission roles only:

- `PLATFORM_ADMIN`: platform administration.
- `BUSINESS_OWNER`: workspace owner/business owner.
- `HR`: employee, department, and business position administration.
- `EXECUTIVE`: executive workspace operations/AI/workload visibility.
- `MANAGER`: task assignment, workload, and recommendation workflows.
- `EMPLOYEE`: assigned work, progress updates, and reports.
- `SYSTEM_ADMIN`, `OWNER`, `SYSTEM`: compatibility aliases for legacy rows.

Business/job positions are workspace master data, not system roles. Examples: Backend Java Developer, Business Analyst, HR Staff, Tech Lead. A business position carries `permissionGroup = EMPLOYEE | MANAGER | EXECUTIVE`; it never creates `PLATFORM_ADMIN`, `BUSINESS_OWNER`, or `HR`.

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

Employee `role` is derived from the selected active business position's `permissionGroup`. If no business position is assigned, the employee defaults to `EMPLOYEE`.

## Departments And Business Positions

Use workspace aliases for new FE:

- GET `/api/workspace/hr/departments`
- POST `/api/workspace/hr/departments`
- GET `/api/workspace/hr/departments/{id}`
- PUT `/api/workspace/hr/departments/{id}`
- PATCH `/api/workspace/hr/departments/{id}/activate`
- PATCH `/api/workspace/hr/departments/{id}/deactivate`
- GET `/api/workspace/hr/business-positions?search=&departmentId=&permissionGroup=&status=`
- POST `/api/workspace/hr/business-positions`
- GET `/api/workspace/hr/business-positions/{id}`
- PUT `/api/workspace/hr/business-positions/{id}`
- PATCH `/api/workspace/hr/business-positions/{id}/activate`
- PATCH `/api/workspace/hr/business-positions/{id}/deactivate`

Production rules:

- Department name is unique per workspace; department code is unique per workspace when provided.
- Business position name is unique per workspace and department; business position code is unique per workspace when provided.
- Business positions, employee profiles, task requirement context, and AI mapping can only use active departments.
- Employee profiles and task requirement context can only use active business positions.
- Department deactivate is blocked while active business positions, active employees, or open tasks still reference it.
- Business position deactivate is blocked while active employees or open tasks still reference it.
- Legacy `/hr/job-positions` endpoints remain compatibility aliases; new UI should use `/hr/business-positions`.

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
- POST `/ai/tasks/analyze`
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
- Task/domain analysis runs before recommendation when FE/backend lacks `departmentId`, `requiredJobPositionId`, `requiredSkills`, or `taskDomain`. Backend sends only real active departments, active business positions, and workspace skills to AI, then maps AI text output back to real workspace IDs before scoring.
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

1. Add Supabase Storage file upload integration.
2. Add scheduled notification jobs for overdue/missing updates/missing reports.
3. Add automated tests for AI endpoint contracts.
4. Add broader integration tests for department/business-position lifecycle guards.
