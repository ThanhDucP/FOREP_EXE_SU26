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
   - Body: `{ "paymentMethod": "MOMO" }`
   - Creates a `PENDING` `PaymentTransaction`.
   - If the registration already has a non-expired `PENDING`/`PROCESSING` transaction, the backend returns that transaction instead of creating another payment instruction.
   - Returns a public payment status payload with real MoMo provider data.
   - If MoMo provider config is missing, backend returns a business-rule error and does not create a payment. Public UI must ask the user to wait for admin to complete payment setup.

5. `GET /api/public/payments/{paymentCode}/status?token={registrationToken}`
   - Polls payment status for the payment instruction/result pages.
   - The registration token is required so public users cannot inspect payment details by payment code alone.

6. Payment confirmation
   - MoMo callback: `POST /api/payments/momo/ipn` (legacy alias: `/api/payment-callbacks/momo`)
   - MoMo callbacks must include a valid signature using the `secretKey` saved by Platform Admin in the MoMo payment setting.
   - Manual admin confirmation: `PATCH /api/admin/payments/{paymentId}/confirm`
   - Manual admin rejection: `PATCH /api/admin/payments/{paymentId}/reject`
   - Admin reconciliation: `GET /api/admin/payments`, `GET /api/admin/payments/{paymentId}`, and `GET /api/admin/audit-logs`.

On confirmed success, the backend updates the payment, marks the registration as paid, creates the workspace, applies selected plan limits, creates the allowed Business Owner accounts, and activates the workspace in one transaction. Employee accounts are not created during registration.

The activation transaction also creates a dedicated `workspace_subscriptions` ACTIVE row. This row is the billing/audit snapshot for the selected plan, price, owner/employee limits, start date, end date, renewal date, and payment transaction id. Workspace fields such as `subscriptionPlanId`, `maxOwnerAccounts`, and `maxEmployeeAccounts` stay available as compatibility/current-state fields, but admin UI should prefer `activeSubscription` when rendering the current package.

When a Platform Admin changes the plan of an active workspace, the backend closes the previous ACTIVE subscription as `UPGRADED` or `DOWNGRADED` and opens a new ACTIVE subscription snapshot.

Payment transactions expire after their `expiredAt` timestamp. A scheduled backend job marks stale `PENDING`/`PROCESSING` payments as `EXPIRED`, and public payment polling also refreshes the expired state. Workspace registrations that pass their registration expiry date without approval are marked `EXPIRED`.

## Provider Modes

MoMo uses the real provider API only when all production config values are present in the Platform Admin payment UI:

- `providerEndpoint`
- `partnerCode`
- `accessKey`
- `secretKey`
- `returnUrl`
- `ipnUrl` (`notifyUrl` is accepted only as a legacy alias)

MoMo uses the real provider only. If provider config is incomplete, backend does not create MoMo sandbox/stub instructions and asks the user to wait.

## Platform Admin MoMo Settings

- `GET /api/admin/momo-payment-setting`
- `PUT /api/admin/momo-payment-setting`
- Body: `{ "providerEndpoint": "https://test-payment.momo.vn", "partnerCode": "...", "accessKey": "...", "secretKey": "...", "returnUrl": "...", "ipnUrl": ".../api/payments/momo/ipn", "transferContentPrefix": "...", "enabled": true }`

Rules:

- Admin config must not submit `qrCodeUrl`, `paymentUrl`, or `deeplink`; backend rejects URL-based payment settings.
- `secretKey` is write-only in FE: the backend response only returns `secretKeyConfigured`.
- `providerEndpoint` is a base URL. Backend normalizes trailing slashes and appends the fixed create/query paths.
- `MOMO_CONFIG_ENCRYPTION_KEY` must contain at least 32 characters. Migration V30 clears legacy plaintext Secret Keys and disables MoMo; the admin must enter the Secret Key again once after deployment.
- New payments copy the current MoMo provider setting into `PaymentTransaction`; changing config later does not mutate old payment instructions.
- Frontend must never generate fake QR codes.

## Demo Data

Migration `V16__demo_saas_operational_seed.sql` seeds production-like QA data:

- 3 active workspaces: `SV`, `MD`, `HC`
- 30 employees per workspace
- Departments and business positions where Developer/BA/Tech Lead/HR Specialist are job positions, not system roles
- Tasks, team/individual assignments, daily reports, workload buckets, payments, active subscriptions, AI history, cached AI suggestions, and feedback
- Demo owner logins: `SV0000A`, `MD0000A`, `HC0000A`; initial password `123456`

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

After `POST /api/public/workspace-registrations/{registrationId}/payments?token={registrationToken}` succeeds, navigate to `/workspace-registration/{registrationId}/payments/{paymentCode}`.
If the backend returns an existing pending payment, reuse the returned `paymentCode` and do not show duplicate payment instructions.

### Payment Instruction Page

Route suggestion: `/workspace-registration/{registrationId}/payments/{paymentCode}`

For MoMo:
- show QR code if `providerQrCodeUrl` exists
- show payment button if `providerPaymentUrl` exists
- show deeplink if `providerDeeplink` exists
- show amount, payment code, and status

Poll `GET /api/public/payments/{paymentCode}/status?token={registrationToken}` until `SUCCESS`, `FAILED`, or `EXPIRED`, then navigate to the result page.

### Payment Result Page

Route suggestion: `/workspace-registration/{registrationId}/payments/{paymentCode}/result`

Display:
- payment status
- workspace activation status from `GET /api/public/workspace-registrations/{registrationId}?token={registrationToken}`
- next action: login/contact admin/retry payment
