# Dac Ta Xay Front-End FOREP EXE

Repository hien chua co frontend source. Tai lieu nay la **Frontend can trien khai**, khong phai mo ta UI da ton tai. Cac endpoint/schema duoc danh dau **Da co trong backend**; muc ghi **Chua duoc backend ho tro** khong duoc hien thi nhu tinh nang hoan thanh.

Dung `http://localhost:8080` lam API origin. Auth/legacy operations dung prefix `/api/v1`, public registration/payment dung `/api/public`, workspace UI moi dung `/api/workspace`, va Platform Admin moi dung `/api/admin`. `docs/FE.md` giai thich hanh vi nguoi dung; file nay chi giu route, component, API mapping, schema, state va acceptance criteria de tranh lap lai phan giai thich dai.

## Muc luc

- [1. Nguyen tac tich hop API](#1-nguyen-tac-tich-hop-api)
- [2. Enum dung trong UI](#2-enum-dung-trong-ui)
- [3. Data model front-end](#3-data-model-front-end)
- [4-5. API client va contract theo module](#4-api-client-can-co)
- [6-8. Route, navigation va CRUD theo man hinh](#6-route-va-man-hinh-de-xuat)
- [9-12. Button, validation, state va mapping](#9-button-matrix)
- [13-15. Cache, bao mat va cau truc source](#13-cacherefetch-de-xuat)
- [16-17. Checklist va production delta](#16-checklist-nghiem-thu-front-end)

## 1. Nguyen tac tich hop API

- Front-end chi goi Backend API, khong goi truc tiep AI Service.
- Tat ca endpoint, tru `GET /api/v1/health`, `POST /api/v1/auth/login`, cac endpoint `/api/public/**`, va provider callback `/api/payment-callbacks/**`, can header `Authorization: Bearer <token>`.
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
- `POST /api/v1/auth/login` tra `{ token, user, permissions }`; `GET /api/v1/auth/me` tra `User` co `permissions: Permission[]`. FE auth store phai luu permissions va expose `hasPermission()` / `hasAnyPermission()`.
- Khong goi Developer, BA, HR Staff, Tech Lead... la system role. Day la Business Position/Job Position trong workspace, khac voi system role.

## 2. Enum dung trong UI

### Role

- `PLATFORM_ADMIN`: quan tri nen tang, goi subscription, thanh toan, workspace va business owner account khoi tao.
- `BUSINESS_OWNER`: chu workspace, quan ly tai khoan HR, task/workload/report/dashboard va chi xem employee/department/business position; payment/subscription chi read-only neu co permission.
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

### PaymentTransactionStatus

- `PENDING`: giao dich da tao, dang cho user thanh toan.
- `SUCCESS`: payment da duoc provider/admin xac nhan, workspace duoc kich hoat.
- `FAILED`: payment that bai/bi tu choi, user co the tao giao dich moi.
- `EXPIRED`: payment het han, user can tao giao dich moi.
- `CANCELLED`: giao dich bi huy.

### RegistrationStatus

- `DRAFT`: ho so nhap do, chi render neu backend tra du lieu da luu tu truoc.
- `PENDING_PLAN_SELECTION`: da gui thong tin dang ky, dang cho chon goi.
- `PENDING_PAYMENT`: da chon goi, dang cho tao/xac nhan payment.
- `PAYMENT_CONFIRMED`: payment da xac nhan, backend dang/da kich hoat workspace.
- `APPROVED`: da duyet va tao workspace.
- `ACTIVATED`: workspace da kich hoat, subscription/owner accounts da duoc tao.
- `REJECTED`: bi tu choi.
- `CANCELLED`: ho so bi huy.
- `EXPIRED`: ho so da het han.
- `SUBMITTED`, `PAYMENT_PENDING`, `PAYMENT_SUBMITTED`: cac gia tri van co trong enum backend; UI phai render an toan neu API tra ve du lieu theo luong cu.

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
- `ACCEPTED`: nhan vien da nhan task.
- `IN_PROGRESS`: dang lam.
- `BLOCKED`: co vuong mac.
- `SUBMITTED`: nhan vien da gui hoan thanh, dang cho nguoi quan ly duyet.
- `RETURNED`: nguoi quan ly tra lai de chinh sua.
- `COMPLETED`: hoan thanh.
- `CANCELLED`: da huy.

### UpdateType

- `ACCEPTANCE`: ban ghi backend tao khi nhan vien nhan task.
- `PROGRESS`: cap nhat tien do thuong.
- `BLOCKER`: bao vuong mac, backend set task thanh `BLOCKED`.
- `COMPLETION`: gui hoan thanh, backend set progress thanh `100` va status thanh `SUBMITTED`.
- `COMPLETION_APPROVAL`: ban ghi backend tao khi nguoi quan ly duyet hoan thanh.
- `RETURN`: ban ghi backend tao khi nguoi quan ly tra task de chinh sua.

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
  meta: { requestId: string; timestamp: string };
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
  role: 'PLATFORM_ADMIN' | 'BUSINESS_OWNER' | 'HR' | 'EXECUTIVE' | 'MANAGER' | 'EMPLOYEE' | 'SYSTEM' | 'SYSTEM_ADMIN' | 'OWNER';
  permissions: Permission[];
  avatar: string | null;
  avatarFileId: string | null;
  status: 'ACTIVE' | 'INACTIVE' | 'INVITED';
  jobTitle: string | null;
  seniorityLevel: 'INTERN' | 'JUNIOR' | 'MIDDLE' | 'SENIOR' | 'LEAD' | null;
  skillRating: 1 | 2 | 3 | 4 | 5 | null;
  yearsOfExperience: number | null;
  skills: string | null;
  departmentId: string | null;
  jobPositionId: string | null;
  dateOfBirth: string | null;
  gender: string | null;
  address: string | null;
  personalSummary: string | null;
  employmentType: 'FULL_TIME' | 'PART_TIME' | 'CONTRACTOR' | 'INTERN' | null;
  workingStatus: 'WORKING' | 'ON_LEAVE' | 'RESIGNED' | null;
  employeeLevel: 'INTERN' | 'FRESHER' | 'JUNIOR' | 'MIDDLE' | 'SENIOR' | 'LEAD' | 'MANAGER' | null;
  monthlyWorkingCapacityHours: number | null;
  mainExpertise: string | null;
  secondaryExpertise: string | null;
  mustChangePassword: boolean;
  initialAccountGenerated: boolean;
  createdAt: string;
  updatedAt: string;
};

type AccountProvisioning = {
  id: string;
  username: string;
  fullName: string;
  email: string;
  role: 'BUSINESS_OWNER' | 'HR';
  status: 'ACTIVE' | 'INACTIVE' | 'INVITED';
  workspaceId: string;
  temporaryPassword: string | null;
  mustChangePassword: boolean;
  credentialsVisibleOnce: boolean;
};

type CreatedUserAccount = {
  user: User;
  username: string;
  temporaryPassword: string;
  credentialsVisibleOnce: boolean;
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
  | 'HR_ACCOUNT_MANAGE'
  | 'EMPLOYEE_IMPORT'
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
  status: 'ASSIGNED' | 'ACCEPTED' | 'IN_PROGRESS' | 'BLOCKED' | 'SUBMITTED' | 'RETURNED' | 'COMPLETED' | 'CANCELLED';
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
  updateType: 'ACCEPTANCE' | 'PROGRESS' | 'BLOCKER' | 'COMPLETION' | 'COMPLETION_APPROVAL' | 'RETURN';
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
  departmentId: string | null;
  businessPositionId: string | null;
  businessPositionName: string | null;
  permissionGroup: 'EMPLOYEE' | 'MANAGER' | 'EXECUTIVE' | null;
  employeeLevel: 'INTERN' | 'FRESHER' | 'JUNIOR' | 'MIDDLE' | 'SENIOR' | 'LEAD' | 'MANAGER' | null;
  monthlyCapacityHours: number | null;
  currentMonthlyHours: number;
  newTaskAllocatedHours: number;
  projectedMonthlyHours: number;
  projectedUtilizationRatio: number;
  projectedWorkloadLevel: 'IDLE' | 'LIGHT' | 'OK' | 'WARNING' | 'OVERLOADED' | 'HARD_OVERLOAD' | null;
  eligibilityStatus: 'ELIGIBLE' | 'WARNING' | 'NOT_ELIGIBLE';
  eligibilityReasons: string[];
  departmentSuitabilityScore: number;
  businessPositionSuitabilityScore: number;
  employeeLevelFitScore: number;
  seniorityFitScore: number;
  skillMatchScore: number;
  workloadAvailabilityScore: number;
  performanceScore: number;
  performanceMetrics: Record<string, unknown>;
  scoreComponents: Record<string, unknown>;
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
  registrationToken: string;
  subscriptionPlanId: string | null;
  maxUsers: number;
  maxOwnerAccounts: number;
  maxEmployeeAccounts: number;
  ownerFullName: string;
  ownerEmail: string;
  ownerPhone: string | null;
  paymentProofUrl: string | null;
  paymentStatus: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CORRECTION_REQUESTED';
  registrationStatus: 'SUBMITTED' | 'DRAFT' | 'PAYMENT_PENDING' | 'PAYMENT_SUBMITTED' | 'PENDING_PLAN_SELECTION' | 'PENDING_PAYMENT' | 'PAYMENT_CONFIRMED' | 'APPROVED' | 'ACTIVATED' | 'REJECTED' | 'CANCELLED' | 'EXPIRED';
  workspaceId: string | null;
  reviewedBy: string | null;
  reviewedAt: string | null;
  reviewNote: string | null;
  expiredAt: string;
  generatedOwnerAccounts: AccountProvisioning[];
  createdAt: string;
  updatedAt: string;
};

type PlatformWorkspace = {
  id: string;
  businessName: string | null;
  workspaceName: string;
  workspaceIdentifier: string | null;
  organizationAbbreviation: string | null;
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
  generatedOwnerAccounts: AccountProvisioning[];
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
  paymentMethod: 'MOMO';
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
  paymentMethod: 'MOMO';
  providerEndpoint: string | null;
  partnerCode: string | null;
  accessKey: string | null;
  secretKeyConfigured: boolean;
  returnUrl: string | null;
  notifyUrl: string | null;
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
  paymentMethod: 'MOMO';
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
| Login | POST | `/api/v1/auth/login` | `{ email, password }` hoac `{ username, password }` | `{ token, user, permissions }` |
| Logout | POST | `/api/v1/auth/logout` | none | `{ message }` |
| Current user | GET | `/api/v1/auth/me` | none | `User` |
| Doi mat khau | PATCH | `/api/v1/auth/change-password` | `{ currentPassword, newPassword }` | `User` |

`POST /api/v1/auth/logout` hien chi tra message, khong revoke token o server. Front-end phai tu xoa token local.
`PATCH /api/v1/auth/change-password` can token dang nhap, dung cho moi authenticated user. `newPassword` dai 8-72 ky tu; sau thanh cong backend set `mustChangePassword=false`.

Auth store bat buoc:

- `permissions` lay tu `login.permissions` hoac `me.permissions`.
- `hasPermission(permission)` va `hasAnyPermission(permissions)` la helper duy nhat cho route/sidebar/button.
- Luu `token`, `currentUser`, `role`, `permissions`, `workspaceId`, `status`, `mustChangePassword`; khong dung workspace trong localStorage lam nguon xac thuc.
- Neu `status != ACTIVE`, login bi backend tu choi. Neu workspace bi suspend/expired/payment chua confirmed, login workspace user cung bi tu choi.
- Neu `mustChangePassword=true`, dieu huong den trang doi mat khau va khong luu/ghi log temporary password. Day la UI policy; backend hien chua middleware chan tat ca API truoc khi doi mat khau.
- Token het han/HTTP 401: clear auth state va redirect `/login` kem message het phien.
- HTTP 403: giu session, dieu huong `/403`; khong thu refresh token vi backend hien khong co refresh-token endpoint.
- Guest route pricing/register/payment/result khong dung `RequireAuth`.
- Role chi dung de redirect sau login va hien thi label, khong dung de hien/hide action.

### Workspace

| Chuc nang | Method | Path | Body | Quyen |
|---|---|---|---|---|
| Danh sach goi dang ky active | GET | `/api/public/subscription-plans` | none | public |
| Gui thong tin dang ky workspace | POST | `/api/public/workspace-registrations` | `WorkspaceRegistrationRequest` ben duoi | public |
| Xem ho so dang ky | GET | `/api/public/workspace-registrations/{id}?token={registrationToken}` | none | public |
| Chon goi dang ky | PATCH | `/api/public/workspace-registrations/{id}/select-plan?token={registrationToken}` | `{ subscriptionPlanId }` | public |
| Tao giao dich thanh toan | POST | `/api/public/workspace-registrations/{id}/payments?token={registrationToken}` | `{ paymentMethod: 'MOMO' }` | public |
| Xem payment public | GET | `/api/public/payments/{paymentCode}/status?token={registrationToken}` | none | public |
| Xem workspace | GET | `/api/v1/workspaces/current` | none | `WORKSPACE_VIEW` |
| Sua workspace | PUT | `/api/v1/workspaces/current` | `{ name, shortCode, logo, address }` | `WORKSPACE_UPDATE` |

Khong dung `/api/v1/workspaces/register` cho user public nua. Endpoint nay da bi chan de tranh tao workspace/account khi chua thanh toan.

Flow dang ky workspace public:

1. Trang workspace registration nhap thong tin doanh nghiep, nguoi dai dien va thong tin Business Owner dau tien, goi `POST /api/public/workspace-registrations`, sau do chuyen sang trang chon goi bang `registrationId` va `registrationToken`.
2. Trang chon goi goi `GET /api/public/subscription-plans`, hien thi name, description, price, duration, `maxOwnerAccounts`, `maxEmployeeAccounts`, full features va nut chon goi.
3. Khi user chon goi, UI goi `PATCH /api/public/workspace-registrations/{id}/select-plan?token={registrationToken}` roi chuyen sang trang chon phuong thuc thanh toan.
4. Trang payment method chi hien `MOMO`, sau do goi `POST /api/public/workspace-registrations/{id}/payments?token={registrationToken}`.
5. Trang payment instruction hien thi theo `PublicPaymentStatus`: MoMo provider data neu provider that da cau hinh; hien amount, paymentCode, status. Khong phu thuoc `orderCode`, `requestId`, `providerTransactionId` o UI public. Neu backend tra ve payment pending con han da ton tai, UI dung lai `paymentCode` do va khong tao them instruction moi.
6. UI poll `GET /api/public/payments/{paymentCode}/status?token={registrationToken}` moi 3-5 giay den khi status la `SUCCESS`, `FAILED` hoac `EXPIRED`.
7. Trang ket qua goi them `GET /api/public/workspace-registrations/{id}?token={registrationToken}` de hien thi payment result va workspace activation status.
8. UI khong cho login owner khi payment chua `SUCCESS`; frontend khong tu tin payment success tu query string/callback client.

`WorkspaceRegistrationRequest` thuc te:

```ts
type WorkspaceRegistrationRequest = {
  businessName: string;              // required
  workspaceName: string;             // required
  workspaceIdentifier?: string;      // neu co: dung 2 ky tu chu/so
  contactEmail: string;              // required, email
  contactPhone?: string;
  businessAddress?: string;
  subscriptionPlanId?: string;       // UI flow thuong chon o buoc sau
  maxUsers?: number;                 // khong cho user public tu dieu khien quota
  ownerFullName?: string;            // fallback representativeFullName
  ownerEmail?: string;               // fallback representativeEmail
  ownerPhone?: string;               // fallback representativePhone
  ownerPassword: string;             // required, 8-72 ky tu
  representativeFullName: string;    // required
  representativeEmail: string;       // required, email
  representativePhone?: string;
  paymentProofUrl?: string;
  paymentNote?: string;
};
```

UI khong hien `subscriptionPlanId`/`maxUsers` trong form thong tin dau tien; plan duoc chon qua endpoint `select-plan`. Backend hash `ownerPassword` bang BCrypt va khong tra lai password/password hash trong `WorkspaceRegistrationView`.

Neu khong thanh toan, user public khong tao duoc workspace/account. Chi Platform Admin moi duoc tao workspace truc tiep qua API admin legacy; canonical `/api/admin` hien khong co create-workspace truc tiep.

Production payment note: MoMo callback can xac thuc signature bang `secretKey` do Platform Admin cau hinh trong UI. Backend khong dung bank transfer, khong dung sandbox/stub URL.

MoMo provider mode:

- FE khong hien input URL thanh toan/URL anh QR/deeplink trong admin payment settings.
- Neu backend tra `providerPaymentUrl`/`providerDeeplink`/`providerQrCodeUrl` tu MoMo provider that, FE duoc render cac truong nay cho user thanh toan.
- Neu backend bao MoMo chua cau hinh/chua san sang, payment method page hien message: "MoMo chua san sang. Vui long doi quan tri vien cap nhat cau hinh thanh toan." va khong tiep tuc tao payment.
- Khong tu sinh QR trong FE, khong dung QR fake, khong dung QR tu third-party client-side.

Admin MoMo payment setting:

- `GET /api/admin/momo-payment-setting`
- `PUT /api/admin/momo-payment-setting`
- Body canonical: `{ providerEndpoint, partnerCode, accessKey, secretKey?, returnUrl, ipnUrl, transferContentPrefix?, enabled }`; `notifyUrl` chi la alias tuong thich cu.
- `secretKey` la password field: FE chi gui khi admin nhap/cap nhat, khong hien lai gia tri cu; dung `secretKeyConfigured` de hien trang thai da cau hinh.
- Khong co upload QR, khong co bank account fields, khong co `BANK_TRANSFER`, khong gui `qrCodeUrl`, `paymentUrl`, `deeplink`.
- Sau khi update, invalidate/refetch `momoPaymentSetting`; cac payment moi se dung cau hinh moi, payment cu giu snapshot cau hinh tai thoi diem tao.

Admin MoMo setup UX requirement:

- Dat menu/page la `Thanh toan MoMo`, khong dat `QR ngan hang` hay `Payment QR` de tranh hieu nham.
- Dau trang co status card lon: `Chua cau hinh`, `Thieu thong tin`, `Da cau hinh - dang tat`, `San sang nhan thanh toan`, kem last updated va nguoi cap nhat neu backend co `updatedBy/updatedAt`.
- Status card hien checklist 6 muc: `Endpoint`, `Partner Code`, `Access Key`, `Secret Key`, `Return URL`, `Notify/IPN URL`; muc nao thieu hien icon warning va text ngan gon.
- Form chia 3 nhom: `Thong tin merchant` (`partnerCode`, `accessKey`, `secretKey`), `URL tich hop` (`providerEndpoint`, `returnUrl`, `ipnUrl`), `Van hanh` (`transferContentPrefix`, `enabled`).
- `secretKey` la write-only: neu `secretKeyConfigured=true`, hien badge `Da luu secret`, input placeholder `Nhap secret moi neu muon doi`; khong gui `secretKey` khi input rong.
- Toggle `enabled` bi disable neu form thieu bat ky field bat buoc nao; khi enable lan dau, hien confirm modal "Bat MoMo se cho phep user tao giao dich that".
- FE validate truoc khi submit: URL phai la `https://` tren production, endpoint/returnUrl/ipnUrl khong duoc rong, `partnerCode`/`accessKey` khong duoc co khoang trang dau/cuoi, `transferContentPrefix` toi da 30 ky tu.
- `ipnUrl` nen hien helper/copy text: `Backend IPN callback phai tro ve /api/payments/momo/ipn`; neu FE biet API base URL thi hien nut copy full callback URL.
- Sau khi save thanh cong, refetch va hien toast ro rang: `Da luu cau hinh MoMo`; neu backend bao thieu config, scroll den checklist va highlight field loi.
- Khong hien nut/link "Mo thanh toan MoMo" trong admin setting; URL/deeplink/QR chi duoc render o public payment instruction sau khi backend tao transaction thanh cong.
- Them nut secondary `Xem nhat ky cau hinh` dieu huong den System Logs filter `action=ADMIN_UPDATE_MOMO_PAYMENT_SETTING`; giup admin doi soat ai da thay doi config.
- Neu can test ket noi tren UI, yeu cau BE bo sung endpoint rieng `POST /api/admin/momo-payment-setting/test` hoac `POST /api/admin/momo-payment-setting/validate`; truoc khi co endpoint nay, FE khong duoc fake test success.

Workspace subscription snapshot:

- Platform workspace response co `activeSubscription`.
- FE hien goi hien tai/renewal/limit tu `activeSubscription` neu khac null.
- `subscriptionPlanId`, `maxUsers`, `maxOwnerAccounts`, `maxEmployeeAccounts` tren workspace chi la field tuong thich/fallback.
- Sau admin update plan/status/payment, invalidate/refetch `workspaces`, `workspaceDetail:{id}`, `adminDashboardOverview`, `adminRevenue:{period}`, `adminWorkspaceCharts`.

Demo seed data cho QA:

- Migration `V16__demo_saas_operational_seed.sql` tao 3 workspace active: `SV`, `MD`, `HC`.
- Moi workspace co 30 employee, department, business position, 18 task, assignment, daily report, workload bucket, AI history/suggestion cache, feedback, payment va active subscription.
- Seed cu co the con username/hash lich su de QA tuong thich. Khong dung credential seed lam provisioning contract hoac copy production; tai khoan moi phai theo `AccountProvisioning` va quy tac username/password moi.

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

### Platform Admin

**Da co trong backend.** UI moi dung endpoint canonical `/api/admin`; cac endpoint `/api/v1/admin/**` chi la compatibility va khong dua vao route moi neu canonical da co.

| Man hinh/Hanh dong | Method | Endpoint | Permission | Request | Response `data` |
|---|---|---|---|---|---|
| Registration list | GET | `/api/admin/workspace-registrations` | `WORKSPACE_MANAGE` | none | `WorkspaceRegistration[]` |
| Registration detail | GET | `/api/admin/workspace-registrations/{id}` | `WORKSPACE_MANAGE` | path UUID | `WorkspaceRegistration` |
| Approve/activate registration | PATCH | `/api/admin/workspace-registrations/{id}/approve` | `WORKSPACE_MANAGE` | `{ "note"?: string }` hoac no body | `WorkspaceRegistration` |
| Reject registration | PATCH | `/api/admin/workspace-registrations/{id}/reject` | `WORKSPACE_MANAGE` | `{ "note"?: string }` hoac no body | `WorkspaceRegistration` |
| Payment list/detail | GET | `/api/admin/payments`, `/api/admin/payments/{paymentId}` | `PAYMENT_HISTORY_VIEW` | none | list/detail payment |
| Confirm payment + activation | PATCH | `/api/admin/payments/{paymentId}/confirm` | `PAYMENT_CONFIRM` | `{ "note"?: string }` hoac no body | `PaymentTransaction` |
| Reject payment | PATCH | `/api/admin/payments/{paymentId}/reject` | `PAYMENT_CONFIRM` | `{ "note"?: string }` hoac no body | `PaymentTransaction` |
| Workspace list/detail | GET | `/api/admin/workspaces`, `/api/admin/workspaces/{id}` | `WORKSPACE_MANAGE` | none | list/`PlatformWorkspace` |
| Suspend/restore workspace | PATCH | `/api/admin/workspaces/{id}/suspend`, `/api/admin/workspaces/{id}/restore` | `WORKSPACE_MANAGE` | none | `PlatformWorkspace` |
| Owner list | GET | `/api/admin/workspaces/{id}/business-owners` | `WORKSPACE_MANAGE` | none | `User[]` |
| Create owner | POST | `/api/admin/workspaces/{id}/business-owners` | `WORKSPACE_MANAGE` | `{ fullName, email, phone? }` | `AccountProvisioning` |
| Reset owner password | PATCH | `/api/admin/business-owners/{id}/reset-password` | `WORKSPACE_MANAGE` | none | `AccountProvisioning` |
| Owner status | PATCH | `/api/admin/business-owners/{id}/status?status=ACTIVE|INACTIVE` | `WORKSPACE_MANAGE` | query | `User` |
| Provision initial owner for legacy workspace | POST | `/api/admin/workspaces/{id}/provision-owner-accounts` | `WORKSPACE_MANAGE` | none | `AccountProvisioning[]` |

Activation canonical khong co nut/API rieng. Confirm payment se lock payment + registration va kich hoat workspace trong cung transaction; approve chi hop le sau payment confirmed. Double click/request lap khong tao them workspace, subscription hoac owner. FE van phai disable button va refetch sau thanh cong.

Owner dau tien tu registration dung `ownerPassword` da BCrypt, nen activation thuong tra `temporaryPassword=null`, `mustChangePassword=false`, `credentialsVisibleOnce=false`. Manual create/reset/provision sinh password ngau nhien 16 ky tu va chi tra mot lan.

Form create owner tuyet doi khong co `username`, `temporaryPassword`, `status`, `role`, `permissions`, `passwordHash` hoac workspace picker. Backend chi tao mot owner ban dau; `maxOwnerAccounts` la gioi han de Admin tao them owner co chu dich, khong phai so account duoc auto-fill.

UI Platform Admin khong hien task detail, task assignment, employee workload noi bo, daily report chi tiet hay thao tac nghiep vu trong workspace.

### Business Owner quan ly HR accounts

| Hanh dong | Method | Endpoint | Permission | Request | Response `data` |
|---|---|---|---|---|---|
| List HR | GET | `/api/workspace/business-owner/hr-accounts` | `HR_ACCOUNT_MANAGE` | Khong co query | `User[]` |
| Create HR | POST | `/api/workspace/business-owner/hr-accounts` | `HR_ACCOUNT_MANAGE` | `{ fullName, email, phone? }` | `AccountProvisioning` |
| Lock/unlock HR | PATCH | `/api/workspace/business-owner/hr-accounts/{id}/status` | `HR_ACCOUNT_MANAGE` | `{ status: 'ACTIVE' | 'INACTIVE' }` | `User` |

Service con bat buoc principal phai la `BUSINESS_OWNER`/legacy `OWNER`, co workspace context va chi thao tac HR cung workspace. `PLATFORM_ADMIN`, `HR` va `EMPLOYEE` khong the dung API nay ngay ca khi permission seed bi cau hinh sai.

**Chua duoc backend ho tro:** server-side search, status filter, pagination, `lastLoginAt`, HR detail endpoint rieng va reset password HR. List page chi duoc filter/paginate client-side tren danh sach hien tai; khong hien action reset password.

### Quy tac provisioning account

**Da co trong backend:** frontend chi render username backend tra ve, khong tu normalize hoac them suffix.

| Account | Format backend |
|---|---|
| Business Owner | `owner.<workspaceCode>` |
| HR | `hr.<workspaceCode>.<normalizedFullName>` |
| Employee | `emp.<workspaceCode>.<normalizedFullName>` |

`normalizedFullName` duoc bo dau tieng Viet (ke ca `đ`), chuyen chu thuong, bo khoang trang/ky tu dac biet; backend them suffix so khi trung. Manual create/reset owner, create HR va create/reset employee sinh password ngau nhien 16 ky tu, BCrypt de luu, tra `temporaryPassword` mot lan, `mustChangePassword=true`, `credentialsVisibleOnce=true`. Owner dau tien tu registration dung `ownerPassword` user da nhap, nen activation khong tra password tam va `mustChangePassword=false`.

`PATCH /api/v1/auth/change-password` da co va xoa co `mustChangePassword`. UI can ep dieu huong den trang doi mat khau khi co nay, nhung day la policy frontend vi backend chua chan toan bo API den khi doi xong. Khong luu credential tam vao local/session storage, log, telemetry hay cache.

### Employees

Business Owner chi co `EMPLOYEE_VIEW`. Create/update/status/reset/import duoc service hard-check role `HR` ngoai permission check.

| Chuc nang | Method | Path | Body hoac query | Permission |
|---|---|---|---|---|
| Danh sach nhan vien | GET | `/api/workspace/hr/employees` | none | `EMPLOYEE_VIEW` |
| Chi tiet nhan vien | GET | `/api/workspace/hr/employees/{id}` | none | `EMPLOYEE_VIEW` |
| Tao nhan vien | POST | `/api/workspace/hr/employees` | `CreateEmployeeRequest` | `EMPLOYEE_CREATE` + role HR |
| Sua nhan vien | PUT | `/api/workspace/hr/employees/{id}` | `UpdateEmployeeRequest` | `EMPLOYEE_UPDATE` + role HR |
| Doi trang thai | PATCH | `/api/workspace/hr/employees/{id}/status` | `{ status: 'ACTIVE' | 'INACTIVE' }` | `EMPLOYEE_DEACTIVATE` + role HR |
| Import validate | POST multipart | `/api/workspace/hr/employees/import` | part `file` | `EMPLOYEE_IMPORT` + role HR |
| Import history/detail/errors/confirm/cancel | mixed | `/api/workspace/hr/employees/imports/**` | theo controller | `EMPLOYEE_IMPORT` + role HR |
| Reset mat khau (legacy, chua co workspace alias) | PATCH | `/api/v1/employees/{id}/reset-password` | none | `EMPLOYEE_UPDATE` + role HR |

Backend sinh `employeeCode`, username `emp.<workspaceCode>.<normalizedFullName>` va password tam ngau nhien. Create/reset tra `CreatedUserAccount { user, username, temporaryPassword, credentialsVisibleOnce }`; list/detail `User` khong co password. UI chi hien credential trong modal ket qua mot lan va khong luu vao storage/log.

### Tasks

| Chuc nang | Method | Path | Body |
|---|---|---|---|
| Danh sach task | GET | `/api/workspace/tasks` | none |
| Tao task | POST | `/api/workspace/tasks` | `{ title, requirements, description, customerPhone, customerEmail, customerDescription, assignmentType, assigneeId, teamLeaderId, teamMemberIds, priority, deadline, startDate, estimatedHours, difficulty, requiredSkills, requiredJobPositionId, taskDomain, projectId, departmentId, attachments }` |
| Chi tiet task | GET | `/api/workspace/tasks/{id}` | none |
| Sua task | PUT | `/api/workspace/tasks/{id}` | same body voi tao task |
| Sua thong tin khach hang | PATCH | `/api/workspace/tasks/{id}/customer-info` | `{ customerPhone, customerEmail, customerDescription }` |
| Giao ca nhan | PATCH | `/api/workspace/tasks/{id}/assign-individual` | `{ employeeId }` |
| Giao nhom | PATCH | `/api/workspace/tasks/{id}/assign-team` | `{ teamLeaderId, teamMemberIds }` |
| Nhan task | PATCH | `/api/workspace/tasks/{id}/accept` | none |
| Gui hoan thanh | PATCH | `/api/workspace/tasks/{id}/submit-completion` | `{ content, attachment? }` |
| Duyet hoan thanh | PATCH | `/api/workspace/tasks/{id}/approve-completion` | none |
| Tra lai | PATCH | `/api/workspace/tasks/{id}/return` | `{ reason, attachment? }` |
| Danh sach attachment | GET | `/api/workspace/tasks/{id}/attachments` | none |
| Them attachment | POST | `/api/workspace/tasks/{id}/attachments` | `{ fileName, fileUrl, contentType?, fileSize?, attachmentType? }` |
| Cap nhat tien do/blocker (legacy alias con can cho UI) | PATCH | `/api/v1/tasks/{id}/progress` | `{ progressPercent, content, updateType: 'PROGRESS' \| 'BLOCKER', attachment }` |
| Lich su cap nhat (legacy alias) | GET | `/api/v1/tasks/{id}/updates` | none |
| Huy task (legacy alias) | PATCH | `/api/v1/tasks/{id}/cancel` | none |

Workspace UI moi uu tien `/api/workspace/tasks`. Khong dung generic `/api/v1/tasks/{id}/status` de gan `ACCEPTED`, `SUBMITTED`, `RETURNED` hoac `COMPLETED`; cac status nay chi di qua endpoint workflow chuyen dung.

Quyen:

- BUSINESS_OWNER/EXECUTIVE/MANAGER xem tat ca task trong workspace, tao/sua/giao lai/huy task theo service rule.
- EMPLOYEE chi xem task duoc giao.
- BUSINESS_OWNER/EXECUTIVE/MANAGER hoac assignee/participant co the cap nhat tien do theo backend rule.
- Sua thong tin khach hang:
  - Task ca nhan: BUSINESS_OWNER/EXECUTIVE/MANAGER hoac nhan vien duoc giao duoc sua.
  - Task nhom: BUSINESS_OWNER/EXECUTIVE/MANAGER hoac team leader duoc sua.
  - Team member thuong chi xem, khong hien nut sua.

Luat UI:

- Nut gui hoan thanh goi `submit-completion`; khong gui `updateType=COMPLETION` qua generic progress form.
- Khi `updateType = BLOCKER`, bat buoc nhap `content`.
- Progress slider tu `0` den `100`.
- Nen khoa sua task `CANCELLED` hoac `COMPLETED` neu chua co quyet dinh san pham ro rang.

### Analytics

Analytics/workload danh cho `BUSINESS_OWNER`, `EXECUTIVE`, `MANAGER`, va `HR` theo backend policy; an action neu service tra business-rule error.

| Man hinh | Method | Path | Data |
|---|---|---|---|
| Dashboard owner production | GET | `/api/workspace/business-owner/dashboard` | `{ overviewCards, dailyReportInsight, workloadInsight, deadlineRisks, blockedTasks, taskStatusChart, workloadDistributionChart, recentlyUpdatedTasks, metadata }` |
| Dashboard owner legacy | GET | `/api/v1/analytics/owner-dashboard` | compatibility only |
| Workload toan bo | GET | `/api/v1/analytics/workload` | `Workload[]` |
| Workload nhan vien | GET | `/api/v1/analytics/employees/{id}/workload` | `Workload` |

### AI

Phan lon endpoint AI danh cho BUSINESS_OWNER/EXECUTIVE/MANAGER/HR theo route policy. Cac endpoint phan tich/giao viec (`tasks/analyze`, `recommend-assignee`, `recommend-team-leaders`, `recommend-team-members`) danh cho workflow tao/giao task cua BUSINESS_OWNER/EXECUTIVE/MANAGER theo service rule.

| Chuc nang | Method | Path | Body hoac query |
|---|---|---|---|
| Phan tich domain task | POST | `/api/v1/ai/tasks/analyze` | `{ taskTitle, taskDescription, projectDescription, departmentName, startDate, deadline }` |
| Goi y nguoi nhan | POST | `/api/v1/ai/recommend-assignee` | `{ title, requirements, startDate, deadline, estimatedHours, priority, assignmentType, teamSize, departmentId, requiredJobPositionId, requiredEmployeeLevel, requiredSeniorityLevel, requiredSkills, taskDomain }` |
| Goi y team lead | POST | `/api/v1/ai/recommend-team-leaders` | `{ title, requirements, startDate, deadline, estimatedHours, priority, assignmentType: "TEAM", teamSize, departmentId, requiredJobPositionId, requiredEmployeeLevel, requiredSeniorityLevel, requiredSkills, taskDomain }` |
| Goi y thanh vien nhom | POST | `/api/v1/ai/recommend-team-members` | `{ title, requirements, startDate, deadline, estimatedHours, priority, assignmentType: "TEAM", teamSize, departmentId, requiredJobPositionId, requiredEmployeeLevel, requiredSeniorityLevel, requiredSkills, taskDomain }` |
| Tom tat workload | GET | `/api/v1/ai/workload-summary` | none |
| Rui ro tre han | GET | `/api/v1/ai/delay-risks` | none |
| Phan tich daily reports | GET | `/api/v1/ai/daily-reports/insights` | none |
| Nhan vien thieu report | GET | `/api/v1/ai/daily-reports/missing` | none |
| Tao task tu mo ta/bien ban | POST | `/api/v1/ai/tasks/extract` | `{ text, defaultDeadline }` |
| De xuat chia nho task | POST | `/api/v1/ai/tasks/{id}/split` | none |
| De xuat deadline/priority | POST | `/api/v1/ai/tasks/{id}/adjust` | none |
| Danh sach AI suggestion | GET | `/api/v1/ai/suggestions` | none |
| Doi trang thai suggestion | PATCH | `/api/v1/ai/suggestions/{id}/status?status=ACCEPTED` | query `status` |
| Tom tat ngay | GET | `/api/v1/ai/business-summary/daily` | none |
| Tom tat tuan | GET | `/api/v1/ai/business-summary/weekly` | none |
| Tom tat thang | GET | `/api/v1/ai/business-summary/monthly` | none |

`outputData` va `inputData` cua AI suggestion la string JSON. Front-end nen parse an toan bang try/catch.

AI team recommendation note:

- `recommend-team-leaders` tra `AssigneeRecommendation[]` voi `requiredRole = TEAM_LEADER`. Backend score dua tren department/business position, employee level, seniority, leadershipScore, lich su lam leader, lead completion rate, domain match, similar task count va projected workload.
- `recommend-team-members` tra `AssigneeRecommendation[]` voi `requiredRole = TEAM_MEMBER`. Backend score dua tren department/business position, employee level, seniority, skill/domain match, similar task count, performance metrics va projected workload.
- FE hien reason/risk de manager/owner hieu tai sao AI de xuat, nhung khong auto assign. User phai bam chon lead/member.
- FE khong sort lai danh sach recommendation; thu tu BE la final. `eligibilityStatus = WARNING` phai hien chip canh bao kem `eligibilityReasons`; BE da loai `NOT_ELIGIBLE` nen neu gap gia tri nay trong du lieu cu thi disable nut chon.

Operational action suggestions have been removed from Owner dashboard and AI Center. FE must not call `/api/v1/ai/action-suggestions` and must ignore old `ACTION_SUGGESTION` cache rows if any legacy response contains them.

### Daily reports

| Chuc nang | Method | Path | Body |
|---|---|---|---|
| Danh sach report | GET | `/api/v1/daily-reports` | none |
| Tao report | POST | `/api/v1/daily-reports` | `{ reportDate, todayCompleted, currentWork, blockers, tomorrowPlan }` |
| Chi tiet report | GET | `/api/v1/daily-reports/{id}` | none |
| Business owner danh dau da review | PATCH | `/api/v1/daily-reports/{id}/review` | none |

Quyen:

- EMPLOYEE xem/tao report cua chinh minh.
- BUSINESS_OWNER xem tat ca report va review.
- Backend chan tao trung report theo ngay cho cung user.

### Notifications

| Chuc nang | Method | Path |
|---|---|---|
| Danh sach thong bao | GET | `/api/v1/notifications` |
| Danh dau mot thong bao da doc | PATCH | `/api/v1/notifications/{id}/read` |
| Danh dau tat ca da doc | PATCH | `/api/v1/notifications/read-all` |

Backend tu sinh thong bao van hanh khi goi danh sach notifications: task qua han, deadline sap den, thieu daily report.

## 6. Route va man hinh de xuat

Tat ca route duoi day la **Frontend can trien khai**. Cot guard dung permission runtime; role chi duoc bo sung khi service co hard role check.

| Route | Page | Guard | API chinh | Ghi chu |
|---|---|---|---|---|
| `/admin/dashboard` | Platform dashboard | `REVENUE_VIEW` | `/api/admin/dashboard/**` | Platform Admin |
| `/admin/workspace-registrations` | Registration list | `WORKSPACE_MANAGE` | `GET /api/admin/workspace-registrations` | Platform Admin |
| `/admin/workspace-registrations/:id` | Registration/payment detail | `WORKSPACE_MANAGE`; action confirm can `PAYMENT_CONFIRM` | `/api/admin/workspace-registrations/{id}`, `/api/admin/payments/**` | Confirm payment tu activation |
| `/admin/workspaces/:id` | Workspace/owner detail | `WORKSPACE_MANAGE` | `GET /api/admin/workspaces/{id}` | Owner account actions |
| `/workspace/hr-accounts` | HR account list | `HR_ACCOUNT_MANAGE` + BO service rule | `GET /api/workspace/business-owner/hr-accounts` | Khong hien cho HR |
| `/workspace/hr-accounts/new` | Create HR | `HR_ACCOUNT_MANAGE` + BO service rule | `POST /api/workspace/business-owner/hr-accounts` | Khong co role/workspace field |
| `/workspace/employees` | Employee list | `EMPLOYEE_VIEW` | `GET /api/workspace/hr/employees` | BO read-only, HR co action theo permission |
| `/workspace/employees/new` | Create employee | `EMPLOYEE_CREATE` + HR | `POST /api/workspace/hr/employees` | HR only |
| `/workspace/employees/import` | Employee import | `EMPLOYEE_IMPORT` + HR | `/api/workspace/hr/employees/import`, `/api/workspace/hr/employees/imports/**` | HR only |
| `/workspace/departments` | Departments | `DEPARTMENT_VIEW` | `/api/workspace/hr/departments` | Mutations need `DEPARTMENT_MANAGE` + HR |
| `/workspace/business-positions` | Positions | `POSITION_VIEW` | `/api/workspace/hr/business-positions` | Mutations need `POSITION_MANAGE` + HR |

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
- `/workspace/hr-accounts`: xem/tao/khoa/mo HR.
- `/workspace/employees`: danh sach nhan vien read-only.
- `/workspace/employees/:id`: chi tiet nhan vien read-only neu co `EMPLOYEE_VIEW`.
- `/owner/tasks/new`: tao task.
- `/owner/workspace`: cau hinh workspace.
- `/owner/analytics/workload`: bang workload.
- `/owner/ai`: goi y AI, delay risks, summaries.

### HR routes

- `/workspace/employees`: CRUD ho so nhan su.
- `/workspace/employees/import`: import Excel.
- `/workspace/departments`: department master data.
- `/workspace/business-positions`: business position master data.

### MANAGER/EXECUTIVE routes

- `/manager/tasks`: task/workload workspace.
- `/manager/ai`: task analysis and recommendations.

### EMPLOYEE routes

- `/employee/home`: task cua toi + report hom nay + thong bao.
- `/employee/tasks`: task duoc giao.
- `/employee/reports`: report cua toi.

Sau login, redirect theo role:

- `PLATFORM_ADMIN` hoac legacy `SYSTEM_ADMIN`/`SYSTEM` -> `/admin/dashboard`
- `BUSINESS_OWNER` hoac legacy `OWNER` -> `/owner/dashboard`
- `HR` -> `/workspace/employees`
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
- Employees / Departments / Business Positions (read-only)
- Subscription / Payment (read-only; chi khi co `SUBSCRIPTION_VIEW`/payment view permission)
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

Khong hien HR Accounts, Payment, Subscription, Platform Dashboard hoac Audit cho HR.

Navigation EMPLOYEE:

- Viec cua toi
- Bao cao ngay
- Thong bao
- Ho so

Guard implementation:

```ts
const can = (permission: Permission) => auth.user?.permissions.includes(permission) === true;
```

- Sidebar, route va button deu dung `can(...)`/`hasAnyPermission(...)`.
- Direct URL khong co permission -> `/403`; chua login -> `/login`; account/workspace bi backend tu choi -> clear session khi 401, con 403 thi giu session.
- An menu khong thay the backend security.

## 8. CRUD chi tiet theo man hinh

### Login

Fields:

- Email hoac username: required.
- Password: required.

Buttons:

- `Dang nhap`: submit `POST /api/v1/auth/login`.
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
- Workspace code: optional; neu nhap phai dung 2 ky tu chu/so.
- Email doanh nghiep: required, email.
- So dien thoai doanh nghiep: optional.
- Dia chi doanh nghiep: optional.
- Ho ten nguoi dai dien: required.
- Email nguoi dai dien: required, email.
- So dien thoai nguoi dai dien: optional.
- Ho ten Business Owner: optional, mac dinh bang nguoi dai dien.
- Email Business Owner: optional, mac dinh bang email nguoi dai dien.
- So dien thoai Business Owner: optional, mac dinh bang nguoi dai dien.
- Mat khau Business Owner: required, 8-72 ky tu; khong log va khong dua vao URL.

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

### Platform Admin registration va activation

#### Registration list

- API: `GET /api/admin/workspace-registrations`; backend hien khong co query search/filter/pagination.
- FE co the search/filter/paginate client-side theo business/workspace/email/paymentStatus/registrationStatus, nhung khong gui query gia.
- Columns: business name, workspace name/code, representative, owner email, plan, payment status, registration status, created/updated time.
- Loading: skeleton; empty: “Chua co registration”; error: retry va hien `requestId` neu co.

#### Registration detail

- API detail: `GET /api/admin/workspace-registrations/{id}`.
- Payment list/detail: `GET /api/admin/payments`, `GET /api/admin/payments/{paymentId}`; FE match payment bang `workspaceRegistrationId` vi registration response khong co payment list long san.
- Sections: business, representative, requested owner, subscription plan, payment, review, activation result.
- Confirm: `PATCH /api/admin/payments/{paymentId}/confirm` body optional `{ "note": "Da doi soat" }`.
- Reject: `PATCH /api/admin/payments/{paymentId}/reject` body optional `{ "note": "Sai noi dung chuyen khoan" }`.
- Approve registration: `PATCH /api/admin/workspace-registrations/{id}/approve`, chi enable khi payment da `CONFIRMED`/transaction `SUCCESS`.
- Disable action trong luc submit, khong double click. Backend idempotent theo payment/registration lock; header `Idempotency-Key` duoc CORS cho phep nhung service hien chua doc header nay, nen khong mo ta la API idempotency-key.
- Sau thanh cong invalidate/refetch `workspaceRegistrations`, `adminPayments`, `workspaces`, workspace detail va admin dashboard.
- Neu registration da co `workspaceId`, coi nhu da activation; khong hien nut tao owner lan nua.

### HR account management

#### List page

- Route: `/workspace/hr-accounts`.
- API: `GET /api/workspace/business-owner/hr-accounts`.
- Server query/pagination/search/status filter: **Chua duoc backend ho tro**; filter/pagination client-side neu danh sach lon.
- Columns: `fullName`, `username`, `email`, `phone`, `status`, `createdAt`.
- Khong co `lastLoginAt`; khong render placeholder nhu du lieu that.
- Actions: create, set `ACTIVE`, set `INACTIVE`. Khong co reset password HR.
- Loading skeleton, empty CTA create HR, retry state va confirmation truoc khi lock/unlock.

#### Create HR form

| Field | Label | Required | Type/validation | Trim |
|---|---|:---:|---|:---:|
| `fullName` | Ho va ten | Co | string, khong rong (`@NotBlank`); backend chua co max length DTO | Co |
| `email` | Email | Co | string, `@Email` + `@NotBlank`; unique toan he thong khong phan biet hoa/thuong | Co, lowercase de UX nhat quan |
| `phone` | So dien thoai | Khong | string; backend chua co regex, unique trong workspace neu co | Co |

Khong them `role`, `permissions`, `workspaceId`, `status`, `username`, `temporaryPassword`, `passwordHash`, `platformRole`.

#### Create HR response va modal mot lan

```json
{
  "data": {
    "id": "b1d4b1bb-760b-4a9d-a85a-1e0cc26f73f0",
    "username": "hr.forep.nguyenvanan",
    "fullName": "Nguyen Van An",
    "email": "an@example.com",
    "role": "HR",
    "status": "ACTIVE",
    "workspaceId": "326738d5-9675-4d73-a4a8-2d56009fdb8f",
    "temporaryPassword": "generated-on-server",
    "mustChangePassword": true,
    "credentialsVisibleOnce": true
  },
  "meta": {
    "requestId": "...",
    "timestamp": "2026-08-04T21:00:00+07:00"
  },
  "errors": []
}
```

Modal hien ID, full name, username, email, role, status, temporary password va must-change flag. Copy password chi thao tac trong component state; khi dong modal xoa password khoi state va refetch `hrAccounts`.

### Owner dashboard

APIs:

- `GET /api/workspace/business-owner/dashboard`
- `GET /api/v1/notifications`

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

List API: `GET /api/workspace/hr/employees`.

Business Owner co `EMPLOYEE_VIEW` chi xem. Tat ca nut create/edit/status/reset/import can permission tuong ung va service chi chap nhan role HR.

Table columns:

- Ho ten
- Email
- Phone
- Username
- Ma nhan vien
- Status
- CreatedAt
- Actions

Buttons/actions:

- `Them nhan vien`: HR + `EMPLOYEE_CREATE`.
- `Sua`: HR + `EMPLOYEE_UPDATE`.
- `Kich hoat`/`Tam ngung`: `PATCH /api/workspace/hr/employees/{id}/status` voi JSON body, HR + `EMPLOYEE_DEACTIVATE`.
- `Reset mat khau`: legacy `PATCH /api/v1/employees/{id}/reset-password`, HR + `EMPLOYEE_UPDATE`.
- Business Owner chi co `Xem chi tiet`/workload neu API va permission cho phep.

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

Sau create/reset thanh cong, hien `username`, `employeeCode`, `temporaryPassword`, `credentialsVisibleOnce` trong modal mot lan. Sau create/update/status, refetch `GET /api/workspace/hr/employees`.

### Task list

API: `GET /api/workspace/tasks`

Filters client-side:

- Search title/requirements.
- Status.
- Priority.
- Assignee, chi BUSINESS_OWNER/EXECUTIVE/MANAGER.
- Overdue: `deadline < now` va status khong thuoc `COMPLETED`, `CANCELLED`.

Fields:

- Title.
- Assignee name, map tu `GET /api/workspace/hr/employees` khi co `EMPLOYEE_VIEW`; manager/executive co the map tu task participants neu employee list bi service chan.
- Priority badge.
- Status badge.
- Progress bar.
- Deadline.
- Estimated hours.

Buttons:

- BUSINESS_OWNER/EXECUTIVE/MANAGER: `Tao task`, `Sua`, `Giao lai`, `Huy`.
- Assignee/participant hop le: `Nhan task`, `Cap nhat tien do`, `Bao blocker`, `Gui hoan thanh` theo status.
- BUSINESS_OWNER/EXECUTIVE/MANAGER co `TASK_APPROVE`: `Duyet hoan thanh` hoac `Tra lai` khi status `SUBMITTED`.
- Shared: `Xem chi tiet`.

### Task create/edit

Create API: `POST /api/workspace/tasks`

Edit API: `PUT /api/workspace/tasks/{id}`

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
- Required skills, required business position, required employee level, required seniority, task domain, department: optional nhung nen co de AI recommend chinh xac. Neu thieu, backend se goi AI task/domain analysis va map ve active department/business-position ID that.
- Khi bam goi y AI, FE gui ca `startDate`, `deadline`, `estimatedHours`, `assignmentType`, `teamSize`, `priority`, `departmentId`, `requiredJobPositionId`, `requiredEmployeeLevel`, `requiredSeniorityLevel`. Backend se tinh projected monthly workload = current task hours + hours cua task moi chia theo working days/so nguoi.

Buttons:

- `Luu task`: create/update.
- `Phan tich task`: goi `POST /api/v1/ai/tasks/analyze` de prefill department, required business position, required skills, task domain.
- `Goi y nguoi nhan`: goi `POST /api/v1/ai/recommend-assignee`, chi BUSINESS_OWNER/EXECUTIVE/MANAGER.
- `Goi y team lead`: goi `POST /api/v1/ai/recommend-team-leaders`, chi BUSINESS_OWNER/EXECUTIVE/MANAGER neu form dang la TEAM.
- `Goi y thanh vien`: goi `POST /api/v1/ai/recommend-team-members`, chi BUSINESS_OWNER/EXECUTIVE/MANAGER neu form dang la TEAM.
- `Huy`: quay lai list.

AI recommendation panel:

- Hien thi score, workloadLevel, reason, risk, role fit, projected monthly hours/capacity va projectedWorkloadLevel.
- Hien chip `ELIGIBLE/WARNING`; neu `WARNING`, render tung `eligibilityReasons` trong accordion ngan. Khong cho chon candidate `NOT_ELIGIBLE` neu co data legacy.
- Hien breakdown: departmentSuitabilityScore, businessPositionSuitabilityScore, employeeLevelFitScore, seniorityFitScore, skillMatchScore, workloadAvailabilityScore, performanceScore.
- Hien performanceMetrics o dang so lieu ngan: totalAssignedTasks, completedTasks, completionRate, onTimeCompletionRate, riskRate, averageActiveProgress.
- Neu response co `requiredRole`, `roleFit`, `roleFitReason`, hien thi de owner thay AI da doi chieu vai tro chuyen mon voi task.
- Nut `Chon nguoi nay` set `assigneeId` neu task ca nhan, set `teamLeaderId` neu `requiredRole = TEAM_LEADER`, hoac them vao `teamMemberIds` neu `requiredRole = TEAM_MEMBER`.
- Khong auto-submit task khi chon goi y.

### Task detail

APIs:

- `GET /api/workspace/tasks/{id}`
- `PATCH /api/workspace/tasks/{id}/customer-info`
- `PATCH /api/workspace/tasks/{id}/accept`
- `PATCH /api/workspace/tasks/{id}/submit-completion`
- `PATCH /api/workspace/tasks/{id}/approve-completion`
- `PATCH /api/workspace/tasks/{id}/return`
- `GET /api/workspace/tasks/{id}/attachments`
- `GET /api/v1/tasks/{id}/updates` (legacy alias vi workspace controller chua co endpoint updates)

Sections:

- Thong tin task.
- Thong tin khach hang: customerPhone, customerEmail, customerDescription.
- Progress/status.
- Timeline updates.
- Panel action.

Buttons:

- `Cap nhat tien do`: mo form update.
- `Bao blocker`: preset `updateType=BLOCKER`.
- `Nhan task`: goi `/accept` khi workflow cho phep.
- `Gui hoan thanh`: goi `/submit-completion` voi `content` bat buoc.
- `Duyet hoan thanh`: chi nguoi co `TASK_APPROVE`, goi `/approve-completion` khi task `SUBMITTED`.
- `Tra lai`: chi nguoi co `TASK_APPROVE`, goi `/return` voi `reason` bat buoc.
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
- Submit `PATCH /api/v1/tasks/{id}/customer-info`, sau do refetch task detail.

Progress update form:

- updateType select.
- progressPercent slider/number 0-100.
- content textarea required.
- attachment URL optional.

Submit:

- `PATCH /api/v1/tasks/{id}/progress`.
- Sau submit, refetch task + updates.

### Daily reports

List API: `GET /api/v1/daily-reports`

Create API: `POST /api/v1/daily-reports`

Review API: `PATCH /api/v1/daily-reports/{id}/review`

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

- `GET /api/v1/notifications`
- `PATCH /api/v1/notifications/{id}/read`
- `PATCH /api/v1/notifications/read-all`

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

- `GET /api/v1/ai/workload-summary`
- `GET /api/v1/ai/delay-risks`
- `GET /api/v1/ai/daily-reports/insights`
- `GET /api/v1/ai/daily-reports/missing`
- `POST /api/v1/ai/tasks/extract`
- `POST /api/v1/ai/tasks/{id}/split`
- `POST /api/v1/ai/tasks/{id}/adjust`
- `GET /api/v1/ai/business-summary/daily`
- `GET /api/v1/ai/business-summary/weekly`
- `GET /api/v1/ai/business-summary/monthly`
- `GET /api/v1/ai/suggestions`

UI sections:

- Workload summary.
- Delay risks list.
- Daily/weekly/monthly business summary.
- Daily report insights: summary, blockers `{ severity, description }`.
- Missing report list with `employeeId`, `employeeName`, `reportDate`, `daysMissing`, `recommendedAction`, `confidence`.
- Task extraction form from text/minutes.
- Task split and deadline/priority recommendation for selected task.
- AI suggestion history.

Fallback note: `recommend-assignee` can return top-3 rule-based recommendations when LLM/provider times out; the list shape is unchanged and each item marks fallback in `reason`/`risk`. `workload-summary`, `delay-risks`, `daily-reports/insights`, and `daily-reports/missing` can also return rule-based data when LLM/provider fails. These card responses keep their normal keys and include `source: "RULE_BASED_FALLBACK"`, `aiProviderFailed: true`, and `fallbackReason`; show this as fallback/source metadata, not as confirmed LLM output. Operational AI action suggestions are removed and FE must not call `/api/v1/ai/action-suggestions`.

Overload note: AI endpoints without fallback can return HTTP 429 with `AI_RATE_LIMITED` when too many AI calls are running. UI should show a retry-later message, keep the button disabled briefly, and avoid immediate auto-retry loops.

Buttons:

- `Tai lai`: refetch tung section.
- `Chap nhan`: `PATCH /api/v1/ai/suggestions/{id}/status?status=ACCEPTED`.
- `Tu choi`: `PATCH /api/v1/ai/suggestions/{id}/status?status=REJECTED`.

## 9. Button matrix

| Button | Man hinh | Role | API | Disabled khi |
|---|---|---|---|---|
| Dang nhap | Login | Public | `POST /api/v1/auth/login` | form invalid/loading |
| Gui thong tin dang ky | Registration information | Public | `POST /api/public/workspace-registrations` | form invalid/loading |
| Chon goi dang ky | Plan selection | Public | `PATCH /api/public/workspace-registrations/{id}/select-plan?token={registrationToken}` | no plan selected/loading |
| Tiep tuc thanh toan | Payment method | Public | `POST /api/public/workspace-registrations/{id}/payments?token={registrationToken}` | no payment method selected/loading |
| Mo trang MoMo | Payment instruction | Public | external `providerPaymentUrl` | no providerPaymentUrl |
| Thu lai thanh toan | Payment result | Public | `POST /api/public/workspace-registrations/{id}/payments?token={registrationToken}` | loading |
| Dang xuat | User menu | Authenticated | `POST /api/v1/auth/logout` | loading |
| Doi mat khau | Profile/User menu | Authenticated | `PATCH /api/v1/auth/change-password` | form invalid/loading |
| Xac nhan payment | Registration detail | `PAYMENT_CONFIRM` | `PATCH /api/admin/payments/{paymentId}/confirm` | invalid status/loading |
| Tu choi payment | Registration detail | `PAYMENT_CONFIRM` | `PATCH /api/admin/payments/{paymentId}/reject` | final status/loading |
| Tao Business Owner | Workspace detail | `WORKSPACE_MANAGE` | `POST /api/admin/workspaces/{id}/business-owners` | form invalid/limit/loading |
| Tao HR | HR Accounts | `HR_ACCOUNT_MANAGE` + BO | `POST /api/workspace/business-owner/hr-accounts` | form invalid/loading |
| Khoa/mo HR | HR Accounts | `HR_ACCOUNT_MANAGE` + BO | `PATCH /api/workspace/business-owner/hr-accounts/{id}/status` | same status/loading |
| Them nhan vien | Employees | HR | none, mo modal | none |
| Luu nhan vien | Employee modal | HR | `POST /api/workspace/hr/employees` hoac `PUT /api/workspace/hr/employees/{id}` | form invalid/loading |
| Kich hoat | Employees | HR | `PATCH /api/workspace/hr/employees/{id}/status` body `{ "status": "ACTIVE" }` | user already ACTIVE/loading |
| Tam ngung | Employees | HR | `PATCH /api/workspace/hr/employees/{id}/status` body `{ "status": "INACTIVE" }` | user already INACTIVE/loading |
| Reset mat khau nhan vien | Employees | HR | `PATCH /api/v1/employees/{id}/reset-password` | loading |
| Tao task | Tasks/Dashboard | BUSINESS_OWNER/EXECUTIVE/MANAGER | none, route create | no active employee |
| Luu task | Task form | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /api/workspace/tasks` hoac `PUT /api/workspace/tasks/{id}` | form invalid/loading |
| Phan tich task | Task form | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /api/v1/ai/tasks/analyze` | thieu title/description/loading |
| Goi y nguoi nhan | Task form | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /api/v1/ai/recommend-assignee` | thieu title/requirements/deadline/loading |
| Goi y team lead | Task form | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /api/v1/ai/recommend-team-leaders` | assignmentType khac TEAM hoac thieu title/requirements/deadline/loading |
| Goi y thanh vien nhom | Task form | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /api/v1/ai/recommend-team-members` | assignmentType khac TEAM hoac thieu title/requirements/deadline/loading |
| Chon nguoi nay | AI recommendation | BUSINESS_OWNER/EXECUTIVE/MANAGER | none, set assignee | employee inactive neu co data |
| Tao task bang AI | AI center | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /api/v1/ai/tasks/extract` | text empty/loading |
| Chia nho task | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /api/v1/ai/tasks/{id}/split` | loading |
| De xuat deadline/priority | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER | `POST /api/v1/ai/tasks/{id}/adjust` | loading |
| Giao ca nhan/nhom | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER | `PATCH /api/workspace/tasks/{id}/assign-individual` hoac `/assign-team` | no assignee/loading |
| Sua thong tin khach hang | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER/assignee/leader | `PATCH /api/workspace/tasks/{id}/customer-info` | no permission/loading |
| Huy task | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER | `PATCH /api/v1/tasks/{id}/cancel` | status CANCELLED/COMPLETED/loading |
| Cap nhat tien do | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER/assignee | `PATCH /api/v1/tasks/{id}/progress` | content empty/loading |
| Bao blocker | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER/assignee | `PATCH /api/v1/tasks/{id}/progress` | content empty/loading |
| Nhan task | Task detail | assignee/participant hop le | `PATCH /api/workspace/tasks/{id}/accept` | status khong hop le/loading |
| Gui hoan thanh | Task detail | assignee hoac team leader | `PATCH /api/workspace/tasks/{id}/submit-completion` | content rong/status khong hop le/loading |
| Duyet hoan thanh | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER + `TASK_APPROVE` | `PATCH /api/workspace/tasks/{id}/approve-completion` | status khac SUBMITTED/loading |
| Tra lai | Task detail | BUSINESS_OWNER/EXECUTIVE/MANAGER + `TASK_APPROVE` | `PATCH /api/workspace/tasks/{id}/return` | reason rong/status khac SUBMITTED/loading |
| Gui bao cao | Daily report form | BUSINESS_OWNER/EMPLOYEE | `POST /api/v1/daily-reports` | form invalid/loading |
| Da review | Daily report detail | BUSINESS_OWNER | `PATCH /api/v1/daily-reports/{id}/review` | already reviewed/loading |
| Danh dau da doc | Notifications | BUSINESS_OWNER/EMPLOYEE | `PATCH /api/v1/notifications/{id}/read` | already read/loading |
| Danh dau tat ca da doc | Notifications | BUSINESS_OWNER/EMPLOYEE | `PATCH /api/v1/notifications/read-all` | no unread/loading |
| Chap nhan suggestion | AI center | BUSINESS_OWNER/EXECUTIVE/MANAGER/HR | `PATCH /api/v1/ai/suggestions/{id}/status?status=ACCEPTED` | status ACCEPTED/loading |
| Tu choi suggestion | AI center | BUSINESS_OWNER/EXECUTIVE/MANAGER/HR | `PATCH /api/v1/ai/suggestions/{id}/status?status=REJECTED` | status REJECTED/loading |

## 10. Form validation

- Email: phai dung format email.
- Required text: trim roi kiem tra rong.
- deadline: phai la ngay gio hop le. UI nen canh bao neu deadline nam trong qua khu.
- estimatedHours: so >= 0, cho phep decimal.
- progressPercent: integer 0-100.
- reportDate: required `YYYY-MM-DD`.
- Khong gui field `undefined`; optional co the gui `null` hoac bo field, nen thong nhat bo field khi khong nhap.

Frontend validation chi de cai thien UX. Backend `@Valid`, unique constraint, permission, hard role va workspace check van la lop quyet dinh. Neu response co field error thi gan vao field tuong ung; neu Spring validation response khong theo `ApiResponse`, hien fallback message va giu form data.

## 10A. HTTP va error handling

| Tinh huong | Hanh vi FE | Retry/giu form |
|---|---|---|
| HTTP `200` + `errors` khac rong | Xem la that bai; hien `errors[0].message` va `meta.requestId` | Giu form; chi retry khi user sua/chu dong |
| `400 Bad Request` | Validation/body sai; map field neu co, fallback “Du lieu chua hop le” | Giu form, khong auto retry |
| `401 Unauthorized` | Clear token/auth store, redirect `/login`, thong bao het phien | Khong retry cho toi khi login lai |
| `403 Forbidden` | Giu session, route `/403` hoac toast khong co quyen | Khong retry |
| `404 Not Found` | Hien khong tim thay resource/page | Khong retry tu dong |
| `409 Conflict` | **Backend hien chua dung co he thong**; neu gap thi hien conflict va refetch | Giu form |
| `422 Unprocessable Entity` | **Backend hien chua dung**; xu ly nhu validation neu tuong lai co | Giu form |
| `429 Too Many Requests` | AI `AI_RATE_LIMITED`: hien retry-later, cooldown button | Retry thu cong sau delay |
| `500 Internal Server Error` | Hien loi he thong + request ID | Cho retry thu cong, giu form |
| `502 Bad Gateway` | AI `AI_PROVIDER_ERROR`: hien AI tam unavailable | Retry thu cong, khong mat form |
| Network error/timeout | Hien mat ket noi/qua thoi gian | Giu form; retry thu cong, khong tu submit mutation lan hai |

Controller hien map `IllegalArgumentException` thanh `BUSINESS_RULE_ERROR` nhung khong gan non-2xx status, do do API client bat buoc validate ca HTTP status va `errors.length`. Security `401/403` va Spring validation co the khong theo envelope; client can parser fallback.

## 11. Loading, empty, error states

Moi man hinh list can co:

- Loading skeleton hoac spinner.
- Empty state co action chinh, vi du task rong thi OWNER thay `Tao task`.
- Error state co nut `Thu lai`.
- Toast thanh cong cho create/update.
- Confirm dialog cho hanh dong huy task, tam ngung nhan vien, logout.

Mutation `confirm payment`, `reject payment`, `approve registration`, `create HR`, `change HR status`, `create/reset owner` va `reset employee password` phai co mutex/loading per action, disable button va khong double submit. CORS cho phep `Idempotency-Key` nhung code hien tai khong consume header; chi dung khi backend bo sung contract ro rang. Idempotency activation van do transaction/lock/unique constraint phia backend dam bao.

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
- `workspaceRegistrations`, `adminPayments`, `workspaceDetail(id)`: invalidate sau confirm/reject/approve/owner mutation.
- `hrAccounts`: invalidate sau create/status.
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
- Khong luu/log `temporaryPassword`, password hash hoac owner registration password.
- Khong goi AI service truc tiep vi AI token la secret noi bo backend.
- Khong hien thi route OWNER cho EMPLOYEE.
- Voi route bi 403, hien thi trang `Khong co quyen` va nut ve trang phu hop role.

Workspace isolation:

- Create HR/Employee khong co workspace selector va khong gui `workspaceId`.
- Workspace user context lay tu JWT/principal; query string/localStorage chi la UI state, khong phai authority.
- Khong cho Business Owner doi workspace de goi HR API. ID row HR van duoc backend kiem tra cung workspace.
- Platform Admin endpoint co workspace ID trong path la mot boundary khac va can `WORKSPACE_MANAGE`.
- Moi API workspace-scoped phai xu ly 403/business error khi backend tu choi cross-workspace; FE khong loc client-side roi coi la bao mat.

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

Admin activation:

- Admin thay dung registration/payment; confirm bi disable khi status khong hop le.
- Confirm payment thanh cong refetch ra `workspaceId`, active subscription va mot Business Owner dau tien.
- Click/gui lai khong tao owner thu hai.
- User thieu `PAYMENT_CONFIRM` khong thay nut; goi API truc tiep bi backend tu choi.
- Form create owner chi co `fullName`, `email`, `phone`; modal credential chi hien khi response co `credentialsVisibleOnce=true`.

Business Owner quan ly HR:

- Chi user co `HR_ACCOUNT_MANAGE` va role BO thay menu/route.
- HR/Employee khong thay menu; direct URL vao `/workspace/hr-accounts` bi guard chan.
- Form khong co role, permission, workspaceId, username, status hoac password.
- Create thanh cong hien username backend sinh va temporary password dung mot lan.
- Email trung/phone trung/limit workspace hien message backend de hieu va giu form.
- Khong thao tac duoc HR workspace khac; lock/unlock cap nhat list dung status.
- Khong hien reset password hoac last-login column vi backend chua support.

Permission separation:

- Business Owner khong thay create/edit/deactivate/import employee hay mutate department/position.
- HR khong thay HR Accounts, payment, subscription hoac Platform Admin.
- Employee khong thay HR/employee management.
- Direct API van bi backend security/hard role/workspace check tu choi, ke ca khi UI guard bi bypass.

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
- Platform registration detail dung `/api/admin/payments/{id}/confirm`; activation xay ra backend-side, khong co canonical activation request rieng.
- Business Owner HR screens dung `/api/workspace/business-owner/hr-accounts`; form chi co `fullName`, `email`, `phone`.
- Account result dung `temporaryPassword`/`credentialsVisibleOnce`, xoa moi tham chieu UI production toi `initialPassword` va mat khau mac dinh.
- Username chi render tu response theo `owner.<workspace>`, `hr.<workspace>.<name>`, `emp.<workspace>.<name>`; FE khong generate/suffix.
- HR screens: Department/Business Position mutation button dung `DEPARTMENT_MANAGE`/`POSITION_MANAGE`; Business Owner khong duoc hien mutate actions cho master data nay.
- Task screens: create `TASK_CREATE`, assign `TASK_ASSIGN`, approve/return/cancel `TASK_APPROVE`, employee self update `TASK_UPDATE_OWN`.
- AI screens: analyze `AI_ANALYZE`, recommendation/explanation `AI_RECOMMENDATION`, owner/platform summary `AI_SUMMARY`, history/suggestions `AI_HISTORY`; HR khong thay AI center neu khong co permission.
- Platform screens: plans `PACKAGE_MANAGE`, registrations/workspaces `WORKSPACE_MANAGE`, payments `PAYMENT_HISTORY_VIEW`, confirm/reject `PAYMENT_CONFIRM`, QR settings `PAYMENT_QR_MANAGE`, revenue `REVENUE_VIEW`, audit `AUDIT_LOG_VIEW`.
- Payment QR UI must use backend-returned `providerQrCodeUrl`; if missing QR business error, show waiting state and do not generate fallback QR client-side.
