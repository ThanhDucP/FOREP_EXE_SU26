# FOREP EXE Backend API Implementation

Base path: `/api/v1`

Backend hiện đã expose API MVP theo mô hình Workspace + OWNER + EMPLOYEE. Dữ liệu hiện đang in-memory để validate luồng nghiệp vụ nhanh; bước kế tiếp là thay bằng PostgreSQL repository.

## Auth

- POST `/auth/login`
- POST `/auth/logout`
- GET `/auth/me`

## Workspace

- POST `/workspaces/register`
- GET `/workspaces/current`
- PUT `/workspaces/current`

## Employees

- GET `/employees`
- POST `/employees`
- GET `/employees/{id}`
- PUT `/employees/{id}`
- PATCH `/employees/{id}/status?status=ACTIVE|INACTIVE|INVITED`

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

## AI Integration

Backend-only integration. Frontend calls these backend endpoints; backend calls AI Service internally.

- POST `/ai/recommend-assignee`
- GET `/ai/workload-summary`
- GET `/ai/delay-risks`
- GET `/ai/business-summary/daily`
- GET `/ai/business-summary/weekly`
- GET `/ai/business-summary/monthly`

Current behavior:

- If `AI_SERVICE_URL` and `AI_SERVICE_TOKEN` are configured and AI service is reachable, backend calls AI service.
- If AI service is offline, backend returns rule-based fallback so the MVP remains usable.

## Notifications

- GET `/notifications`
- PATCH `/notifications/{id}/read`
- PATCH `/notifications/read-all`

Notifications are created for:

- Task assigned.
- Task reassigned.
- Task completed.
- Task cancelled.
- Task blocked.

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

1. Replace in-memory store with PostgreSQL entities and repositories.
2. Implement real JWT issuance and validation.
3. Add password hashing.
4. Add workspace-scoped authorization middleware.
5. Add audit logs.
6. Add Supabase Storage file upload integration.
7. Add scheduled notification jobs for overdue/missing updates/missing reports.

