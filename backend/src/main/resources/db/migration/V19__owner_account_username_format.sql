with ranked_owner_accounts as (
    select
        u.id,
        upper(coalesce(nullif(w.short_code, ''), nullif(w.organization_abbreviation, ''), 'XX')) as prefix,
        row_number() over (partition by u.workspace_id order by u.created_at asc, u.id asc) as account_index
    from users u
    join workspaces w on w.id = u.workspace_id
    where u.role in ('BUSINESS_OWNER', 'OWNER')
      and u.initial_account_generated = true
),
proposed_owner_accounts as (
    select
        id,
        substring(prefix, 1, 2) || '0000' || chr(64 + account_index::int) as username
    from ranked_owner_accounts
    where account_index between 1 and 26
)
update users u
set username = proposed.username,
    email = lower(proposed.username) || '@workspace.local',
    initial_password = '123456',
    must_change_password = true,
    updated_at = now()
from proposed_owner_accounts proposed
where u.id = proposed.id
  and not exists (
      select 1
      from users existing
      where existing.id <> u.id
        and lower(existing.username) = lower(proposed.username)
  );
