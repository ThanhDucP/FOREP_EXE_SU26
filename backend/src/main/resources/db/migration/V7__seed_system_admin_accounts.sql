insert into workspaces (
    id,
    name,
    logo,
    address,
    owner_id,
    created_at,
    short_code,
    next_employee_number,
    business_name,
    contact_email,
    contact_phone,
    subscription_plan_id,
    max_users,
    status,
    payment_status,
    activated_at,
    expires_at,
    last_activity_at,
    max_owner_accounts,
    max_employee_accounts
)
select
    '20000000-0000-0000-0000-000000000001',
    'System Administration',
    null,
    'Internal platform administration workspace',
    null,
    current_timestamp,
    'AD',
    1,
    'FOREP Platform',
    'admin@forep.local',
    null,
    null,
    2,
    'ACTIVE',
    'CONFIRMED',
    current_timestamp,
    null,
    null,
    2,
    0
where not exists (
    select 1 from workspaces where id = '20000000-0000-0000-0000-000000000001'
)
and not exists (
    select 1 from workspaces where short_code = 'AD'
);

insert into users (
    id,
    workspace_id,
    full_name,
    email,
    phone,
    username,
    employee_code,
    initial_password,
    password_hash,
    role,
    avatar,
    status,
    job_title,
    seniority_level,
    skill_rating,
    years_of_experience,
    skills,
    created_at,
    updated_at
)
select
    '20000000-0000-0000-0000-000000000101',
    '20000000-0000-0000-0000-000000000001',
    'System Admin 1',
    'admin1@forep.local',
    null,
    'admin1',
    null,
    null,
    '$2a$10$DPOjLgBORa1u/go2BGNkNOIKRgH9..lgsNybNyy49P4KAjdBwNltK',
    'PLATFORM_ADMIN',
    null,
    'ACTIVE',
    'System Administrator',
    null,
    null,
    null,
    null,
    current_timestamp,
    current_timestamp
where not exists (
    select 1 from users where id = '20000000-0000-0000-0000-000000000101'
)
and not exists (
    select 1 from users where username = 'admin1'
);

insert into users (
    id,
    workspace_id,
    full_name,
    email,
    phone,
    username,
    employee_code,
    initial_password,
    password_hash,
    role,
    avatar,
    status,
    job_title,
    seniority_level,
    skill_rating,
    years_of_experience,
    skills,
    created_at,
    updated_at
)
select
    '20000000-0000-0000-0000-000000000102',
    '20000000-0000-0000-0000-000000000001',
    'System Admin 2',
    'admin2@forep.local',
    null,
    'admin2',
    null,
    null,
    '$2a$10$M21BfYRcDGnPhOkVbWCnyeF4t/AagU7BiRmOiTEFU1/UiXxJst2qS',
    'PLATFORM_ADMIN',
    null,
    'ACTIVE',
    'System Administrator',
    null,
    null,
    null,
    null,
    current_timestamp,
    current_timestamp
where not exists (
    select 1 from users where id = '20000000-0000-0000-0000-000000000102'
)
and not exists (
    select 1 from users where username = 'admin2'
);
