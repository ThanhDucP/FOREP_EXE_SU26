create extension if not exists pgcrypto;

create table if not exists departments (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    name varchar(255) not null,
    code varchar(120),
    description text,
    status varchar(30) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint departments_status_check check (status in ('ACTIVE','INACTIVE')),
    constraint departments_workspace_name_unique unique (workspace_id, name),
    constraint departments_workspace_code_unique unique (workspace_id, code)
);

create index if not exists idx_departments_workspace on departments(workspace_id);

insert into departments (id, workspace_id, name, code, status, created_at, updated_at)
select
    gen_random_uuid(),
    workspace_id,
    trim(department_name),
    null,
    'ACTIVE',
    now(),
    now()
from job_positions
where department_name is not null
  and trim(department_name) <> ''
on conflict (workspace_id, name) do nothing;

update job_positions jp
set department_id = d.id
from departments d
where jp.workspace_id = d.workspace_id
  and jp.department_name is not null
  and lower(trim(jp.department_name)) = lower(d.name)
  and (jp.department_id is null or jp.department_id not in (select id from departments));

insert into departments (id, workspace_id, name, code, description, status, created_at, updated_at)
select
    gen_random_uuid(),
    jp.workspace_id,
    'Unassigned',
    null,
    'System-created fallback department for legacy positions without a valid department.',
    'ACTIVE',
    now(),
    now()
from job_positions jp
where jp.department_id is null
   or not exists (select 1 from departments d where d.id = jp.department_id)
group by jp.workspace_id
on conflict (workspace_id, name) do nothing;

update job_positions jp
set department_id = d.id,
    department_name = d.name
from departments d
where jp.workspace_id = d.workspace_id
  and d.name = 'Unassigned'
  and (jp.department_id is null or not exists (select 1 from departments existing where existing.id = jp.department_id));

update users u
set department_id = null
where u.department_id is not null
  and not exists (select 1 from departments d where d.id = u.department_id);

update tasks t
set department_id = null
where t.department_id is not null
  and not exists (select 1 from departments d where d.id = t.department_id);

alter table job_positions drop constraint if exists job_positions_department_fk;
alter table job_positions add constraint job_positions_department_fk
    foreign key (department_id) references departments(id);

alter table users drop constraint if exists users_department_fk;
alter table users add constraint users_department_fk
    foreign key (department_id) references departments(id);

alter table tasks drop constraint if exists tasks_department_fk;
alter table tasks add constraint tasks_department_fk
    foreign key (department_id) references departments(id);
