# Authorization Audit And Business Redesign

## 0. Production Override 2026-07-21

- `BUSINESS_OWNER` owns workspace operations: task create/assign/approve/update, monthly workload, HR account creation/status, subscription/payment, workspace profile, and view-only employee/department/business position data.
- `BUSINESS_OWNER` must not mutate employee records, departments, business positions, role mappings, or platform/payment admin configuration.
- `HR` owns workforce master data: employee CRUD/import, departments, and business positions; HR must not assign/approve tasks, manage owner accounts, or manage subscription/payment.
- Operational AI action suggestions are removed. Do not expose `/api/v1/ai/action-suggestions`; keep AI assignee recommendation for task assignment.
- `/api/admin/audit-logs` supports filtering/pagination by `workspaceId`, `actorId`, `action`, `entityType`, `result`, `from`, `to`, `search`, `page`, and `size`.

## 1. Authorization Audit Report

### Current State

- Backend currently mixes business roles with endpoint authorization in `SecurityConfig`.
- JWT currently emits only `ROLE_*` authority, so route access is role-path based instead of permission-workflow based.
- Controllers do not use method annotations; authorization is centralized in `SecurityFilterChain` plus service-level role helper methods.
- Workspace isolation exists in service methods for tasks, reports, departments, job positions, employees, and workspace-scoped entities.
- Frontend source is not present in this repository, so FE authorization is specified as implementation requirements in docs.

### Current Risks

| Area | Current permission | Expected permission | Correct? | Business impact | Required modification |
|---|---|---|---|---|---|
| Public registration APIs | `permitAll` for `/api/public/**` and callbacks | Guest workflow permissions: `PACKAGE_VIEW`, `WORKSPACE_REGISTER`, `PAYMENT_CREATE`, `PAYMENT_STATUS_VIEW` without JWT | Mostly yes | Guest can complete onboarding; must not be redirected to login | Keep public, enforce registration token/payment code/provider verification in service |
| Legacy `/api/v1/workspace-registrations/**` and `/api/v1/payments/**` | Platform admin roles only | Public v1 registration should be migrated to `/api/public/**`; admin legacy actions need platform permissions | Partly | Legacy public clients may fail; admin actions are coarse | Prefer `/api/public/**`; map legacy admin operations by permission |
| `/api/admin/**` | `PLATFORM_ADMIN`/`SYSTEM_ADMIN` role | Platform permissions: `PACKAGE_MANAGE`, `WORKSPACE_MANAGE`, `PAYMENT_CONFIRM`, `PAYMENT_HISTORY_VIEW`, `REVENUE_VIEW`, `AUDIT_LOG_VIEW`, `PAYMENT_QR_MANAGE` | Partly | Admin can be over-granted by path wildcard | Replace with permission authorities by endpoint group |
| HR endpoints | Owner/HR role wildcard | `EMPLOYEE_*`, `DEPARTMENT_*`, `POSITION_*`, `ROLE_MANAGE` | Partly | Business owner/HR cannot be separated by action | Map read/write actions separately |
| Task endpoints | Owner/executive/manager/employee role wildcard for workspace tasks | `TASK_VIEW`, `TASK_CREATE`, `TASK_ASSIGN`, `TASK_APPROVE`, `TASK_UPDATE_OWN` | No | Employee sees path access too broad; relies only on service checks | Add permission gate by action and keep object ownership checks |
| AI endpoints | Owner/executive/manager/HR role wildcard | `AI_ANALYZE`, `AI_RECOMMENDATION`, `AI_SUMMARY`, `AI_HISTORY` | No | HR can reach AI summaries not aligned with HR workflow | Split AI permissions; HR excluded from operational AI unless explicitly granted |
| Owner dashboard | Owner/executive/manager/HR path group | Business owner/executive summary permission only | No | HR/manager may access owner-only business summary route | Gate by `AI_SUMMARY`/owner service check |
| Employee endpoints | Owner/HR role wildcard | `EMPLOYEE_VIEW`, `EMPLOYEE_CREATE`, `EMPLOYEE_UPDATE`, `EMPLOYEE_DEACTIVATE` | Partly | Cannot express least privilege per action | Split by HTTP method/action |
| Reports | Authenticated fallback + service checks | `REPORT_VIEW`, `REPORT_SUBMIT`, `REPORT_REVIEW`, `REPORT_EXPORT` | Partly | Some endpoints depend on generic auth path | Add explicit permission mapping |
| Notifications/profile | Authenticated fallback | `NOTIFICATION_VIEW`, profile self-access | Partly | Acceptable for own account but undocumented | Document and map explicitly |
| Workspace isolation | Service-scoped checks | Workspace-scoped object isolation | Mostly yes | Cross-workspace reads blocked by service; still needs tests | Keep and add manual tests |

## 2. Business Flow Report

| Actor | Business workflow | Must allow | Must deny |
|---|---|---|---|
| Guest | Landing/pricing/register/select plan/pay/check activation | Package view, workspace registration, QR payment creation, payment status | Dashboard, workspace internals, task, employee, AI, report APIs |
| Platform Admin | Operate SaaS platform | Plans, registrations, payments, QR settings, revenue, workspace lifecycle, feedback, audit, AI usage | Internal workspace employee/task/project management |
| Business Owner | Operate one business workspace | Dashboard, employees, managers, HR accounts, departments, positions, subscription, workspace payment history, AI summaries | Platform admin features, other workspaces |
| HR | Human resource operations | Employees, departments, business positions, profiles, imports/templates | Projects, task assignment, subscription, platform payments |
| Manager | Delivery operations | Projects/tasks, team assignment, AI recommendations/explanations, approve/reject work | HR administration, subscription, platform |
| Executive | Business oversight | Workspace dashboard, reports, AI summaries, workload | HR mutation unless granted, platform |
| Employee | Individual execution | Own tasks, own reports, own stats/profile | Other employees, HR, platform, subscription |
| System | Provider callbacks/scheduled jobs | Verified callbacks, scheduled maintenance | User-facing JWT access |

## 3. Permission Matrix

| Permission | Guest | Platform Admin | Business Owner | HR | Manager | Executive | Employee |
|---|---:|---:|---:|---:|---:|---:|---:|
| `PACKAGE_VIEW` | ✓ | ✓ |  |  |  |  |  |
| `PACKAGE_MANAGE` |  | ✓ |  |  |  |  |  |
| `WORKSPACE_REGISTER` | ✓ |  |  |  |  |  |  |
| `WORKSPACE_VIEW` |  | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `WORKSPACE_UPDATE` |  |  | ✓ |  |  |  |  |
| `WORKSPACE_MANAGE` |  | ✓ |  |  |  |  |  |
| `PAYMENT_CREATE` | ✓ |  |  |  |  |  |  |
| `PAYMENT_CONFIRM` |  | ✓ |  |  |  |  |  |
| `PAYMENT_STATUS_VIEW` | ✓ | ✓ | ✓ |  |  |  |  |
| `PAYMENT_HISTORY_VIEW` |  | ✓ | ✓ |  |  |  |  |
| `PAYMENT_QR_MANAGE` |  | ✓ |  |  |  |  |  |
| `SUBSCRIPTION_VIEW` |  | ✓ | ✓ |  |  | ✓ |  |
| `SUBSCRIPTION_RENEW` |  |  | ✓ |  |  |  |  |
| `SUBSCRIPTION_UPGRADE` |  |  | ✓ |  |  |  |  |
| `EMPLOYEE_VIEW` |  |  | ✓ | ✓ | ✓ | ✓ |  |
| `EMPLOYEE_CREATE` |  |  | ✓ | ✓ |  |  |  |
| `EMPLOYEE_UPDATE` |  |  | ✓ | ✓ |  |  |  |
| `EMPLOYEE_DEACTIVATE` |  |  | ✓ | ✓ |  |  |  |
| `DEPARTMENT_VIEW` |  |  | ✓ | ✓ | ✓ | ✓ |  |
| `DEPARTMENT_MANAGE` |  |  | ✓ | ✓ |  |  |  |
| `POSITION_VIEW` |  |  | ✓ | ✓ | ✓ | ✓ |  |
| `POSITION_MANAGE` |  |  | ✓ | ✓ |  |  |  |
| `ROLE_MANAGE` |  |  | ✓ | ✓ |  |  |  |
| `PROJECT_CREATE` |  |  | ✓ |  | ✓ |  |  |
| `PROJECT_UPDATE` |  |  | ✓ |  | ✓ |  |  |
| `TASK_VIEW` |  |  | ✓ |  | ✓ | ✓ | ✓ |
| `TASK_CREATE` |  |  | ✓ |  | ✓ |  |  |
| `TASK_ASSIGN` |  |  | ✓ |  | ✓ |  |  |
| `TASK_APPROVE` |  |  | ✓ |  | ✓ |  |  |
| `TASK_UPDATE_OWN` |  |  | ✓ |  | ✓ |  | ✓ |
| `AI_ANALYZE` |  |  | ✓ |  | ✓ | ✓ |  |
| `AI_RECOMMENDATION` |  |  | ✓ |  | ✓ |  |  |
| `AI_SUMMARY` |  | ✓ | ✓ |  |  | ✓ |  |
| `AI_HISTORY` |  | ✓ | ✓ |  | ✓ | ✓ |  |
| `REPORT_VIEW` |  |  | ✓ | ✓ | ✓ | ✓ | ✓ |
| `REPORT_SUBMIT` |  |  |  |  |  |  | ✓ |
| `REPORT_REVIEW` |  |  | ✓ | ✓ | ✓ |  |  |
| `REPORT_EXPORT` |  |  | ✓ | ✓ |  | ✓ |  |
| `AUDIT_LOG_VIEW` |  | ✓ |  |  |  |  |  |
| `SYSTEM_CONFIGURATION` |  | ✓ |  |  |  |  |  |
| `REVENUE_VIEW` |  | ✓ |  |  |  |  |  |
| `FEEDBACK_CREATE` |  |  | ✓ | ✓ | ✓ | ✓ | ✓ |
| `FEEDBACK_MANAGE` |  | ✓ |  |  |  |  |  |
| `NOTIFICATION_VIEW` |  |  | ✓ | ✓ | ✓ | ✓ | ✓ |

## 4. Endpoint Permission Mapping

### Public

| Endpoint | Expected authorization |
|---|---|
| `GET /api/public/subscription-plans` | Public, `PACKAGE_VIEW` workflow |
| `GET /api/public/subscription-plans/{id}` | Public, `PACKAGE_VIEW` workflow |
| `POST /api/public/workspace-registrations` | Public, registration token validation |
| `GET /api/public/workspace-registrations/{id}` | Public, registration token/status-safe response |
| `PATCH /api/public/workspace-registrations/{id}/select-plan` | Public, registration token validation |
| `PATCH /api/public/workspace-registrations/{id}/cancel` | Public, registration token validation |
| `POST /api/public/workspace-registrations/{id}/payments` | Public, QR setting required, registration token validation |
| `GET /api/public/payments/{paymentCode}/status` | Public, payment code validation |
| `POST /api/payment-callbacks/momo` | Public callback, provider verification |
| `POST /api/payment-callbacks/bank` | Public callback/manual bank verification |

### Platform Admin

| Endpoint group | Expected permissions |
|---|---|
| `/api/admin/subscription-plans/**`, `/api/v1/admin/subscription-plans/**` | `PACKAGE_MANAGE` |
| `/api/admin/workspace-registrations/**`, `/api/v1/admin/workspace-registrations/**` | `WORKSPACE_MANAGE` plus `PAYMENT_CONFIRM` for payment confirmation |
| `/api/admin/payments/**`, `/api/v1/admin/payments/**` | `PAYMENT_HISTORY_VIEW`, `PAYMENT_CONFIRM` |
| `/api/admin/payment-qr-settings/**` | `PAYMENT_QR_MANAGE` |
| `/api/admin/workspaces/**`, `/api/v1/admin/workspaces/**` | `WORKSPACE_MANAGE` |
| `/api/admin/business-feedback/**`, `/api/v1/admin/business-feedback/**` | `FEEDBACK_MANAGE` |
| `/api/admin/audit-logs` | `AUDIT_LOG_VIEW` |
| `/api/admin/dashboard/**` | `REVENUE_VIEW` |
| `/api/admin/ai/platform-summary` | `AI_SUMMARY` |

### Workspace

| Endpoint group | Expected permissions |
|---|---|
| `/api/workspace/tasks` `GET`, `/api/v1/tasks` `GET` | `TASK_VIEW` and object visibility |
| `/api/workspace/tasks` `POST`, `/api/v1/tasks` `POST` | `TASK_CREATE` |
| `/api/workspace/tasks/{id}` `PUT`, `/api/v1/tasks/{id}` `PUT` | `TASK_ASSIGN` or task manager scope |
| `/api/workspace/tasks/{id}/assign*`, `/api/v1/tasks/{id}/assign*` | `TASK_ASSIGN` |
| `/api/workspace/tasks/{id}/approve-completion`, `/api/v1/tasks/{id}/approve-completion` | `TASK_APPROVE` |
| `/api/workspace/tasks/{id}/return`, `/api/workspace/tasks/{id}/cancel`, `/api/v1/tasks/{id}/return`, `/api/v1/tasks/{id}/cancel` | `TASK_APPROVE` |
| `/api/workspace/tasks/{id}/accept`, `/api/workspace/tasks/{id}/submit-completion`, `/api/workspace/tasks/{id}/progress`, `/api/workspace/tasks/{id}/updates`, `/api/v1/tasks/{id}/accept`, `/api/v1/tasks/{id}/submit-completion`, `/api/v1/tasks/{id}/progress`, `/api/v1/tasks/{id}/updates` | `TASK_UPDATE_OWN` and participant visibility |
| `/api/workspace/hr/departments/**`, `/api/v1/hr/departments/**` | `DEPARTMENT_VIEW` for GET, `DEPARTMENT_MANAGE` for writes |
| `/api/workspace/hr/job-positions/**`, `/api/workspace/hr/business-positions/**`, `/api/v1/hr/job-positions/**` | `POSITION_VIEW` for GET, `POSITION_MANAGE` for writes |
| `/api/v1/employees/**` | `EMPLOYEE_VIEW`, `EMPLOYEE_CREATE`, `EMPLOYEE_UPDATE`, `EMPLOYEE_DEACTIVATE` by action |
| `/api/workspace/ai/recommendations/**`, `/api/v1/ai/recommend-*` | `AI_RECOMMENDATION` |
| `/api/workspace/ai/tasks/**`, `/api/v1/ai/tasks/**`, `/api/v1/ai/workload/risk` | `AI_ANALYZE` |
| `/api/workspace/ai/business-owner/**`, `/api/workspace/business-owner/**`, `/api/v1/analytics/owner-dashboard`, `/api/v1/ai/business-summary/**` | `AI_SUMMARY` |
| `/api/workspace/ai-history`, `/api/v1/ai/suggestions/**`, `/api/v1/ai/action-suggestions` | `AI_HISTORY` |
| `/api/workspace/workload/**`, `/api/v1/analytics/workload/**` | `REPORT_VIEW` or `AI_SUMMARY` for aggregate |
| `/api/v1/daily-reports` `GET`, `/api/v1/daily-reports/{id}` `GET` | `REPORT_VIEW` and ownership |
| `/api/v1/daily-reports` `POST` | `REPORT_SUBMIT` |
| `/api/v1/daily-reports/{id}/review` | `REPORT_REVIEW` |
| `/api/v1/workspaces/current` | `WORKSPACE_VIEW` for GET, `WORKSPACE_UPDATE` for PUT |
| `/api/v1/business-feedback`, `/api/workspace/feedback` | `FEEDBACK_CREATE` |
| `/api/v1/notifications/**` | `NOTIFICATION_VIEW` |

## 5. Frontend Permission Mapping

- Guest routes must never use `RequireAuth`: landing, pricing, package detail, workspace registration, plan selection, payment QR, payment status, activation result.
- Authenticated shell must load `user.permissions` from login/me response or decode permission authorities only as a fallback.
- Sidebar items must use `hasPermission()`:
  - Platform admin: plans, registrations, payments, QR settings, workspaces, revenue, feedback, audit.
  - Business owner: dashboard, employees, departments, positions, tasks, AI summaries, subscription/payment history.
  - HR: employees, departments, business positions, profiles.
  - Manager: projects/tasks, task assignment, AI recommendation/explanation, project dashboard.
  - Employee: my tasks, submit report, my profile, notifications.
- Buttons/dialogs must be permission-gated, not role-gated:
  - Create/update plan: `PACKAGE_MANAGE`
  - Confirm/reject payment: `PAYMENT_CONFIRM`
  - Update payment QR: `PAYMENT_QR_MANAGE`
  - Create employee: `EMPLOYEE_CREATE`
  - Disable employee: `EMPLOYEE_DEACTIVATE`
  - Create/assign task: `TASK_CREATE`/`TASK_ASSIGN`
  - Approve task: `TASK_APPROVE`
  - Submit own work: `TASK_UPDATE_OWN`
  - AI recommendation: `AI_RECOMMENDATION`
  - Owner summary: `AI_SUMMARY`

## 6. Security Test Checklist

- Guest can complete package → registration → select plan → create QR payment → status without JWT.
- Guest receives 401/403 for dashboard, workspace, task, employee, AI, and report endpoints.
- Platform admin can manage SaaS platform but receives 403 for workspace task/employee/HR mutation APIs.
- Business owner can operate only own workspace and cannot access `/api/admin/**`.
- HR can manage employees/departments/positions but cannot call task assignment, subscription, payment confirmation, or AI summary endpoints.
- Manager can create/assign/approve tasks and call AI recommendation, but cannot manage HR or subscription.
- Employee can view/update only own tasks/reports and cannot view other employee data.
- Cross-workspace IDs return 404/403 consistently.
- JWT expired/invalid signature returns unauthenticated.
- Callback endpoints require provider/payment verification; no JWT requirement.

## 7. Remaining Risks To Track

- Legacy `/api/v1/**` endpoints duplicate public/admin/workspace behavior; FE should move to `/api/public`, `/api/admin`, and `/api/workspace`.
- Service helper names are still role-oriented in some areas and should be gradually renamed to permission-oriented helper names.
- No FE source exists in this repository, so frontend compliance must be implemented in the actual FE project using this document and `docs/FE.md`.
