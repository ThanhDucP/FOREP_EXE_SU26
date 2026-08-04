create unique index if not exists ux_users_email_normalized
    on users(lower(email));

create unique index if not exists ux_users_workspace_phone
    on users(workspace_id, phone)
    where phone is not null and phone <> '';

delete from role_permissions
where role = 'BUSINESS_OWNER'
  and permission in (
      'PACKAGE_MANAGE', 'WORKSPACE_MANAGE', 'PAYMENT_CREATE', 'PAYMENT_CONFIRM',
      'PAYMENT_QR_MANAGE', 'SUBSCRIPTION_RENEW', 'SUBSCRIPTION_UPGRADE',
      'EMPLOYEE_CREATE', 'EMPLOYEE_UPDATE', 'EMPLOYEE_DEACTIVATE', 'EMPLOYEE_IMPORT',
      'DEPARTMENT_MANAGE', 'POSITION_MANAGE', 'ROLE_MANAGE',
      'SYSTEM_CONFIGURATION', 'REVENUE_VIEW'
  );

delete from role_permissions
where role = 'HR'
  and permission in (
      'PACKAGE_VIEW', 'PACKAGE_MANAGE', 'WORKSPACE_UPDATE', 'WORKSPACE_MANAGE',
      'PAYMENT_CREATE', 'PAYMENT_CONFIRM', 'PAYMENT_STATUS_VIEW', 'PAYMENT_HISTORY_VIEW',
      'PAYMENT_QR_MANAGE', 'SUBSCRIPTION_VIEW', 'SUBSCRIPTION_RENEW', 'SUBSCRIPTION_UPGRADE',
      'ROLE_MANAGE', 'HR_ACCOUNT_MANAGE', 'SYSTEM_CONFIGURATION', 'REVENUE_VIEW'
  );

insert into role_permissions (id, role, permission, enabled, created_at, updated_at)
select gen_random_uuid(), seed.role, seed.permission, true, now(), now()
from (
    values
        ('BUSINESS_OWNER', 'WORKSPACE_VIEW'),
        ('BUSINESS_OWNER', 'WORKSPACE_UPDATE'),
        ('BUSINESS_OWNER', 'EMPLOYEE_VIEW'),
        ('BUSINESS_OWNER', 'DEPARTMENT_VIEW'),
        ('BUSINESS_OWNER', 'POSITION_VIEW'),
        ('BUSINESS_OWNER', 'HR_ACCOUNT_MANAGE'),
        ('BUSINESS_OWNER', 'TASK_VIEW'),
        ('BUSINESS_OWNER', 'AI_SUMMARY'),
        ('BUSINESS_OWNER', 'AI_HISTORY'),
        ('BUSINESS_OWNER', 'REPORT_VIEW'),
        ('BUSINESS_OWNER', 'FEEDBACK_CREATE'),
        ('HR', 'WORKSPACE_VIEW'),
        ('HR', 'EMPLOYEE_VIEW'),
        ('HR', 'EMPLOYEE_CREATE'),
        ('HR', 'EMPLOYEE_UPDATE'),
        ('HR', 'EMPLOYEE_DEACTIVATE'),
        ('HR', 'EMPLOYEE_IMPORT'),
        ('HR', 'DEPARTMENT_VIEW'),
        ('HR', 'DEPARTMENT_MANAGE'),
        ('HR', 'POSITION_VIEW'),
        ('HR', 'POSITION_MANAGE'),
        ('HR', 'REPORT_VIEW'),
        ('HR', 'REPORT_REVIEW'),
        ('HR', 'REPORT_EXPORT'),
        ('HR', 'FEEDBACK_CREATE'),
        ('HR', 'NOTIFICATION_VIEW')
) as seed(role, permission)
on conflict (role, permission)
do update set enabled = true, updated_at = now();
