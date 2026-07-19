# Báo cáo hoàn tất phạm vi Backend và AI

Ngày: 2026-07-19  
Phạm vi thực hiện: Backend + AI. Frontend chỉ phân tích trong tài liệu, không sửa source.

## 1. Root cause workspace registration 403

Security dùng allowlist quá rộng ở một số chỗ nhưng filter JWT lại không đồng nhất và public namespace không được mô hình hoá theo exact method/path. Đã chuyển sang allowlist chính xác cho registration/payment status/callback/file QR; guest POST được kiểm thử không cần JWT.

## 2. Root cause CORS

Cấu hình thiếu tập custom header và exposed header cần cho flow registration/workspace/idempotency/correlation. Đã khai báo origin rõ ràng, OPTIONS, method, header và expose `X-Request-Id`, `Location`, `Content-Disposition`; không dùng wildcard origin với credential.

## 3. Root cause AI 502

Core service ném lỗi provider xuyên qua controller. Đã thêm fallback typed cho estimate, recommendation explanation, workload risk và report/summary; history ghi `FALLBACK`, không trả raw provider error cho người dùng.

## 4. Root cause repeated AI requests

Contract trước không phân biệt kết quả tính toán và lỗi diễn giải, khiến client có xu hướng retry toàn bộ. BE nay trả kết quả fallback hợp lệ. FE phải dùng single explicit mutation và chỉ retry explanation theo hành động người dùng như tài liệu FE.

## 5. Root cause duplicate leader recommendation

Thiếu invariant rõ ràng giữa candidate ranking và selection UI. Backend giữ nguyên thứ tự/rank; AI không được phép sắp lại. FE contract quy định leader single-select, member không chứa leader và không gọi lại API khi đổi candidate detail.

## 6. Payment configuration changes

Bank transfer dùng cấu hình có cấu trúc, VietQR động và tùy chọn file QR upload DB; không nhận image URL ngoài. MoMo chỉ nhận dữ liệu provider động. Giao dịch lưu provider/config/QR/response snapshot bất biến; QR upload được kiểm MIME/magic/dimension/size.

## 7. Workspace activation changes

Payment và registration được pessimistic-lock; callback/confirm ghi provider transaction, người xác nhận và thời điểm; activation reload registration dưới lock, có unique guard registration→workspace và xử lý idempotent để tránh tạo workspace/subscription/owner lặp.

## 8. HR và Business Owner

Thêm `HR_ACCOUNT_MANAGE`, `EMPLOYEE_IMPORT`. Owner chỉ quản lý vòng đời tài khoản HR và xem tổng quan, không còn quyền CRUD vận hành hằng ngày. HR quản lý employee/department/position/role/import. Endpoint employee chuẩn hoá dưới `/api/workspace/hr/employees`.

## 9. Frontend layout changes

Không sửa FE vì không có source và theo yêu cầu người dùng. Đặc tả đầy đủ nằm tại `docs/frontend-detailed-analysis-be-ai-contract-2026-07-19.md`, gồm route theo vai trò, state machine, list/detail, import wizard, AI push-up panel, responsive và accessibility.

## 10. AI fallback design

Backend tiếp tục tính candidate/score/rank và trả dữ liệu có cấu trúc khi provider lỗi hoặc hết quota. AI chỉ bổ sung explanation. Fallback có metadata/status riêng, lưu history/suggestion và không thay đổi thứ tự candidate.

## 11. Database migrations

- V20: payment snapshot, QR files, activation unique guard/index.
- V21: permission Owner/HR theo trách nhiệm mới.
- V22: audit context, nullable system actor/workspace, AI fallback index.
- V23: xoá plaintext `initial_password`.
- V24: employee import batches/rows/index.

V19 là thay đổi username owner đã tồn tại trong working tree trước phiên làm việc và được giữ nguyên. Không sửa migration cũ để tránh làm lệch Flyway checksum.

## 12. Files changed

Nhóm thay đổi: Spring Security/CORS/JWT/request correlation, controllers, DTO/enums, authorization/service, payment/QR/import persistence, V20–V24, application/env config, Maven tests, AI test fixture và docs. Danh sách chính xác nên lấy bằng `git status --short` trước commit vì working tree đã có thay đổi của người dùng từ trước.

## 13. Endpoints added/changed

- Public: registration/payment status/callback allowlist chính xác; `GET /api/public/payment-files/{fileId}`.
- Admin: upload/delete QR tại `/api/admin/payment-qr-settings/{paymentMethod}/qr-image`; transaction/config view mở rộng.
- Owner: `/api/workspace/business-owner/hr-accounts` list/create/status.
- HR: canonical employee CRUD và `/api/workspace/hr/employees/import*` cho template, validate, history, detail, confirm, error report, cancel.

## 14. Permissions

Thêm `HR_ACCOUNT_MANAGE`, `EMPLOYEE_IMPORT`; gỡ khỏi BUSINESS_OWNER các permission employee/department/position/role/project/task/AI vận hành. Service employee mutation kiểm permission cụ thể thay vì shortcut owner-or-HR.

## 15. Test data

Không thêm seed production mới. Excel template tự sinh có sheet `Employees` và `MasterData`; test dùng H2 schema tạm, không ghi dữ liệu thật. Seed cũ được giữ nguyên; V23 đảm bảo plaintext initial password bị xoá/drop.

## 16. Automated test results

- Backend: `mvn test` — 6 tests, 0 failure, 0 error, BUILD SUCCESS. Bao gồm Spring context/repository, exact public security/CORS và authorization responsibility.
- AI: `python -m unittest discover -s tests -v` — 13 tests, OK; gồm provider fallback, cache, estimate/recommendation, workload và bảo toàn rank/backend numbers.

Spring context test chủ động dùng Hibernate `create-drop`; Flyway production migrations vẫn dành cho PostgreSQL. Đây là lựa chọn để kiểm entity/repository mà không sửa checksum migration lịch sử dùng PostgreSQL-specific SQL.

## 17. Manual test checklist

- Guest register không JWT; origin hợp lệ/lạ; preflight custom headers.
- Create payment bank/MoMo, callback duplicate, admin confirm/fail concurrent.
- Activation duplicate/concurrent chỉ sinh một workspace/subscription/owner.
- Upload PNG/JPEG/WEBP hợp lệ; giả MIME/quá cỡ/kích thước sai bị chặn.
- Owner tạo/khoá HR và không CRUD employee; HR CRUD/import đúng workspace.
- Import duplicate email/department-position mismatch/limit/confirm lại/error workbook.
- Ngắt AI provider và xác minh core endpoint trả fallback, rank không đổi, history `FALLBACK`.
- Audit SYSTEM/admin/workspace có requestId, result và không chứa secret.

## 18. Remaining limitations

Chưa có PostgreSQL/Testcontainers trong workspace nên migration V20–V24 cần chạy staging PostgreSQL trước production. Audit chưa có interceptor toàn cục để ghi mọi failure; một số legacy AI endpoint ngoài core flow vẫn có handler 502. `ForepService` vẫn là monolith lớn cần tách module dần. Chưa có FE source/E2E và chưa kiểm provider MoMo thật. Không cam kết zero-downtime cho V23 nếu còn binary cũ đọc `initial_password`.

## 19. Deployment environment variables

Tối thiểu: PostgreSQL datasource URL/user/password; JWT secret/expiry; `CORS_ALLOWED_ORIGINS`; AI service URL/timeout/provider key/model/quota; MoMo endpoint/partner/access/secret/callback/return URL; bank/VietQR config; public backend base URL. Secret phải ở secret manager của Render/Vercel, không commit `.env`.

Triển khai theo thứ tự: backup DB → deploy binary tương thích schema mới → Flyway V20–V24 trên staging → smoke test → production. Với V23, dừng/upgrade mọi instance binary cũ trước khi drop column hoặc tách thành rollout hai bước nếu cần zero downtime.

## 20. Rollback plan

Backup/snapshot PostgreSQL trước migration. Rollback ứng dụng bằng artifact trước; các migration thêm bảng/cột có thể giữ lại khi rollback binary. V23 là destructive nên phục hồi column chỉ khôi phục schema, không khôi phục plaintext đã xoá; đây là chủ ý bảo mật. Nếu import/payment có sự cố, disable endpoint bằng security/config, không xoá transaction/audit. QR file có thể giữ orphan tạm và dọn bằng job sau khi đối soát.
