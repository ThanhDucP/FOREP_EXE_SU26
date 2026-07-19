create table employee_import_batches (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    file_name varchar(255) not null,
    status varchar(30) not null,
    total_rows integer not null default 0,
    valid_rows integer not null default 0,
    invalid_rows integer not null default 0,
    imported_rows integer not null default 0,
    created_by uuid not null references users(id),
    confirmed_by uuid references users(id),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    confirmed_at timestamptz,
    cancelled_at timestamptz,
    constraint employee_import_batches_status check (status in ('VALIDATED', 'CONFIRMED', 'CANCELLED'))
);

create table employee_import_rows (
    id uuid primary key,
    batch_id uuid not null references employee_import_batches(id) on delete cascade,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    row_number integer not null,
    raw_data text not null,
    valid boolean not null,
    errors text,
    imported boolean not null default false,
    imported_user_id uuid references users(id),
    created_at timestamptz not null,
    constraint uq_employee_import_rows_batch_row unique (batch_id, row_number)
);

create index idx_employee_import_batches_workspace_created_at on employee_import_batches(workspace_id, created_at desc);
create index idx_employee_import_rows_batch_valid on employee_import_rows(batch_id, valid, row_number);
