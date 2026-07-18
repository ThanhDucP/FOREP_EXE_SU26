# FOREP EXE Backend API Implementation

Base path: `/api/v1`

Backend expose API theo mo hinh Workspace SaaS. Du lieu duoc luu bang Spring Data JPA/PostgreSQL repository.

Workspace operation screens should prefer `/api/workspace/...` aliases. `/api/v1/...` remains available for compatibility where controllers expose both.

## Authorization Model

Backend authorization is workflow-first:

`System Role -> role_permissions table -> PERMISSION_* authority -> endpoint permission -> service object/workspace isolation`

System roles are identity buckets only:

- `PLATFORM_ADMIN`: platform administration.
- `BUSINESS_OWNER`: workspace owner/business owner.
- `HR`: employee, department, and business position administration.
- `EXECUTIVE`: executive workspace operations/AI/workload visibility.
- `MANAGER`: task assignment, workload, and recommendation workflows.
- `EMPLOYEE`: assigned work, progress updates, and reports.
- `SYSTEM_ADMIN`, `OWNER`, `SYSTEM`: compatibility aliases for legacy rows.

Business/job positions are workspace master data, not system roles. Examples: Backend Java Developer, Business Analyst, HR Staff, Tech Lead. A business position carries `permissionGroup = EMPLOYEE | MANAGER | EXECUTIVE`; it never creates `PLATFORM_ADMIN`, `BUSINESS_OWNER`, or `HR`.

Runtime permission source:

- `role_permissions` stores enabled permissions per system role and is seeded by `V18__dynamic_role_permissions.sql`.
- JWT authentication expands a user's role into Spring authorities like `PERMISSION_TASK_ASSIGN`.
- `SecurityConfig` maps endpoint groups to permissions, not `hasRole`.
- `ForepService` keeps workspace/object guards after route permission passes.
- `POST /auth/login` returns `{ token, user, permissions }`.
- `GET /auth/me` returns `UserView` with `permissions`.
- FE must use `permissions`, not role names, for page/menu/button visibility.

Core permission categories:

- Public/guest flow: `PACKAGE_VIEW`, `WORKSPACE_REGISTER`, `PAYMENT_CREATE`, `PAYMENT_STATUS_VIEW`.
- Platform flow: `PACKAGE_MANAGE`, `WORKSPACE_MANAGE`, `PAYMENT_CONFIRM`, `PAYMENT_HISTORY_VIEW`, `PAYMENT_QR_MANAGE`, `REVENUE_VIEW`, `AUDIT_LOG_VIEW`, `SYSTEM_CONFIGURATION`.
- Workspace owner/HR flow: `WORKSPACE_VIEW`, `WORKSPACE_UPDATE`, `EMPLOYEE_*`, `DEPARTMENT_*`, `POSITION_*`, `ROLE_MANAGE`, `SUBSCRIPTION_*`.
- Manager/employee execution flow: `TASK_VIEW`, `TASK_CREATE`, `TASK_ASSIGN`, `TASK_APPROVE`, `TASK_UPDATE_OWN`, `REPORT_*`.
- AI flow: `AI_ANALYZE`, `AI_RECOMMENDATION`, `AI_SUMMARY`, `AI_HISTORY`.

## Auth

- POST `/auth/login`
- POST `/auth/logout`
- GET `/auth/me`

Login chap nhan email hoac username. Employee username duoc tao tu ten nhan vien + chu cai dau ho/ten dem + employeeCode; password ban dau bang employeeCode.

Auth response:

```json
{
  "token": "jwt",
  "user": {
    "role": "MANAGER",
    "permissions": ["TASK_VIEW", "TASK_ASSIGN"]
  },
  "permissions": ["TASK_VIEW", "TASK_ASSIGN"]
}
```

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
- PATCH `/tasks/{id}/accept`
- PATCH `/tasks/{id}/submit-completion`
- PATCH `/tasks/{id}/approve-completion`
- PATCH `/tasks/{id}/return`
- GET `/tasks/{id}/updates`
- POST `/tasks/{id}/updates`
- PATCH `/tasks/{id}/cancel`

Task workflow:

- `ASSIGNED`: assigned but not accepted.
- `ACCEPTED`: employee/participant accepted.
- `IN_PROGRESS`: work in progress.
- `BLOCKED`: employee reported blocker.
- `SUBMITTED`: employee submitted completion, waiting for manager confirmation.
- `RETURNED`: manager returned task for revision.
- `COMPLETED`: manager/executive/business owner approved completion.
- `CANCELLED`: business owner cancelled.

Employees cannot directly finalize `COMPLETED`. Use `submit-completion`, then task manager uses `approve-completion` or `return`.

## Daily Reports

- GET `/daily-reports`
- POST `/daily-reports`
- GET `/daily-reports/{id}`
- PATCH `/daily-reports/{id}/review`

## Analytics

- GET `/analytics/owner-dashboard`
- GET `/analytics/workload`
- GET `/analytics/employees/{id}/workload`
- GET `/api/workspace/business-owner/dashboard`

`/api/workspace/business-owner/dashboard` is the production workspace dashboard. Backend calculates chart/card-ready data: `overviewCards.today|week|month`, `dailyReportInsight`, `workloadInsight`, `deadlineRisks`, `blockedTasks`, `taskStatusChart`, `workloadDistributionChart`, `recommendedActions`, `recentlyUpdatedTasks`, `aiRecommendations`, and `metadata`. FE must render these values directly and must not ask AI to calculate task counts, overdue counts, missing reports, workload buckets, or completion rate.

`/analytics/owner-dashboard` remains legacy-compatible. Dashboard AI recommendations are read from cached `ai_suggestions` with status `GENERATED`; dashboard refresh does not call the LLM.

## Platform Admin Dashboard

New `/api/admin/dashboard/**` endpoints return backend-computed numbers and chart-ready `series`:

- GET `/api/admin/dashboard/overview`
- GET `/api/admin/dashboard/revenue/monthly`
- GET `/api/admin/dashboard/revenue/quarterly`
- GET `/api/admin/dashboard/revenue/yearly`
- GET `/api/admin/dashboard/revenue/by-plan`
- GET `/api/admin/dashboard/workspaces/by-status`
- GET `/api/admin/dashboard/workspaces/by-plan`
- GET `/api/admin/dashboard/payments/summary`
- GET `/api/admin/dashboard/feedback/summary`

Revenue endpoints return `{ title, series: [{ label, value, currency }], total, currency, metadata }`. Workspace/payment/feedback endpoints return labels, values, totals, percentages, and recent table rows where useful. FE must not calculate revenue, success rate, workspace status counts, or feedback averages from raw payment/workspace lists.

## Public Registration And Payment

Production public flow uses:

- GET `/api/public/subscription-plans`
- GET `/api/public/subscription-plans/{id}`
- POST `/api/public/workspace-registrations`
- PATCH `/api/public/workspace-registrations/{id}/select-plan?token={registrationToken}`
- POST `/api/public/workspace-registrations/{id}/payments?token={registrationToken}`
- GET `/api/public/payments/{paymentCode}/status?token={registrationToken}`
- POST `/api/payment-callbacks/momo`
- POST `/api/payment-callbacks/bank`
- GET `/api/admin/payment-qr-settings`
- PUT `/api/admin/payment-qr-settings/{paymentMethod}`

Rules:

- Public payment status is read by `paymentCode + registrationToken`; frontend success pages cannot activate workspaces.
- Public payment creation requires an enabled Platform Admin QR setting for the selected method. Missing QR returns a business-rule error asking the user to wait; backend does not create a payment transaction.
- QR for public users comes from `payment_qr_settings`, not frontend-generated QR and not fake third-party QR generation.
- Platform Admin can update `MOMO` and `BANK_TRANSFER` QR settings from UI. New payment transactions snapshot the latest QR/settings; old payment instructions are unchanged.
- Successful verified payment sets `PaymentTransaction.status=SUCCESS`, confirms registration payment, activates workspace, creates Business Owner accounts, and returns generated credentials only in the activation response.
- Final registration state after workspace/account provisioning is `ACTIVATED`.
- Activation also creates one `workspace_subscriptions` ACTIVE row. This row is the subscription snapshot for billing/audit: plan id, price, owner/employee limits, start/end/renewal dates, and optional payment transaction id.
- Platform workspace responses include `activeSubscription`. FE/admin screens should show current package, renewal date, and limits from `activeSubscription` when it exists; workspace columns remain compatibility fields.
- Admin plan changes close the previous ACTIVE subscription as `UPGRADED` or `DOWNGRADED` and create a new ACTIVE subscription snapshot.

MoMo provider mode:

- If `MOMO_SANDBOX_MODE=false` and all of `MOMO_PAYMENT_ENDPOINT`, `MOMO_PARTNER_CODE`, `MOMO_ACCESS_KEY`, `MOMO_SECRET_KEY`, `MOMO_RETURN_URL`, and `MOMO_NOTIFY_URL` are configured, backend calls the real MoMo create-payment endpoint with signed HMAC payload.
- If sandbox mode is true, or any required provider config is missing, backend returns sandbox instructions only and clearly marks the provider payload mode as sandbox/missing-config.
- FE behavior is identical in both modes: render `providerPaymentUrl`, `providerDeeplink`, and `providerQrCodeUrl` when present, then rely on backend public status polling for success.

Seed/demo data:

- `V16__demo_saas_operational_seed.sql` creates 3 active workspaces, 30 employees per workspace, departments, business positions, tasks, assignments, daily reports, workload rows, payment rows, subscription snapshots, AI history, cached AI suggestions, and feedback.
- Demo login pattern uses generated owner usernames such as `adminSV0001`, `adminMD0001`, `adminHC0001`; employee usernames use `svuser01`, `mduser01`, `hcuser01`; initial password is `123456` for seeded accounts.

## AI Integration

Backend-only integration. Frontend calls these backend endpoints; backend calls AI Service internally.

- POST `/ai/recommend-assignee`
- GET `/ai/workload-summary`
- GET `/ai/delay-risks`
- GET `/ai/daily-reports/insights`
- GET `/ai/daily-reports/missing`
- POST `/ai/tasks/extract`
- POST `/ai/tasks/analyze`
- POST `/ai/tasks/estimate-hours`
- POST `/ai/recommendations/explain`
- POST `/ai/recommendations/result/explain`
- POST `/ai/workload/risk`
- POST `/ai/employee-report`
- GET `/ai/business-owner/operational-summary`
- POST `/ai/tasks/{id}/split`
- POST `/ai/tasks/{id}/adjust`
- GET `/ai/action-suggestions`
- GET `/ai/suggestions`
- PATCH `/ai/suggestions/{id}/status?status=ACCEPTED|REJECTED`
- GET `/ai/business-summary/daily`
- GET `/ai/business-summary/weekly`
- GET `/ai/business-summary/monthly`
- GET `/api/admin/ai/platform-summary`

Current behavior:

- If `AI_SERVICE_URL` and `AI_SERVICE_TOKEN` are configured and AI service is reachable, backend calls AI service.
- Weekly/monthly business summary call LLM through `/internal/ai/business-summary`; they are not internal rule summaries.
- Assignee recommendation ranking/eligibility do backend tinh: chi employee ACTIVE, workspace-scoped, co `candidateScore` va `scoreComponents`; AI tu nhan dien `requiredRole`, `roleFit`, `roleFitReason` tu title/requirements va profile ung vien, sau do backend validate lai employeeId/fullName/workload/score theo candidate input.
- Task/domain analysis runs before recommendation when FE/backend lacks `departmentId`, `requiredJobPositionId`, `requiredSkills`, or `taskDomain`. Backend sends only real active departments, active business positions, and workspace skills to AI, then maps AI text output back to real workspace IDs before scoring.
- Estimate-hours, recommendation explanation, recommendation-result explanation, workload-risk explanation, employee-report draft, owner operational summary, and platform system summary are exposed through backend only. Backend enforces permission, AI quota, AI history, and suggestion persistence before calling AI Service.
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
