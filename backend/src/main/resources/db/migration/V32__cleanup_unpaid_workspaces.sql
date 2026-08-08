-- One-time cleanup requested for existing data.
-- Keep only customer workspaces that have provider-confirmed successful payment.
-- Platform/system-admin workspaces are excluded from this cleanup because they are infrastructure,
-- not customer workspaces.

create temporary table forep_unpaid_workspace_cleanup (
    workspace_id uuid primary key
) on commit drop;

insert into forep_unpaid_workspace_cleanup (workspace_id)
select w.id
from workspaces w
where not exists (
    select 1
    from workspace_registrations wr
    join payment_transactions pt
      on pt.workspace_registration_id = wr.id
    where wr.workspace_id = w.id
      and pt.status in ('SUCCESS', 'PAID')
)
and not exists (
    select 1
    from workspace_subscriptions ws
    join payment_transactions pt
      on pt.id = ws.payment_transaction_id
    where ws.workspace_id = w.id
      and pt.status in ('SUCCESS', 'PAID')
)
and not exists (
    select 1
    from users u
    where u.workspace_id = w.id
      and u.role in ('SYSTEM_ADMIN', 'PLATFORM_ADMIN')
);

-- Detach nullable global references to users that are about to be removed.
update workspace_registrations wr
set reviewed_by = null,
    updated_at = current_timestamp
where wr.reviewed_by in (
    select u.id
    from users u
    join forep_unpaid_workspace_cleanup cleanup on cleanup.workspace_id = u.workspace_id
);

update business_feedback feedback
set reviewed_by = null,
    updated_at = current_timestamp
where feedback.reviewed_by in (
    select u.id
    from users u
    join forep_unpaid_workspace_cleanup cleanup on cleanup.workspace_id = u.workspace_id
);

update payment_transactions payment
set user_id = null,
    updated_at = current_timestamp
where payment.user_id in (
    select u.id
    from users u
    join forep_unpaid_workspace_cleanup cleanup on cleanup.workspace_id = u.workspace_id
);

update payos_config config
set updated_by = null,
    updated_at = current_timestamp
where config.updated_by in (
    select u.id
    from users u
    join forep_unpaid_workspace_cleanup cleanup on cleanup.workspace_id = u.workspace_id
);

update payment_qr_settings setting
set updated_by = null,
    updated_at = current_timestamp
where setting.updated_by in (
    select u.id
    from users u
    join forep_unpaid_workspace_cleanup cleanup on cleanup.workspace_id = u.workspace_id
);

-- Break the workspace -> owner user reference before cascading user deletion.
update workspaces w
set owner_id = null
where w.id in (select workspace_id from forep_unpaid_workspace_cleanup);

-- Remove partial subscriptions first because they may reference payment transactions.
delete from workspace_subscriptions ws
where ws.workspace_id in (select workspace_id from forep_unpaid_workspace_cleanup);

-- Remove payment attempts/history belonging only to registrations of workspaces being removed.
delete from payment_transactions pt
where pt.workspace_registration_id in (
    select wr.id
    from workspace_registrations wr
    where wr.workspace_id in (select workspace_id from forep_unpaid_workspace_cleanup)
);

-- Remove the obsolete registrations after their payment rows are gone.
delete from workspace_registrations wr
where wr.workspace_id in (select workspace_id from forep_unpaid_workspace_cleanup);

-- Cascades remove workspace-scoped users, tasks, reports, notifications, AI data,
-- feedback, imports, files and other workspace-owned rows.
delete from workspaces w
where w.id in (select workspace_id from forep_unpaid_workspace_cleanup);
