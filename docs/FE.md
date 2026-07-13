# FOREP Frontend Specification

## Roles

- `PLATFORM_ADMIN`: manages plans, workspace registrations, payment review, workspace activation, and owner account provisioning.
- `BUSINESS_OWNER`: manages workspace settings, employees, tasks, AI insights, reports, and dashboard views.
- `HR`: manages employee profiles, business positions, employee import, documents, and profile completeness.
- `MANAGER`: creates and manages tasks, assigns individual or team work, monitors workload, and consumes recommendation views.
- `EMPLOYEE`: views assigned individual tasks and team tasks where they are leader/member, updates progress, submits reports.

## AI History

AI history must be shown as a compact workspace-scoped list.

- Show function name, caller name, caller role, status, and call time.
- Sort newest first.
- Do not expose system-wide history inside workspace screens.
- Keep the action focused on the last executed AI function, not the raw prompt text.

## Workspace Task Creation

Task creation must support two assignment modes with a segmented control:

- `INDIVIDUAL`: select one employee. Submit `assignmentType=INDIVIDUAL` and `assigneeId`.
- `TEAM`: select one leader and multiple members. Submit `assignmentType=TEAM`, `teamLeaderId`, and `teamMemberIds`.

Validation:

- `estimatedHours` is required and must be greater than 0.
- Team leader is required for team assignment.
- Member duplicates must not be shown in the UI. Backend also deduplicates.
- The leader may appear in the member picker, but the final participant list must show that person once.
- Deadline is required. Start date is optional and used for calendar/workload allocation.

Expected task form fields:

- Title, requirements, description.
- Priority, start date, deadline, estimated working hours, difficulty.
- Required skills, required business position, task domain, project, department.
- Attachments with file name, URL, content type, size, and attachment type.

Primary APIs:

- `GET /api/workspace/tasks`
- `POST /api/workspace/tasks`
- `GET /api/workspace/tasks/{id}`
- `PUT /api/workspace/tasks/{id}`
- `PATCH /api/workspace/tasks/{id}/assign-individual`
- `PATCH /api/workspace/tasks/{id}/assign-team`
- `GET /api/workspace/tasks/{id}/attachments`
- `POST /api/workspace/tasks/{id}/attachments`

## AI Recommendations

Task assignment UI must provide tabs:

- Individual recommendation: `POST /api/workspace/ai/recommendations/individual`
- Team leader recommendation: `POST /api/workspace/ai/recommendations/team-leaders`
- Team member recommendation: `POST /api/workspace/ai/recommendations/team-members`

Recommendation cards should show:

- Employee name and score.
- Workload level.
- Role fit and role fit reason.
- Risk notes.
- Key matching skills and current estimated workload when available.
- Department and business-position match when the API returns them.
- For team leader and team member views, surface why department and business position were ranked before workload.

Do not use mock employee lists. Use workspace employees and real task/workload data returned by the API.

Recommendation logic should follow this order:

- Department match.
- Business-position match.
- Role and skill match.
- Similar task or leadership history.
- Workload and overdue risk.

## HR Module

HR navigation should include:

- Employee list and employee profile.
- Business position management.
- Employee import from Excel.
- Employee documents.
- Profile completeness and missing profile data.

Business position APIs:

- `GET /api/workspace/hr/job-positions`
- `POST /api/workspace/hr/job-positions`
- `PUT /api/workspace/hr/job-positions/{id}`
- `PATCH /api/workspace/hr/job-positions/{id}/status`

HR screens should treat `code`, `permissionGroup`, and `department` as first-class fields when creating or editing a business position.

Employee profile fields:

- Basic account: full name, email, phone, username, employee code, role, status.
- Work profile: job title, job position, department, seniority, employee level, employment type, working status.
- Capacity: monthly working capacity hours, default 168.
- Expertise: skills, main expertise, secondary expertise, skill rating, years of experience.
- CV-like data: date of birth, gender, address, personal summary, avatar file.
- Extended sections: skills, experiences, education, certifications, documents.

## Workload And Calendar

Monthly workload API:

- `GET /api/workspace/workload/monthly?year=2026&month=7`

Frontend must display five levels:

- `NO_WORK`: Rảnh rỗi.
- `LOW`: Thong thả.
- `NORMAL`: Đủ việc.
- `HIGH`: Cao tải.
- `OVERLOADED`: Quá tải.

Calculation expectations:

- Default monthly capacity is 168 hours.
- Task hours are split across participants.
- Task hours are allocated by working days between start date and deadline.
- Saturdays and Sundays are excluded.
- Employee calendar should show assigned tasks, team role, task status, deadline, allocated hours, and overload warning.

## Dashboards

Business Owner dashboard should prioritize:

- Total/open/completed/overdue tasks.
- Monthly workload chart with the four workload labels.
- Overloaded employees and idle employees.
- Recent tasks and AI recommendations based on real workspace data.

Business Owner views should also expose the generated owner account count and when owner provisioning completed for each workspace.

Platform Admin dashboard should prioritize:

- Workspace registrations.
- Payment status.
- Active/suspended/expired workspaces.
- Subscription plan usage.
- Operational feedback and review queue.
- Workspace owner provisioning status and generated initial account credentials when applicable.
