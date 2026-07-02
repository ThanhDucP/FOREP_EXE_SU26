alter table workspaces
    add column business_name varchar(255);

alter table workspaces
    add column contact_email varchar(255);

alter table workspaces
    add column contact_phone varchar(50);

alter table workspaces
    add column subscription_plan_id uuid;

alter table workspaces
    add column max_users integer not null default 50;

alter table workspaces
    add column status varchar(30) not null default 'ACTIVE';

alter table workspaces
    add column payment_status varchar(30) not null default 'CONFIRMED';

alter table workspaces
    add column activated_at timestamp with time zone;

alter table workspaces
    add column expires_at timestamp with time zone;

alter table workspaces
    add column last_activity_at timestamp with time zone;

alter table workspaces
    add constraint workspaces_max_users_positive check (max_users > 0);

create table subscription_plans (
    id uuid primary key,
    name varchar(120) not null unique,
    price numeric(12, 2) not null,
    duration_days integer not null,
    max_users integer not null,
    max_workspaces integer,
    ai_usage_limit integer,
    features text,
    status varchar(30) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint subscription_plans_price_non_negative check (price >= 0),
    constraint subscription_plans_duration_positive check (duration_days > 0),
    constraint subscription_plans_max_users_positive check (max_users > 0)
);

alter table workspaces
    add constraint workspaces_subscription_plan_fk foreign key (subscription_plan_id) references subscription_plans(id);

create table workspace_registrations (
    id uuid primary key,
    business_name varchar(255) not null,
    workspace_name varchar(255) not null,
    workspace_identifier varchar(20) not null,
    contact_email varchar(255) not null,
    contact_phone varchar(50) not null,
    business_address text,
    subscription_plan_id uuid not null references subscription_plans(id),
    max_users integer not null,
    activation_date timestamp with time zone,
    expiration_date timestamp with time zone,
    payment_proof_url varchar(1000),
    payment_note text,
    payment_status varchar(30) not null,
    registration_status varchar(30) not null,
    workspace_id uuid references workspaces(id),
    reviewed_by uuid references users(id),
    reviewed_at timestamp with time zone,
    review_note text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint workspace_registrations_identifier_unique unique (workspace_identifier),
    constraint workspace_registrations_max_users_positive check (max_users > 0)
);

create table business_feedback (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    rating integer not null,
    content text not null,
    support_note text,
    status varchar(30) not null,
    reviewed_by uuid references users(id),
    reviewed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint business_feedback_rating_range check (rating between 1 and 5)
);

create index idx_workspaces_status on workspaces(status);
create index idx_workspaces_subscription_plan on workspaces(subscription_plan_id);
create index idx_workspace_registrations_status on workspace_registrations(registration_status, payment_status);
create index idx_business_feedback_workspace on business_feedback(workspace_id);
create index idx_business_feedback_status on business_feedback(status);
