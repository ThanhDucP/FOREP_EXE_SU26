alter table payment_qr_settings
    alter column qr_code_url drop not null;

alter table payment_qr_settings
    drop constraint if exists payment_qr_settings_qr_required;

create table payment_qr_files (
    id uuid primary key,
    file_name varchar(255) not null,
    content_type varchar(100) not null,
    file_size bigint not null,
    content bytea not null,
    uploaded_by uuid references users(id),
    created_at timestamptz not null,
    constraint payment_qr_files_size_positive check (file_size > 0 and file_size <= 5242880),
    constraint payment_qr_files_content_type check (content_type in ('image/png', 'image/jpeg', 'image/webp'))
);

alter table payment_qr_settings
    add column qr_file_id uuid references payment_qr_files(id);

alter table payment_transactions
    add column provider_name varchar(80),
    add column payment_configuration_snapshot text,
    add column qr_display_data text,
    add column confirmed_at timestamptz,
    add column confirmed_by uuid references users(id),
    add column failure_reason text;

create unique index if not exists ux_workspace_registrations_workspace_id
    on workspace_registrations(workspace_id)
    where workspace_id is not null;

create index if not exists idx_payment_transactions_status_created_at
    on payment_transactions(status, created_at desc);

create index if not exists idx_workspace_registrations_status_created_at
    on workspace_registrations(registration_status, created_at desc);

create index if not exists idx_tasks_workspace_deadline
    on tasks(workspace_id, deadline);
