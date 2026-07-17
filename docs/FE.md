# FOREP Frontend Requirements

Tài liệu này là yêu cầu chi tiết cho FE mới. FE phải coi backend là source of truth, không tự suy diễn quyền, không tự tạo dữ liệu giả cho AI/recommendation, và không gọi trực tiếp AI Service.

## 1. Nguyên Tắc Không Được Sai

### 1.1 System Role Khác Business Position

System role là quyền thật sự trong hệ thống:

- `PLATFORM_ADMIN`: quản trị nền tảng, gói subscription, đăng ký workspace, payment review, workspace activation, owner account provisioning.
- `BUSINESS_OWNER`: chủ workspace, xem dashboard kinh doanh, cấu hình workspace, xem dữ liệu HR/task/workload/AI theo quyền workspace.
- `HR`: quản lý nhân sự, hồ sơ nhân viên, phòng ban, business position.
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

## 2. Role Matrix Cho FE

| Module | PLATFORM_ADMIN | BUSINESS_OWNER | HR | EXECUTIVE | MANAGER | EMPLOYEE |
|---|---:|---:|---:|---:|---:|---:|
| Platform admin dashboard | Yes | No | No | No | No | No |
| Subscription plans | Yes | View own plan only | No | No | No | No |
| Workspace registration/payment review | Yes | No | No | No | No | No |
| Business owner generated accounts | Yes | No | No | No | No | No |
| Workspace owner dashboard | No | Yes | No | No | No | No |
| Employee management | No | Yes | Yes | No | No | No |
| Department management | No | View | Manage | No | No | No |
| Business position management | No | View | Manage | No | No | No |
| Task list | No | All | All/visible | All | All | Assigned only |
| Task create/edit/assign | No | Yes | No | Yes | Yes | No |
| Task progress update | No | Yes | No | Yes | Yes | Assigned/participant |
| Customer info update | No | Yes | No | Yes | Yes | Assignee/leader only |
| AI task analysis/recommendation | No | Yes | No | Yes | Yes | No |
| AI history | No | Yes | Yes | Yes | Yes | No |
| Daily report create | No | Yes | Yes | Yes | Yes | Yes |
| Daily report review | No | Yes | No | No | No | No |
| Notifications | No | Own/workspace | Own | Own/workspace | Own/workspace | Own |

FE phải hide navigation/action theo matrix, nhưng backend vẫn là lớp bảo vệ cuối.

## 3. App Layout Và Route Guard

### 3.1 Sau Login

`GET /api/v1/auth/me` trả `UserView`. FE dùng `user.role` để redirect:

- `PLATFORM_ADMIN` hoặc legacy `SYSTEM_ADMIN`/`SYSTEM`: `/platform/dashboard`
- `BUSINESS_OWNER` hoặc legacy `OWNER`: `/owner/dashboard`
- `HR`: `/hr/employees`
- `EXECUTIVE`: `/operations/tasks`
- `MANAGER`: `/operations/tasks`
- `EMPLOYEE`: `/employee/home`

### 3.2 Navigation Theo Role

Platform Admin:

- Dashboard.
- Subscription Plans.
- Workspace Registrations.
- Payments.
- Workspaces.
- Business Feedback.
- Audit Logs.

Business Owner:

- Dashboard.
- Tasks.
- Workload.
- AI Center.
- AI History.
- Employees.
- Departments.
- Business Positions.
- Daily Reports.
- Notifications.
- Workspace Settings.
- Subscription/Upgrade.

HR:

- Employees.
- Departments.
- Business Positions.
- AI History.
- Notifications.

Executive/Manager:

- Tasks.
- Workload.
- AI Task Analysis.
- AI Recommendations.
- AI History.
- Daily Reports.
- Notifications.

Employee:

- My Tasks.
- My Daily Reports.
- Notifications.
- Profile.

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
- Edit: HR only.
- Activate: HR only, show when status `INACTIVE`.
- Deactivate: HR only, show when status `ACTIVE`.

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

- HR tạo/sửa/activate/deactivate Department được.
- Business Owner xem list/detail nhưng không thấy nút mutate.
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
- Edit: HR only.
- Activate: HR only, show when inactive.
- Deactivate: HR only, show when active.

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

- HR tạo Business Position “Tech Lead” với `permissionGroup=MANAGER` được.
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

- HR/Business Owner CRUD employee được.
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

### 8.4 Assignment Rules

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

### 8.5 Requirement Context Rules

If `requiredJobPositionId` is selected:

- FE auto-fill/lock `departmentId` from that Business Position.
- FE sends both IDs if available.
- Backend validates department must match position.

If no `requiredJobPositionId`:

- FE allows active department selection.

Inactive department/position:

- Can be displayed in old task detail.
- Must be disabled in new create/edit selection.

### 8.6 Task List

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
- Update customer info: Business Owner, Executive, Manager, assigned employee, or team leader.
- Update progress/blocker/completion: users allowed by backend visibility/update rule.

### 8.7 Task Detail

Sections:

- Summary: title, status, priority, progress, deadline.
- Requirement context: department, required business position, skills, domain, difficulty.
- Assignment: individual assignee or team leader/members.
- Customer info.
- Attachments.
- Update timeline.
- AI recommendations/history related to this flow if available.

### 8.8 Acceptance Criteria

- Manager/Executive can create and assign task.
- Employee only sees assigned/participant tasks.
- Required Business Position and Department never mismatch in form.
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
- HR/Business Owner/Executive/Manager can view according to backend rule.

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
- Business Owner/Executive/Manager can view workspace reports according to backend visibility.
- Business Owner reviews reports.

### 13.4 Validation

- Prevent duplicate report UI-side if current list already has same date.
- Backend remains source of truth for duplicate blocking.

## 14. Platform Admin Owner Account Provisioning

### 14.1 UX After Workspace Activation

After Platform Admin activates a workspace, backend may return generated Business Owner accounts.

Show generated accounts once:

- Username.
- Initial password.
- Full name.
- Copy button.
- Export CSV button if implemented.
- Warning: “These credentials are shown only once. Business Owners must change password on first login.”

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
- After report mutation: `dailyReports`, `notifications`.

## 17. FE Acceptance Checklist

Authorization:

- FE hides platform admin screens from workspace users.
- FE hides HR mutate actions from Business Owner/Executive/Manager/Employee.
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
- Employee only sees assigned/participant tasks.
- Manager/Executive can manage task workflow.

AI:

- Analyze task fills suggestions but does not create master data.
- Recommendation uses real employees and real workspace context.
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
