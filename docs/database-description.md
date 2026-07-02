# Database Description

Tài liệu này mô tả schema hiện tại theo các migration `V1__initial_schema.sql` đến `V5__workspace_registration_owner_account.sql`.

## workspaces

Thông tin tổ chức/workspace.

| Cột | Kiểu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | uuid | Primary key | ID workspace |
| name | varchar(255) | not null | Tên workspace |
| logo | varchar(1000) | nullable | URL/logo workspace |
| address | text | nullable | Địa chỉ |
| owner_id | uuid | Foreign key -> users(id) | Chủ workspace |
| subscription_plan_id | uuid | Foreign key -> subscription_plans(id) | Gói subscription hiện tại |
| business_name | varchar(255) | nullable | Tên pháp nhân/doanh nghiệp |
| contact_email | varchar(255) | nullable | Email liên hệ doanh nghiệp |
| contact_phone | varchar(50) | nullable | Số điện thoại liên hệ |
| max_users | integer | not null default 50, check > 0 | Giới hạn người dùng theo gói |
| status | varchar(30) | not null default ACTIVE | PENDING_PAYMENT, ACTIVE, INACTIVE, SUSPENDED, EXPIRED |
| payment_status | varchar(30) | not null default CONFIRMED | PENDING, CONFIRMED, REJECTED, CORRECTION_REQUESTED |
| activated_at | timestamp with time zone | nullable | Thời điểm kích hoạt |
| expires_at | timestamp with time zone | nullable | Thời điểm hết hạn |
| last_activity_at | timestamp with time zone | nullable | Hoạt động gần nhất |
| created_at | timestamp with time zone | not null | Thời điểm tạo |
| short_code | varchar(2) | unique, check uppercase length 2 | Mã viết tắt dùng tạo mã nhân viên |
| next_employee_number | integer | not null default 1, check 1..1001 | Số thứ tự nhân viên kế tiếp |

## subscription_plans

Gói subscription do System Administrator quản lý.

| Cột | Kiểu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | uuid | Primary key | ID gói |
| name | varchar(120) | not null, unique | Tên gói |
| price | numeric(12, 2) | not null, check >= 0 | Giá gói |
| duration_days | integer | not null, check > 0 | Thời hạn theo ngày |
| max_users | integer | not null, check > 0 | Số user tối đa |
| max_workspaces | integer | nullable | Số workspace tối đa nếu áp dụng |
| ai_usage_limit | integer | nullable | Giới hạn AI nếu áp dụng |
| features | text | nullable | Tính năng của gói |
| status | varchar(30) | not null | ACTIVE, INACTIVE |
| created_at | timestamp with time zone | not null | Thời điểm tạo |
| updated_at | timestamp with time zone | not null | Thời điểm cập nhật |

## workspace_registrations

Thông tin đăng ký workspace và xác nhận thanh toán trước khi kích hoạt.

| Cột | Kiểu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | uuid | Primary key | ID đăng ký |
| business_name | varchar(255) | not null | Tên doanh nghiệp |
| workspace_name | varchar(255) | not null | Tên workspace |
| workspace_identifier | varchar(20) | not null, unique | Mã workspace |
| contact_email | varchar(255) | not null | Email liên hệ |
| contact_phone | varchar(50) | not null | Số điện thoại liên hệ |
| business_address | text | nullable | Địa chỉ |
| subscription_plan_id | uuid | not null, Foreign key -> subscription_plans(id) | Gói đã chọn |
| max_users | integer | not null, check > 0 | Giới hạn user |
| owner_full_name | varchar(255) | not null | Họ tên Business Owner đầu tiên |
| owner_email | varchar(255) | not null | Email Business Owner đầu tiên |
| owner_phone | varchar(50) | nullable | SĐT Business Owner đầu tiên |
| owner_password_hash | varchar(255) | not null | Mật khẩu owner đã hash, chỉ dùng khi admin approve |
| activation_date | timestamp with time zone | nullable | Ngày kích hoạt dự kiến |
| expiration_date | timestamp with time zone | nullable | Ngày hết hạn dự kiến |
| payment_proof_url | varchar(1000) | nullable | Minh chứng thanh toán |
| payment_note | text | nullable | Ghi chú thanh toán |
| payment_status | varchar(30) | not null | PENDING, CONFIRMED, REJECTED, CORRECTION_REQUESTED |
| registration_status | varchar(30) | not null | SUBMITTED, PAYMENT_PENDING, PAYMENT_SUBMITTED, APPROVED, REJECTED |
| workspace_id | uuid | Foreign key -> workspaces(id) | Workspace được tạo sau khi duyệt |
| reviewed_by | uuid | Foreign key -> users(id) | System Admin review |
| reviewed_at | timestamp with time zone | nullable | Thời điểm review |
| review_note | text | nullable | Ghi chú review |
| created_at | timestamp with time zone | not null | Thời điểm tạo |
| updated_at | timestamp with time zone | not null | Thời điểm cập nhật |

## users

Thông tin tài khoản owner và employee.

| Cột | Kiểu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | uuid | Primary key | ID người dùng |
| workspace_id | uuid | not null, Foreign key -> workspaces(id) on delete cascade | Workspace sở hữu user |
| full_name | varchar(255) | not null | Họ tên |
| email | varchar(255) | not null, unique(workspace_id, email) | Email đăng nhập/liên hệ |
| phone | varchar(50) | nullable | Số điện thoại |
| password_hash | varchar(255) | not null | Mật khẩu đã hash |
| role | varchar(30) | not null | OWNER hoặc EMPLOYEE |
| avatar | varchar(1000) | nullable | URL avatar |
| status | varchar(30) | not null | ACTIVE, INACTIVE, INVITED |
| created_at | timestamp with time zone | not null | Thời điểm tạo |
| updated_at | timestamp with time zone | not null | Thời điểm cập nhật |
| job_title | varchar(120) | nullable | Chức danh |
| seniority_level | varchar(30) | nullable | Cấp bậc |
| skill_rating | integer | nullable, check 1..5 | Đánh giá kỹ năng |
| years_of_experience | integer | nullable, check >= 0 | Số năm kinh nghiệm |
| skills | text | nullable | Danh sách kỹ năng |
| username | varchar(120) | unique | Tên đăng nhập nhân viên |
| employee_code | varchar(6) | unique, check uppercase length 6 | Mã nhân viên |
| initial_password | varchar(20) | nullable | Mật khẩu ban đầu |

## tasks

Task được owner tạo và giao cho employee.

| Cột | Kiểu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | uuid | Primary key | ID task |
| workspace_id | uuid | not null, Foreign key -> workspaces(id) on delete cascade | Workspace sở hữu task |
| title | varchar(255) | not null | Tiêu đề task |
| requirements | text | not null | Yêu cầu chính |
| description | text | nullable | Mô tả bổ sung |
| assignee_id | uuid | not null, Foreign key -> users(id) | Người được giao |
| creator_id | uuid | not null, Foreign key -> users(id) | Người tạo |
| priority | varchar(30) | not null | LOW, MEDIUM, HIGH, CRITICAL |
| deadline | timestamp with time zone | not null | Deadline |
| estimated_hours | numeric(10, 2) | nullable | Giờ ước tính |
| progress_percent | integer | not null | Tiến độ phần trăm |
| status | varchar(30) | not null | ASSIGNED, IN_PROGRESS, BLOCKED, COMPLETED, CANCELLED |
| created_at | timestamp with time zone | not null | Thời điểm tạo |
| updated_at | timestamp with time zone | not null | Thời điểm cập nhật |
| completed_at | timestamp with time zone | nullable | Thời điểm hoàn thành |

## task_updates

Lịch sử cập nhật tiến độ, blocker, hoàn thành task.

| Cột | Kiểu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | uuid | Primary key | ID cập nhật |
| task_id | uuid | not null, Foreign key -> tasks(id) on delete cascade | Task liên quan |
| user_id | uuid | not null, Foreign key -> users(id) | Người cập nhật |
| progress_percent | integer | not null | Tiến độ tại thời điểm cập nhật |
| content | text | not null | Nội dung cập nhật |
| attachment | varchar(1000) | nullable | Tệp đính kèm |
| update_type | varchar(30) | not null | PROGRESS, BLOCKER, COMPLETION |
| created_at | timestamp with time zone | not null | Thời điểm cập nhật |

## daily_reports

Báo cáo hằng ngày của employee.

| Cột | Kiểu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | uuid | Primary key | ID report |
| workspace_id | uuid | not null, Foreign key -> workspaces(id) on delete cascade | Workspace sở hữu report |
| user_id | uuid | not null, Foreign key -> users(id) | Người gửi report |
| report_date | date | not null, unique(workspace_id, user_id, report_date) | Ngày report |
| today_completed | text | not null | Việc đã hoàn thành hôm nay |
| current_work | text | not null | Việc đang làm |
| blockers | text | nullable | Vướng mắc |
| tomorrow_plan | text | nullable | Kế hoạch ngày mai |
| reviewed_at | timestamp with time zone | nullable | Thời điểm owner review |
| created_at | timestamp with time zone | not null | Thời điểm tạo |
| updated_at | timestamp with time zone | not null | Thời điểm cập nhật |

## notifications

Thông báo theo từng người dùng.

| Cột | Kiểu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | uuid | Primary key | ID thông báo |
| workspace_id | uuid | not null, Foreign key -> workspaces(id) on delete cascade | Workspace sở hữu thông báo |
| user_id | uuid | not null, Foreign key -> users(id) | Người nhận thông báo |
| type | varchar(80) | not null | Loại thông báo |
| title | varchar(255) | not null | Tiêu đề |
| message | text | not null | Nội dung |
| related_entity_type | varchar(80) | nullable | Loại entity liên quan |
| related_entity_id | uuid | nullable | ID entity liên quan |
| is_read | boolean | not null | Trạng thái đã đọc |
| created_at | timestamp with time zone | not null | Thời điểm tạo |

## ai_suggestions

Lưu input/output của các chức năng AI.

| Cột | Kiểu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | uuid | Primary key | ID suggestion |
| workspace_id | uuid | not null, Foreign key -> workspaces(id) on delete cascade | Workspace sở hữu suggestion |
| type | varchar(80) | not null | ASSIGNEE_RECOMMENDATION, WORKLOAD_SUMMARY, BUSINESS_SUMMARY, TASK_EXTRACTION, DAILY_REPORT_INSIGHTS, TASK_SPLIT, TASK_ADJUSTMENT, DELAY_RISK, MISSING_REPORT, ACTION_SUGGESTION |
| input_data | text | not null | Payload đầu vào |
| output_data | text | not null | Kết quả AI/fallback |
| status | varchar(30) | not null | GENERATED, ACCEPTED, REJECTED |
| created_by | uuid | not null, Foreign key -> users(id) | Người tạo suggestion |
| created_at | timestamp with time zone | not null | Thời điểm tạo |

## files

Metadata file upload.

| Cột | Kiểu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | uuid | Primary key | ID file |
| workspace_id | uuid | not null, Foreign key -> workspaces(id) on delete cascade | Workspace sở hữu file |
| uploaded_by | uuid | not null, Foreign key -> users(id) | Người upload |
| file_name | varchar(255) | not null | Tên file |
| file_type | varchar(120) | not null | Kiểu file |
| file_url | varchar(1000) | not null | URL file |
| related_entity_type | varchar(80) | nullable | Loại entity liên quan |
| related_entity_id | uuid | nullable | ID entity liên quan |
| created_at | timestamp with time zone | not null | Thời điểm tạo |

## audit_logs

Lịch sử audit thay đổi nghiệp vụ.

| Cột | Kiểu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | uuid | Primary key | ID log |
| workspace_id | uuid | not null, Foreign key -> workspaces(id) on delete cascade | Workspace liên quan |
| actor_id | uuid | not null, Foreign key -> users(id) | Người thực hiện |
| action | varchar(120) | not null | Hành động |
| entity_type | varchar(80) | not null | Loại entity |
| entity_id | uuid | not null | ID entity |
| old_value | text | nullable | Giá trị cũ |
| new_value | text | nullable | Giá trị mới |
| created_at | timestamp with time zone | not null | Thời điểm tạo |

## business_feedback

Feedback cấp doanh nghiệp/workspace do System Administrator review.

| Cột | Kiểu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | uuid | Primary key | ID feedback |
| workspace_id | uuid | not null, Foreign key -> workspaces(id) on delete cascade | Workspace gửi feedback |
| rating | integer | not null, check 1..5 | Điểm hài lòng |
| content | text | not null | Nội dung feedback |
| support_note | text | nullable | Ghi chú hỗ trợ của admin |
| status | varchar(30) | not null | NEW, REVIEWED |
| reviewed_by | uuid | Foreign key -> users(id) | Admin review |
| reviewed_at | timestamp with time zone | nullable | Thời điểm review |
| created_at | timestamp with time zone | not null | Thời điểm tạo |
| updated_at | timestamp with time zone | not null | Thời điểm cập nhật |

## Indexes

| Index | Bảng | Cột |
| --- | --- | --- |
| idx_users_workspace | users | workspace_id |
| idx_tasks_workspace | tasks | workspace_id |
| idx_tasks_assignee | tasks | assignee_id |
| idx_task_updates_task | task_updates | task_id |
| idx_daily_reports_workspace_date | daily_reports | workspace_id, report_date |
| idx_notifications_user_read | notifications | user_id, is_read |
| idx_ai_suggestions_workspace | ai_suggestions | workspace_id |
| idx_workspaces_status | workspaces | status |
| idx_workspaces_subscription_plan | workspaces | subscription_plan_id |
| idx_workspace_registrations_status | workspace_registrations | registration_status, payment_status |
| idx_business_feedback_workspace | business_feedback | workspace_id |
| idx_business_feedback_status | business_feedback | status |
