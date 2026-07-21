# FOREP Frontend Requirements

Tài liệu này là yêu cầu chi tiết cho FE mới. FE phải coi backend là source of truth, không tự suy diễn quyền, không tự tạo dữ liệu giả cho AI/recommendation, và không gọi trực tiếp AI Service.

## 1. Nguyên Tắc Không Được Sai

### 1.1 System Role Khác Business Position

System role là quyền thật sự trong hệ thống:

- `PLATFORM_ADMIN`: quản trị nền tảng, gói subscription, đăng ký workspace, payment review, workspace activation, owner account provisioning.
- `BUSINESS_OWNER`: chủ workspace, quản lý owner/HR accounts, giao task, duyệt task, xem workload, subscription/payment và dashboard; chỉ xem dữ liệu employee/department/business position.
- `HR`: quản lý nhân sự, hồ sơ nhân viên, phòng ban, business position/import; không quản lý task assignment, owner accounts, subscription/payment.
- `EXECUTIVE`: xem operation/workload/task/AI ở cấp điều hành; có quyền task manager theo backend service.
- `MANAGER`: quản lý task, giao việc, workload, AI recommendation.
- `EMPLOYEE`: xem task được giao, cập nhật tiến độ, gửi daily report.

Business Position / Job Position là dữ liệu nghiệp vụ trong workspace, không phải system role. Ví dụ: Backend Java Developer, Frontend React Developer, Business Analyst, HR Staff, HR Manager, Tech Lead, Accountant, Sales Staff.

FE tuyệt đối không gọi Developer, BA, HR Staff, Tech Lead là system role. Các vị trí này chỉ nằm trong Business Position.

### 1.2 PermissionGroup Của Business Position

Business Position có `permissionGroup` để map quyền cho nhân viên được gán vị trí:

- `EMPLOYEE`
- `MANAGER`
- `EXECUTIVE`

FE không được hiển thị `PLATFORM_ADMIN`, `BUSINESS_OWNER`, hoặc `HR` trong dropdown `permissionGroup` của Business Position.

Khi tạo/sửa employee:

- FE không gửi role thủ công.
- Backend tự set role theo `jobPositionId.permissionGroup`.
- Nếu không chọn `jobPositionId`, backend mặc định role `EMPLOYEE`.

### 1.3 Workspace API Prefix

Màn hình operation mới nên ưu tiên `/api/workspace/...`.

Các endpoint `/api/v1/...` còn tồn tại để tương thích nhưng không phải lựa chọn chính cho màn hình workspace mới.

### 1.4 Response Và Error Contract

Mọi response chuẩn:

```json
{
  "data": {},
  "meta": {},
  "errors": []
}
```

FE rule:

- Nếu `errors` khác rỗng, hiển thị `errors[0].message`.
- Nếu HTTP `401`, clear token và redirect login.
- Nếu HTTP `403` hoặc `BUSINESS_RULE_ERROR`, hiển thị message từ backend, không tự đổi rule ở FE.
- Disable nút khi request đang chạy để tránh double submit.
- Không show raw AI prompt, token, request/response body kỹ thuật cho user thường.

### 1.5 Authorization Bắt Buộc Theo Permission

Backend đã chuyển sang mô hình:

`Role -> Permission -> Endpoint -> Object/workspace check`

FE không được guard page/menu/button bằng role trực tiếp nữa, trừ redirect mặc định sau login. Sau `POST /api/v1/auth/login` và `GET /api/v1/auth/me`, `UserView` có field:

```ts
permissions: string[]
```

FE phải có helper duy nhất:

```ts
hasPermission('TASK_ASSIGN')
hasAnyPermission(['TASK_ASSIGN', 'TASK_APPROVE'])
```

Rule bắt buộc:

- Page route dùng `requiredPermissions`.
- Sidebar/menu dùng `hasPermission`.
- Button/dialog/action dùng `hasPermission`.
- Role chỉ dùng để hiển thị nhãn người dùng và redirect landing sau login.
- Nếu backend trả `403`/business-rule error, FE hiển thị message backend và không tự override.
- Guest flow pricing/register/payment/result tuyệt đối không bọc `RequireAuth`.
- Không decode token làm source of truth nếu API `me` đã trả `permissions`.

## 2. Permission Matrix Cho FE

Role matrix cũ chỉ dùng để hiểu nghiệp vụ. Runtime guard của FE phải theo permission dưới đây.

| Module/Action | Required permission |
|---|---|
| Platform admin dashboard | `REVENUE_VIEW` |
| Subscription plan management | `PACKAGE_MANAGE` |
| Workspace registration review | `WORKSPACE_MANAGE` |
| Payment review/history | `PAYMENT_HISTORY_VIEW` |
| Payment confirm/reject | `PAYMENT_CONFIRM` |
| Payment QR settings | `PAYMENT_QR_MANAGE` |
| Business feedback review | `FEEDBACK_MANAGE` |
| Audit logs | `AUDIT_LOG_VIEW` |
| Workspace owner dashboard | `AI_SUMMARY` |
| Workspace settings edit | `WORKSPACE_UPDATE` |
| Employee list/detail | `EMPLOYEE_VIEW` |
| Employee create | `EMPLOYEE_CREATE` |
| Employee update/reset password | `EMPLOYEE_UPDATE` |
| Employee deactivate | `EMPLOYEE_DEACTIVATE` |
| Department list/detail | `DEPARTMENT_VIEW` |
| Department create/update/activate/deactivate | `DEPARTMENT_MANAGE` |
| Business position list/detail | `POSITION_VIEW` |
| Business position create/update/activate/deactivate | `POSITION_MANAGE` |
| Task list/detail | `TASK_VIEW` |
| Task create | `TASK_CREATE` |
| Task assign/reassign | `TASK_ASSIGN` |
| Task approve/return/cancel | `TASK_APPROVE` |
| Task own progress/accept/submit/update | `TASK_UPDATE_OWN` |
| AI task analysis | `AI_ANALYZE` |
| AI recommendations/explanations | `AI_RECOMMENDATION` |
| AI business summaries | `AI_SUMMARY` |
| AI history/suggestions | `AI_HISTORY` |
| Daily report list/detail | `REPORT_VIEW` |
| Daily report submit | `REPORT_SUBMIT` |
| Daily report review | `REPORT_REVIEW` |
| Notifications | `NOTIFICATION_VIEW` |

FE phải hide navigation/action theo permission matrix, nhưng backend vẫn là lớp bảo vệ cuối.

## 3. App Layout Và Route Guard

### 3.1 Sau Login

`GET /api/v1/auth/me` trả `UserView` gồm `permissions`. FE dùng `user.role` chỉ để redirect mặc định sau login:

- `PLATFORM_ADMIN` hoặc legacy `SYSTEM_ADMIN`/`SYSTEM`: `/platform/dashboard`
- `BUSINESS_OWNER` hoặc legacy `OWNER`: `/owner/dashboard`
- `HR`: `/hr/employees`
- `EXECUTIVE`: `/operations/tasks`
- `MANAGER`: `/operations/tasks`
- `EMPLOYEE`: `/employee/home`

### 3.2 Navigation Theo Permission

Platform Admin:

- Dashboard: `REVENUE_VIEW`.
- Subscription Plans: `PACKAGE_MANAGE`.
- Workspace Registrations: `WORKSPACE_MANAGE`.
- Payments: `PAYMENT_HISTORY_VIEW`.
- Payment QR Settings: `PAYMENT_QR_MANAGE`.
- Workspaces: `WORKSPACE_MANAGE`.
- Business Feedback: `FEEDBACK_MANAGE`.
- Audit Logs: `AUDIT_LOG_VIEW`.

Business Owner:

- Dashboard: `AI_SUMMARY`.
- Tasks: `TASK_VIEW`, `TASK_CREATE`, `TASK_ASSIGN`, `TASK_APPROVE`, `TASK_UPDATE_OWN`.
- Workload: `REPORT_VIEW`.
- AI Center: `AI_RECOMMENDATION` cho gợi ý người nhận việc; không còn operational action suggestions.
- AI History: `AI_HISTORY`.
- Employees: `EMPLOYEE_VIEW`.
- Departments: `DEPARTMENT_VIEW`.
- Business Positions: `POSITION_VIEW`.
- HR Accounts: `HR_ACCOUNT_MANAGE`.
- Daily Reports: `REPORT_VIEW`.
- Notifications: `NOTIFICATION_VIEW`.
- Workspace Settings: `WORKSPACE_UPDATE`.
- Subscription/Upgrade: `SUBSCRIPTION_VIEW`, `SUBSCRIPTION_UPGRADE`, `SUBSCRIPTION_RENEW`.

HR:

- Employees: `EMPLOYEE_VIEW`, `EMPLOYEE_CREATE`, `EMPLOYEE_UPDATE`, `EMPLOYEE_DEACTIVATE`, `EMPLOYEE_IMPORT`.
- Departments: `DEPARTMENT_VIEW`, `DEPARTMENT_MANAGE`.
- Business Positions: `POSITION_VIEW`, `POSITION_MANAGE`.
- Daily Reports: `REPORT_VIEW`.
- Notifications: `NOTIFICATION_VIEW`.

Executive/Manager:

- Tasks: `TASK_VIEW`.
- Workload: `REPORT_VIEW`.
- AI Task Analysis: `AI_ANALYZE`.
- AI Recommendations: `AI_RECOMMENDATION`.
- AI History: `AI_HISTORY`.
- Daily Reports: `REPORT_VIEW`.
- Notifications: `NOTIFICATION_VIEW`.

Employee:

- My Tasks: `TASK_VIEW`.
- My Daily Reports: `REPORT_VIEW`/`REPORT_SUBMIT`.
- Notifications: `NOTIFICATION_VIEW`.
- Profile: authenticated user self-view.

## 4. Shared UI Components

### 4.1 Data Table

Table chuẩn cần có:

- Search/filter area.
- Loading skeleton.
- Empty state có mô tả rõ.
- Error state có retry.
- Row action menu.
- Status badge.
- Confirmation modal cho action deactivate/cancel/reject.

### 4.2 Form Drawer/Modal

Form chuẩn cần có:

- Required marker.
- Inline validation.
- Server error banner.
- Submit loading.
- Cancel/back confirmation nếu form dirty.
- Sau save thành công: toast + refetch list + đóng modal/drawer.

### 4.3 Select Dữ Liệu Master

Select Department:

- Lấy từ `GET /api/workspace/hr/departments`.
- Với create/edit dữ liệu mới, chỉ cho chọn `ACTIVE`.
- Với detail/history, vẫn hiển thị được `INACTIVE` nếu record cũ đang tham chiếu.

Select Business Position:

- Lấy từ `GET /api/workspace/hr/business-positions`.
- Với employee/task mới, chỉ cho chọn `ACTIVE`.
- Filter theo department nếu có.

## 5. HR Departments

Department là master data của workspace. Business Position, Employee, Task và AI mapping đều phải tham chiếu Department thật trong workspace.

### 5.1 APIs

- `GET /api/workspace/hr/departments`
- `POST /api/workspace/hr/departments`
- `GET /api/workspace/hr/departments/{id}`
- `PUT /api/workspace/hr/departments/{id}`
- `PATCH /api/workspace/hr/departments/{id}/activate`
- `PATCH /api/workspace/hr/departments/{id}/deactivate`

### 5.2 Request Body

```json
{
  "name": "Technology",
  "code": "TECH",
  "description": "Engineering and product technology",
  "status": "ACTIVE"
}
```

### 5.3 Response Fields

- `id`
- `workspaceId`
- `name`
- `code`
- `description`
- `status`: `ACTIVE | INACTIVE`
- `createdAt`
- `updatedAt`

### 5.4 List Screen

Columns:

- Department name.
- Code.
- Description preview.
- Status badge.
- Created at.
- Updated at.
- Actions.

Filters:

- Search by name/code client-side.
- Status: All / Active / Inactive.

Actions:

- View detail.
- Edit: `DEPARTMENT_MANAGE`.
- Activate: `DEPARTMENT_MANAGE`, show when status `INACTIVE`.
- Deactivate: `DEPARTMENT_MANAGE`, show when status `ACTIVE`.

### 5.5 Form Validation

- `name`: required, trim, max UI nên giới hạn 120 ký tự.
- `code`: optional, trim, uppercase trước khi gửi, max UI nên giới hạn 30 ký tự.
- `description`: optional, multiline.
- `status`: default `ACTIVE`.

### 5.6 Business Rules FE Phải Thể Hiện

- Department name unique trong workspace.
- Department code unique trong workspace nếu có.
- Không cho tạo Business Position mới bằng inactive department.
- Không cho gán employee/task mới vào inactive department.
- Deactivate department có thể fail nếu còn:
  - active business position,
  - active employee,
  - open task: `ASSIGNED`, `IN_PROGRESS`, `BLOCKED`.
- Khi deactivate fail, show message backend và gợi ý user xử lý phụ thuộc trước.

### 5.7 Acceptance Criteria

- HR tạo/sửa/activate/deactivate Department được khi có `DEPARTMENT_MANAGE`; Business Owner chỉ xem vì không có quyền này.
- User chỉ có `DEPARTMENT_VIEW` xem list/detail nhưng không thấy nút mutate.
- Department inactive vẫn hiển thị trong record cũ nhưng bị disable trong form mới.
- Deactivate có confirmation modal nêu rõ ảnh hưởng.

## 6. HR Business Positions

Business Position là Job Position nghiệp vụ. Đây là tên vị trí trong công ty, không phải system role.

### 6.1 APIs

- `GET /api/workspace/hr/business-positions?search=&departmentId=&permissionGroup=&status=`
- `POST /api/workspace/hr/business-positions`
- `GET /api/workspace/hr/business-positions/{id}`
- `PUT /api/workspace/hr/business-positions/{id}`
- `PATCH /api/workspace/hr/business-positions/{id}/activate`
- `PATCH /api/workspace/hr/business-positions/{id}/deactivate`

Không dùng endpoint legacy `/api/workspace/hr/job-positions` cho UI mới.

### 6.2 Request Body

```json
{
  "name": "Backend Java Developer",
  "code": "BE-JAVA",
  "permissionGroup": "EMPLOYEE",
  "departmentId": "uuid",
  "description": "Build and maintain backend services",
  "status": "ACTIVE"
}
```

### 6.3 Response Fields

- `id`
- `workspaceId`
- `name`
- `code`
- `permissionGroup`: `EMPLOYEE | MANAGER | EXECUTIVE`
- `departmentId`
- `departmentName`
- `description`
- `status`: `ACTIVE | INACTIVE`
- `createdAt`
- `updatedAt`

### 6.4 List Screen

Columns:

- Position name.
- Code.
- Department.
- Permission group.
- Status.
- Description preview.
- Created at.
- Updated at.
- Actions.

Filters:

- Search by name.
- Department.
- Permission group.
- Status.

Actions:

- View detail.
- Edit: `POSITION_MANAGE`.
- Activate: `POSITION_MANAGE`, show when inactive.
- Deactivate: `POSITION_MANAGE`, show when active.

### 6.5 Form Validation

- `name`: required, trim.
- `code`: optional, trim, uppercase.
- `permissionGroup`: required, enum only.
- `departmentId`: required, must be active department.
- `description`: optional.
- `status`: default `ACTIVE`.

### 6.6 PermissionGroup UX

Dropdown labels nên giải thích:

- `EMPLOYEE`: nhân viên thực thi công việc.
- `MANAGER`: quản lý task/workload/team assignment.
- `EXECUTIVE`: điều hành, xem và xử lý operation cấp cao.

Không dùng label “System Role” ở màn hình này. Dùng “Permission Group” hoặc “Workspace Permission Group”.

### 6.7 Business Rules FE Phải Thể Hiện

- Position name unique trong cùng workspace và department.
- Position code unique trong workspace nếu có.
- Department phải active khi tạo/sửa.
- Deactivate business position có thể fail nếu còn:
  - active employee đang dùng vị trí,
  - open task đang yêu cầu vị trí.
- Nếu deactivate fail, show message backend và gợi ý chuyển employee/task sang vị trí khác trước.

### 6.8 Acceptance Criteria

- HR tạo Business Position “Tech Lead” với `permissionGroup=MANAGER` được khi có `POSITION_MANAGE`; Business Owner chỉ xem danh mục.
- FE không gọi “Tech Lead” là system role.
- Employee được gán Business Position có role do backend trả về.
- Inactive Business Position không xuất hiện trong select cho employee/task mới.

## 7. Employees

### 7.1 APIs

- `GET /api/v1/employees`
- `POST /api/v1/employees`
- `GET /api/v1/employees/{id}`
- `PUT /api/v1/employees/{id}`
- `PATCH /api/v1/employees/{id}/status?status=ACTIVE|INACTIVE|INVITED`
- `PATCH /api/v1/employees/{id}/reset-password`

### 7.2 Create/Update Fields

Identity:

- `fullName`: required.
- `email`: required email.
- `phone`: optional.

Work profile:

- `jobTitle`: optional free text for display; not permission.
- `departmentId`: optional if no business position; auto-derived if business position selected.
- `jobPositionId`: optional Business Position.
- `seniorityLevel`: `INTERN | JUNIOR | MIDDLE | SENIOR | LEAD`.
- `skillRating`: 1-5.
- `yearsOfExperience`: number >= 0.
- `skills`: comma/newline text.

Personal/profile:

- `dateOfBirth`
- `gender`
- `address`
- `personalSummary`
- `employmentType`: `FULL_TIME | PART_TIME | CONTRACTOR | INTERN`
- `workingStatus`: `WORKING | ON_LEAVE | RESIGNED`
- `employeeLevel`: `INTERN | FRESHER | JUNIOR | MIDDLE | SENIOR | LEAD | MANAGER`
- `monthlyWorkingCapacityHours`: default backend 168.
- `mainExpertise`
- `secondaryExpertise`

### 7.3 Employee Role UX

FE không có dropdown system role trong employee form.

Nếu chọn Business Position:

- FE tự fill/lock department theo position.
- FE hiển thị text: “System role will be derived from selected Business Position permission group.”
- Sau save, FE hiển thị `role` backend trả về.

Nếu không chọn Business Position:

- FE cho chọn active Department.
- Backend role mặc định `EMPLOYEE`.

### 7.4 Credentials UX

Sau tạo employee:

- Hiển thị `username`.
- Hiển thị `employeeCode`.
- Hiển thị `initialPassword`.
- Có nút copy từng field.
- Có warning: “Initial password is sensitive. Only share with the employee through a secure channel.”

Sau reset password:

- Hiển thị `initialPassword` mới trong modal kết quả.
- Không lưu password vào local state lâu hơn cần thiết.

### 7.5 List Screen

Columns:

- Full name.
- Email.
- Username.
- Employee code.
- System role.
- Department.
- Business position.
- Status.
- Working status.
- Skill rating.
- Capacity hours.
- Actions.

Filters:

- Search by name/email/username/employeeCode.
- Status.
- Role.
- Department.
- Business position.
- Working status.

### 7.6 Acceptance Criteria

- HR CRUD employee được; Business Owner chỉ xem employee để giao task/workload và tạo HR account riêng.
- FE không gửi `role` trong create/update employee.
- Chọn Business Position tự đồng bộ department.
- Role hiển thị sau save đúng theo backend.
- Inactive Department/Business Position không chọn được cho employee mới.

## 8. Task Management

### 8.1 APIs

- `GET /api/workspace/tasks`
- `POST /api/workspace/tasks`
- `GET /api/workspace/tasks/{id}`
- `PUT /api/workspace/tasks/{id}`
- `PATCH /api/workspace/tasks/{id}/customer-info`
- `PATCH /api/workspace/tasks/{id}/assign-individual`
- `PATCH /api/workspace/tasks/{id}/assign-team`
- `PATCH /api/workspace/tasks/{id}/accept`
- `PATCH /api/workspace/tasks/{id}/submit-completion`
- `PATCH /api/workspace/tasks/{id}/approve-completion`
- `PATCH /api/workspace/tasks/{id}/return`
- `GET /api/workspace/tasks/{id}/attachments`
- `POST /api/workspace/tasks/{id}/attachments`

Legacy `/api/v1/tasks/...` còn dùng được nhưng workspace UI mới nên dùng `/api/workspace/tasks`.

### 8.2 Create/Update Body

```json
{
  "title": "Build payment reconciliation API",
  "requirements": "Implement endpoint and validation",
  "description": "Detailed scope",
  "customerPhone": "0900000000",
  "customerEmail": "customer@example.com",
  "customerDescription": "Customer background",
  "assignmentType": "TEAM",
  "assigneeId": null,
  "teamLeaderId": "uuid",
  "teamMemberIds": ["uuid"],
  "priority": "HIGH",
  "deadline": "2026-07-30T17:00:00+07:00",
  "startDate": "2026-07-20T09:00:00+07:00",
  "estimatedHours": 24,
  "difficulty": 4,
  "requiredSkills": "Java, Spring Boot, PostgreSQL",
  "requiredJobPositionId": "uuid",
  "taskDomain": "Backend Payment",
  "projectId": null,
  "departmentId": "uuid",
  "attachments": []
}
```

### 8.3 Required Fields

- `title`: required.
- `requirements`: required.
- `deadline`: required ISO datetime with offset.
- `estimatedHours`: required, > 0.
- `assignmentType`: default `INDIVIDUAL` if UI allows.
- `priority`: default `MEDIUM`.

### 8.4 Task Status Workflow

Status values:

- `ASSIGNED`: task has been assigned but employee has not accepted yet.
- `ACCEPTED`: assigned employee/participant accepted the task.
- `IN_PROGRESS`: employee is actively working.
- `BLOCKED`: employee reported a blocker.
- `SUBMITTED`: employee submitted completion and waits for manager confirmation.
- `RETURNED`: manager returned the task for revision.
- `COMPLETED`: manager confirmed completion.
- `CANCELLED`: business owner cancelled the task.

Human-in-the-loop rule:

- Employee cannot directly mark a task as final `COMPLETED`.
- Individual assignee or team leader submits completion by `PATCH /api/workspace/tasks/{id}/submit-completion`.
- Manager/Executive/Business Owner confirms by `PATCH /api/workspace/tasks/{id}/approve-completion`.
- Manager/Executive/Business Owner returns by `PATCH /api/workspace/tasks/{id}/return`.
- Legacy `PATCH /tasks/{id}/status` must not be used for `ACCEPTED`, `SUBMITTED`, `RETURNED`, or `COMPLETED`.

### 8.5 Assignment Rules

Individual:

- `assignmentType=INDIVIDUAL`
- `assigneeId` required before final save if product requires assigned task.
- `teamLeaderId` null.
- `teamMemberIds` empty/null.

Team:

- `assignmentType=TEAM`
- `teamLeaderId` required.
- `teamMemberIds` optional.
- Leader cannot duplicate in members.
- Team member list should only include active employees.

### 8.6 Requirement Context Rules

If `requiredJobPositionId` is selected:

- FE auto-fill/lock `departmentId` from that Business Position.
- FE sends both IDs if available.
- Backend validates department must match position.

If no `requiredJobPositionId`:

- FE allows active department selection.

Inactive department/position:

- Can be displayed in old task detail.
- Must be disabled in new create/edit selection.

### 8.7 Task List

Columns:

- Title.
- Assignment type.
- Assignee/team leader.
- Department.
- Required Business Position.
- Priority.
- Status.
- Progress.
- Deadline.
- Estimated hours.
- Overdue indicator.
- Actions.

Filters:

- Search title/requirements.
- Status.
- Priority.
- Assignment type.
- Department.
- Required Business Position.
- Overdue.

Actions:

- View detail.
- Create task: Business Owner, Executive, Manager.
- Edit task: Business Owner, Executive, Manager.
- Assign/Reassign: Business Owner, Executive, Manager.
- Accept task: assigned employee/participant, show when `ASSIGNED` or `RETURNED`.
- Submit completion: individual assignee or team leader, show when task is not `COMPLETED`/`CANCELLED`.
- Approve completion: Business Owner, Executive, Manager, show when `SUBMITTED`.
- Return task: Business Owner, Executive, Manager, show when `SUBMITTED`.
- Update customer info: Business Owner, Executive, Manager, assigned employee, or team leader.
- Update progress/blocker/completion: users allowed by backend visibility/update rule.

### 8.8 Task Detail

Sections:

- Summary: title, status, priority, progress, deadline.
- Requirement context: department, required business position, skills, domain, difficulty.
- Assignment: individual assignee or team leader/members.
- Customer info.
- Attachments.
- Update timeline.
- AI recommendations/history related to this flow if available.

### 8.9 Acceptance Criteria

- Manager hoặc Business Owner can create and assign task khi có `TASK_CREATE`/`TASK_ASSIGN`; Executive chỉ thấy nếu backend cấp các permission này.
- Employee only sees assigned/participant tasks.
- Required Business Position and Department never mismatch in form.
- Employee can accept assigned/returned task.
- Employee completion goes to `SUBMITTED`, not final `COMPLETED`.
- Manager/Executive/Business Owner can approve submitted task to `COMPLETED`.
- Manager/Executive/Business Owner can return submitted task with reason.
- Backend business-rule error is shown as-is.

## 9. AI Task Analysis

### 9.1 API

`POST /api/workspace/ai/tasks/analyze`

### 9.2 Request Body

```json
{
  "taskTitle": "Build payment reconciliation API",
  "taskDescription": "Need API to compare provider callback with internal transaction",
  "projectDescription": "Payment module",
  "departmentName": "Technology",
  "startDate": "2026-07-20T09:00:00+07:00",
  "deadline": "2026-07-30T17:00:00+07:00"
}
```

### 9.3 Expected Output Fields

Backend returns a map. FE should safely read:

- `taskType`
- `taskDomain`
- `suggestedDifficulty`
- `suggestedEmployeeLevel`
- `requiredSkills`
- `requiredJobPositions`
- `relatedDepartment`
- `estimatedWorkingHoursSuggestion`
- `missingInformation`
- `clarifyingQuestions`
- `summary`

### 9.4 UX Flow

On task form:

1. User enters title and requirements/description.
2. FE enables “Analyze task” when title and description/requirements are non-empty.
3. FE calls analysis API.
4. FE shows preview panel with suggestions.
5. User can apply suggestions:
   - `taskDomain` -> task domain field.
   - `requiredSkills` -> required skills field.
   - `relatedDepartment` -> match existing active department by name.
   - first matching `requiredJobPositions` -> match existing active Business Position by name.
   - estimated hours suggestion -> fill `estimatedHours` only after user confirms.

### 9.5 Safety Rules

- FE must not create new Department/Business Position from AI output automatically.
- If AI suggests a department/position that cannot be matched to active workspace data, show it as text suggestion, not selected ID.
- User must confirm before applying estimated hours/difficulty.
- If backend returns fallback summary, show “AI fallback / needs confirmation” badge.

## 10. AI Recommendations

### 10.1 APIs

- Individual: `POST /api/workspace/ai/recommendations/individual`
- Team leader: `POST /api/workspace/ai/recommendations/team-leaders`
- Team member: `POST /api/workspace/ai/recommendations/team-members`
- Explain current ranking: `POST /api/workspace/ai/recommendations/explain`
- Explain final selected result: `POST /api/workspace/ai/recommendations/result/explain`
- Estimate task hours: `POST /api/workspace/ai/tasks/estimate-hours`
- Explain workload risk: `POST /api/workspace/ai/workload/risk`
- Generate employee report draft: `POST /api/workspace/ai/employee-report`
- Business owner operational summary: `GET /api/workspace/ai/business-owner/operational-summary`
- Platform admin system summary: `GET /api/admin/ai/platform-summary`

### 10.2 Request Body

```json
{
  "title": "Build payment reconciliation API",
  "requirements": "Implement endpoint and validation",
  "deadline": "2026-07-30T17:00:00+07:00",
  "estimatedHours": 24,
  "taskDomain": "Backend Payment",
  "departmentId": "uuid",
  "requiredJobPositionId": "uuid",
  "requiredSkills": "Java, Spring Boot, PostgreSQL"
}
```

If FE omits `departmentId`, `requiredJobPositionId`, `requiredSkills`, or `taskDomain`, backend may run AI task/domain analysis internally and map output to real active workspace IDs before scoring.

### 10.3 Recommendation Response Fields

Each item can include:

- `employeeId`
- `fullName`
- `score`
- `workloadLevel`
- `requiredRole`
- `roleFit`
- `roleFitReason`
- `reason`
- `risk`
- `departmentId`
- `businessPositionId`
- `businessPositionName`
- `permissionGroup`
- `departmentSuitabilityScore`
- `businessPositionSuitabilityScore`
- `leadExperienceScore`
- `domainExperienceScore`
- `skillMatchScore`
- `similarTaskExperienceScore`
- `workloadAvailabilityScore`
- `performanceScore`
- `previousLeaderCount`
- `leadCompletionRate`
- `similarTaskCount`
- `scoreComponents`

### 10.4 Individual Recommendation Panel

Columns/cards:

- Rank.
- Full name.
- Department.
- Business position.
- Permission group.
- Final score.
- Workload level.
- Department suitability score.
- Business position suitability score.
- Skill match score.
- Domain experience score.
- Workload availability score.
- Performance score.
- AI explanation.
- Risk.
- Select button.

Select action:

- Set `assigneeId`.
- Set `assignmentType=INDIVIDUAL`.
- Do not auto-submit task.

### 10.5 Team Leader Recommendation Panel

Columns/cards:

- Rank.
- Full name.
- Department.
- Business position.
- Permission group.
- Final leader score.
- Department suitability score.
- Business position suitability score.
- Previous leader count.
- Lead completion rate.
- Lead experience score.
- Domain experience score.
- Skill match score.
- Workload level.
- Reason.
- Risk.
- Select as leader.

Select action:

- Set `teamLeaderId`.
- Remove selected employee from `teamMemberIds` if duplicated.
- Do not auto-submit task.

### 10.6 Team Member Recommendation Panel

Columns/cards:

- Rank.
- Full name.
- Department.
- Business position.
- Permission group.
- Final member score.
- Department suitability score.
- Business position suitability score.
- Skill match score.
- Similar task count.
- Similar task experience score.
- Workload availability score.
- Performance score.
- Workload level.
- Reason.
- Risk.
- Select checkbox/add member button.

Select action:

- Add employee to `teamMemberIds`.
- Prevent duplicate team leader/member.
- Do not auto-submit task.

### 10.7 Score Explanation Priority

Team leader explanation order:

1. Department suitability.
2. Business position suitability.
3. Previous team leader experience.
4. Domain experience.
5. Skill match.
6. Workload and overload risk.
7. General performance.

Team member explanation order:

1. Department suitability.
2. Business position suitability.
3. Skill match.
4. Similar task/domain experience.
5. Workload and overload risk.
6. General performance.

Backend ranking is final. LLM explanation must not reorder candidates.

### 10.8 Empty/Fallback States

- No candidates: show “No active employees match this workspace/task context.”
- AI provider fallback: show backend returned recommendations, but badge “Rule-based fallback”.
- Overloaded candidate: keep candidate visible but highlight risk.
- Missing task fields: disable recommendation buttons until title, requirements, deadline are present.

### 10.9 Recommendation Explanation

Use `POST /api/workspace/ai/recommendations/explain` after backend returns a ranked recommendation list.

Request:

```json
{
  "recommendationType": "TEAM_LEADER",
  "task": {
    "title": "Build payment reconciliation API",
    "taskDomain": "Backend Payment",
    "requiredSkills": ["Java", "Spring Boot"],
    "requiredJobPositions": ["Backend Java Developer"],
    "estimatedWorkingHours": 24,
    "deadline": "2026-07-30T17:00:00+07:00"
  },
  "candidates": []
}
```

Rules:

- FE passes candidates in backend ranking order.
- AI explanation is explanatory only; it must not reorder candidates.
- Show explanation in a drawer/modal named “Why this ranking?”.

Use `POST /api/workspace/ai/recommendations/result/explain` after user selects an assignee/team and wants final decision explanation.

### 10.10 Estimate Hours

Use `POST /api/workspace/ai/tasks/estimate-hours`.

Request:

```json
{
  "taskTitle": "Build payment reconciliation API",
  "taskDescription": "Compare provider callback with internal transaction",
  "difficulty": "HIGH",
  "taskType": "BACKEND",
  "startDate": "2026-07-20T09:00:00+07:00",
  "deadline": "2026-07-30T17:00:00+07:00",
  "backendWorkingDays": 8,
  "backendDefaultHours": 24
}
```

UX:

- Never auto-apply `suggestedHours`.
- User must click “Apply suggested hours”.
- Show `calculationBasis`, `confidence`, and `userConfirmationRequired`.

### 10.11 Workload Risk Explanation

Use `POST /api/workspace/ai/workload/risk` before assigning extra work to a candidate.

Display:

- `overallRisk`.
- `monthlyWarnings`.
- `recommendation`.

AI only explains backend workload numbers; FE must not use it to overwrite backend capacity/workload.

### 10.12 Employee Report Draft

Use `POST /api/workspace/ai/employee-report`.

Inputs are FE/backend aggregated maps:

- `employee`
- `period`
- `metrics`
- `notableTasks`
- `risks`

Output is an AI draft. FE must label it “AI draft - HR/manager review required”.

### 10.13 Business Owner Operational Summary

Use `GET /api/workspace/ai/business-owner/operational-summary`.

Backend builds the payload from real workspace employees, tasks, workload, plan limits, subscription state, and AI suggestion counts.

### 10.14 Platform Admin System Summary

Use `GET /api/admin/ai/platform-summary`.

Backend builds the payload from platform workspaces, payment transactions, revenue buckets, feedback, and AI suggestion stats.

## 10A. Business Owner Dashboard

### 10A.1 API

Use `GET /api/workspace/business-owner/dashboard`.

Backend returns production dashboard data, not AI text:

- `overviewCards.today|week|month`: completed, active, overdue, blocked, submitted, missing daily report, overloaded employees, completion rate.
- `dailyReportInsight`: expected/received/missing/reviewed reports and `missingEmployees`.
- `workloadInsight`: idle/light/normal/high/overloaded employee lists.
- `deadlineRisks`: chart/table-ready risky tasks.
- `blockedTasks`: blocked task table.
- `taskStatusChart`: `{ title, series, total }`.
- `workloadDistributionChart`: `{ title, series, total }`.
- `metadata`: data source, generated time, empty-state note.

FE requirements:

- Render summary cards for today/week/month.
- Render task status chart from `taskStatusChart.series`.
- Render workload distribution chart from `workloadDistributionChart.series`.
- Render missing report list from `dailyReportInsight.missingEmployees`.
- Render overdue/upcoming/deadline-risk table from `deadlineRisks`.
- Render blocked task table from `blockedTasks`.
- Render AI summary separately; do not use AI to compute dashboard numbers.
- Empty arrays and zero values are valid states; show friendly empty UI using `metadata.note`.

## 10B. Platform Admin Dashboard

### 10B.1 APIs

- `GET /api/admin/dashboard/overview`
- `GET /api/admin/dashboard/revenue/monthly`
- `GET /api/admin/dashboard/revenue/quarterly`
- `GET /api/admin/dashboard/revenue/yearly`
- `GET /api/admin/dashboard/revenue/by-plan`
- `GET /api/admin/dashboard/workspaces/by-status`
- `GET /api/admin/dashboard/workspaces/by-plan`
- `GET /api/admin/dashboard/payments/summary`
- `GET /api/admin/dashboard/feedback/summary`

FE requirements:

- Use overview cards for total/active/suspended/expired/new workspaces, users, revenue, success rate, failed payments, pending manual payments, feedback average, AI usage.
- Use revenue charts from each endpoint's `series`; do not calculate revenue client-side.
- Use workspace status/package charts from backend `series`.
- Use payment summary cards and pending manual payment table from backend.
- Use feedback rating chart and recent feedback table from backend.
- Platform Admin dashboard must not expose workspace internal task/employee management actions.

## 11. AI History

### 11.1 API

`GET /api/workspace/ai-history`

Query params:

- `function`: partial function-name search.
- `status`: `SUCCESS | FAILED | PROCESSING | CANCELLED`.
- `from`: ISO datetime.
- `to`: ISO datetime.
- `caller`: partial caller-name search.
- `limit`: default `100`, max `500`.
- `offset`: default `0`.

### 11.2 List Screen

Columns:

- Called time.
- Caller name.
- Caller role.
- Function.
- Status.

Filters:

- Function.
- Status.
- Date range.
- Caller.
- Limit/page size.

Rules:

- Do not show raw prompt.
- Do not show token usage.
- Do not show technical provider logs.
- User có `AI_HISTORY` can view according to backend rule.

## 12. Workload And Calendar

### 12.1 API

`GET /api/workspace/workload/monthly?year=2026&month=7`

### 12.2 Display Fields

- Employee name.
- Year.
- Month.
- Allocated hours.
- Capacity hours.
- Utilization ratio.
- Workload level.
- Workload label.

### 12.3 Workload Levels

- `NO_WORK`
- `LOW`
- `NORMAL`
- `HIGH`
- `OVERLOADED`

### 12.4 Role Behavior

- `GET /api/workspace/workload/monthly` is for Business Owner, Executive, and Manager.
- Older `/api/v1/analytics/workload` service methods are Business Owner-only; prefer workspace monthly workload for operation screens.

### 12.5 UI Requirements

- Calendar/month view by employee.
- Color workload cells by level.
- Show open tasks contributing to load when data is available.
- Warn before assigning more work to `HIGH` or `OVERLOADED` employees.

## 13. Daily Reports

### 13.1 APIs

- `GET /api/v1/daily-reports`
- `POST /api/v1/daily-reports`
- `GET /api/v1/daily-reports/{id}`
- `PATCH /api/v1/daily-reports/{id}/review`

### 13.2 Form Fields

- `reportDate`: required, date only.
- `todayCompleted`: required.
- `currentWork`: required.
- `blockers`: optional.
- `tomorrowPlan`: optional.

### 13.3 Role Behavior

- Any authenticated workspace user can create own daily report.
- Employee and HR see own reports unless backend grants broader visibility later.
- User có `REPORT_VIEW` can view workspace/own reports according to backend visibility.
- Business Owner reviews reports.

### 13.4 Validation

- Prevent duplicate report UI-side if current list already has same date.
- Backend remains source of truth for duplicate blocking.

## 14. Platform Admin Owner Account Provisioning

### 14.1 UX After Workspace Creation/Activation

After Platform Admin creates or activates a workspace, backend may return generated Business Owner accounts.

Show generated accounts once:

- Username.
- Initial password.
- Full name.
- Copy button.
- Export CSV button if implemented.
- Warning: “These credentials are shown only once. Business Owners must change password on first login.”

- Username format: `{XX}0000{suffix}`, for example `SV0000A`, `SV0000B`.
- Default generated password: `123456`.
- Owner accounts can be created before activation, but login works only after workspace is `ACTIVE` and payment is `CONFIRMED`.

### 14.2 Workspace Detail Fields

- Workspace name.
- Business name.
- Workspace identifier/short code.
- Subscription plan.
- Max users.
- Max owner accounts.
- Max employee accounts.
- Owner account count.
- Current users.
- Status.
- Payment status.
- Activated at.
- Expires at.
- Owner provisioning timestamp.
- `ownerAccounts` table: username, full name, email, phone, status, must-change-password, created at, updated at.
- Row actions: reset password, activate/deactivate account.
- `generatedOwnerAccounts` modal after create/provision/reset.

### 14.3 Required Platform Admin Actions

- Create workspace and automatically receive owner credentials.
- Open business/workspace detail and manage Business Owner accounts.
- Add Business Owner manually; if username/password are empty, backend uses `{XX}0000{suffix}` and `123456`.
- Reset Business Owner password to `123456`.
- Activate/deactivate Business Owner account.
- Re-provision missing Business Owner accounts when package limit increases.

## 15. Error Messages FE Nên Map Thân Thiện

Không hard-code thay backend message, nhưng có thể thêm helper text:

- Department deactivate failed: “Hãy chuyển/deactivate business position, employee hoặc task đang tham chiếu trước.”
- Business position deactivate failed: “Hãy chuyển employee/task đang dùng vị trí này trước.”
- Department mismatch with business position: “Department phải khớp với Business Position đã chọn.”
- AI provider unavailable: “AI tạm thời không sẵn sàng, vui lòng thử lại hoặc dùng dữ liệu fallback.”
- AI rate limited: “AI đang bận, thử lại sau vài giây.”

## 16. Data Fetching Và Cache Invalidation

Suggested query keys:

- `me`
- `departments`
- `businessPositions`
- `employees`
- `tasks`
- `taskDetail:{id}`
- `aiHistory`
- `workloadMonthly:{year}:{month}`
- `dailyReports`
- `notifications`

Invalidate:

- After department mutation: `departments`, `businessPositions`, `employees`, `tasks`.
- After business position mutation: `businessPositions`, `employees`, `tasks`.
- After employee mutation: `employees`, `tasks`, `workloadMonthly`.
- After task mutation: `tasks`, `taskDetail`, `workloadMonthly`, `notifications`.
- After AI recommendation: no mutation unless user applies selection; keep recommendation result local to form.
- After AI estimate/explain/report/summary: invalidate `aiHistory`; keep output scoped to the current screen context.
- After report mutation: `dailyReports`, `notifications`.

## 17. FE Acceptance Checklist

Authorization:

- FE hides platform admin screens from workspace users.
- FE hides HR mutate actions from users without `DEPARTMENT_MANAGE`, `POSITION_MANAGE`, or employee mutation permissions.
- FE hides task manager actions from HR/Employee.
- FE never treats Business Position name as system role.

HR:

- HR creates Department.
- HR creates Business Position under active Department.
- PermissionGroup dropdown only has `EMPLOYEE`, `MANAGER`, `EXECUTIVE`.
- Deactivate Department/Business Position shows confirmation and handles dependency error.

Employee:

- Create employee without manual role.
- Assign Business Position and see backend-derived role after save.
- Reset password modal shows generated initial password securely.

Task:

- Create individual task.
- Create team task.
- Required Business Position auto-syncs Department.
- Accept/submit/approve/return task workflow works end-to-end.
- Employee only sees assigned/participant tasks.
- Manager hoặc Business Owner can manage task workflow khi có `TASK_ASSIGN`/`TASK_APPROVE`; Executive only if backend grants those permissions later.

AI:

- Analyze task fills suggestions but does not create master data.
- Estimate hours requires manual apply.
- Recommendation uses real employees and real workspace context.
- Recommendation explanation never changes backend ranking.
- Workload risk explanation uses backend workload numbers.
- Employee report is shown as AI draft.
- Candidate ranking follows backend order.
- FE shows score breakdown and risk clearly.

Daily Report:

- Employee submits report.
- Business Owner reviews report.
- Duplicate same-day report is handled gracefully.

Production UX:

- Loading/empty/error states exist for every major screen.
- All destructive/lifecycle actions require confirmation.
- All backend business-rule errors are shown to user.

## 18. FE Change Log / Production Delta

Phần này ghi lại các thay đổi FE bắt buộc sau khi backend/AI đã được chỉnh theo business model production. Đội FE phải coi đây là checklist migrate từ UI cũ sang UI mới.

### 18.1 Role Và Business Position

Must change:

- Đổi toàn bộ label “system role” đang dùng cho Developer, BA, HR Staff, Tech Lead, Accountant... thành `Business Position` hoặc `Job Position`.
- Chỉ dùng system role thật để route/guard UI: `EMPLOYEE`, `MANAGER`, `EXECUTIVE`, `BUSINESS_OWNER`, `HR`, `PLATFORM_ADMIN`.
- Form employee không có dropdown role. FE chỉ chọn Department và Business Position; backend tự trả `role`.
- Form Business Position chỉ có dropdown `permissionGroup`: `EMPLOYEE`, `MANAGER`, `EXECUTIVE`.
- Không cho chọn `PLATFORM_ADMIN`, `BUSINESS_OWNER`, `HR` trong Business Position.

Screens impacted:

- Login redirect / app shell navigation.
- HR Department list/form.
- HR Business Position list/form.
- Employee create/update/detail.
- Task create/update requirement context.
- AI task analysis and recommendation forms.

### 18.2 Department Và Business Position Master Data

Must change:

- Add HR Department management screens using `/api/workspace/hr/departments`.
- Add Business Position management screens using `/api/workspace/hr/business-positions`.
- Business Position phải thuộc một active Department.
- Employee form: khi chọn Business Position, FE auto-fill và lock Department theo position.
- Task form: khi chọn Required Business Position, FE auto-fill và lock Department theo position.
- Inactive Department/Business Position không xuất hiện trong select tạo mới; detail cũ vẫn hiển thị read-only nếu backend trả về.
- Deactivate Department/Business Position phải có confirm modal và phải show backend business-rule error nếu còn dependency.

### 18.3 Task Workflow Production

FE không được coi `progressPercent=100` là hoàn thành cuối cùng. Workflow chuẩn:

1. Manager/Executive/Business Owner tạo/giao task.
2. Employee/team leader bấm Accept: `PATCH /api/workspace/tasks/{id}/accept`.
3. Employee/team leader update progress bằng `PATCH /api/workspace/tasks/{id}/progress`.
4. Employee/team leader submit hoàn thành: `PATCH /api/workspace/tasks/{id}/submit-completion`.
5. Manager/Executive/Business Owner review.
6. Nếu đạt: `PATCH /api/workspace/tasks/{id}/approve-completion`.
7. Nếu chưa đạt: `PATCH /api/workspace/tasks/{id}/return`.

Status UI required:

- `ASSIGNED`: show CTA Accept cho assignee/team participant.
- `ACCEPTED`: show task đã nhận.
- `IN_PROGRESS`: show progress and update form.
- `BLOCKED`: show blocker state and unblock/update progress option if backend allows.
- `SUBMITTED`: show pending manager confirmation; hide employee final-complete CTA.
- `RETURNED`: show return reason/update trail; allow assignee/team leader resubmit after fixing.
- `COMPLETED`: read-only success state.
- `CANCELLED`: read-only cancelled state.

Action visibility:

- Employee can accept assigned task, update own/participating task progress, and submit completion only if individual assignee or team leader.
- Team member can update progress but cannot submit completion for the whole team.
- Manager/Executive/Business Owner can assign, return, approve completion, and cancel where backend permits.
- HR không có task manager actions.
- FE must not call generic status update to set `ACCEPTED`, `SUBMITTED`, `RETURNED`, or `COMPLETED`; use dedicated workflow endpoints.

Request body:

```json
{
  "content": "Completion note",
  "attachment": "optional URL or file reference"
}
```

for `submit-completion`, and:

```json
{
  "reason": "What must be fixed",
  "attachment": "optional URL or file reference"
}
```

for `return`.

### 18.4 AI Screens Và Contract

FE chỉ gọi Backend API, không gọi trực tiếp AI Service.

Task create assistant:

- Add “Analyze task” CTA using `POST /api/workspace/ai/tasks/analyze`.
- Use result only as suggestion preview.
- Không auto-create Department, Business Position, or Skill từ AI output.
- User must explicitly apply suggested Department/Business Position/Skills/Domain.

Recommendation:

- Use `/api/workspace/ai/recommendations/individual`, `/team-leaders`, `/team-members`.
- FE sends real task context when available: `departmentId`, `requiredJobPositionId`, `requiredSkills`, `taskDomain`.
- If missing, backend may run AI analysis internally; FE still renders backend ranking as source of truth.
- Do not re-sort recommendation candidates client-side.
- Render score breakdown, risk, reason, current workload, matched skills/position/department.

AI explanation and helper actions:

- Estimate hours: `POST /api/workspace/ai/tasks/estimate-hours`.
- Explain current ranking: `POST /api/workspace/ai/recommendations/explain`.
- Explain selected result: `POST /api/workspace/ai/recommendations/result/explain`.
- Explain workload risk: `POST /api/workspace/ai/workload/risk`.
- Employee report draft: `POST /api/workspace/ai/employee-report`.
- Business owner operational summary: `GET /api/workspace/ai/business-owner/operational-summary`.
- Platform admin system summary: `GET /api/admin/ai/platform-summary`.
- AI history: `GET /api/workspace/ai-history`.

Production AI UX rules:

- Label all AI output as suggestion/draft, never final authority.
- Require manual apply/confirm before changing task assignment, estimated hours, or report content.
- Show fallback/source metadata if backend returns rule-based fallback.
- Show AI quota/rate-limit/provider errors from backend message.
- Keep AI output local to current screen unless user explicitly saves a business action.

Dashboard changes:

- Business Owner dashboard must call `GET /api/workspace/business-owner/dashboard`.
- Platform Admin dashboard must call `/api/admin/dashboard/**` chart endpoints.
- FE must render backend `series` directly for charts.
- FE must not calculate revenue, payment success rate, workload buckets, overdue counts, missing report counts, or completion rate from raw rows when backend dashboard endpoints provide them.
- AI summaries explain dashboard data; AI does not calculate the dashboard metrics.

Subscription/payment/admin workspace changes:

- Platform Admin workspace list/detail must read `activeSubscription` from backend responses.
- Use `activeSubscription.status`, `startDate`, `endDate`, `renewalDate`, `price`, `maxOwnerAccounts`, and `maxEmployeeAccounts` for current package display when present.
- Keep `subscriptionPlanId`, `maxUsers`, `maxOwnerAccounts`, and `maxEmployeeAccounts` as compatibility/fallback fields only; do not infer billing history from workspace fields.
- When admin changes a workspace plan, refetch workspace detail/list because backend creates a new ACTIVE subscription snapshot and closes the old one as `UPGRADED`/`DOWNGRADED`.
- Payment QR is Platform Admin uploaded/managed data, not FE-generated data. Admin Payment Settings uses `GET /api/admin/payment-qr-settings`, `PUT /api/admin/payment-qr-settings/{paymentMethod}`, and multipart upload `POST /api/admin/payment-qr-settings/BANK_TRANSFER/qr-image`.
- Admin payment form must not show or submit `qrCodeUrl`, `paymentUrl`, or `deeplink`; remove the UI fields “URL ảnh QR”, “Payment URL”, and “Deeplink”.
- Admin can upload/update bank QR any time. New payment instructions use the latest enabled QR snapshot; existing payment transactions keep their snapshot.
- MoMo UI must use only real provider config/status from backend/env. FE must not let admin type MoMo URL/deeplink/QR URL.
- MoMo/Bank UI must render returned `providerQrCodeUrl`; FE must not generate fake QR or call third-party QR services client-side.
- If backend returns missing/unready QR error when public user creates payment, show a blocking message asking the user to wait for admin to update the QR, and keep them on payment method/instruction recovery state.
- MoMo UI must not depend on sandbox/stub mode. Render returned provider fields only when backend returns them from real provider; if backend says MoMo chưa cấu hình, show waiting/error state and do not create fake URLs.
- Demo seed data now includes 3 active workspaces with 30 employees each, cached AI suggestions, dashboard workload/task data, subscriptions, and payments. QA can use owners `SV0000A`, `MD0000A`, `HC0000A` with initial password `123456`.

### 18.5 Required FE Query Keys / Cache

Add or update query keys:

- `departments`
- `businessPositions`
- `employees`
- `tasks`
- `taskDetail:{id}`
- `workloadMonthly:{year}:{month}`
- `aiHistory`
- `aiRecommendation:{taskDraftHash}:{type}`
- `aiTaskAnalysis:{taskDraftHash}`
- `businessOwnerDashboard`
- `adminDashboardOverview`
- `adminRevenue:{period}`
- `adminWorkspaceCharts`
- `adminPaymentSummary`
- `adminFeedbackSummary`

Invalidation required:

- Department mutation -> invalidate `departments`, `businessPositions`, `employees`, `tasks`.
- Business Position mutation -> invalidate `businessPositions`, `employees`, `tasks`.
- Employee mutation -> invalidate `employees`, `tasks`, `workloadMonthly:{year}:{month}`.
- Task create/update/assign/workflow -> invalidate `tasks`, `taskDetail:{id}`, `workloadMonthly:{year}:{month}`, `notifications`.
- AI call -> invalidate `aiHistory`; do not invalidate business data unless user applies a real mutation.
- Successful payment/admin confirmation/workspace activation -> invalidate `adminDashboardOverview`, `adminRevenue:{period}`, `adminWorkspaceCharts`, `adminPaymentSummary`, `workspaceRegistrations`, `payments`, `workspaces`.

### 18.6 Production Readiness Criteria For FE

FE is not considered production-ready until:

- No UI copy calls Developer/BA/Tech Lead/HR Staff a system role.
- All role guards use backend `user.role`.
- All forms use active Department/Business Position master data.
- Employee role is displayed from backend response only.
- Task cannot be finalized by employee directly; completion requires manager confirmation.
- Team member cannot submit final completion for team task.
- AI recommendation ranking is rendered in backend order.
- AI analysis never creates master data automatically.
- Platform admin AI summary is visible only to `PLATFORM_ADMIN`.
- Business owner operational summary is hidden from `EMPLOYEE`, `MANAGER`, `HR` unless backend later changes policy.
- Business Owner dashboard renders backend cards/charts/lists from `/api/workspace/business-owner/dashboard`.
- Platform Admin dashboard renders backend chart-ready series from `/api/admin/dashboard/**`.
- Payment result page treats `ACTIVATED` as final successful registration state and never activates workspace from URL/query params.
- Platform Admin workspace screens render `activeSubscription` and refetch after plan/status/payment lifecycle changes.
- Payment instruction page supports real MoMo provider URLs/admin-configured QR without changing UI logic.
- Public payment flow handles missing QR as a waiting state; FE never creates or substitutes QR codes.
- Platform Admin can upload/change/enable/disable payment QR settings for both MoMo and bank transfer.
- QA validates dashboards against seeded workspaces `SV`, `MD`, and `HC`, each with 30 employees and mixed workload/task states.
- Every destructive/lifecycle action has confirmation, loading, success, and backend-error states.
