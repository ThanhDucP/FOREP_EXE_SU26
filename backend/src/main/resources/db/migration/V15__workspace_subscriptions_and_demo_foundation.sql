create table if not exists workspace_subscriptions (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    subscription_plan_id uuid not null references subscription_plans(id),
    status varchar(30) not null,
    start_date timestamptz not null,
    end_date timestamptz not null,
    renewal_date timestamptz,
    price numeric(12, 2) not null,
    max_owner_accounts integer not null,
    max_employee_accounts integer not null,
    payment_transaction_id uuid references payment_transactions(id),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint workspace_subscriptions_status_check check (status in ('ACTIVE','EXPIRED','CANCELLED','PENDING_RENEWAL','UPGRADED','DOWNGRADED')),
    constraint workspace_subscriptions_price_non_negative check (price >= 0),
    constraint workspace_subscriptions_owner_limit_positive check (max_owner_accounts > 0),
    constraint workspace_subscriptions_employee_limit_non_negative check (max_employee_accounts >= 0),
    constraint workspace_subscriptions_date_order check (end_date > start_date)
);

create index if not exists idx_workspace_subscriptions_workspace on workspace_subscriptions(workspace_id);
create index if not exists idx_workspace_subscriptions_plan on workspace_subscriptions(subscription_plan_id);
create unique index if not exists ux_workspace_subscriptions_payment
    on workspace_subscriptions(payment_transaction_id)
    where payment_transaction_id is not null;
create unique index if not exists ux_workspace_subscriptions_one_active
    on workspace_subscriptions(workspace_id)
    where status = 'ACTIVE';

insert into workspace_subscriptions (
    id,
    workspace_id,
    subscription_plan_id,
    status,
    start_date,
    end_date,
    renewal_date,
    price,
    max_owner_accounts,
    max_employee_accounts,
    payment_transaction_id,
    created_at,
    updated_at
)
select
    gen_random_uuid(),
    w.id,
    w.subscription_plan_id,
    case when w.status = 'EXPIRED' then 'EXPIRED' else 'ACTIVE' end,
    coalesce(w.activated_at, w.created_at, now()),
    coalesce(w.expires_at, coalesce(w.activated_at, w.created_at, now()) + (sp.duration_in_months || ' months')::interval),
    coalesce(w.expires_at, coalesce(w.activated_at, w.created_at, now()) + (sp.duration_in_months || ' months')::interval),
    sp.price,
    coalesce(nullif(w.max_owner_accounts, 0), sp.max_owner_accounts),
    coalesce(w.max_employee_accounts, sp.max_employee_accounts),
    null,
    now(),
    now()
from workspaces w
join subscription_plans sp on sp.id = w.subscription_plan_id
where w.subscription_plan_id is not null
  and not exists (select 1 from workspace_subscriptions ws where ws.workspace_id = w.id);
