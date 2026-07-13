alter table workspaces add column if not exists organization_abbreviation varchar(120);
alter table workspaces add column if not exists owner_account_provisioned_at timestamptz;
alter table workspaces add column if not exists owner_account_count integer not null default 0;

alter table users add column if not exists must_change_password boolean not null default false;
alter table users add column if not exists initial_account_generated boolean not null default false;

alter table job_positions add column if not exists code varchar(120);
alter table job_positions add column if not exists permission_group varchar(30) not null default 'EMPLOYEE';
alter table job_positions add column if not exists department_id uuid;

alter table job_positions drop constraint if exists job_positions_unique_title;
alter table job_positions drop constraint if exists job_positions_workspace_id_title_department_id_key;
alter table job_positions add constraint job_positions_unique_title_department unique (workspace_id, title, department_id);
alter table job_positions drop constraint if exists job_positions_workspace_id_code_key;
alter table job_positions add constraint job_positions_workspace_code_unique unique (workspace_id, code);

create table if not exists ai_history (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    caller_id uuid not null references users(id),
    caller_name varchar(255) not null,
    caller_role varchar(30) not null,
    function_name varchar(255) not null,
    status varchar(30) not null,
    called_at timestamptz not null,
    created_at timestamptz not null
);

create index if not exists idx_ai_history_workspace_created_at on ai_history(workspace_id, created_at desc);