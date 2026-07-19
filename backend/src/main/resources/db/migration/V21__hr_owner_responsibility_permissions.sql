delete from role_permissions
where role = 'BUSINESS_OWNER'
  and permission in (
      'EMPLOYEE_CREATE', 'EMPLOYEE_UPDATE', 'EMPLOYEE_DEACTIVATE',
      'DEPARTMENT_MANAGE', 'POSITION_MANAGE', 'ROLE_MANAGE',
      'PROJECT_CREATE', 'PROJECT_UPDATE', 'TASK_CREATE', 'TASK_ASSIGN',
      'TASK_APPROVE', 'TASK_UPDATE_OWN', 'AI_ANALYZE', 'AI_RECOMMENDATION'
  );

insert into role_permissions (id, role, permission, enabled, created_at, updated_at)
values
    (gen_random_uuid(), 'BUSINESS_OWNER', 'HR_ACCOUNT_MANAGE', true, now(), now()),
    (gen_random_uuid(), 'HR', 'EMPLOYEE_IMPORT', true, now(), now())
on conflict (role, permission) do update set enabled = true, updated_at = now();
