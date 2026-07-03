alter table subscription_plans
    add column description text;

alter table subscription_plans
    add column duration_in_months integer not null default 1;

alter table subscription_plans
    add column max_owner_accounts integer not null default 1;

alter table subscription_plans
    add column max_employee_accounts integer not null default 1;

alter table subscription_plans
    add column has_full_features boolean not null default true;

alter table subscription_plans
    drop constraint if exists subscription_plans_price_non_negative;

alter table subscription_plans
    add constraint subscription_plans_price_positive check (price > 0);

alter table subscription_plans
    add constraint subscription_plans_owner_limit_positive check (max_owner_accounts > 0);

alter table subscription_plans
    add constraint subscription_plans_employee_limit_positive check (max_employee_accounts > 0);

alter table workspaces
    add column max_owner_accounts integer not null default 1;

alter table workspaces
    add column max_employee_accounts integer not null default 49;

alter table workspace_registrations
    drop constraint if exists workspace_registrations_owner_required;

alter table workspace_registrations
    alter column subscription_plan_id drop not null;

alter table workspace_registrations
    alter column max_users drop not null;

alter table workspace_registrations
    add column max_owner_accounts integer not null default 0;

alter table workspace_registrations
    add column max_employee_accounts integer not null default 0;

alter table workspace_registrations
    add column representative_full_name varchar(255);

alter table workspace_registrations
    add column representative_email varchar(255);

alter table workspace_registrations
    add column representative_phone varchar(50);

create table payment_transactions (
    id uuid primary key,
    workspace_registration_id uuid not null references workspace_registrations(id),
    subscription_plan_id uuid not null references subscription_plans(id),
    payment_method varchar(30) not null,
    amount numeric(12, 2) not null,
    currency varchar(10) not null default 'VND',
    order_code varchar(80) not null unique,
    request_id varchar(80) not null unique,
    provider_transaction_id varchar(120),
    provider_payment_url varchar(1000),
    provider_deeplink varchar(1000),
    provider_qr_code_url varchar(1000),
    bank_code varchar(50),
    bank_name varchar(120),
    bank_account_number varchar(80),
    bank_account_name varchar(255),
    transfer_content varchar(255),
    status varchar(30) not null,
    raw_provider_request text,
    raw_provider_response text,
    paid_at timestamp with time zone,
    expired_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint payment_transactions_amount_positive check (amount > 0),
    constraint payment_transactions_method_valid check (payment_method in ('MOMO', 'BANK_TRANSFER')),
    constraint payment_transactions_status_valid check (status in ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED'))
);

create index idx_payment_transactions_registration on payment_transactions(workspace_registration_id);
create index idx_payment_transactions_status on payment_transactions(status);

insert into subscription_plans (
    id, name, price, duration_days, duration_in_months, max_users, max_owner_accounts,
    max_employee_accounts, max_workspaces, ai_usage_limit, description, features, has_full_features,
    status, created_at, updated_at
)
select '10000000-0000-0000-0000-000000000001', 'Starter Workspace Plan', 399000, 30, 1, 32, 2, 30, null, null,
       'Full system features for starter teams', 'Full system features', true, 'ACTIVE', current_timestamp, current_timestamp
where not exists (select 1 from subscription_plans where lower(name) = lower('Starter Workspace Plan'));

insert into subscription_plans (
    id, name, price, duration_days, duration_in_months, max_users, max_owner_accounts,
    max_employee_accounts, max_workspaces, ai_usage_limit, description, features, has_full_features,
    status, created_at, updated_at
)
select '10000000-0000-0000-0000-000000000002', 'Small Business Workspace Plan', 799000, 30, 1, 125, 5, 120, null, null,
       'Full system features for small businesses', 'Full system features', true, 'ACTIVE', current_timestamp, current_timestamp
where not exists (select 1 from subscription_plans where lower(name) = lower('Small Business Workspace Plan'));

insert into subscription_plans (
    id, name, price, duration_days, duration_in_months, max_users, max_owner_accounts,
    max_employee_accounts, max_workspaces, ai_usage_limit, description, features, has_full_features,
    status, created_at, updated_at
)
select '10000000-0000-0000-0000-000000000003', 'Medium Business Workspace Plan', 1499000, 30, 1, 310, 10, 300, null, null,
       'Full system features for growing businesses', 'Full system features', true, 'ACTIVE', current_timestamp, current_timestamp
where not exists (select 1 from subscription_plans where lower(name) = lower('Medium Business Workspace Plan'));
