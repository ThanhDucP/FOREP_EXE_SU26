# FOREP BE/AI System Audit

Ngày audit: 2026-07-19  
Phạm vi triển khai: Backend Spring Boot, AI service FastAPI và database migration. Frontend chỉ được phân tích, không sửa mã nguồn.

## 1. Executive summary

Codebase hiện là modular monolith ở mức triển khai nhưng phần lớn nghiệp vụ tập trung trong `ForepService` (hơn 5.000 dòng). Contract mới dưới `/api/public`, `/api/admin`, `/api/workspace` cùng tồn tại với contract cũ dưới `/api/v1`, tạo ra endpoint trùng, rule security trùng và nguy cơ hành vi không nhất quán.

Các blocker chính:

| Priority | Vấn đề | Root cause | Impact | Hướng sửa | Test |
|---|---|---|---|---|---|
| P0 | Public registration có thể bị 403 ở môi trường triển khai | Security cho phép toàn bộ `/api/public/**` thay vì allowlist theo method; JWT filter không có skip matcher rõ ràng; cấu hình production có thể chưa dùng đúng build; không có integration test để khóa hành vi | Guest không thể bắt đầu subscription flow | Allowlist chính xác từng public endpoint/method, skip JWT trên public/OPTIONS, thêm MockMvc security test | POST không Authorization trả 200; endpoint public không nằm trong allowlist trả 401/403 |
| P0 | CORS chặn `Authorization` và custom headers | `allowedHeaders` chỉ có 5 header, thiếu `X-Workspace-Id`, `X-Registration-Token`, `Idempotency-Key`; default origin là port 5173, không phải contract local 3000/3001 | Browser chặn request trước khi vào controller | CORS tập trung trong SecurityFilterChain, explicit origins/methods/headers/exposed headers | OPTIONS từ Vercel/local chứa đủ allow headers và trả 200/204 |
| P0 | Payment/activation chưa an toàn khi callback đồng thời | Repository không có pessimistic lock; `confirmPayment` và activation là private methods trong cùng bean nên boundary transaction/idempotency chưa được bảo đảm; check `registration.workspaceId` là check-then-act | Callback lặp có thể tạo duplicate workspace/account/subscription | Tách `WorkspaceActivationService`, khóa payment và registration trong transaction, unique constraints/idempotency key | Gọi callback lặp và concurrent chỉ sinh một workspace/subscription |
| P1 | AI summary/explanation trả 502 | Các path dùng `invokeAiMap` rethrow `AiProviderException`; controller cố ý map exception sang 502 | Dashboard và explanation bị hỏng khi provider lỗi | Fallback typed response tại backend; trả `source=FALLBACK`, `aiAvailable=false`; vẫn ghi history FAILED/FALLBACK | Mock provider timeout/invalid JSON và assert HTTP 200 + fallback |
| P1 | Payment configuration sai mô hình | `payment_qr_settings` bắt buộc `qr_code_url` cho cả MoMo và bank; service cho phép URL/paymentUrl/deeplink tĩnh; transaction thiếu configuration snapshot đầy đủ | QR cũ thay đổi theo config, MoMo bị nhập dữ liệu động ở admin | Tách configuration theo method; MoMo dùng secret/env; bank dùng managed file/VietQR; snapshot vào transaction | Update config sau khi tạo payment không làm thay đổi payment cũ |
| P1 | HR import chưa có | Không có entity, repository, controller, service hoặc dependency Apache POI cho import batch/row | HR không thể import/preview/confirm/error-report | Thêm batch/row schema, POI streaming validation, preview/confirm idempotent | Template, validation, limit, confirm và error report tests |
| P1 | Workspace isolation chưa được test hệ thống | Nhiều service lookup dùng `findById` rồi kiểm tra thủ công; attachment/update repositories chỉ query theo task id | Nguy cơ IDOR giữa tenant nếu thiếu một guard | Mọi aggregate workspace dùng scoped repository/service guard | Token workspace A không đọc/sửa ID workspace B |
| P1 | Business Owner/HR boundary chưa hoàn chỉnh | Permission matrix động đã có nhưng endpoint employee cũ `/api/v1/employees` vẫn là entry chính; chưa có initial-HR workflow riêng | Owner có thể tiếp tục daily HR nếu permission seed/config sai | Endpoint HR namespace, permission-only authorization, owner chỉ có `HR_ACCOUNT_MANAGE` cho account HR ban đầu | Owner bị 403 với employee CRUD, HR được phép |
| P2 | Audit log thiếu dữ liệu điều tra | Entity chỉ lưu actorId/action/entity/old/new/createdAt; thiếu actor snapshots, result, IP, UA, requestId, metadata; write failure bị nuốt | Trang audit nghèo dữ liệu và khó truy vết | Mở rộng schema/context, ghi ở service và security-critical paths, filter pageable | Critical action tạo log chứa request/actor/result |
| P2 | API envelope thiếu metadata | `ApiResponse.ok/error` trả `meta={}`; exception handlers lặp ở 3 controller | Không có correlation ID/timestamp; contract lỗi không đồng nhất | Global advice + request-id filter + envelope chuẩn | Success/error đều có requestId/timestamp |
| P2 | AI saved suggestion chứa JSON string thô | `AiSuggestionEntity.outputData` là text JSON và dashboard trả trực tiếp | FE dễ hiển thị raw JSON/generic content | Persist summary/title/details/actions/risks hoặc typed JSON contract, list trả summary | List không trả payload lớn; detail có typed content |
| P2 | AI history chưa đủ contract | Có table/history nhưng list chưa có duration, source, fallback status, error code; lưu đồng bộ theo từng call | Không phân tích reliability/cost và UI khó compact | Mở rộng history, pageable filters, status `FALLBACK` | Filter/date/status và duration test |
| P2 | Endpoint duplicate/dead contract | `ForepController` `/api/v1` trùng đáng kể với ba controller namespace mới | Matcher order và DTO drift; khó deprecate | Chọn canonical `/api/public|admin|workspace`, đánh dấu `/api/v1` deprecated và có kế hoạch xóa | Contract inventory/OpenAPI không còn ambiguity |
| P3 | Encoding source/docs bị mojibake | Nhiều string tiếng Việt trong source/migration đã bị decode sai | Message/log/data seed không đọc được | Chuẩn hóa UTF-8 theo file, tránh rewrite hàng loạt không kiểm soát | Build/resource UTF-8 và snapshot message |

## 2. Existing architecture inventory

### 2.1 Modules

- `backend`: Spring Boot 3.3.6, Java 21, Spring Security, JWT, Spring Data JPA, Flyway, PostgreSQL/H2.
- `ai-service`: FastAPI/Pydantic; provider orchestration, structured request/response and local deterministic fallbacks.
- `docs`: product, API, authorization, AI and payment documentation.
- Không có frontend source trong repository được audit. Chỉ có tài liệu FE; vì vậy không thể xác nhận route guard, hook retry, duplicate leader rendering hoặc current page implementation bằng code.

### 2.2 Controllers and endpoint families

- `PublicRegistrationController`: subscription plans, guest registration, plan selection, payment creation/status, MoMo/bank callbacks.
- `AdminPlatformController`: plans, registrations, payments, QR settings, workspace lifecycle, feedback, audit, dashboards, platform AI summary.
- `WorkspaceOperationsController`: tasks, HR departments/positions, recommendation/AI endpoints, history, workload and owner dashboard.
- `WorkspaceFeedbackController`: workspace feedback.
- `ForepController`: legacy `/api/v1` contract containing auth, registrations, payments, admin, employee, task, analytics, AI, report and notification endpoints. Đây là duplicate implementation lớn nhất.

### 2.3 Entities/tables currently mapped

`workspaces`, `workspace_registrations`, `workspace_subscriptions`, `subscription_plans`, `payment_transactions`, `payment_qr_settings`, `users`, `role_permissions`, `departments`, `job_positions`, `tasks`, `task_assignees`, `task_attachments`, `task_updates`, `daily_reports`, `notifications`, `business_feedback`, `ai_history`, `ai_suggestions`, `audit_logs`.

Thiếu so với target schema: `payment_configurations` theo kiểu/provider mới, managed files, `employee_profiles` tách riêng, `employee_import_batches`, `employee_import_rows`, project aggregate rõ ràng, task team member aggregate chuẩn hóa, AI saved content fields, audit context fields.

### 2.4 Security and authorization

- Stateless JWT; authority được tạo từ `RolePermissionRepository` và fallback matrix trong `AuthorizationService`.
- Security dùng permission matchers, đây là nền tảng đúng hướng.
- Sai lệch: public matcher quá rộng (`/api/public/**`), chưa có method-specific allowlist; JWT filter parse token trên mọi request và không override `shouldNotFilter`; exception/entry point envelope chưa chuẩn hóa.
- Workspace isolation chủ yếu thực hiện trong service, chưa có integration test.

### 2.5 CORS

- Một `CorsConfigurationSource` trong `SecurityConfig`, không thấy filter CORS thứ hai.
- Methods đã đủ.
- Allowed headers thiếu ba header nghiệp vụ bắt buộc và exposed headers hoàn toàn chưa cấu hình.
- `setAllowedOriginPatterns` nhận env; với credentials nên dùng explicit origins cho production/local.

### 2.6 AI integration

- Backend `AiServiceClient` có connect/read timeout, concurrency semaphore, in-flight dedupe, circuit breaker và rate-limit exception.
- AI service dùng Pydantic strict schemas và nhiều response contract typed.
- Backend đã có rule-based candidate scoring/fallback cho một số chức năng.
- Lỗ hổng: summary/explanation path còn gọi generic `invokeAiMap` và rethrow; controller trả 502; fallback metadata chứa provider reason có thể rò kỹ thuật; recommendation path có bước gọi AI để enrich domain trước scoring dù core vẫn có fallback.

### 2.7 Payment and activation

- Backend lấy amount từ subscription plan, không tin amount frontend.
- Callback có signature validation và amount check.
- Transaction có provider URLs/request/response nhưng chưa có immutable configuration snapshot và confirmation metadata đầy đủ.
- Activation tạo workspace/subscription/owners và có check workspaceId/payment subscription, nhưng thiếu lock/concurrency transaction service rõ ràng.
- User worktree hiện có thay đổi owner provisioning/username; phần này phải được giữ và tích hợp, không ghi đè.

### 2.8 Audit and Excel

- Audit ghi từ service cho một số critical action; lỗi ghi log chỉ được log warning.
- Không có request correlation/filter metadata hoặc result.
- Excel employee import hoàn toàn chưa được triển khai.

## 3. Database review

- Có Flyway V1–V19; production dùng `ddl-auto: validate`, đúng nguyên tắc không auto-update.
- Các index hiện có chưa phủ đầy đủ target query: cần rà `status`, `created_at`, `deadline`, scoped compound indexes và callback identifiers.
- `V17` bắt buộc QR URL, xung đột trực tiếp với yêu cầu upload/VietQR và MoMo dynamic response.
- Unique payment/subscription đã có một phần, nhưng activation cần unique relation registration→workspace và transaction-safe locking.
- Một số seed dùng URL QR bên ngoài; chỉ phù hợp demo cũ, không phù hợp production contract mới.

## 4. Test baseline

- `mvn test`: build thành công, nhưng báo `No tests to run`.
- Python test chưa chạy ở baseline do runtime hiện tại chưa có `pytest`; dependency này cũng không có trong `requirements.txt`.
- Chưa có automated evidence cho registration security, CORS, isolation, payment callback, activation, authorization, AI fallback hoặc audit.

## 5. Implementation decision

Thứ tự thực hiện:

1. P0 security/CORS/public registration tests.
2. Payment snapshot, callback idempotency và activation transaction.
3. Permission boundary HR/Owner và employee import backend.
4. AI typed fallback/history/suggestion contract; không trả 502 cho optional explanation/summary.
5. Audit/envelope/indexes/test data và integration tests.
6. Viết riêng FE implementation specification trong `docs`; không sửa frontend.

## 6. Verification gates

Mỗi phase chỉ được xem là đạt khi build xanh và có test tự động tương ứng. Những mục phụ thuộc provider thật, Render/Vercel, object storage hoặc callback thực tế phải được ghi là manual/deployment verification, không được báo hoàn tất chỉ dựa trên unit test local.
