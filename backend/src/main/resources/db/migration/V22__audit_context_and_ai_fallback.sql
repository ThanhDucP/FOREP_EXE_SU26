alter table audit_logs alter column workspace_id drop not null;
alter table audit_logs alter column actor_id drop not null;

alter table audit_logs
    add column actor_name_snapshot varchar(255),
    add column actor_role_snapshot varchar(80),
    add column result varchar(30) not null default 'SUCCESS',
    add column ip_address varchar(120),
    add column user_agent text,
    add column request_id varchar(120),
    add column metadata text;

create index if not exists idx_audit_logs_workspace_created_at
    on audit_logs(workspace_id, created_at desc);
create index if not exists idx_audit_logs_action_result_created_at
    on audit_logs(action, result, created_at desc);

create index if not exists idx_ai_history_workspace_status_called_at
    on ai_history(workspace_id, status, called_at desc);
