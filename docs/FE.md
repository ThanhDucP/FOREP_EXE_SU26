# FOREP Frontend Specification

## Roles

- `PLATFORM_ADMIN`: manages plans, workspace registrations, payment review, platform dashboards.
- `BUSINESS_OWNER`: manages workspace settings, employees, tasks, AI insights, reports, dashboards.
- `HR`: manages employee profiles, job positions, employee import, documents, and profile completeness.
- `MANAGER`: creates and manages tasks, assigns individual or team work, monitors workload.
- `EMPLOYEE`: views assigned individual tasks and team tasks where they are leader/member, updates progress, submits reports.

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
- Required skills, required job position, task domain, project, department.
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

Do not use mock employee lists. Use workspace employees and real task/workload data returned by the API.

## HR Module

HR navigation should include:

- Employee list and employee profile.
- Job position management.
- Employee import from Excel.
- Employee documents.
- Profile completeness and missing profile data.

Job position APIs:

- `GET /api/workspace/hr/job-positions`
- `POST /api/workspace/hr/job-positions`
- `PUT /api/workspace/hr/job-positions/{id}`
- `PATCH /api/workspace/hr/job-positions/{id}/status`

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

Frontend must display four levels:

- `IDLE`: Rảnh rỗi.
- `LIGHT`: Thong thả.
- `FULL`: Đủ việc.
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

Platform Admin dashboard should prioritize:

- Workspace registrations.
- Payment status.
- Active/suspended/expired workspaces.
- Subscription plan usage.
- Operational feedback and review queue.
