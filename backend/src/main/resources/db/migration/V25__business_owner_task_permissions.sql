delete from role_permissions
where role = 'BUSINESS_OWNER'
  and permission in (
      'EMPLOYEE_CREATE', 'EMPLOYEE_UPDATE', 'EMPLOYEE_DEACTIVATE',
      'DEPARTMENT_MANAGE', 'POSITION_MANAGE', 'ROLE_MANAGE',
      'PROJECT_CREATE', 'PROJECT_UPDATE', 'AI_ANALYZE'
  );

insert into role_permissions (id, role, permission, enabled, created_at, updated_at)
values
    (gen_random_uuid(), 'BUSINESS_OWNER', 'TASK_CREATE', true, now(), now()),
    (gen_random_uuid(), 'BUSINESS_OWNER', 'TASK_ASSIGN', true, now(), now()),
    (gen_random_uuid(), 'BUSINESS_OWNER', 'TASK_APPROVE', true, now(), now()),
    (gen_random_uuid(), 'BUSINESS_OWNER', 'TASK_UPDATE_OWN', true, now(), now()),
    (gen_random_uuid(), 'BUSINESS_OWNER', 'AI_RECOMMENDATION', true, now(), now()),
    (gen_random_uuid(), 'BUSINESS_OWNER', 'HR_ACCOUNT_MANAGE', true, now(), now())
on conflict (role, permission) do update set enabled = true, updated_at = now();
