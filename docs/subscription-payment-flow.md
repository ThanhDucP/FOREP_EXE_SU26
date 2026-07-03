# Workspace Registration Subscription Payment Flow

This backend implements a staged workspace registration flow:

1. `POST /api/v1/workspace-registrations`
   - Collects business and representative information only.
   - Creates `WorkspaceRegistration` with `PENDING_PLAN_SELECTION`.
   - Does not create or activate a workspace.

2. `GET /api/v1/subscription-plans/active`
   - Returns only active System Administrator managed plans.
   - Response includes price, duration, owner limit, employee limit, features, and status.

3. `PATCH /api/v1/workspace-registrations/{id}/select-plan`
   - Body: `{ "subscriptionPlanId": "uuid" }`
   - Validates the selected plan is `ACTIVE`.
   - Updates registration to `PENDING_PAYMENT`.

4. `POST /api/v1/workspace-registrations/{id}/payments`
   - Body: `{ "paymentMethod": "MOMO" }` or `{ "paymentMethod": "BANK_TRANSFER" }`
   - Creates a `PENDING` `PaymentTransaction`.
   - Returns MoMo payment URL/deeplink/QR information or VietQR bank transfer information.

5. `GET /api/v1/payments/{paymentId}`
   - Polls payment status for the payment instruction/result pages.

6. Payment confirmation
   - MoMo callback: `POST /api/v1/payments/momo/callback`
   - Bank callback: `POST /api/v1/payments/bank-transfer/callback`
   - Manual admin confirmation: `PATCH /api/v1/admin/payments/{paymentId}/confirm`
   - Manual admin rejection: `PATCH /api/v1/admin/payments/{paymentId}/reject`

On confirmed success, the backend updates the payment, marks the registration as paid, creates the workspace, applies selected plan limits, creates the allowed Business Owner accounts, and activates the workspace in one transaction. Employee accounts are not created during registration.

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

Load plans from `GET /api/v1/subscription-plans/active`.

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

After `POST /payments` succeeds, navigate to `/workspace-registration/{registrationId}/payments/{paymentId}`.

### Payment Instruction Page

Route suggestion: `/workspace-registration/{registrationId}/payments/{paymentId}`

For MoMo:
- show QR code if `providerQrCodeUrl` exists
- show payment button if `providerPaymentUrl` exists
- show deeplink if `providerDeeplink` exists
- show amount, order code, and status

For Bank Transfer / VietQR:
- show QR code from `providerQrCodeUrl`
- show bank name/code
- show account number/name
- show transfer amount and content
- show payment status

Poll `GET /api/v1/payments/{paymentId}` until `SUCCESS`, `FAILED`, or `EXPIRED`, then navigate to the result page.

### Payment Result Page

Route suggestion: `/workspace-registration/{registrationId}/payments/{paymentId}/result`

Display:
- payment status
- workspace activation status from `GET /api/v1/workspace-registrations/{registrationId}`
- next action: login/contact admin/retry payment
