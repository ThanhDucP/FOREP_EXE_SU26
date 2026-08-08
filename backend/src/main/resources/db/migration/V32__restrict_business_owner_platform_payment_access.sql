-- Platform payment management/history is an administrator concern.
-- Business owners can still view their workspace/subscription information,
-- but must not receive platform payment permissions or admin payment navigation.
delete from role_permissions
where role in ('BUSINESS_OWNER', 'OWNER')
  and permission in (
      'PAYMENT_CREATE',
      'PAYMENT_CONFIRM',
      'PAYMENT_STATUS_VIEW',
      'PAYMENT_HISTORY_VIEW',
      'PAYMENT_QR_MANAGE'
  );

-- Keep HR account management explicitly available to the workspace owner.
insert into role_permissions (id, role, permission, enabled, created_at, updated_at)
values (gen_random_uuid(), 'BUSINESS_OWNER', 'HR_ACCOUNT_MANAGE', true, now(), now())
on conflict (role, permission)
do update set enabled = true, updated_at = now();
