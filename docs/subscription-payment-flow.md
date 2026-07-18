# Workspace Registration Subscription Payment Flow

This backend implements a staged workspace registration flow:

1. `POST /api/public/workspace-registrations`
   - Collects business and representative information only.
   - Creates `WorkspaceRegistration` with `PENDING_PLAN_SELECTION`.
   - Does not create or activate a workspace.

2. `GET /api/public/subscription-plans`
   - Returns only active System Administrator managed plans.
   - Response includes price, duration, owner limit, employee limit, features, and status.

3. `PATCH /api/public/workspace-registrations/{id}/select-plan?token={registrationToken}`
   - Body: `{ "subscriptionPlanId": "uuid" }`
   - Validates the selected plan is `ACTIVE`.
   - Updates registration to `PENDING_PAYMENT`.

4. `POST /api/public/workspace-registrations/{id}/payments?token={registrationToken}`
   - Body: `{ "paymentMethod": "MOMO" }` or `{ "paymentMethod": "BANK_TRANSFER" }`
   - Creates a `PENDING` `PaymentTransaction`.
   - If the registration already has a non-expired `PENDING`/`PROCESSING` transaction, the backend returns that transaction instead of creating another payment instruction.
   - Returns a public payment status payload with MoMo payment URL/deeplink/QR information or bank transfer information.
   - If Platform Admin has not enabled and configured a QR for the selected payment method, backend returns a business-rule error and does not create a payment. Public UI must ask the user to wait for admin to update the QR.

5. `GET /api/public/payments/{paymentCode}/status?token={registrationToken}`
   - Polls payment status for the payment instruction/result pages.
   - The registration token is required so public users cannot inspect payment details by payment code alone.

6. Payment confirmation
   - MoMo callback: `POST /api/payment-callbacks/momo`
   - Bank callback: `POST /api/payment-callbacks/bank`
   - MoMo callbacks must include a valid signature when `MOMO_SECRET_KEY` is configured. If `MOMO_SECRET_KEY` is empty, callbacks are rejected unless `MOMO_SANDBOX_MODE=true`.
   - Bank callbacks must include a valid HMAC SHA-256 signature using `BANK_TRANSFER_WEBHOOK_SECRET`; otherwise they are rejected.
   - Manual admin confirmation: `PATCH /api/admin/payments/{paymentId}/confirm`
   - Manual admin rejection: `PATCH /api/admin/payments/{paymentId}/reject`
   - Admin reconciliation: `GET /api/admin/payments`, `GET /api/admin/payments/{paymentId}`, and `GET /api/admin/audit-logs`.

On confirmed success, the backend updates the payment, marks the registration as paid, creates the workspace, applies selected plan limits, creates the allowed Business Owner accounts, and activates the workspace in one transaction. Employee accounts are not created during registration.

The activation transaction also creates a dedicated `workspace_subscriptions` ACTIVE row. This row is the billing/audit snapshot for the selected plan, price, owner/employee limits, start date, end date, renewal date, and payment transaction id. Workspace fields such as `subscriptionPlanId`, `maxOwnerAccounts`, and `maxEmployeeAccounts` stay available as compatibility/current-state fields, but admin UI should prefer `activeSubscription` when rendering the current package.

When a Platform Admin changes the plan of an active workspace, the backend closes the previous ACTIVE subscription as `UPGRADED` or `DOWNGRADED` and opens a new ACTIVE subscription snapshot.

Payment transactions expire after their `expiredAt` timestamp. A scheduled backend job marks stale `PENDING`/`PROCESSING` payments as `EXPIRED`, and public payment polling also refreshes the expired state. Workspace registrations that pass their registration expiry date without approval are marked `EXPIRED`.

## Provider Modes

MoMo uses the real provider API only when all production config values are present and `MOMO_SANDBOX_MODE=false`:

- `MOMO_PAYMENT_ENDPOINT`
- `MOMO_PARTNER_CODE`
- `MOMO_ACCESS_KEY`
- `MOMO_SECRET_KEY`
- `MOMO_RETURN_URL`
- `MOMO_NOTIFY_URL`

QR display is controlled by Platform Admin settings, not by frontend QR generation. Public users scan the QR stored in `payment_qr_settings` at the time a payment is created. If no enabled QR exists for `MOMO` or `BANK_TRANSFER`, payment creation is blocked with a clear waiting message.

If MoMo real provider config is complete, backend may create provider payUrl/deeplink, but the QR shown to the user is still the admin-configured QR. If provider config is incomplete, backend uses admin-configured QR only.

## Platform Admin QR Settings

- `GET /api/admin/payment-qr-settings`
- `PUT /api/admin/payment-qr-settings/{paymentMethod}`
- `paymentMethod`: `MOMO` or `BANK_TRANSFER`
- Body: `{ "qrCodeUrl": "...", "paymentUrl": "...", "deeplink": "...", "bankCode": "...", "bankName": "...", "bankAccountNumber": "...", "bankAccountName": "...", "transferContentPrefix": "...", "enabled": true }`

Rules:

- `qrCodeUrl` is required before enabling a method.
- For bank transfer, `bankAccountNumber` and `bankAccountName` must be present before public payment can be created.
- New payments copy the current QR/settings into `PaymentTransaction`; changing QR later does not mutate old payment instructions.
- Frontend must never generate fake QR codes.

## Demo Data

Migration `V16__demo_saas_operational_seed.sql` seeds production-like QA data:

- 3 active workspaces: `SV`, `MD`, `HC`
- 30 employees per workspace
- Departments and business positions where Developer/BA/Tech Lead/HR Specialist are job positions, not system roles
- Tasks, team/individual assignments, daily reports, workload buckets, payments, active subscriptions, AI history, cached AI suggestions, and feedback
- Demo owner logins: `adminSV0001`, `adminMD0001`, `adminHC0001`; initial password `123456`

## Frontend Pages

### Workspace Registration Page

Route suggestion: `/workspace-registration`

Fields:
- `businessName`
- `workspaceName`
- `contactEmail`
- `contactPhone`
- `businessAddress`
- `representativeFullName`
- `representativeEmail`
- `representativePhone`

After success, navigate to `/workspace-registration/{registrationId}/plans`.

### Subscription Plan Selection Page

Route suggestion: `/workspace-registration/{registrationId}/plans`

Load plans from `GET /api/public/subscription-plans`.

Each plan card should display:
- plan name and description
- monthly price in VND
- maximum Business Owner accounts
- maximum Employee accounts
- full feature availability
- select button

After `PATCH /select-plan` succeeds, navigate to `/workspace-registration/{registrationId}/payment-method`.

### Payment Method Selection Page

Route suggestion: `/workspace-registration/{registrationId}/payment-method`

Options:
- MoMo
- Bank Transfer / VietQR

After `POST /api/public/workspace-registrations/{registrationId}/payments?token={registrationToken}` succeeds, navigate to `/workspace-registration/{registrationId}/payments/{paymentCode}`.
If the backend returns an existing pending payment, reuse the returned `paymentCode` and do not show duplicate payment instructions.

### Payment Instruction Page

Route suggestion: `/workspace-registration/{registrationId}/payments/{paymentCode}`

For MoMo:
- show QR code if `providerQrCodeUrl` exists
- show payment button if `providerPaymentUrl` exists
- show deeplink if `providerDeeplink` exists
- show amount, payment code, and status

For Bank Transfer / VietQR:
- show QR code from `providerQrCodeUrl`
- show bank name/code
- show account number/name
- show transfer amount and content
- show payment status

Poll `GET /api/public/payments/{paymentCode}/status?token={registrationToken}` until `SUCCESS`, `FAILED`, or `EXPIRED`, then navigate to the result page.

### Payment Result Page

Route suggestion: `/workspace-registration/{registrationId}/payments/{paymentCode}/result`

Display:
- payment status
- workspace activation status from `GET /api/public/workspace-registrations/{registrationId}?token={registrationToken}`
- next action: login/contact admin/retry payment
