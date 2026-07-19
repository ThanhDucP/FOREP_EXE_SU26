# Phân tích triển khai Frontend theo hợp đồng BE/AI

Ngày: 2026-07-19  
Phạm vi: tài liệu phân tích; **không thay đổi mã nguồn Frontend**.

## 1. Hiện trạng và nguyên tắc biên

Repository hiện không có ứng dụng Next.js để kiểm tra hoặc chỉnh sửa trực tiếp. Vì vậy tài liệu này là đặc tả triển khai cho đội FE dựa trên contract BE/AI đã được rà soát. FE chỉ hiển thị dữ liệu có cấu trúc; không tự tính xếp hạng, không diễn giải JSON thô và không suy đoán trạng thái thanh toán/kích hoạt.

Ba biên trách nhiệm:

- BE tính điểm, xếp hạng, giới hạn workspace, quyền và trạng thái nghiệp vụ.
- AI chỉ diễn giải kết quả BE; lỗi AI không được làm hỏng luồng chính.
- FE quản lý trạng thái trình bày, điều hướng, nhập liệu và thông báo có thể hành động.

## 2. Kiến trúc FE đề xuất

Next.js App Router, TypeScript strict, TanStack Query cho server state, React Hook Form + Zod cho form, Shadcn/UI và TailwindCSS. Không lưu JWT trong source code hoặc query string; ưu tiên cookie bảo mật do BFF, nếu hệ thống hiện tại buộc bearer token thì giữ trong memory và có chiến lược refresh riêng.

Các lớp nên tách:

```text
app/                  route + loading/error boundary
features/             registration, payment, employees, ai-recommendation...
components/domain/    component nghiệp vụ dùng lại
components/ui/        primitive Shadcn
lib/api/              client, envelope, error mapping, generated types
lib/auth/             session, permission guard, route policy
lib/query/            keys, invalidation policy
types/contracts/      DTO đồng bộ OpenAPI
```

## 3. API client và envelope

Một API client duy nhất phải:

- Gửi `Authorization`, `Content-Type`, `Accept`, `X-Workspace-Id`, `X-Registration-Token`, `Idempotency-Key` đúng ngữ cảnh.
- Nhận và log `X-Request-Id`; hiển thị mã này trong vùng chi tiết hỗ trợ khi có lỗi.
- Giải nén envelope thống nhất gồm `success`, `data`, `message`, `errors`, `metadata.requestId`, `metadata.timestamp`.
- Phân loại 400 validation, 401 hết phiên, 403 thiếu quyền, 404 không tồn tại/không thuộc workspace, 409 xung đột/idempotency, 422 lỗi nghiệp vụ và 5xx lỗi tạm thời.
- Không hiển thị stack trace, provider response, “null response”, “AI provider error” hoặc raw JSON.

Không tự retry mutation. Query GET chỉ retry tối đa 1 lần với network/5xx; không retry 401/403/404. Payment status polling dùng backoff, dừng khi trạng thái terminal hoặc khi tab ẩn quá lâu.

## 4. Route map theo vai trò

### Guest

- `/plans`: chọn gói.
- `/register?planId=...`: form đăng ký workspace.
- `/registration/{registrationId}/payment`: chọn phương thức và tạo giao dịch.
- `/registration/{registrationId}/payment/{paymentCode}`: QR/deeplink/trạng thái.
- `/registration/{registrationId}/result`: chờ xác nhận hoặc đã kích hoạt.

Các trang sau bước đăng ký phải dùng registration token trong header, không đưa token vào URL. Refresh trang vẫn hoạt động bằng sessionStorage hoặc secure short-lived cookie; nếu mất token, yêu cầu quay lại tra cứu an toàn, không đoán dữ liệu bằng registrationId.

### Platform Admin

- `/admin/dashboard`, `/admin/registrations`, `/admin/payments`, `/admin/workspaces`.
- `/admin/payment-settings`: bank transfer cho phép nhập cấu hình + upload file QR; MoMo chỉ cấu hình kết nối provider, không có trường URL ảnh/URL thanh toán/deeplink thủ công.
- `/admin/audit-logs`, `/admin/ai-history`.

### Business Owner

- `/owner/dashboard`, `/owner/hr-accounts`, các trang báo cáo tổng quan.
- Owner tạo/khoá/mở HR ban đầu; không hiển thị nút CRUD nhân viên, phòng ban, chức danh, dự án, task hoặc gọi AI vận hành nếu permission không có.
- Mật khẩu tạm chỉ hiện một lần sau create/reset, có nút copy và xác nhận đã lưu; không có API xem lại.

### HR

- `/hr/dashboard`, `/hr/employees`, `/hr/employees/imports`, `/hr/departments`, `/hr/positions`, `/hr/roles`.
- HR quản trị workforce; không có quyền xác nhận payment hay quản trị workspace nền tảng.

### Manager / Executive / Employee

Menu sinh từ permission contract, không chỉ từ role string. Manager/Executive nhận các route dự án, task, workload, AI phù hợp quyền; Employee chỉ thấy công việc và báo cáo của mình. Route guard chỉ cải thiện UX; BE vẫn là nguồn quyền cuối cùng.

## 5. State machine đăng ký và thanh toán

```text
PLAN_SELECTED -> REGISTRATION_CREATED -> PAYMENT_CREATED
       -> PENDING -> CONFIRMED -> ACTIVATING -> ACTIVATED
                         \-> FAILED / EXPIRED / CANCELLED
```

FE phải render theo trạng thái server và cho phép refresh an toàn. Khi create payment bị timeout, tra cứu lại bằng registration/token trước khi tạo mới. Nút tạo/xác nhận vô hiệu hoá trong lúc request; dùng `Idempotency-Key` ổn định cho cùng một ý định người dùng.

Màn hình QR hiển thị `qrDisplayData`/display URL do BE trả về, mã thanh toán, số tiền, hạn, hướng dẫn và trạng thái. Không tự ghép VietQR, không sửa amount/reference. MoMo ưu tiên deeplink/provider QR trả về từ transaction; bank transfer dùng snapshot cấu hình của giao dịch nên thay đổi cấu hình admin không làm đổi giao dịch cũ.

## 6. Payment Settings

Form bank gồm bank code/name, account number/name, template/nội dung mặc định và upload ảnh QR tùy chọn. Upload chỉ nhận PNG/JPEG/WEBP, hiển thị preview local rồi URL kiểm soát từ BE sau khi lưu. Nút xoá yêu cầu confirm và cập nhật preview.

Form MoMo chỉ gồm credential/config mà BE cho phép; tuyệt đối không có image URL, payment URL hoặc deeplink nhập tay. Secret phải là password field, không đọc ngược giá trị đã lưu; UI dùng trạng thái “đã cấu hình”.

## 7. Employee list và Excel import

List nhân viên ngắn gọn: avatar/tên, mã, phòng ban, chức danh, trạng thái và menu hành động. Chi tiết mở drawer/sheet; mobile chuyển thành full-screen sheet. Filter/search/page là URL state.

Import là wizard:

1. Tải template.
2. Chọn `.xlsx`, hiển thị tên/kích thước và upload.
3. Hiển thị tổng dòng, hợp lệ, lỗi; preview theo tab và lỗi cụ thể từng ô/dòng.
4. Chỉ bật Confirm khi còn dòng hợp lệ và batch chưa terminal.
5. Sau confirm hiển thị số thành công/thất bại; invalidate employee list và import history.
6. Cho tải error workbook; cancel chỉ áp dụng batch chưa confirm.

Không parse/import Excel ở browser làm nguồn dữ liệu chính. Validation FE chỉ phản hồi sớm; kết quả BE mới có giá trị.

## 8. Quy chuẩn list/detail toàn hệ thống

Mọi list chỉ để thông tin nhận diện, trạng thái, 1–2 metric và action. Dữ liệu dài mở detail drawer/modal/master-detail. Desktop dùng bảng + drawer 480–720px hoặc split panel; tablet/mobile dùng card và full-screen sheet. Skeleton giữ layout; empty state nói rõ hành động tiếp theo; lỗi có retry có kiểm soát.

Không nhồi description, AI explanation, audit metadata hoặc provider snapshot vào cột bảng. Dữ liệu kỹ thuật chỉ dành cho admin trong accordion “Chi tiết kỹ thuật”, có permission phù hợp.

## 9. AI Recommendation UX

Từ task form, người dùng bấm “Gợi ý nhân sự” đúng một lần. Mutation key gắn với fingerprint input; trong lúc chạy vô hiệu hoá nút. Không dùng effect phụ thuộc state kết quả để gọi lại API.

Kết quả mở push-up panel lớn:

- Trái: candidate đã theo thứ tự BE, score, rank, tag phù hợp và workload.
- Phải: chi tiết candidate đang chọn, breakdown điểm BE, lý do có cấu trúc, rủi ro/cảnh báo và kinh nghiệm.
- Chọn candidate khác chỉ đổi detail, không gọi lại AI.
- Leader là lựa chọn đơn; member là multi-select và không được chứa leader hai lần.
- Confirm trả selection về task form; close không tự lưu.

FE không sort lại theo explanation hoặc score AI. Nếu response có `fallback=true`/status fallback, hiển thị banner trung tính: “Đã dùng phân tích quy tắc do phần giải thích AI tạm thời không khả dụng”; vẫn cho chọn kết quả BE. Chỉ nút “Thử tạo lại phần giải thích” mới phát sinh request mới và phải rate-limit ở UI.

## 10. Task form và dữ liệu bắt buộc

Form phải tổ chức title, mô tả, project/domain, department/position phù hợp, kỹ năng, độ ưu tiên, ngày bắt đầu/hạn, estimated hours, leader/member và constraint. Validation chéo: hạn không trước ngày bắt đầu; leader không trùng member; tổng lựa chọn không vượt giới hạn; dữ liệu recommendation cũ bị đánh dấu stale khi input ảnh hưởng xếp hạng thay đổi.

## 11. AI history và saved suggestions

History list hiển thị thời gian, loại yêu cầu, subject, trạng thái `SUCCESS/FALLBACK/FAILED`, model/provider khi có. Detail render structured sections; JSON provider chỉ có thể mở với admin debug permission. Saved suggestion phải gắn task/workspace/user, hiển thị snapshot xếp hạng và selection đã chọn; không tái gọi AI chỉ để xem lại.

## 12. Audit và dashboard

Audit list: thời gian, actor snapshot, role, action, resource, result, requestId. Filter theo thời gian/action/result/actor/workspace. Detail mới hiển thị IP, user-agent và metadata. Không hiển thị credential/token.

Dashboard chỉ dùng aggregate BE: KPI cards, xu hướng theo thời gian, phân bố workload/status và danh sách cảnh báo ngắn. Chart phải có legend, tooltip, bảng dữ liệu thay thế và màu đạt contrast; không tự cộng số từ nhiều trang list ở client.

## 13. Error, loading và accessibility

- Toast dành cho mutation ngắn; lỗi form đặt cạnh field; lỗi trang dùng error state có retry.
- 401: xoá session và chuyển login, giữ `returnTo` nội bộ an toàn.
- 403: trang không có quyền, không giả thành 404 ở UI.
- 409: tải lại dữ liệu và giải thích đã có tiến trình khác hoàn tất.
- Mọi dialog có focus trap; panel trả focus về nút mở; icon button có accessible name.
- QR có alt text và mã thanh toán dạng text để copy; bảng sử dụng header semantic.

## 14. Query key và invalidation

Query key phải chứa workspaceId và filter, ví dụ `['employees', workspaceId, filters]`. Khi workspace/session đổi, clear toàn bộ cache tenant trước. Confirm import invalidate employees/import batches/dashboard; confirm payment invalidate transaction/registration/workspace; lưu task invalidate task/detail/workload nhưng không tự gọi recommendation.

## 15. Bảo mật phía FE

Không dùng role guard làm lớp bảo mật duy nhất. Không đưa registration token, JWT, provider secret vào analytics/log/error monitoring. Không render HTML từ AI nếu chưa sanitize; mặc định render text/structured components. Chặn open redirect ở `returnTo`. File upload kiểm tra extension/MIME để UX nhưng luôn tin kết quả validation BE.

## 16. Contract types cần sinh

Nên sinh TypeScript từ OpenAPI trong CI và fail khi drift. Các type trọng tâm: ApiResponse, RegistrationStatus, PaymentStatus, PaymentTransactionView, ActivationResult, Permission, CreatedUserAccountView, EmployeeImportBatch/Row, AiRecommendationResult/Candidate/ScoreBreakdown và AuditLogView.

## 17. Ma trận acceptance FE

- Guest đăng ký và xem trạng thái mà không gửi JWT; endpoint ngoài allowlist vẫn bị chặn.
- Preflight từ Vercel/localhost hợp lệ với mọi custom header; origin lạ bị từ chối.
- Refresh ở mọi bước payment không tạo giao dịch hoặc workspace trùng.
- Hai lần bấm confirm không tạo hai workspace/owner.
- Owner không thấy và không gọi được employee CRUD; HR làm được đúng workspace.
- Import hiển thị preview, lỗi dòng, confirm idempotent, error report tải đúng.
- AI lỗi vẫn có ranking BE, UI không lộ lỗi provider và không lặp request.
- Candidate order giữ nguyên; leader không bị trùng; selection được trả về task form một lần.
- Chuyển workspace không rò cache tenant trước.
- List/detail sử dụng được bằng bàn phím và responsive 360px–desktop.

## 18. Thứ tự triển khai FE khuyến nghị

1. API client, envelope, requestId, auth/permission và tenant cache isolation.
2. Guest registration + payment state machine.
3. Admin payment settings/activation/audit.
4. Owner HR-account onboarding và HR employee/import.
5. Task form + AI recommendation panel/history/fallback.
6. Dashboard, responsive/accessibility hardening và E2E acceptance suite.

## 19. Test FE bắt buộc khi có source

Unit test error mapper, permission policy, state reducers và selection invariant. Component test import wizard/AI panel/payment settings. MSW integration test contract + fallback. Playwright E2E cho guest registration, payment refresh/idempotency, role navigation, employee import, AI failure, mobile layout và keyboard accessibility.

## 20. Điểm cần đội FE xác nhận

Repository FE thực tế, cơ chế lưu/refresh JWT, domain Vercel chính thức, OpenAPI URL và policy analytics/error tracking chưa có trong workspace hiện tại. Các mục này không cản BE/AI nhưng phải được quyết định trước khi triển khai FE production.
