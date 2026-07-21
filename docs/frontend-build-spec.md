# Dac Ta Xay Front-End FOREP EXE

Tai lieu nay mo ta day du phan front-end moi can xay cho FOREP EXE sau khi front-end cu da bi go khoi repo. Front-end moi phai fit truc tiep voi Backend API hien tai. Dung `http://localhost:8080` lam API origin; cac module authenticated cu dung prefix `/api/v1`, public registration/payment dung prefix `/api/public`, payment provider callbacks dung `/api/payment-callbacks`, va admin platform moi dung `/api/admin`.

`docs/FE.md` la source of truth moi cho FE requirements chi tiet: permission matrix, role vs business position, HR master data, task workflow, AI analysis/recommendation, lifecycle rules va acceptance checklist. FE moi nen uu tien cac alias `/api/workspace/...` cho cac man hinh workspace van hanh. Doc nay co them section `17. FE Change Log - Authorization Production Delta` de doi FE track tat ca thay doi bat buoc sau khi BE/AI duoc chuan hoa production.

## 1. Nguyen tac tich hop API

- Front-end chi goi Backend API, khong goi truc tiep AI Service.
- Tat ca endpoint, tru `GET /health`, `POST /auth/login`, cac endpoint `/api/public/**`, va provider callback `/api/payment-callbacks/**`, can header `Authorization: Bearer <token>`.
- FE khong duoc goi legacy public registration/payment qua `/api/v1/workspace-registrations/**` hoac `/api/v1/payments/**`; cac route nay duoc backend siết thanh admin-only. Public registration/payment bat buoc dung `/api/public/**` kem `registrationToken`.
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
- Backend phan quyen theo `Role -> Permission -> Endpoint`. UI phai guard page/menu/button/action bang `user.permissions`, khong guard truc tiep bang role tru redirect mac dinh sau login.
- `POST /auth/login` tra `{ token, user, permissions }`; `GET /auth/me` tra `User` co `permissions: Permission[]`. FE auth store phai luu permissions va expose `hasPermission()` / `hasAnyPermission()`.
- Khong goi Developer, BA, HR Staff, Tech Lead... la system role. Day la Business Position/Job Position trong workspace, khac voi system role.

## 2. Enum dung trong UI

### Role

- `PLATFORM_ADMIN`: quan tri nen tang, goi subscription, thanh toan, workspace va business owner account khoi tao.
- `BUSINESS_OWNER`: chu workspace, quan ly tai khoan owner/HR, task assignment, workload, subscription/payment va dashboard; chi xem employee/department/business position.
- `HR`: quan ly ho so nhan su, phong ban, business position, import nhan vien; khong giao task va khong quan ly subscription/payment.
- `EXECUTIVE`: xem operation/workload/AI cap dieu hanh theo workspace policy.
- `MANAGER`: tao va quan ly task, giao viec ca nhan/nhom, xem workload.
- `EMPLOYEE`: nhan vien, xem task duoc giao, cap nhat tien do, gui daily report.
- `SYSTEM_ADMIN`, `OWNER`: alias tuong thich nguoc cho du lieu cu.

### PermissionGroup

Dung cho Business Position, khong phai system role rieng:

- `EMPLOYEE`
- `MANAGER`
- `EXECUTIVE`

Khong hien `PLATFORM_ADMIN`, `BUSINESS_OWNER`, hoac `HR` trong dropdown permission group cua Business Position.

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
- `ACTIVATED`: workspace da kich hoat, subscription/owner accounts da duoc tao.
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
  role: 'PLATFORM_ADMIN' | 'BUSINESS_OWNER' | 'HR' | 'EXECUTIVE' | 'MANAGER' | 'EMPLOYEE' | 'SYSTEM' | 'SYSTEM_ADMIN' | 'OWNER';
  permissions: Permission[];
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

type Permission =
  | 'PACKAGE_VIEW'
  | 'PACKAGE_MANAGE'
  | 'WORKSPACE_REGISTER'
  | 'WORKSPACE_VIEW'
  | 'WORKSPACE_UPDATE'
  | 'WORKSPACE_MANAGE'
  | 'PAYMENT_CREATE'
  | 'PAYMENT_CONFIRM'
  | 'PAYMENT_STATUS_VIEW'
  | 'PAYMENT_HISTORY_VIEW'
  | 'PAYMENT_QR_MANAGE'
  | 'SUBSCRIPTION_VIEW'
  | 'SUBSCRIPTION_RENEW'
  | 'SUBSCRIPTION_UPGRADE'
  | 'EMPLOYEE_VIEW'
  | 'EMPLOYEE_CREATE'
  | 'EMPLOYEE_UPDATE'
  | 'EMPLOYEE_DEACTIVATE'
  | 'DEPARTMENT_VIEW'
  | 'DEPARTMENT_MANAGE'
  | 'POSITION_VIEW'
  | 'POSITION_MANAGE'
  | 'ROLE_MANAGE'
  | 'PROJECT_CREATE'
  | 'PROJECT_UPDATE'
  | 'TASK_VIEW'
  | 'TASK_CREATE'
  | 'TASK_ASSIGN'
  | 'TASK_APPROVE'
  | 'TASK_UPDATE_OWN'
  | 'AI_ANALYZE'
  | 'AI_RECOMMENDATION'
  | 'AI_SUMMARY'
  | 'AI_HISTORY'
  | 'REPORT_VIEW'
  | 'REPORT_SUBMIT'
  | 'REPORT_REVIEW'
  | 'REPORT_EXPORT'
  | 'AUDIT_LOG_VIEW'
  | 'SYSTEM_CONFIGURATION'
  | 'REVENUE_VIEW'
  | 'FEEDBACK_CREATE'
  | 'FEEDBACK_MANAGE'
  | 'NOTIFICATION_VIEW';

type Task = {
  id: string;
  workspaceId: string;
  title: string;
  requirements: string;
  description: string | null;
  customerPhone: string | null;
  customerEmail: string | null;
  customerDescription: string | null;
  assignmentType: 'INDIVIDUAL' | 'TEAM';
  assigneeId: string;
  creatorId: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  deadline: string;
  startDate: string | null;
  estimatedHours: number;
  difficulty: 1 | 2 | 3 | 4 | 5 | null;
  requiredSkills: string | null;
  requiredJobPositionId: string | null;
  taskDomain: string | null;
  projectId: string | null;
  departmentId: string | null;
  participants: TaskAssignee[];
  attachments: TaskAttachment[];
  progressPercent: number;
  status: 'ASSIGNED' | 'IN_PROGRESS' | 'BLOCKED' | 'COMPLETED' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
};

type TaskAssignee = {
  id: string;
  taskId: string;
  employeeId: string;
  participantRole: 'ASSIGNEE' | 'LEADER' | 'MEMBER';
  leader: boolean;
  allocatedHours: number;
  createdAt: string;
};

type TaskAttachment = {
  id: string;
  taskId: string;
  fileName: string;
  fileUrl: string;
  contentType: string | null;
  fileSize: number | null;
  attachmentType: 'REQUIREMENT' | 'REFERENCE' | 'RESULT' | 'OTHER' | null;
  uploadedBy: string;
  createdAt: string;
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
  registrationStatus: 'PENDING_PLAN_SELECTION' | 'PENDING_PAYMENT' | 'PAYMENT_CONFIRMED' | 'APPROVED' | 'ACTIVATED' | 'REJECTED' | 'CANCELLED' | 'SUBMITTED' | 'PAYMENT_PENDING' | 'PAYMENT_SUBMITTED';
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
  activeSubscription: WorkspaceSubscription | null;
  maxUsers: number;
  maxOwnerAccounts: number;
  maxEmployeeAccounts: number;
  ownerAccountCount: number;
  currentUsers: number;
  status: 'PENDING_PAYMENT' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'EXPIRED';
  paymentStatus: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CORRECTION_REQUESTED';
  ownerId: string | null;
  ownerAccountProvisionedAt: string | null;
  activatedAt: string | null;
  expiresAt: string | null;
  lastActivityAt: string | null;
  ownerAccounts: User[];
  generatedOwnerAccounts?: GeneratedOwnerAccount[];
  createdAt: string;
};

type WorkspaceSubscription = {
  id: string;
  workspaceId: string;
  subscriptionPlanId: string;
  status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'PENDING_RENEWAL' | 'UPGRADED' | 'DOWNGRADED';
  startDate: string;
  endDate: string;
  renewalDate: string;
  price: number;
  maxOwnerAccounts: number;
  maxEmployeeAccounts: number;
  paymentTransactionId: string | null;
  createdAt: string;
  updatedAt: string;
};

type PaymentTransaction = {
  id: string;
  workspaceRegistrationId: string;
  subscriptionPlanId: string;
  paymentMethod: 'MOMO' | 'BANK_TRANSFER';
  amount: number;
  currency: 'VND';
  paymentCode: string;
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
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'EXPIRED' | 'CANCELLED' | 'REFUNDED' | 'MANUAL_REVIEW';
  paidAt: string | null;
  expiredAt: string | null;
  createdAt: string;
  updatedAt: string;
};

type PaymentQrSetting = {
  id: string;
  paymentMethod: 'MOMO' | 'BANK_TRANSFER';
  qrCodeUrl: null;
  qrFileId: string | null;
  qrDisplayUrl: string | null;
  paymentUrl: null;
  deeplink: null;
  bankCode: string | null;
  bankName: string | null;
  bankAccountNumber: string | null;
  bankAccountName: string | null;
  transferContentPrefix: string | null;
  enabled: boolean;
  updatedBy: string | null;
  createdAt: string;
  updatedAt: string;
};

type PublicPaymentStatus = {
  workspaceRegistrationId: string;
  workspaceId: string | null;
  registrationPaymentStatus: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CORRECTION_REQUESTED';
  registrationStatus: string;
  paymentMethod: 'MOMO' | 'BANK_TRANSFER';
  amount: number;
  currency: 'VND';
  paymentCode: string;
  providerPaymentUrl: string | null;
  providerDeeplink: string | null;
  providerQrCodeUrl: string | null;
  bankCode: string | null;
  bankName: string | null;
  bankAccountNumber: string | null;
  bankAccountName: string | null;
  transferContent: string | null;
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'EXPIRED' | 'CANCELLED' | 'REFUNDED' | 'MANUAL_REVIEW';
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
| Login | POST | `/auth/login` | `{ email, password }` hoac `{ username, password }` | `{ token, user, permissions }` |
| Logout | POST | `/auth/logout` | none | `{ message }` |
| Current user | GET | `/auth/me` | none | `User` |
| Doi mat khau | PATCH | `/auth/change-password` | `{ currentPassword, newPassword }` | `User` |

`POST /auth/logout` hien chi tra message, khong revoke token o server. Front-end phai tu xoa token local.
`PATCH /auth/change-password` can token dang nhap, dung cho employee tu doi mat khau trong trang tai khoan/bao mat. Sau khi thanh cong, UI nen xoa password tam dang hien thi trong local state neu co va thong bao "Doi mat khau thanh cong".

Auth store bat buoc:

- `permissions` lay tu `login.permissions` hoac `me.permissions`.
- `hasPermission(permission)` va `hasAnyPermission(permissions)` la helper duy nhat cho route/sidebar/button.
- Guest route pricing/register/payment/result khong dung `RequireAuth`.
- Role chi dung de redirect sau login va hien thi label, khong dung de hien/hide action.

### Workspace

| Chuc nang | Method | Path | Body | Quyen |
|---|---|---|---|---|
| Danh sach goi dang ky active | GET | `/api/public/subscription-plans` | none | public |
| Gui thong tin dang ky workspace | POST | `/api/public/workspace-registrations` | `{ businessName, workspaceName, contactEmail, contactPhone, businessAddress, representativeFullName, representativeEmail, representativePhone }` | public |
| Xem ho so dang ky | GET | `/api/public/workspace-registrations/{id}?token={registrationToken}` | none | public |
| Chon goi dang ky | PATCH | `/api/public/workspace-registrations/{id}/select-plan?token={registrationToken}` | `{ subscriptionPlanId }` | public |
| Tao giao dich thanh toan | POST | `/api/public/workspace-registrations/{id}/payments?token={registrationToken}` | `{ paymentMethod: 'MOMO' \| 'BANK_TRANSFER' }` | public |
| Xem payment public | GET | `/api/public/payments/{paymentCode}/status?token={registrationToken}` | none | public |
| Xem workspace | GET | `/workspaces/current` | none | BUSINESS_OWNER/HR/EXECUTIVE/MANAGER/EMPLOYEE |
| Sua workspace | PUT | `/workspaces/current` | `{ name, shortCode, logo, address }` | BUSINESS_OWNER |

Khong dung `/workspaces/register` cho user public nua. Endpoint nay da bi chan de tranh tao workspace/account khi chua thanh toan.

Flow dang ky workspace public:

1. Trang workspace registration nhap thong tin doanh nghiep va nguoi dai dien, goi `POST /api/public/workspace-registrations`, sau do chuyen sang trang chon goi bang `registrationId` va `registrationToken`.
2. Trang chon goi goi `GET /api/public/subscription-plans`, hien thi name, description, price, duration, `maxOwnerAccounts`, `maxEmployeeAccounts`, full features va nut chon goi.
3. Khi user chon goi, UI goi `PATCH /api/public/workspace-registrations/{id}/select-plan?token={registrationToken}` roi chuyen sang trang chon phuong thuc thanh toan.
4. Trang chon payment method bat buoc user chon `MOMO` hoac `BANK_TRANSFER`, sau do goi `POST /api/public/workspace-registrations/{id}/payments?token={registrationToken}`.
5. Trang payment instruction hien thi theo `PublicPaymentStatus`: MoMo provider data neu provider that da cau hinh, hoac bank info + QR file upload do Platform Admin cap nhat; hien amount, paymentCode, status. Khong phu thuoc `orderCode`, `requestId`, `providerTransactionId` o UI public. Neu backend tra ve payment pending con han da ton tai, UI dung lai `paymentCode` do va khong tao them instruction moi.
6. UI poll `GET /api/public/payments/{paymentCode}/status?token={registrationToken}` moi 3-5 giay den khi status la `SUCCESS`, `FAILED` hoac `EXPIRED`.
7. Trang ket qua goi them `GET /api/public/workspace-registrations/{id}?token={registrationToken}` de hien thi payment result va workspace activation status.
8. UI khong cho login owner khi payment chua `SUCCESS`; frontend khong tu tin payment success tu query string/callback client.

Neu khong thanh toan, user public khong tao duoc workspace/account. Chi System Admin moi duoc tao workspace truc tiep bang API admin.

Production payment note: MoMo callback can xac thuc signature bang `MOMO_SECRET_KEY`. Neu chua co secret, backend chi chap nhan callback khi `MOMO_SANDBOX_MODE=true`; production phai de `false`.

MoMo provider mode:

- FE khong hien input URL thanh toan/URL anh QR/deeplink trong admin payment settings.
- Bank transfer QR hien thi cho public user la file QR do Platform Admin upload qua backend.
- Neu backend tra `providerPaymentUrl`/`providerDeeplink` tu MoMo provider that, co the hien nut mo MoMo; khong cho admin nhap tay cac URL nay.
- Neu backend tra `providerQrCodeUrl`, hien QR do backend tra ve.
- Neu backend bao loi thieu QR/chua san sang, payment method page hien message: "Phuong thuc thanh toan nay chua san sang. Vui long doi quan tri vien cap nhat ma QR." va khong tiep tuc tao payment.
- Khong tu sinh QR trong FE, khong dung QR fake, khong dung QR tu third-party client-side.

Admin payment QR settings:

- `GET /api/admin/payment-qr-settings`
- `PUT /api/admin/payment-qr-settings/{paymentMethod}`
- Body: `{ bankCode?, bankName?, bankAccountNumber?, bankAccountName?, transferContentPrefix?, enabled }`; khong gui `qrCodeUrl`, `paymentUrl`, `deeplink`.
- Platform Admin chi upload/cap nhat QR cho `BANK_TRANSFER` bang `POST /api/admin/payment-qr-settings/BANK_TRANSFER/qr-image`.
- MoMo chi hien trang thai provider/config tu backend/env; khong co form URL anh QR, payment URL, deeplink.
- Chi bat `enabled=true` khi bank info hop le va da co QR file. Voi `BANK_TRANSFER`, FE bat buoc nhap account number/account name.
- Sau khi update QR, invalidate/refetch `paymentQrSettings`; cac payment moi se dung QR moi, payment cu giu snapshot QR tai thoi diem tao.

Workspace subscription snapshot:

- Platform workspace response co `activeSubscription`.
- FE hien goi hien tai/renewal/limit tu `activeSubscription` neu khac null.
- `subscriptionPlanId`, `maxUsers`, `maxOwnerAccounts`, `maxEmployeeAccounts` tren workspace chi la field tuong thich/fallback.
- Sau admin update plan/status/payment, invalidate/refetch `workspaces`, `workspaceDetail:{id}`, `adminDashboardOverview`, `adminRevenue:{period}`, `adminWorkspaceCharts`.

Demo seed data cho QA:

- Migration `V16__demo_saas_operational_seed.sql` tao 3 workspace active: `SV`, `MD`, `HC`.
- Moi workspace co 30 employee, department, business position, 18 task, assignment, daily report, workload bucket, AI history/suggestion cache, feedback, payment va active subscription.
- Owner demo: `SV0000A`, `MD0000A`, `HC0000A`; password ban dau `123456`.

FE implementation requirements cho registration/payment:

- Sau `POST /api/public/workspace-registrations`, FE phai luu `registrationId` va `registrationToken` trong state/session storage cua flow. Neu mat token, hien thong bao het phien dang ky va yeu cau user bat dau lai hoac lien he admin; khong thu goi API public thieu token.
- Tat ca buoc sau dang ky gom xem ho so, chon goi, tao payment va poll payment deu phai truyen `?token={registrationToken}`.
- Payment instruction/result dung `paymentCode` lam route param. Khong dung `paymentId`, `orderCode`, `requestId`, `providerTransactionId` trong UI public.
- Khi submit tao payment ma backend tra ve `PublicPaymentStatus` co `paymentCode` trung voi payment dang hien thi hoac status `PENDING/PROCESSING`, FE reuse instruction hien tai va tiep tuc polling; khong hien thong bao loi "da ton tai payment".
- Khi status `EXPIRED`, FE dung polling, hien nut tao giao dich moi. Khi user tao lai, goi lai endpoint create payment; backend se tao payment moi neu payment cu da het han.
- Khi status `FAILED`, FE dung polling, hien nut thu lai thanh toan va thong tin lien he ho tro.
- Khi status `SUCCESS`, FE dung polling va goi `GET /api/public/workspace-registrations/{registrationId}?token={registrationToken}` de lay `workspaceId/registrationStatus`, sau do hien CTA dang nhap.
- FE khong tu xac nhan thanh toan tu query string redirect cua MoMo hoac trang return URL. Chi coi thanh toan thanh cong khi backend public status tra `SUCCESS`.
- Trang public khong hien cac truong noi bo cua admin payment nhu `orderCode`, `requestId`, `providerTransactionId`, raw provider payload.

### System Admin

Tat ca endpoint duoi day chi danh cho `SYSTEM_ADMIN`.

| Chuc nang | Method | Path | Body hoac query |
|---|---|---|---|
| Monitoring platform | GET | `/admin/monitoring` | none |
| Admin dashboard overview | GET | `/api/admin/dashboard/overview` | none |
| Revenue monthly chart | GET | `/api/admin/dashboard/revenue/monthly` | none |
| Revenue quarterly chart | GET | `/api/admin/dashboard/revenue/quarterly` | none |
| Revenue yearly chart | GET | `/api/admin/dashboard/revenue/yearly` | none |
| Revenue by plan chart | GET | `/api/admin/dashboard/revenue/by-plan` | none |
| Workspace status chart | GET | `/api/admin/dashboard/workspaces/by-status` | none |
| Workspace plan chart | GET | `/api/admin/dashboard/workspaces/by-plan` | none |
| Payment summary | GET | `/api/admin/dashboard/payments/summary` | none |
| Feedback summary | GET | `/api/admin/dashboard/feedback/summary` | none |
| Danh sach workspace | GET | `/admin/workspaces` | none |
| Tao workspace truc tiep | POST | `/admin/workspaces` | `{ businessName, workspaceName, workspaceIdentifier, contactEmail, contactPhone, businessAddress, subscriptionPlanId, maxUsers, activationDate, expirationDate, status }` |
| Chi tiet workspace | GET | `/admin/workspaces/{id}` | none |
| Sua workspace | PUT | `/admin/workspaces/{id}` | `{ businessName, workspaceName, contactEmail, contactPhone, businessAddress, subscriptionPlanId, maxUsers, activationDate, expirationDate, status }` |
| Doi trang thai workspace | PATCH | `/admin/workspaces/{id}/status?status=ACTIVE` | query `status` |
| Danh sach Business Owner | GET | `/admin/workspaces/{id}/business-owners` | none |
| Tao Business Owner | POST | `/admin/workspaces/{id}/business-owners` | `{ fullName, email, username, temporaryPassword, phone, status }` |
| Reset password owner | PATCH | `/admin/business-owners/{id}/reset-password` | none |
| Doi status owner | PATCH | `/admin/business-owners/{id}/status?status=ACTIVE` | query `status` |
| Danh sach payment | GET | `/admin/payments` | none |
| Chi tiet payment | GET | `/admin/payments/{paymentId}` | none |
| Xem audit log | GET | `/admin/audit-logs` | none |
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

Business management detail screen is mandatory:

- `GET /admin/workspaces/{id}` returns workspace detail, `activeSubscription`, `ownerAccounts`, `generatedOwnerAccounts`, limits, payment status, activation/expiration dates, and provisioning metadata.
- `ownerAccounts` is the current account table for Business Owners in that workspace; render username, full name, email, phone, status, must-change-password, created/updated time, reset password action, status action.
- `generatedOwnerAccounts` is only for newly created/provisioned credentials; show it in a one-time credentials modal with copy/export CSV.
- When Platform Admin creates a workspace directly, backend automatically creates Business Owner accounts up to plan `maxOwnerAccounts`.
- Auto owner username format is `{XX}0000{suffix}` with password `123456`, for example `SV0000A / 123456`, `SV0000B / 123456`. `XX` is the two-character workspace identifier; suffix increments `A`, `B`, ..., `Z`, `AA` if needed.
- Owner accounts may exist before workspace activation, but login is blocked until workspace status is `ACTIVE` and payment status is `CONFIRMED`.
- Manual create owner defaults to the same username/password format when `username`/`temporaryPassword` are omitted.

### Employees

Employee endpoints danh cho `BUSINESS_OWNER` va `HR`. `OWNER` chi la alias tuong thich nguoc.

| Chuc nang | Method | Path | Body hoac query |
|---|---|---|---|
| Danh sach nhan vien | GET | `/employees` | none |
| Tao nhan vien | POST | `/employees` | `{ fullName, email, phone, jobTitle, seniorityLevel, skillRating, yearsOfExperience, skills }` |
| Chi tiet nhan vien | GET | `/employees/{id}` | none |
| Sua nhan vien | PUT | `/employees/{id}` | `{ fullName, email, phone, status, jobTitle, seniorityLevel, skillRating, yearsOfExperience, skills }` |
| Doi trang thai | PATCH | `/employees/{id}/status?status=ACTIVE` | query `status` |
| Reset mat khau | PATCH | `/employees/{id}/reset-password` | none |

Backend tu sinh `employeeCode` dang `SExxxx`, `username`, va `initialPassword` bang chinh `employeeCode`. UI can hien thi/lap danh sach credential nay cho `BUSINESS_OWNER/HR` sau khi tao nhan vien.
Khi reset mat khau employee thanh cong, backend tra lai `User.initialPassword`; UI chi hien thi gia tri nay cho `BUSINESS_OWNER/HR` trong modal ket qua reset.

### Tasks

| Chuc nang | Method | Path | Body |
|---|---|---|---|
| Danh sach task | GET | `/tasks` | none |
| Tao task | POST | `/tasks` | `{ title, requirements, description, customerPhone, customerEmail, customerDescription, assignmentType, assigneeId, teamLeaderId, teamMemberIds, priority, deadline, startDate, estimatedHours, difficulty, requiredSkills, requiredJobPositionId, taskDomain, projectId, departmentId, attachments }` |
| Chi tiet task | GET | `/tasks/{id}` | none |
| Sua task | PUT | `/tasks/{id}` | same body voi tao task |
| Sua thong tin khach hang | PATCH | `/tasks/{id}/customer-info` | `{ customerPhone, customerEmail, customerDescription }` |
| Giao lai task | PATCH | `/tasks/{id}/assign` | `{ assigneeId }` |
| Giao ca nhan | PATCH | `/tasks/{id}/assign-individual` | `{ employeeId }` |
| Giao nhom | PATCH | `/tasks/{id}/assign-team` | `{ teamLeaderId, teamMemberIds }` |
| Doi status | PATCH | `/tasks/{id}/status` | `{ status }` |
| Cap nhat tien do | PATCH | `/tasks/{id}/progress` | `{ progressPercent, content, updateType, attachment }` |
| Lich su cap nhat | GET | `/tasks/{id}/updates` | none |
| Them update | POST | `/tasks/{id}/updates` | `{ progressPercent, content, updateType, attachment }` |
| Huy task | PATCH | `/tasks/{id}/cancel` | none |

Quyen:

- BUSINESS_OWNER/EXECUTIVE/MANAGER xem tat ca task trong workspace, tao/sua/giao lai/huy task theo service rule.
- EMPLOYEE chi xem task duoc giao.
- BUSINESS_OWNER/EXECUTIVE/MANAGER hoac assignee/participant co the cap nhat tien do theo backend rule.
- Sua thong tin khach hang:
  - Task ca nhan: BUSINESS_OWNER/EXECUTIVE/MANAGER hoac nhan vien duoc giao duoc sua.
  - Task nhom: BUSINESS_OWNER/EXECUTIVE/MANAGER hoac team leader duoc sua.
  - Team member thuong chi xem, khong hien nut sua.

Luat UI:

- Khi `updateType = COMPLETION`, disable input progress hoac tu set `100`.
- Khi `updateType = BLOCKER`, bat buoc nhap `content`.
- Progress slider tu `0` den `100`.
- Nen khoa sua task `CANCELLED` hoac `COMPLETED` neu chua co quyet dinh san pham ro rang.

### Analytics

Analytics/workload danh cho `BUSINESS_OWNER`, `EXECUTIVE`, `MANAGER`, va `HR` theo backend policy; an action neu service tra business-rule error.

| Man hinh | Method | Path | Data |
|---|---|---|---|
| Dashboard owner production | GET | `/api/workspace/business-owner/dashboard` | `{ overviewCards, dailyReportInsight, workloadInsight, deadlineRisks, blockedTasks, taskStatusChart, workloadDistributionChart, recentlyUpdatedTasks, metadata }` |
| Dashboard owner legacy | GET | `/analytics/owner-dashboard` | compatibility only |
| Workload toan bo | GET | `/analytics/workload` | `Workload[]` |
| Workload nhan vien | GET | `/analytics/employees/{id}/workload` | `Workload` |

### AI

Phan lon endpoint AI danh cho BUSINESS_OWNER/EXECUTIVE/MANAGER/HR theo route policy. Cac endpoint phan tich/giao viec (`tasks/analyze`, `recommend-assignee`, `recommend-team-leaders`, `recommend-team-members`) danh cho workflow tao/giao task cua BUSINESS_OWNER/EXECUTIVE/MANAGER theo service rule.

| Chuc nang | Method | Path | Body hoac query |
|---|---|---|---|
| Phan tich domain task | POST | `/ai/tasks/analyze` | `{ taskTitle, taskDescription, projectDescription, departmentName, startDate, deadline }` |
| Goi y nguoi nhan | POST | `/ai/recommend-assignee` | `{ title, requirements, deadline, estimatedHours, departmentId, requiredJobPositionId, requiredSkills, taskDomain }` |
| Goi y team lead | POST | `/ai/recommend-team-leaders` | `{ title, requirements, deadline, estimatedHours }` |
| Goi y thanh vien nhom | POST | `/ai/recommend-team-members` | `{ title, requirements, deadline, estimatedHours }` |
| Tom tat workload | GET | `/ai/workload-summary` | none |
| Rui ro tre han | GET | `/ai/delay-risks` | none |
| Phan tich daily reports | GET | `/ai/daily-reports/insights` | none |
| Nhan vien thieu report | GET | `/ai/daily-reports/missing` | none |
| Tao task tu mo ta/bien ban | POST | `/ai/tasks/extract` | `{ text, defaultDeadline }` |
| De xuat chia nho task | POST | `/ai/tasks/{id}/split` | none |
| De xuat deadline/priority | POST | `/ai/tasks/{id}/adjust` | none |
| Danh sach AI suggestion | GET | `/ai/suggestions` | none |
| Doi trang thai suggestion | PATCH | `/ai/suggestions/{id}/status?status=ACCEPTED` | query `status` |
| Tom tat ngay | GET | `/ai/business-summary/daily` | none |
| Tom tat tuan | GET | `/ai/business-summary/weekly` | none |
| Tom tat thang | GET | `/ai/business-summary/monthly` | none |

`outputData` va `inputData` cua AI suggestion la string JSON. Front-end nen parse an toan bang try/catch.

AI team recommendation note:

- `recommend-team-leaders` tra `AssigneeRecommendation[]` voi `requiredRole = TEAM_LEADER`. Backend score dua tren leadershipScore, lich su lam leader, lead completion rate, domain match, similar task count va workload.
- `recommend-team-members` tra `AssigneeRecommendation[]` voi `requiredRole = TEAM_MEMBER`. Backend score dua tren teamMemberScore, skill/domain match, similar task count va workload.
- FE hien reason/risk de manager/owner hieu tai sao AI de xuat, nhung khong auto assign. User phai bam chon lead/member.

Operational action suggestions have been removed from Owner dashboard and AI Center. FE must not call `/ai/action-suggestions` and must ignore old `ACTION_SUGGESTION` cache rows if any legacy response contains them.

### Daily reports

| Chuc nang | Method | Path | Body |
|---|---|---|---|
| Danh sach report | GET | `/daily-reports` | none |
| Tao report | POST | `/daily-reports` | `{ reportDate, todayCompleted, currentWork, blockers, tomorrowPlan }` |
| Chi tiet report | GET | `/daily-reports/{id}` | none |
| Business owner danh dau da review | PATCH | `/daily-reports/{id}/review` | none |

Quyen:

- EMPLOYEE xem/tao report cua chinh minh.
- BUSINESS_OWNER xem tat ca report va review.
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

### BUSINESS_OWNER routes

- `/owner/dashboard`: tong quan.
- `/owner/employees`: CRUD nhan vien.
- `/owner/employees/:id`: chi tiet nhan vien + workload.
- `/owner/tasks/new`: tao task.
- `/owner/workspace`: cau hinh workspace.
- `/owner/analytics/workload`: bang workload.
- `/owner/ai`: goi y AI, delay risks, summaries.

### HR routes

- `/hr/employees`: CRUD ho so nhan su.
- `/hr/departments`: department master data.
- `/hr/business-positions`: business position master data.

### MANAGER/EXECUTIVE routes

- `/manager/tasks`: task/workload workspace.
- `/manager/ai`: task analysis and recommendations.

### EMPLOYEE routes

- `/employee/home`: task cua toi + report hom nay + thong bao.
- `/employee/tasks`: task duoc giao.
- `/employee/reports`: report cua toi.

Sau login, redirect theo role:

- `BUSINESS_OWNER` hoac legacy `OWNER` -> `/owner/dashboard`
- `HR` -> `/hr/employees`
- `EXECUTIVE` hoac `MANAGER` -> `/manager/tasks`
- `EMPLOYEE` -> `/employee/home`

## 7. Layout va navigation

- App shell co sidebar desktop, drawer hoac bottom navigation tren mobile.
- Header co ten workspace, nut notifications, avatar/user menu.
- User menu gom `Thong tin ca nhan`, `Doi mat khau` va `Dang xuat`.
- Khi token het han, hien toast `Phien dang nhap da het han` roi chuyen ve `/login`.

Navigation BUSINESS_OWNER:

- Dashboard
- Tasks / Create / Assign / Approve
- Monthly Workload
- HR Accounts
- Subscription / Payment
- Bao cao ngay
- Thong bao
- Workspace profile
- Profile / Doi mat khau

Navigation HR:

- Employees
- Import Excel
- Departments
- Business Positions
- Reports
- Profile / Doi mat khau

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

Submit `POST /api/public/workspace-registrations`. Khi thanh cong, lay `data.id` va `data.registrationToken`, luu token trong session/local state cua flow va dieu huong den `/workspace-registration/{registrationId}/plans`.

#### Plan selection

API:

- `GET /api/public/subscription-plans`
- `PATCH /api/public/workspace-registrations/{id}/select-plan?token={registrationToken}`

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

Submit `POST /api/public/workspace-registrations/{id}/payments?token={registrationToken}`, sau do dieu huong den trang instruction bang `paymentCode`. Neu backend tra ve payment pending con han, dung lai `paymentCode` duoc tra ve va khong tao them payment client-side.

#### Payment instruction

API:

- `GET /api/public/payments/{paymentCode}/status?token={registrationToken}` poll moi 3-5 giay.
- Stop polling khi status thuoc `SUCCESS`, `FAILED`, `EXPIRED`, `CANCELLED`; tiep tuc polling khi `PENDING` hoac `PROCESSING`.

MoMo UI:

- Hien `providerQrCodeUrl` neu co.
- Hien nut mo `providerPaymentUrl` neu co.
- Hien deeplink neu co.
- Hien amount, paymentCode, status.

Bank/VietQR UI:

- Hien `providerQrCodeUrl`.
- Hien bankName, bankCode, bankAccountNumber, bankAccountName.
- Hien amount va transferContent.
- Hien status.

#### Payment result

API:

- `GET /api/public/payments/{paymentCode}/status?token={registrationToken}`.
- `GET /api/public/workspace-registrations/{registrationId}?token={registrationToken}`.

States:

- `SUCCESS`: hien thanh toan thanh cong, workspace dang kich hoat/da kich hoat.
- `FAILED`: hien that bai va nut tao giao dich moi.
- `EXPIRED`: hien het han va nut tao giao dich moi.
- `PENDING`/`PROCESSING`: tiep tuc hien instruction/polling.

Backend tu chuyen payment pending qua `EXPIRED` khi qua `expiredAt`; public poll cung co the nhan `EXPIRED` ngay sau khi qua han. Khi user bam thu lai thanh toan, neu van con payment pending chua het han backend se tra lai payment do thay vi tao payment moi.

Buttons:

- `Gui thong tin`: submit `POST /api/public/workspace-registrations`.
- `Chon goi`: submit `PATCH /api/public/workspace-registrations/{id}/select-plan?token={registrationToken}`.
- `Tiep tuc thanh toan`: submit `POST /api/public/workspace-registrations/{id}/payments?token={registrationToken}`.
- `Thu lai thanh toan`: tao payment transaction moi.
- `Da co tai khoan`: chuyen login.

### Owner dashboard

APIs:

- `GET /api/workspace/business-owner/dashboard`
- `GET /notifications`

UI blocks:

- KPI: today/week/month cards from `overviewCards`.
- Task status chart from `taskStatusChart.series`.
- Workload distribution chart from `workloadDistributionChart.series`.
- Missing report list from `dailyReportInsight.missingEmployees`.
- Deadline risk table from `deadlineRisks`.
- Blocked task table from `blockedTasks`.
- Bang task cap nhat gan day.
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
- Assignee, chi BUSINESS_OWNER/EXECUTIVE/MANAGER.
- Overdue: `deadline < now` va status khong thuoc `COMPLETED`, `CANCELLED`.

Fields:

- Title.
- Assignee name, map tu `GET /employees` voi BUSINESS_OWNER/HR; manager/executive co the map tu task participants neu employee list bi service chan.
- Priority badge.
- Status badge.
- Progress bar.
- Deadline.
- Estimated hours.

Buttons:

- BUSINESS_OWNER/EXECUTIVE/MANAGER: `Tao task`, `Sua`, `Giao lai`, `Huy`.
- BUSINESS_OWNER/EXECUTIVE/MANAGER/assignee: `Cap nhat tien do`, `Doi trang thai`.
- Shared: `Xem chi tiet`.

### Task create/edit

Create API: `POST /tasks`

Edit API: `PUT /tasks/{id}`

Fields:

- Title: required.
- Requirements: required.
- Description: optional.
- Customer phone: optional.
- Customer gmail/email: optional, validate email format neu co.
- Customer description: optional, multiline.
- Assignment type: `INDIVIDUAL` hoac `TEAM`.
- Neu `INDIVIDUAL`: Assignee required, select tu active employees.
- Neu `TEAM`: Team leader required, team members optional/multi-select, leader khong bi duplicate trong members.
- Priority: select `LOW | MEDIUM | HIGH | CRITICAL`, default `MEDIUM`.
- Deadline: required date-time.
- Start date: optional date-time.
- Estimated hours: required number >= 1.
- Difficulty: optional 1-5.
- Required skills, required business position, task domain, department: optional nhung nen co de AI recommend chinh xac. Neu thieu, backend se goi AI task/domain analysis va map ve active department/business-position ID that.

Buttons:

- `Luu task`: create/update.
- `Phan tich task`: goi `POST /ai/tasks/analyze` de prefill department, required business position, required skills, task domain.
- `Goi y nguoi nhan`: goi `POST /ai/recommend-assignee`, chi BUSINESS_OWNER/EXECUTIVE/MANAGER.
- `Goi y team lead`: goi `POST /ai/recommend-team-leaders`, chi BUSINESS_OWNER/EXECUTIVE/MANAGER neu form dang la TEAM.
- `Goi y thanh vien`: goi `POST /ai/recommend-team-members`, chi BUSINESS_OWNER/EXECUTIVE/MANAGER neu form dang la TEAM.
- `Huy`: quay lai list.

AI recommendation panel:

- Hien thi score, workloadLevel, reason, risk.
- Neu response co `requiredRole`, `roleFit`, `roleFitReason`, hien thi de owner thay AI da doi chieu vai tro chuyen mon voi task.
- Nut `Chon nguoi nay` set `assigneeId` neu task ca nhan, set `teamLeaderId` neu `requiredRole = TEAM_LEADER`, hoac them vao `teamMemberIds` neu `requiredRole = TEAM_MEMBER`.
- Khong auto-submit task khi chon goi y.

### Task detail

APIs:

- `GET /tasks/{id}`
- `PATCH /tasks/{id}/customer-info`
- `GET /tasks/{id}/updates`

Sections:

- Thong tin task.
- Thong tin khach hang: customerPhone, customerEmail, customerDescription.
- Progress/status.
- Timeline updates.
- Panel action.

Buttons:

- `Cap nhat tien do`: mo form update.
- `Bao blocker`: preset `updateType=BLOCKER`.
- `Hoan thanh`: preset `updateType=COMPLETION`.
- `Sua thong tin khach hang`: hien khi user co quyen sua customer info.
- BUSINESS_OWNER/EXECUTIVE/MANAGER: `Sua task`, `Giao lai`, `Huy task`.

Quyen hien nut `Sua thong tin khach hang`:

- BUSINESS_OWNER/EXECUTIVE/MANAGER: hien voi moi task trong workspace.
- EMPLOYEE voi task `INDIVIDUAL`: hien neu `assigneeId` la user hien tai.
- EMPLOYEE voi task `TEAM`: hien neu user hien tai la participant co `leader = true` hoac `participantRole = LEADER`.
- Khong hien cho team member thuong.

Customer info form:

- customerPhone optional.
- customerEmail optional, validate email format neu co.
- customerDescription optional multiline.
- Submit `PATCH /tasks/{id}/customer-info`, sau do refetch task detail.

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
- user name, chi BUSINESS_OWNER/HR can map user qua employee list; role khac dung data co san neu backend tra ve.
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
- BUSINESS_OWNER: `Da review`: call review API.
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
- `GET /ai/business-summary/daily`
- `GET /ai/business-summary/weekly`
- `GET /ai/business-summary/monthly`
- `GET /ai/suggestions`

UI sections:

- Workload summary.
- Delay risks list.
- Daily/weekly/monthly business summary.
- Daily report insights: summary, blockers `{ severity, description }`.
- Missing report list with `employeeId`, `employeeName`, `reportDate`, `daysMissing`, `recommendedAction`, `confidence`.
- Task extraction form from text/minutes.
- Task split and deadline/priority recommendation for selected task.
- AI suggestion history.

Fallback note: `recommend-assignee` can return top-3 rule-based recommendations when LLM/provider times out; the list shape is unchanged and each item marks fallback in `reason`/`risk`. `workload-summary`, `delay-risks`, `daily-reports/insights`, and `daily-reports/missing` can also return rule-based data when LLM/provider fails. These card responses keep their normal keys and include `source: "RULE_BASED_FALLBACK"`, `aiProviderFailed: true`, and `fallbackReason`; show this as fallback/source metadata, not as confirmed LLM output. Operational AI action suggestions are removed and FE must not call `/ai/action-suggestions`.

Overload note: AI endpoints without fallback can return HTTP 429 with `AI_RATE_LIMITED` when too many AI calls are running. UI should show a retry-later message, keep the button disabled briefly, and avoid immediate auto-retry loops.

Buttons:

- `Tai lai`: refetch tung section.
- `Chap nhan`: `PATCH /ai/suggestions/{id}/status?status=ACCEPTED`.
- `Tu choi`: `PATCH /ai/suggestions/{id}/status?status=REJECTED`.

## 9. Button matrix

| Button | Man hinh | Role | API | Disabled khi |
|---|---|---|---|---|
| Dang nhap | Login | Public | `POST /auth/login` | form invalid/loading |
| Gui thong tin dang ky | Registration information | Public | `POST /api/public/workspace-registrations` | form invalid/loading |
| Chon goi dang ky | Plan selection | Public | `PATCH /api/public/workspace-registrations/{id}/select-plan?token={registrationToken}` | no plan selected/loading |
| Tiep tuc thanh toan | Payment method | Public | `POST /api/public/workspace-registrations/{id}/payments?token={registrationToken}` | no payment method selected/loading |
| Mo trang MoMo | Payment instruction | Public | external `providerPaymentUrl` | no providerPaymentUrl |
| Thu lai thanh toan | Payment result | Public | `POST /api/public/workspace-registrations/{id}/payments?token={registrationToken}` | loading |
| Dang xuat | User menu | Authenticated | `POST /auth/logout` | loading |
| Doi mat khau | Profile/User menu | Authenticated | `PATCH /auth/change-password` | form invalid/loading |
| Them nhan vien | Employees | HR | none, mo modal | none |
| Luu nhan vien | Employee modal | HR | `POST /employees` hoac `PUT /employees/{id}` | form invalid/loading |
| Kich hoat | Employees | HR | `PATCH /employees/{id}/status?status=ACTIVE` | user already ACTIVE/loading |
| Tam ngung | Employees | HR | `PATCH /employees/{id}/status?status=INACTIVE` | user already INACTIVE/loading |
| Reset mat khau nhan vien | Employees | HR | `PATCH /employees/{id}/reset-password` | loading |
| Tao task | Tasks/Dashboard | BUSINESS_OWNER/EXECUTIVE/MANAGER | none, route create | no active employee |
| Luu task | Task form | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /tasks` hoac `PUT /tasks/{id}` | form invalid/loading |
| Phan tich task | Task form | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /ai/tasks/analyze` | thieu title/description/loading |
| Goi y nguoi nhan | Task form | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /ai/recommend-assignee` | thieu title/requirements/deadline/loading |
| Goi y team lead | Task form | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /ai/recommend-team-leaders` | assignmentType khac TEAM hoac thieu title/requirements/deadline/loading |
| Goi y thanh vien nhom | Task form | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /ai/recommend-team-members` | assignmentType khac TEAM hoac thieu title/requirements/deadline/loading |
| Chon nguoi nay | AI recommendation | BUSINESS_OWNER/EXECUTIVE/MANAGER | none, set assignee | employee inactive neu co data |
| Tao task bang AI | AI center | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /ai/tasks/extract` | text empty/loading |
| Chia nho task | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /ai/tasks/{id}/split` | loading |
| De xuat deadline/priority | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /ai/tasks/{id}/adjust` | loading |
| Giao lai | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER | `PATCH /tasks/{id}/assign` | no assignee/loading |
| Sua thong tin khach hang | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER/assignee/leader | `PATCH /tasks/{id}/customer-info` | no permission/loading |
| Huy task | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER | `PATCH /tasks/{id}/cancel` | status CANCELLED/COMPLETED/loading |
| Doi trang thai | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER/assignee | `PATCH /tasks/{id}/status` | no status/loading |
| Cap nhat tien do | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER/assignee | `PATCH /tasks/{id}/progress` | content empty/loading |
| Bao blocker | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER/assignee | `PATCH /tasks/{id}/progress` | content empty/loading |
| Hoan thanh | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER/assignee | `PATCH /tasks/{id}/progress` | status COMPLETED/loading |
| Gui bao cao | Daily report form | BUSINESS_OWNER/EMPLOYEE | `POST /daily-reports` | form invalid/loading |
| Da review | Daily report detail | BUSINESS_OWNER | `PATCH /daily-reports/{id}/review` | already reviewed/loading |
| Danh dau da doc | Notifications | BUSINESS_OWNER/EMPLOYEE | `PATCH /notifications/{id}/read` | already read/loading |
| Danh dau tat ca da doc | Notifications | BUSINESS_OWNER/EMPLOYEE | `PATCH /notifications/read-all` | no unread/loading |
| Chap nhan suggestion | AI center | BUSINESS_OWNER/EXECUTIVE/MANAGER/HR | `PATCH /ai/suggestions/{id}/status?status=ACCEPTED` | status ACCEPTED/loading |
| Tu choi suggestion | AI center | BUSINESS_OWNER/EXECUTIVE/MANAGER/HR | `PATCH /ai/suggestions/{id}/status?status=REJECTED` | status REJECTED/loading |

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
    RequirePermission.tsx
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
- Guest register/select plan/create payment/check payment status duoc khong can login.
- BUSINESS_OWNER xem dashboard duoc.
- HR CRUD employee duoc; BUSINESS_OWNER chi xem employee de giao task/workload va tao HR account rieng.
- BUSINESS_OWNER/MANAGER tao/sua/giao/huy task duoc khi co permission tu backend.
- EMPLOYEE chi thay task duoc giao.
- User co `TASK_UPDATE_OWN` cap nhat progress, blocker, completion dung quyen duoc.
- Daily report tao duoc va khong tao trung cung ngay.
- User co `REPORT_REVIEW` review daily report duoc.
- Notifications doc duoc va mark read duoc.
- AI recommendation chon duoc assignee nhung khong auto assign.
- UI xu ly 401/403 ro rang.
- Khong con request nao tro toi AI service truc tiep.
- Khong con dependency vao front-end cu da xoa.

## 17. FE Change Log - Authorization Production Delta

Bat buoc update trong FE source:

- Replace `RequireRole` bang `RequirePermission(requiredPermissions)`; role chi de redirect sau login.
- Add `Permission` type, `user.permissions`, `login.permissions`, `hasPermission()`, `hasAnyPermission()`.
- Public pages `pricing`, `workspace registration`, `plan selection`, `payment method`, `payment instruction`, `payment result`, `activation result` khong bi redirect login.
- Sidebar/menu/button/dialog/action hide theo permission matrix trong `docs/FE.md`.
- HR screens: Department/Business Position mutation button dung `DEPARTMENT_MANAGE`/`POSITION_MANAGE`; Business Owner khong duoc hien mutate actions cho master data nay.
- Task screens: create `TASK_CREATE`, assign `TASK_ASSIGN`, approve/return/cancel `TASK_APPROVE`, employee self update `TASK_UPDATE_OWN`.
- AI screens: analyze `AI_ANALYZE`, recommendation/explanation `AI_RECOMMENDATION`, owner/platform summary `AI_SUMMARY`, history/suggestions `AI_HISTORY`; HR khong thay AI center neu khong co permission.
- Platform screens: plans `PACKAGE_MANAGE`, registrations/workspaces `WORKSPACE_MANAGE`, payments `PAYMENT_HISTORY_VIEW`, confirm/reject `PAYMENT_CONFIRM`, QR settings `PAYMENT_QR_MANAGE`, revenue `REVENUE_VIEW`, audit `AUDIT_LOG_VIEW`.
- Payment QR UI must use backend-returned `providerQrCodeUrl`; if missing QR business error, show waiting state and do not generate fallback QR client-side.
