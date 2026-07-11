alter table users drop constraint if exists users_role_check;
alter table users add constraint users_role_check check (role in ('PLATFORM_ADMIN','BUSINESS_OWNER','HR','MANAGER','EMPLOYEE','SYSTEM','SYSTEM_ADMIN','OWNER'));

alter table users add column if not exists avatar_file_id varchar(255);
alter table users add column if not exists department_id uuid;
alter table users add column if not exists job_position_id uuid;
alter table users add column if not exists date_of_birth date;
alter table users add column if not exists gender varchar(50);
alter table users add column if not exists address text;
alter table users add column if not exists personal_summary text;
alter table users add column if not exists employment_type varchar(30);
alter table users add column if not exists working_status varchar(30);
alter table users add column if not exists employee_level varchar(30);
alter table users add column if not exists monthly_working_capacity_hours integer not null default 168;
alter table users add column if not exists main_expertise text;
alter table users add column if not exists secondary_expertise text;

alter table tasks add column if not exists assignment_type varchar(30) not null default 'INDIVIDUAL';
alter table tasks add column if not exists start_date timestamptz;
alter table tasks add column if not exists difficulty integer;
alter table tasks add column if not exists required_skills text;
alter table tasks add column if not exists required_job_position_id uuid;
alter table tasks add column if not exists task_domain varchar(255);
alter table tasks add column if not exists project_id uuid;
alter table tasks add column if not exists department_id uuid;
update tasks set estimated_hours = 1 where estimated_hours is null or estimated_hours <= 0;
alter table tasks drop constraint if exists tasks_assignment_type_check;
alter table tasks add constraint tasks_assignment_type_check check (assignment_type in ('INDIVIDUAL','TEAM'));
alter table tasks drop constraint if exists tasks_estimated_hours_positive_check;
alter table tasks add constraint tasks_estimated_hours_positive_check check (estimated_hours is null or estimated_hours > 0);
alter table tasks drop constraint if exists tasks_difficulty_check;
alter table tasks add constraint tasks_difficulty_check check (difficulty is null or difficulty between 1 and 5);

create table if not exists task_assignees (
    id uuid primary key,
    workspace_id uuid not null,
    task_id uuid not null references tasks(id) on delete cascade,
    employee_id uuid not null references users(id),
    participant_role varchar(30) not null,
    leader boolean not null default false,
    allocated_hours numeric(10,2),
    created_at timestamptz not null default now(),
    constraint task_assignees_role_check check (participant_role in ('ASSIGNEE','LEADER','MEMBER')),
    constraint task_assignees_unique_employee unique (task_id, employee_id)
);
create index if not exists idx_task_assignees_task on task_assignees(task_id);
create index if not exists idx_task_assignees_employee on task_assignees(workspace_id, employee_id);
create unique index if not exists ux_task_assignees_one_leader on task_assignees(task_id) where leader;

create table if not exists task_attachments (
    id uuid primary key,
    workspace_id uuid not null,
    task_id uuid not null references tasks(id) on delete cascade,
    file_name varchar(255) not null,
    file_url text not null,
    content_type varchar(255),
    file_size bigint,
    attachment_type varchar(30) not null default 'OTHER',
    uploaded_by uuid references users(id),
    created_at timestamptz not null default now(),
    constraint task_attachments_type_check check (attachment_type in ('REQUIREMENT','REFERENCE','RESULT','OTHER'))
);
create index if not exists idx_task_attachments_task on task_attachments(task_id);

create table if not exists job_positions (
    id uuid primary key,
    workspace_id uuid not null,
    title varchar(255) not null,
    department_name varchar(255),
    description text,
    required_skills text,
    status varchar(30) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint job_positions_status_check check (status in ('ACTIVE','INACTIVE')),
    constraint job_positions_unique_title unique (workspace_id, title)
);
create index if not exists idx_job_positions_workspace on job_positions(workspace_id);

create table if not exists employee_skills (
    id uuid primary key,
    workspace_id uuid not null,
    employee_id uuid not null references users(id) on delete cascade,
    skill_name varchar(255) not null,
    proficiency_level integer,
    years_of_experience numeric(5,2),
    source varchar(100),
    created_at timestamptz not null default now()
);

create table if not exists employee_experiences (
    id uuid primary key,
    workspace_id uuid not null,
    employee_id uuid not null references users(id) on delete cascade,
    company_name varchar(255),
    position_title varchar(255),
    start_date date,
    end_date date,
    description text,
    created_at timestamptz not null default now()
);

create table if not exists employee_education (
    id uuid primary key,
    workspace_id uuid not null,
    employee_id uuid not null references users(id) on delete cascade,
    school_name varchar(255),
    degree varchar(255),
    major varchar(255),
    start_date date,
    end_date date,
    created_at timestamptz not null default now()
);

create table if not exists employee_certifications (
    id uuid primary key,
    workspace_id uuid not null,
    employee_id uuid not null references users(id) on delete cascade,
    certification_name varchar(255) not null,
    issuer varchar(255),
    issued_date date,
    expires_date date,
    document_url text,
    created_at timestamptz not null default now()
);

create table if not exists employee_documents (
    id uuid primary key,
    workspace_id uuid not null,
    employee_id uuid not null references users(id) on delete cascade,
    document_name varchar(255) not null,
    document_type varchar(100),
    file_url text not null,
    uploaded_by uuid references users(id),
    created_at timestamptz not null default now()
);

create table if not exists employee_monthly_workload (
    id uuid primary key,
    workspace_id uuid not null,
    employee_id uuid not null references users(id) on delete cascade,
    year integer not null,
    month integer not null,
    allocated_hours numeric(10,2) not null default 0,
    capacity_hours numeric(10,2) not null default 168,
    workload_level varchar(30) not null,
    workload_label varchar(100) not null,
    calculated_at timestamptz not null default now(),
    constraint employee_monthly_workload_unique unique (employee_id, year, month)
);
