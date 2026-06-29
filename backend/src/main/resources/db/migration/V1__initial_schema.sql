create table workspaces (
    id uuid primary key,
    name varchar(255) not null,
    logo varchar(1000),
    address text,
    owner_id uuid,
    created_at timestamp with time zone not null
);

create table users (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    full_name varchar(255) not null,
    email varchar(255) not null,
    phone varchar(50),
    password_hash varchar(255) not null,
    role varchar(30) not null,
    avatar varchar(1000),
    status varchar(30) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint users_workspace_email_unique unique (workspace_id, email)
);

alter table workspaces
    add constraint workspaces_owner_fk foreign key (owner_id) references users(id);

create table tasks (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    title varchar(255) not null,
    requirements text not null,
    description text,
    assignee_id uuid not null references users(id),
    creator_id uuid not null references users(id),
    priority varchar(30) not null,
    deadline timestamp with time zone not null,
    estimated_hours numeric(10, 2),
    progress_percent integer not null,
    status varchar(30) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    completed_at timestamp with time zone
);

create table task_updates (
    id uuid primary key,
    task_id uuid not null references tasks(id) on delete cascade,
    user_id uuid not null references users(id),
    progress_percent integer not null,
    content text not null,
    attachment varchar(1000),
    update_type varchar(30) not null,
    created_at timestamp with time zone not null
);

create table daily_reports (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    user_id uuid not null references users(id),
    report_date date not null,
    today_completed text not null,
    current_work text not null,
    blockers text,
    tomorrow_plan text,
    reviewed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint daily_reports_user_date_unique unique (workspace_id, user_id, report_date)
);

create table notifications (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    user_id uuid not null references users(id),
    type varchar(80) not null,
    title varchar(255) not null,
    message text not null,
    related_entity_type varchar(80),
    related_entity_id uuid,
    is_read boolean not null,
    created_at timestamp with time zone not null
);

create table ai_suggestions (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    type varchar(80) not null,
    input_data text not null,
    output_data text not null,
    status varchar(30) not null,
    created_by uuid not null references users(id),
    created_at timestamp with time zone not null
);

create table files (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    uploaded_by uuid not null references users(id),
    file_name varchar(255) not null,
    file_type varchar(120) not null,
    file_url varchar(1000) not null,
    related_entity_type varchar(80),
    related_entity_id uuid,
    created_at timestamp with time zone not null
);

create table audit_logs (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    actor_id uuid not null references users(id),
    action varchar(120) not null,
    entity_type varchar(80) not null,
    entity_id uuid not null,
    old_value text,
    new_value text,
    created_at timestamp with time zone not null
);

create index idx_users_workspace on users(workspace_id);
create index idx_tasks_workspace on tasks(workspace_id);
create index idx_tasks_assignee on tasks(assignee_id);
create index idx_task_updates_task on task_updates(task_id);
create index idx_daily_reports_workspace_date on daily_reports(workspace_id, report_date);
create index idx_notifications_user_read on notifications(user_id, is_read);
create index idx_ai_suggestions_workspace on ai_suggestions(workspace_id);
