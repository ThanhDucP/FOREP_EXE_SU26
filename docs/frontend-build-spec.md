# Dac Ta Xay Front-End FOREP EXE

Tai lieu nay mo ta day du phan front-end moi can xay cho FOREP EXE sau khi front-end cu da bi go khoi repo. Front-end moi phai fit truc tiep voi Backend API hien tai tai base URL `http://localhost:8080/api/v1`.

## 1. Nguyen tac tich hop API

- Front-end chi goi Backend API, khong goi truc tiep AI Service.
- Tat ca endpoint, tru `GET /health`, `POST /auth/login`, `GET /subscription-plans`, `GET /subscription-plans/active`, `POST /workspace-registrations`, `GET /workspace-registrations/{id}`, `PATCH /workspace-registrations/{id}/select-plan`, `POST /workspace-registrations/{id}/payments`, `GET /payments/{paymentId}`, `POST /payments/momo/callback`, `POST /payments/bank-transfer/callback`, can header `Authorization: Bearer <token>`.
- Moi response chuan co dang:

```json
{
  "data": {},
  "meta": {},
  "errors": []
}
```

- Khi `errors` khac rong, hien thi loi dau tien theo `errors[0].message`.
- Luu token sau login. Khuyen nghi dung memory state + `localStorage` neu can giu dang nhap sau reload.
- Date-time gui len backend dung ISO 8601 offset, vi du `2026-06-29T17:00:00+07:00`.
- Date cho daily report dung `YYYY-MM-DD`.
- Backend phan quyen theo role `SYSTEM_ADMIN`, `OWNER` va `EMPLOYEE`; UI phai an hanh dong khong dung role.

## 2. Enum dung trong UI

### Role

- `SYSTEM_ADMIN`: quan tri nen tang, goi subscription, thanh toan, workspace va business owner account khoi tao.
- `OWNER`: chu workspace, quan ly nhan vien, task, analytics, AI.
- `EMPLOYEE`: nhan vien, xem task duoc giao, cap nhat tien do, gui daily report.

### WorkspaceStatus

- `PENDING_PAYMENT`: cho thanh toan.
- `ACTIVE`: dang hoat dong.
- `INACTIVE`: chua kich hoat.
- `SUSPENDED`: bi tam dung.
- `EXPIRED`: het han.

### PaymentStatus

- `PENDING`: cho provider/admin xac nhan.
- `CONFIRMED`: da xac nhan thanh toan.
- `REJECTED`: thanh toan bi tu choi.
- `CORRECTION_REQUESTED`: can bo sung/sua thong tin thanh toan.

### PaymentMethod

- `MOMO`: thanh toan MoMo.
- `BANK_TRANSFER`: chuyen khoan ngan hang/VietQR.

### PaymentTransactionStatus

- `PENDING`: giao dich da tao, dang cho user thanh toan.
- `SUCCESS`: payment da duoc provider/admin xac nhan, workspace duoc kich hoat.
- `FAILED`: payment that bai/bi tu choi, user co the tao giao dich moi.
- `EXPIRED`: payment het han, user can tao giao dich moi.
- `CANCELLED`: giao dich bi huy.

### RegistrationStatus

- `PENDING_PLAN_SELECTION`: da gui thong tin dang ky, dang cho chon goi.
- `PENDING_PAYMENT`: da chon goi, dang cho tao/xac nhan payment.
- `PAYMENT_CONFIRMED`: payment da xac nhan, backend dang/da kich hoat workspace.
- `APPROVED`: da duyet va tao workspace.
- `REJECTED`: bi tu choi.
- `CANCELLED`: ho so bi huy.
- `SUBMITTED`, `PAYMENT_PENDING`, `PAYMENT_SUBMITTED`: trang thai cu chi dung de hien thi backward-compatible neu backend tra ve du lieu cu.

### UserStatus

- `ACTIVE`: dang hoat dong.
- `INACTIVE`: bi tat quyen.
- `INVITED`: trang thai du phong, hien backend chua co flow invite rieng.

### TaskPriority

- `LOW`: thap.
- `MEDIUM`: trung binh, default khi tao task neu bo trong.
- `HIGH`: cao.
- `CRITICAL`: khan cap.

### TaskStatus

- `ASSIGNED`: moi giao.
- `IN_PROGRESS`: dang lam.
- `BLOCKED`: co vuong mac.
- `COMPLETED`: hoan thanh.
- `CANCELLED`: da huy.

### UpdateType

- `PROGRESS`: cap nhat tien do thuong.
- `BLOCKER`: bao vuong mac, backend set task thanh `BLOCKED`.
- `COMPLETION`: hoan thanh, backend set progress thanh `100` va status thanh `COMPLETED`.

### WorkloadLevel

- `NO_WORK`: chua co viec mo.
- `LOW`: tai thap.
- `NORMAL`: binh thuong.
- `HIGH`: tai cao.
- `OVERLOADED`: qua tai.

### AiSuggestionStatus

- `GENERATED`: moi tao.
- `ACCEPTED`: owner da chap nhan.
- `REJECTED`: owner da tu choi.

## 3. Data model front-end

```ts
type ApiResponse<T> = {
  data: T | null;
  meta: Record<string, unknown>;
  errors: { code: string; message: string; field: string | null }[];
};

type Workspace = {
  id: string;
  name: string;
  shortCode: string | null;
  logo: string | null;
  address: string | null;
  ownerId: string;
  createdAt: string;
};

type User = {
  id: string;
  workspaceId: string;
  fullName: string;
  email: string;
  phone: string | null;
  username: string | null;
  employeeCode: string | null;
  initialPassword: string | null;
  role: 'OWNER' | 'EMPLOYEE';
  avatar: string | null;
  status: 'ACTIVE' | 'INACTIVE' | 'INVITED';
  jobTitle: string | null;
  seniorityLevel: 'INTERN' | 'JUNIOR' | 'MIDDLE' | 'SENIOR' | 'LEAD' | null;
  skillRating: 1 | 2 | 3 | 4 | 5 | null;
  yearsOfExperience: number | null;
  skills: string | null;
  createdAt: string;
  updatedAt: string;
};

type Task = {
  id: string;
  workspaceId: string;
  title: string;
  requirements: string;
  description: string | null;
  assigneeId: string;
  creatorId: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  deadline: string;
  estimatedHours: number;
  progressPercent: number;
  status: 'ASSIGNED' | 'IN_PROGRESS' | 'BLOCKED' | 'COMPLETED' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
};

type TaskUpdate = {
  id: string;
  taskId: string;
  userId: string;
  progressPercent: number;
  content: string;
  attachment: string | null;
  updateType: 'PROGRESS' | 'BLOCKER' | 'COMPLETION';
  createdAt: string;
};

type DailyReport = {
  id: string;
  workspaceId: string;
  userId: string;
  reportDate: string;
  todayCompleted: string;
  currentWork: string;
  blockers: string | null;
  tomorrowPlan: string | null;
  reviewedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

type Notification = {
  id: string;
  workspaceId: string;
  userId: string;
  type: string;
  title: string;
  message: string;
  relatedEntityType: string | null;
  relatedEntityId: string | null;
  isRead: boolean;
  createdAt: string;
};

type Workload = {
  employeeId: string;
  fullName: string;
  openTasks: number;
  inProgressTasks: number;
  blockedTasks: number;
  completedTasks: number;
  overdueTasks: number;
  estimatedWorkload: number;
  workloadScore: number;
  workloadLevel: 'NO_WORK' | 'LOW' | 'NORMAL' | 'HIGH' | 'OVERLOADED';
};

type AssigneeRecommendation = {
  employeeId: string;
  fullName: string;
  score: number;
  workloadLevel: Workload['workloadLevel'];
  requiredRole: string | null;
  roleFit: 'STRONG' | 'PARTIAL' | 'UNCERTAIN' | null;
  roleFitReason: string | null;
  reason: string;
  risk: string;
};

type SubscriptionPlan = {
  id: string;
  name: string;
  description: string | null;
  price: number;
  durationDays: number;
  durationInMonths: number;
  maxUsers: number;
  maxOwnerAccounts: number;
  maxEmployeeAccounts: number;
  hasFullFeatures: boolean;
  maxWorkspaces: number | null;
  aiUsageLimit: number | null;
  features: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
};

type WorkspaceRegistration = {
  id: string;
  businessName: string;
  workspaceName: string;
  workspaceIdentifier: string;
  contactEmail: string;
  contactPhone: string;
  businessAddress: string | null;
  representativeFullName: string;
  representativeEmail: string;
  representativePhone: string | null;
  subscriptionPlanId: string | null;
  maxUsers: number;
  maxOwnerAccounts: number;
  maxEmployeeAccounts: number;
  ownerFullName: string;
  ownerEmail: string;
  ownerPhone: string | null;
  paymentProofUrl: string | null;
  paymentStatus: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CORRECTION_REQUESTED';
  registrationStatus: 'PENDING_PLAN_SELECTION' | 'PENDING_PAYMENT' | 'PAYMENT_CONFIRMED' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'SUBMITTED' | 'PAYMENT_PENDING' | 'PAYMENT_SUBMITTED';
  workspaceId: string | null;
  reviewedBy: string | null;
  reviewedAt: string | null;
  reviewNote: string | null;
  createdAt: string;
  updatedAt: string;
};

type PlatformWorkspace = {
  id: string;
  businessName: string | null;
  workspaceName: string;
  workspaceIdentifier: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  businessAddress: string | null;
  subscriptionPlanId: string | null;
  maxUsers: number;
  maxOwnerAccounts: number;
  maxEmployeeAccounts: number;
  currentUsers: number;
  status: 'PENDING_PAYMENT' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'EXPIRED';
  paymentStatus: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CORRECTION_REQUESTED';
  ownerId: string | null;
  activatedAt: string | null;
  expiresAt: string | null;
  lastActivityAt: string | null;
  createdAt: string;
};

type PaymentTransaction = {
  id: string;
  workspaceRegistrationId: string;
  subscriptionPlanId: string;
  paymentMethod: 'MOMO' | 'BANK_TRANSFER';
  amount: number;
  currency: 'VND';
  orderCode: string;
  requestId: string;
  providerTransactionId: string | null;
  providerPaymentUrl: string | null;
  providerDeeplink: string | null;
  providerQrCodeUrl: string | null;
  bankCode: string | null;
  bankName: string | null;
  bankAccountNumber: string | null;
  bankAccountName: string | null;
  transferContent: string | null;
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'EXPIRED' | 'CANCELLED';
  paidAt: string | null;
  expiredAt: string | null;
  createdAt: string;
  updatedAt: string;
};
```

## 4. API client can co

- Ham request chung nhan `method`, `path`, `body?`, `query?`.
- Tu gan `Authorization` neu co token.
- Tu set `Content-Type: application/json` khi co body.
- Parse JSON theo `ApiResponse<T>`.
- Neu HTTP loi hoac `errors.length > 0`, throw loi chuan `{ code, message, field }`.
- Neu status `401` hoac `403`, xoa token va chuyen ve login hoac trang khong co quyen.

## 5. API contract theo module

### Auth

| Man hinh | Method | Path | Body | Data tra ve |
|---|---|---|---|---|
| Login | POST | `/auth/login` | `{ email, password }` hoac `{ username, password }` | `{ token, user }` |
| Logout | POST | `/auth/logout` | none | `{ message }` |
| Current user | GET | `/auth/me` | none | `User` |
| Doi mat khau | PATCH | `/auth/change-password` | `{ currentPassword, newPassword }` | `User` |

`POST /auth/logout` hien chi tra message, khong revoke token o server. Front-end phai tu xoa token local.
`PATCH /auth/change-password` can token dang nhap, dung cho employee tu doi mat khau trong trang tai khoan/bao mat. Sau khi thanh cong, UI nen xoa password tam dang hien thi trong local state neu co va thong bao "Doi mat khau thanh cong".

### Workspace

| Chuc nang | Method | Path | Body | Quyen |
|---|---|---|---|---|
| Danh sach goi dang ky active | GET | `/subscription-plans/active` | none | public |
| Gui thong tin dang ky workspace | POST | `/workspace-registrations` | `{ businessName, workspaceName, contactEmail, contactPhone, businessAddress, representativeFullName, representativeEmail, representativePhone }` | public |
| Xem ho so dang ky | GET | `/workspace-registrations/{id}` | none | public |
| Chon goi dang ky | PATCH | `/workspace-registrations/{id}/select-plan` | `{ subscriptionPlanId }` | public |
| Tao giao dich thanh toan | POST | `/workspace-registrations/{id}/payments` | `{ paymentMethod: 'MOMO' \| 'BANK_TRANSFER' }` | public |
| Xem payment | GET | `/payments/{paymentId}` | none | public |
| Xem workspace | GET | `/workspaces/current` | none | OWNER |
| Sua workspace | PUT | `/workspaces/current` | `{ name, shortCode, logo, address }` | OWNER |

Khong dung `/workspaces/register` cho user public nua. Endpoint nay da bi chan de tranh tao workspace/account khi chua thanh toan.

Flow dang ky workspace public:

1. Trang workspace registration nhap thong tin doanh nghiep va nguoi dai dien, goi `POST /workspace-registrations`, sau do chuyen sang trang chon goi bang `registrationId`.
2. Trang chon goi goi `GET /subscription-plans/active`, hien thi name, description, price, duration, `maxOwnerAccounts`, `maxEmployeeAccounts`, full features va nut chon goi.
3. Khi user chon goi, UI goi `PATCH /workspace-registrations/{id}/select-plan` roi chuyen sang trang chon phuong thuc thanh toan.
4. Trang chon payment method bat buoc user chon `MOMO` hoac `BANK_TRANSFER`, sau do goi `POST /workspace-registrations/{id}/payments`.
5. Trang payment instruction hien thi theo `PaymentTransaction`: MoMo QR/payUrl/deeplink hoac VietQR bank info, amount, orderCode, status.
6. UI poll `GET /payments/{paymentId}` moi 3-5 giay den khi status la `SUCCESS`, `FAILED` hoac `EXPIRED`.
7. Trang ket qua goi them `GET /workspace-registrations/{id}` de hien thi payment result va workspace activation status.
8. UI khong cho login owner khi payment chua `SUCCESS`; frontend khong tu tin payment success tu query string/callback client.

Neu khong thanh toan, user public khong tao duoc workspace/account. Chi System Admin moi duoc tao workspace truc tiep bang API admin.

### System Admin

Tat ca endpoint duoi day chi danh cho `SYSTEM_ADMIN`.

| Chuc nang | Method | Path | Body hoac query |
|---|---|---|---|
| Monitoring platform | GET | `/admin/monitoring` | none |
| Danh sach workspace | GET | `/admin/workspaces` | none |
| Tao workspace truc tiep | POST | `/admin/workspaces` | `{ businessName, workspaceName, workspaceIdentifier, contactEmail, contactPhone, businessAddress, subscriptionPlanId, maxUsers, activationDate, expirationDate, status }` |
| Chi tiet workspace | GET | `/admin/workspaces/{id}` | none |
| Sua workspace | PUT | `/admin/workspaces/{id}` | `{ businessName, workspaceName, contactEmail, contactPhone, businessAddress, subscriptionPlanId, maxUsers, activationDate, expirationDate, status }` |
| Doi trang thai workspace | PATCH | `/admin/workspaces/{id}/status?status=ACTIVE` | query `status` |
| Danh sach Business Owner | GET | `/admin/workspaces/{id}/business-owners` | none |
| Tao Business Owner | POST | `/admin/workspaces/{id}/business-owners` | `{ fullName, email, username, temporaryPassword, phone, status }` |
| Reset password owner | PATCH | `/admin/business-owners/{id}/reset-password` | none |
| Doi status owner | PATCH | `/admin/business-owners/{id}/status?status=ACTIVE` | query `status` |
| Danh sach goi | GET | `/admin/subscription-plans` | none |
| Tao goi | POST | `/admin/subscription-plans` | `{ name, description, price, durationDays, durationInMonths, maxUsers, maxOwnerAccounts, maxEmployeeAccounts, hasFullFeatures, maxWorkspaces, aiUsageLimit, features, status }` |
| Sua goi | PUT | `/admin/subscription-plans/{id}` | `{ name, description, price, durationDays, durationInMonths, maxUsers, maxOwnerAccounts, maxEmployeeAccounts, hasFullFeatures, maxWorkspaces, aiUsageLimit, features, status }` |
| Kich hoat goi | PATCH | `/admin/subscription-plans/{id}/activate` | none |
| Tat goi | PATCH | `/admin/subscription-plans/{id}/deactivate` | none |
| Danh sach ho so dang ky | GET | `/admin/workspace-registrations` | none |
| Xac nhan payment transaction | PATCH | `/admin/payments/{paymentId}/confirm` | `{ note }` |
| Tu choi payment transaction | PATCH | `/admin/payments/{paymentId}/reject` | `{ note }` |
| Xac nhan thanh toan theo ho so legacy | PATCH | `/admin/workspace-registrations/{id}/confirm-payment` | `{ note }` |
| Yeu cau sua thanh toan | PATCH | `/admin/workspace-registrations/{id}/request-payment-correction` | `{ note }` |
| Duyet dang ky, tao workspace + owner | PATCH | `/admin/workspace-registrations/{id}/approve` | `{ note }` |
| Kich hoat dang ky | POST | `/admin/workspace-registrations/{id}/activate` | `{ note }` |
| Tu choi dang ky | PATCH | `/admin/workspace-registrations/{id}/reject` | `{ note }` |
| Danh sach feedback | GET | `/admin/business-feedback` | none |
| Review feedback | PATCH | `/admin/business-feedback/{id}/review` | `{ supportNote }` |

UI System Admin khong duoc hien task detail, task assignment, employee workload noi bo, daily report chi tiet hay thao tac nghiep vu trong workspace.

### Employees

Tat ca endpoint employees chi danh cho OWNER.

| Chuc nang | Method | Path | Body hoac query |
|---|---|---|---|
| Danh sach nhan vien | GET | `/employees` | none |
| Tao nhan vien | POST | `/employees` | `{ fullName, email, phone, jobTitle, seniorityLevel, skillRating, yearsOfExperience, skills }` |
| Chi tiet nhan vien | GET | `/employees/{id}` | none |
| Sua nhan vien | PUT | `/employees/{id}` | `{ fullName, email, phone, status, jobTitle, seniorityLevel, skillRating, yearsOfExperience, skills }` |
| Doi trang thai | PATCH | `/employees/{id}/status?status=ACTIVE` | query `status` |
| Reset mat khau | PATCH | `/employees/{id}/reset-password` | none |

Backend tu sinh `employeeCode` dang `SExxxx`, `username`, va `initialPassword` bang chinh `employeeCode`. UI can hien thi/lap danh sach credential nay cho OWNER sau khi tao nhan vien.
Khi reset mat khau employee thanh cong, backend tra lai `User.initialPassword`; UI chi hien thi gia tri nay cho OWNER trong modal ket qua reset.

### Tasks

| Chuc nang | Method | Path | Body |
|---|---|---|---|
| Danh sach task | GET | `/tasks` | none |
| Tao task | POST | `/tasks` | `{ title, requirements, description, assigneeId, priority, deadline, estimatedHours }` |
| Chi tiet task | GET | `/tasks/{id}` | none |
| Sua task | PUT | `/tasks/{id}` | `{ title, requirements, description, assigneeId, priority, deadline, estimatedHours }` |
| Giao lai task | PATCH | `/tasks/{id}/assign` | `{ assigneeId }` |
| Doi status | PATCH | `/tasks/{id}/status` | `{ status }` |
| Cap nhat tien do | PATCH | `/tasks/{id}/progress` | `{ progressPercent, content, updateType, attachment }` |
| Lich su cap nhat | GET | `/tasks/{id}/updates` | none |
| Them update | POST | `/tasks/{id}/updates` | `{ progressPercent, content, updateType, attachment }` |
| Huy task | PATCH | `/tasks/{id}/cancel` | none |

Quyen:

- OWNER xem tat ca task trong workspace, tao/sua/giao lai/huy task.
- EMPLOYEE chi xem task duoc giao.
- OWNER hoac assignee co the doi status/cap nhat tien do.

Luat UI:

- Khi `updateType = COMPLETION`, disable input progress hoac tu set `100`.
- Khi `updateType = BLOCKER`, bat buoc nhap `content`.
- Progress slider tu `0` den `100`.
- Nen khoa sua task `CANCELLED` hoac `COMPLETED` neu chua co quyet dinh san pham ro rang.

### Analytics

Tat ca analytics chi danh cho OWNER.

| Man hinh | Method | Path | Data |
|---|---|---|---|
| Dashboard owner | GET | `/analytics/owner-dashboard` | `{ totalTasks, activeTasks, completedTasks, overdueTasks, employeeWorkload, recentlyUpdatedTasks, aiRecommendations }` |
| Workload toan bo | GET | `/analytics/workload` | `Workload[]` |
| Workload nhan vien | GET | `/analytics/employees/{id}/workload` | `Workload` |

### AI

Tat ca endpoint AI chi danh cho OWNER.

| Chuc nang | Method | Path | Body hoac query |
|---|---|---|---|
| Goi y nguoi nhan | POST | `/ai/recommend-assignee` | `{ title, requirements, deadline, estimatedHours }` |
| Tom tat workload | GET | `/ai/workload-summary` | none |
| Rui ro tre han | GET | `/ai/delay-risks` | none |
| Phan tich daily reports | GET | `/ai/daily-reports/insights` | none |
| Nhan vien thieu report | GET | `/ai/daily-reports/missing` | none |
| Tao task tu mo ta/bien ban | POST | `/ai/tasks/extract` | `{ text, defaultDeadline }` |
| De xuat chia nho task | POST | `/ai/tasks/{id}/split` | none |
| De xuat deadline/priority | POST | `/ai/tasks/{id}/adjust` | none |
| Goi y action cho owner | GET | `/ai/action-suggestions` | none |
| Danh sach AI suggestion | GET | `/ai/suggestions` | none |
| Doi trang thai suggestion | PATCH | `/ai/suggestions/{id}/status?status=ACCEPTED` | query `status` |
| Tom tat ngay | GET | `/ai/business-summary/daily` | none |
| Tom tat tuan | GET | `/ai/business-summary/weekly` | none |
| Tom tat thang | GET | `/ai/business-summary/monthly` | none |

`outputData` va `inputData` cua AI suggestion la string JSON. Front-end nen parse an toan bang try/catch.

`aiRecommendations` tren owner dashboard la cache tu `ai_suggestions`, khong phai LLM call moi moi lan refresh. Moi item co `{ suggestionId, type, source, outputData, createdAt }`; `source` hien tai la `CACHE`.

### Daily reports

| Chuc nang | Method | Path | Body |
|---|---|---|---|
| Danh sach report | GET | `/daily-reports` | none |
| Tao report | POST | `/daily-reports` | `{ reportDate, todayCompleted, currentWork, blockers, tomorrowPlan }` |
| Chi tiet report | GET | `/daily-reports/{id}` | none |
| Owner danh dau da review | PATCH | `/daily-reports/{id}/review` | none |

Quyen:

- EMPLOYEE xem/tao report cua chinh minh.
- OWNER xem tat ca report va review.
- Backend chan tao trung report theo ngay cho cung user.

### Notifications

| Chuc nang | Method | Path |
|---|---|---|
| Danh sach thong bao | GET | `/notifications` |
| Danh dau mot thong bao da doc | PATCH | `/notifications/{id}/read` |
| Danh dau tat ca da doc | PATCH | `/notifications/read-all` |

Backend tu sinh thong bao van hanh khi goi danh sach notifications: task qua han, deadline sap den, thieu daily report.

## 6. Route va man hinh de xuat

### Public routes

- `/login`: dang nhap.
- `/register-workspace`: tao workspace + owner dau tien.

### Shared authenticated routes

- `/tasks`: danh sach task.
- `/tasks/:id`: chi tiet task, lich su update, form cap nhat tien do.
- `/daily-reports`: danh sach bao cao.
- `/daily-reports/new`: tao bao cao ngay.
- `/notifications`: danh sach thong bao.
- `/profile`: thong tin tai khoan hien tai.

### OWNER routes

- `/owner/dashboard`: tong quan.
- `/owner/employees`: CRUD nhan vien.
- `/owner/employees/:id`: chi tiet nhan vien + workload.
- `/owner/tasks/new`: tao task.
- `/owner/workspace`: cau hinh workspace.
- `/owner/analytics/workload`: bang workload.
- `/owner/ai`: goi y AI, delay risks, summaries.

### EMPLOYEE routes

- `/employee/home`: task cua toi + report hom nay + thong bao.
- `/employee/tasks`: task duoc giao.
- `/employee/reports`: report cua toi.

Sau login, redirect theo role:

- `OWNER` -> `/owner/dashboard`
- `EMPLOYEE` -> `/employee/home`

## 7. Layout va navigation

- App shell co sidebar desktop, drawer hoac bottom navigation tren mobile.
- Header co ten workspace, nut notifications, avatar/user menu.
- User menu gom `Thong tin ca nhan`, `Doi mat khau` va `Dang xuat`.
- Khi token het han, hien toast `Phien dang nhap da het han` roi chuyen ve `/login`.

Navigation OWNER:

- Dashboard
- Task
- Nhan vien
- Workload
- AI
- Bao cao ngay
- Thong bao
- Workspace

Navigation EMPLOYEE:

- Viec cua toi
- Bao cao ngay
- Thong bao
- Ho so

## 8. CRUD chi tiet theo man hinh

### Login

Fields:

- Email hoac username: required.
- Password: required.

Buttons:

- `Dang nhap`: submit `POST /auth/login`.
- `Tao workspace moi`: chuyen register.

States:

- Loading khi submit.
- Inline error neu sai tai khoan/password.
- Disable button khi form invalid hoac dang submit.

### Workspace registration

Implement thanh 5 man hinh public rieng, khong gom chon goi va payment vao form dau tien.

#### Registration information

Fields:

- Ten doanh nghiep: required.
- Ten workspace: required.
- Email doanh nghiep: required, email.
- So dien thoai doanh nghiep: optional.
- Dia chi doanh nghiep: optional.
- Ho ten nguoi dai dien: required.
- Email nguoi dai dien: required, email.
- So dien thoai nguoi dai dien: optional.

Submit `POST /workspace-registrations`. Khi thanh cong, lay `data.id` va dieu huong den `/workspace-registration/{registrationId}/plans`.

#### Plan selection

API:

- `GET /subscription-plans/active`
- `PATCH /workspace-registrations/{id}/select-plan`

Card goi can hien:

- Ten goi va mo ta.
- Gia thang VND.
- So Business Owner toi da.
- So Employee toi da.
- Full features/AI usage limit neu co.

#### Payment method

Fields:

- Radio/card `MOMO`.
- Radio/card `BANK_TRANSFER`.

Submit `POST /workspace-registrations/{id}/payments`, sau do dieu huong den trang instruction bang `paymentId`.

#### Payment instruction

API:

- `GET /payments/{paymentId}` poll moi 3-5 giay.

MoMo UI:

- Hien `providerQrCodeUrl` neu co.
- Hien nut mo `providerPaymentUrl` neu co.
- Hien deeplink neu co.
- Hien amount, orderCode, status.

Bank/VietQR UI:

- Hien `providerQrCodeUrl`.
- Hien bankName, bankCode, bankAccountNumber, bankAccountName.
- Hien amount va transferContent.
- Hien status.

#### Payment result

API:

- `GET /payments/{paymentId}`.
- `GET /workspace-registrations/{registrationId}`.

States:

- `SUCCESS`: hien thanh toan thanh cong, workspace dang kich hoat/da kich hoat.
- `FAILED`: hien that bai va nut tao giao dich moi.
- `EXPIRED`: hien het han va nut tao giao dich moi.
- `PENDING`: tiep tuc hien instruction/polling.

Buttons:

- `Gui thong tin`: submit `POST /workspace-registrations`.
- `Chon goi`: submit `PATCH /workspace-registrations/{id}/select-plan`.
- `Tiep tuc thanh toan`: submit `POST /workspace-registrations/{id}/payments`.
- `Thu lai thanh toan`: tao payment transaction moi.
- `Da co tai khoan`: chuyen login.

### Owner dashboard

APIs:

- `GET /analytics/owner-dashboard`
- `GET /notifications`

UI blocks:

- KPI: tong task, active, completed, overdue.
- Bang task cap nhat gan day.
- Bang workload nhan vien.
- Khoi AI recommendation nhanh.
- Notification unread count.

Buttons:

- `Tao task`: toi `/owner/tasks/new`.
- `Xem workload`: toi `/owner/analytics/workload`.
- `Xem AI`: toi `/owner/ai`.

### Employee management

List API: `GET /employees`

Table columns:

- Ho ten
- Email
- Phone
- Username
- Ma nhan vien
- Mat khau ban dau
- Status
- CreatedAt
- Actions

Buttons/actions:

- `Them nhan vien`: mo modal create.
- `Sua`: mo modal edit.
- `Kich hoat`: `PATCH /employees/{id}/status?status=ACTIVE`.
- `Tam ngung`: `PATCH /employees/{id}/status?status=INACTIVE`.
- `Xem workload`: toi `/owner/employees/:id`.

Create form:

- fullName required.
- email required.
- phone optional.
- jobTitle optional.
- seniorityLevel optional.
- skillRating optional, 1-5.
- yearsOfExperience optional, >= 0.
- skills optional.

Edit form:

- fullName required.
- email required.
- phone optional.
- jobTitle optional.
- seniorityLevel optional.
- skillRating optional, 1-5.
- yearsOfExperience optional, >= 0.
- skills optional.
- status select.

Sau create thanh cong, hien `username`, `employeeCode`, `initialPassword` de OWNER gui cho nhan vien. Sau create/update/status, refetch `GET /employees`.

### Task list

API: `GET /tasks`

Filters client-side:

- Search title/requirements.
- Status.
- Priority.
- Assignee, chi OWNER.
- Overdue: `deadline < now` va status khong thuoc `COMPLETED`, `CANCELLED`.

Fields:

- Title.
- Assignee name, map tu `GET /employees` voi OWNER.
- Priority badge.
- Status badge.
- Progress bar.
- Deadline.
- Estimated hours.

Buttons:

- OWNER: `Tao task`, `Sua`, `Giao lai`, `Huy`.
- OWNER/assignee: `Cap nhat tien do`, `Doi trang thai`.
- Shared: `Xem chi tiet`.

### Task create/edit

Create API: `POST /tasks`

Edit API: `PUT /tasks/{id}`

Fields:

- Title: required.
- Requirements: required.
- Description: optional.
- Assignee: required, select tu active employees.
- Priority: select `LOW | MEDIUM | HIGH | CRITICAL`, default `MEDIUM`.
- Deadline: required date-time.
- Estimated hours: optional number, default `0`.

Buttons:

- `Luu task`: create/update.
- `Goi y nguoi nhan`: goi `POST /ai/recommend-assignee`, chi OWNER.
- `Huy`: quay lai list.

AI recommendation panel:

- Hien thi score, workloadLevel, reason, risk.
- Neu response co `requiredRole`, `roleFit`, `roleFitReason`, hien thi de owner thay AI da doi chieu vai tro chuyen mon voi task.
- Nut `Chon nguoi nay` set `assigneeId`.
- Khong auto-submit task khi chon goi y.

### Task detail

APIs:

- `GET /tasks/{id}`
- `GET /tasks/{id}/updates`

Sections:

- Thong tin task.
- Progress/status.
- Timeline updates.
- Panel action.

Buttons:

- `Cap nhat tien do`: mo form update.
- `Bao blocker`: preset `updateType=BLOCKER`.
- `Hoan thanh`: preset `updateType=COMPLETION`.
- OWNER: `Sua task`, `Giao lai`, `Huy task`.

Progress update form:

- updateType select.
- progressPercent slider/number 0-100.
- content textarea required.
- attachment URL optional.

Submit:

- `PATCH /tasks/{id}/progress`.
- Sau submit, refetch task + updates.

### Daily reports

List API: `GET /daily-reports`

Create API: `POST /daily-reports`

Review API: `PATCH /daily-reports/{id}/review`

List fields:

- reportDate.
- user name, chi OWNER can map user.
- todayCompleted.
- currentWork.
- blockers.
- reviewedAt.

Create form:

- reportDate required, default hom nay.
- todayCompleted required.
- currentWork required.
- blockers optional.
- tomorrowPlan optional.

Buttons:

- `Gui bao cao`: submit create.
- OWNER: `Da review`: call review API.
- `Xem chi tiet`: open detail.

### Notifications

APIs:

- `GET /notifications`
- `PATCH /notifications/{id}/read`
- `PATCH /notifications/read-all`

UI:

- Badge unread count.
- Tabs: Tat ca, Chua doc.
- Each item: title, message, createdAt, related entity link.

Buttons:

- `Danh dau da doc`.
- `Danh dau tat ca da doc`.
- Click notification lien quan `TASK` thi mo `/tasks/{relatedEntityId}`.

### AI center

APIs:

- `GET /ai/workload-summary`
- `GET /ai/delay-risks`
- `GET /ai/daily-reports/insights`
- `GET /ai/daily-reports/missing`
- `POST /ai/tasks/extract`
- `POST /ai/tasks/{id}/split`
- `POST /ai/tasks/{id}/adjust`
- `GET /ai/action-suggestions`
- `GET /ai/business-summary/daily`
- `GET /ai/business-summary/weekly`
- `GET /ai/business-summary/monthly`
- `GET /ai/suggestions`

UI sections:

- Workload summary.
- Delay risks list.
- Daily/weekly/monthly business summary.
- Daily report insights: summary, blockers `{ severity, description }`, actionSuggestions.
- Missing report list with `employeeId`, `employeeName`, `reportDate`, `daysMissing`, `recommendedAction`, `confidence`.
- Task extraction form from text/minutes.
- Task split and deadline/priority recommendation for selected task.
- Owner action suggestions with `actionType`, target entity, reason, confidence trong khoang `0..1`.
- AI suggestion history.

Fallback note: `recommend-assignee` can return top-3 rule-based recommendations when LLM/provider times out; the list shape is unchanged and each item marks fallback in `reason`/`risk`. `workload-summary`, `delay-risks`, `action-suggestions`, `daily-reports/insights`, and `daily-reports/missing` can also return rule-based data when LLM/provider fails. These card responses keep their normal keys and include `source: "RULE_BASED_FALLBACK"`, `aiProviderFailed: true`, and `fallbackReason`; show this as fallback/source metadata, not as confirmed LLM output.

Overload note: AI endpoints without fallback can return HTTP 429 with `AI_RATE_LIMITED` when too many AI calls are running. UI should show a retry-later message, keep the button disabled briefly, and avoid immediate auto-retry loops.

Buttons:

- `Tai lai`: refetch tung section.
- `Chap nhan`: `PATCH /ai/suggestions/{id}/status?status=ACCEPTED`.
- `Tu choi`: `PATCH /ai/suggestions/{id}/status?status=REJECTED`.

## 9. Button matrix

| Button | Man hinh | Role | API | Disabled khi |
|---|---|---|---|---|
| Dang nhap | Login | Public | `POST /auth/login` | form invalid/loading |
| Gui thong tin dang ky | Registration information | Public | `POST /workspace-registrations` | form invalid/loading |
| Chon goi dang ky | Plan selection | Public | `PATCH /workspace-registrations/{id}/select-plan` | no plan selected/loading |
| Tiep tuc thanh toan | Payment method | Public | `POST /workspace-registrations/{id}/payments` | no payment method selected/loading |
| Mo trang MoMo | Payment instruction | Public | external `providerPaymentUrl` | no providerPaymentUrl |
| Thu lai thanh toan | Payment result | Public | `POST /workspace-registrations/{id}/payments` | loading |
| Dang xuat | User menu | Authenticated | `POST /auth/logout` | loading |
| Doi mat khau | Profile/User menu | Authenticated | `PATCH /auth/change-password` | form invalid/loading |
| Them nhan vien | Employees | OWNER | none, mo modal | none |
| Luu nhan vien | Employee modal | OWNER | `POST /employees` hoac `PUT /employees/{id}` | form invalid/loading |
| Kich hoat | Employees | OWNER | `PATCH /employees/{id}/status?status=ACTIVE` | user already ACTIVE/loading |
| Tam ngung | Employees | OWNER | `PATCH /employees/{id}/status?status=INACTIVE` | user already INACTIVE/loading |
| Reset mat khau nhan vien | Employees | OWNER | `PATCH /employees/{id}/reset-password` | loading |
| Tao task | Tasks/Dashboard | OWNER | none, route create | no active employee |
| Luu task | Task form | OWNER | `POST /tasks` hoac `PUT /tasks/{id}` | form invalid/loading |
| Goi y nguoi nhan | Task form | OWNER | `POST /ai/recommend-assignee` | thieu title/requirements/deadline/loading |
| Chon nguoi nay | AI recommendation | OWNER | none, set assignee | employee inactive neu co data |
| Tao task bang AI | AI center | OWNER | `POST /ai/tasks/extract` | text empty/loading |
| Chia nho task | Task detail | OWNER | `POST /ai/tasks/{id}/split` | loading |
| De xuat deadline/priority | Task detail | OWNER | `POST /ai/tasks/{id}/adjust` | loading |
| Xem action AI | AI center | OWNER | `GET /ai/action-suggestions` | loading |
| Giao lai | Task detail | OWNER | `PATCH /tasks/{id}/assign` | no assignee/loading |
| Huy task | Task detail | OWNER | `PATCH /tasks/{id}/cancel` | status CANCELLED/COMPLETED/loading |
| Doi trang thai | Task detail | OWNER/assignee | `PATCH /tasks/{id}/status` | no status/loading |
| Cap nhat tien do | Task detail | OWNER/assignee | `PATCH /tasks/{id}/progress` | content empty/loading |
| Bao blocker | Task detail | OWNER/assignee | `PATCH /tasks/{id}/progress` | content empty/loading |
| Hoan thanh | Task detail | OWNER/assignee | `PATCH /tasks/{id}/progress` | status COMPLETED/loading |
| Gui bao cao | Daily report form | OWNER/EMPLOYEE | `POST /daily-reports` | form invalid/loading |
| Da review | Daily report detail | OWNER | `PATCH /daily-reports/{id}/review` | already reviewed/loading |
| Danh dau da doc | Notifications | OWNER/EMPLOYEE | `PATCH /notifications/{id}/read` | already read/loading |
| Danh dau tat ca da doc | Notifications | OWNER/EMPLOYEE | `PATCH /notifications/read-all` | no unread/loading |
| Chap nhan suggestion | AI center | OWNER | `PATCH /ai/suggestions/{id}/status?status=ACCEPTED` | status ACCEPTED/loading |
| Tu choi suggestion | AI center | OWNER | `PATCH /ai/suggestions/{id}/status?status=REJECTED` | status REJECTED/loading |

## 10. Form validation

- Email: phai dung format email.
- Required text: trim roi kiem tra rong.
- deadline: phai la ngay gio hop le. UI nen canh bao neu deadline nam trong qua khu.
- estimatedHours: so >= 0, cho phep decimal.
- progressPercent: integer 0-100.
- reportDate: required `YYYY-MM-DD`.
- Khong gui field `undefined`; optional co the gui `null` hoac bo field, nen thong nhat bo field khi khong nhap.

## 11. Loading, empty, error states

Moi man hinh list can co:

- Loading skeleton hoac spinner.
- Empty state co action chinh, vi du task rong thi OWNER thay `Tao task`.
- Error state co nut `Thu lai`.
- Toast thanh cong cho create/update.
- Confirm dialog cho hanh dong huy task, tam ngung nhan vien, logout.

## 12. Mapping hien thi

Priority badge:

- `LOW`: xam hoac xanh nhe.
- `MEDIUM`: xanh.
- `HIGH`: cam.
- `CRITICAL`: do.

Status badge:

- `ASSIGNED`: xam.
- `IN_PROGRESS`: xanh.
- `BLOCKED`: do/cam.
- `COMPLETED`: xanh la.
- `CANCELLED`: xam toi.

Workload badge:

- `NO_WORK`: xam.
- `LOW`: xanh la.
- `NORMAL`: xanh.
- `HIGH`: cam.
- `OVERLOADED`: do.

## 13. Cache/refetch de xuat

Neu dung TanStack Query hoac thu vien tuong tu:

- `auth.me`: refetch khi app mount.
- `tasks`: invalidate sau create/update/assign/status/progress/cancel.
- `task(id)`: invalidate sau moi action cua task do.
- `taskUpdates(id)`: invalidate sau progress update.
- `employees`: invalidate sau create/update/status.
- `workload`: invalidate sau task mutation va employee status mutation.
- `notifications`: refetch khi mo popover, invalidate sau read/read-all.
- `dailyReports`: invalidate sau create/review.
- `aiSuggestions`: invalidate sau recommend-assignee, AI summaries, AI task tools, action suggestions va status change.

## 14. Bao mat front-end

- Khong luu token trong URL.
- Khong log token ra console.
- Khong goi AI service truc tiep vi AI token la secret noi bo backend.
- Khong hien thi route OWNER cho EMPLOYEE.
- Voi route bi 403, hien thi trang `Khong co quyen` va nut ve trang phu hop role.

## 15. Goi y cau truc front-end moi

```text
src/
  app/
    router.tsx
    providers.tsx
  api/
    client.ts
    auth.api.ts
    workspace.api.ts
    employees.api.ts
    tasks.api.ts
    reports.api.ts
    notifications.api.ts
    analytics.api.ts
    ai.api.ts
  auth/
    auth-store.ts
    RequireAuth.tsx
    RequireRole.tsx
  components/
    AppShell.tsx
    DataTable.tsx
    ConfirmDialog.tsx
    StatusBadge.tsx
    PriorityBadge.tsx
    WorkloadBadge.tsx
    EmptyState.tsx
  features/
    login/
    workspace/
    employees/
    tasks/
    reports/
    notifications/
    analytics/
    ai/
  types/
    api.ts
    domain.ts
```

## 16. Checklist nghiem thu front-end

- Login/logout hoat dong.
- Register workspace tao duoc workspace va owner.
- OWNER xem dashboard duoc.
- OWNER CRUD employee duoc.
- OWNER tao/sua/giao/huy task duoc.
- EMPLOYEE chi thay task duoc giao.
- OWNER/EMPLOYEE cap nhat progress, blocker, completion duoc.
- Daily report tao duoc va khong tao trung cung ngay.
- OWNER review daily report duoc.
- Notifications doc duoc va mark read duoc.
- AI recommendation chon duoc assignee nhung khong auto assign.
- UI xu ly 401/403 ro rang.
- Khong con request nao tro toi AI service truc tiep.
- Khong con dependency vao front-end cu da xoa.
