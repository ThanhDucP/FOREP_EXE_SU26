-- Sample FOREP database seed.
-- Creates 20 workspaces. Each workspace has 1 OWNER and 50 EMPLOYEE users.
-- Owner accounts use password: Password123!
-- Employee accounts use password equal to employee_code, for example SE0001.
-- Run manually on a development/staging PostgreSQL database after Flyway migrations.

create extension if not exists pgcrypto;

begin;

with orgs as (
    select *
    from (values
        (1, 'SE'), (2, 'FE'), (3, 'BE'), (4, 'QA'), (5, 'UX'),
        (6, 'DA'), (7, 'MK'), (8, 'SA'), (9, 'CS'), (10, 'OP'),
        (11, 'PM'), (12, 'HR'), (13, 'FN'), (14, 'LG'), (15, 'PR'),
        (16, 'BD'), (17, 'IT'), (18, 'AI'), (19, 'ML'), (20, 'DS')
    ) as item(org_no, short_code)
)
insert into workspaces (id, name, short_code, next_employee_number, logo, address, owner_id, created_at)
select
    ('00000000-0000-0000-0001-' || lpad(org_no::text, 12, '0'))::uuid,
    'Sample Org ' || lpad(org_no::text, 2, '0'),
    short_code,
    51,
    null,
    'Sample address ' || org_no,
    null,
    now()
from orgs
on conflict (id) do update set
    name = excluded.name,
    short_code = excluded.short_code,
    next_employee_number = excluded.next_employee_number,
    logo = excluded.logo,
    address = excluded.address;

with orgs as (
    select *
    from (values
        (1, 'SE'), (2, 'FE'), (3, 'BE'), (4, 'QA'), (5, 'UX'),
        (6, 'DA'), (7, 'MK'), (8, 'SA'), (9, 'CS'), (10, 'OP'),
        (11, 'PM'), (12, 'HR'), (13, 'FN'), (14, 'LG'), (15, 'PR'),
        (16, 'BD'), (17, 'IT'), (18, 'AI'), (19, 'ML'), (20, 'DS')
    ) as item(org_no, short_code)
)
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
    ('00000000-0000-0000-0002-' || lpad(org_no::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0001-' || lpad(org_no::text, 12, '0'))::uuid,
    'Owner Org ' || lpad(org_no::text, 2, '0'),
    'owner' || lpad(org_no::text, 2, '0') || '@sample.forep.local',
    '090' || lpad(org_no::text, 7, '0'),
    lower('owner' || short_code),
    null,
    null,
    crypt('Password123!', gen_salt('bf')),
    'OWNER',
    null,
    'ACTIVE',
    'Operations Manager',
    'LEAD',
    5,
    10,
    'Operations, Planning, Leadership, Review',
    now(),
    now()
from orgs
on conflict (id) do update set
    full_name = excluded.full_name,
    email = excluded.email,
    phone = excluded.phone,
    username = excluded.username,
    employee_code = excluded.employee_code,
    initial_password = excluded.initial_password,
    password_hash = excluded.password_hash,
    role = excluded.role,
    status = excluded.status,
    job_title = excluded.job_title,
    seniority_level = excluded.seniority_level,
    skill_rating = excluded.skill_rating,
    years_of_experience = excluded.years_of_experience,
    skills = excluded.skills,
    updated_at = excluded.updated_at;

with orgs as (
    select *
    from (values
        (1, 'SE'), (2, 'FE'), (3, 'BE'), (4, 'QA'), (5, 'UX'),
        (6, 'DA'), (7, 'MK'), (8, 'SA'), (9, 'CS'), (10, 'OP'),
        (11, 'PM'), (12, 'HR'), (13, 'FN'), (14, 'LG'), (15, 'PR'),
        (16, 'BD'), (17, 'IT'), (18, 'AI'), (19, 'ML'), (20, 'DS')
    ) as item(org_no, short_code)
)
update workspaces workspace
set owner_id = ('00000000-0000-0000-0002-' || lpad(orgs.org_no::text, 12, '0'))::uuid
from orgs
where workspace.id = ('00000000-0000-0000-0001-' || lpad(orgs.org_no::text, 12, '0'))::uuid;

with orgs as (
    select *
    from (values
        (1, 'SE'), (2, 'FE'), (3, 'BE'), (4, 'QA'), (5, 'UX'),
        (6, 'DA'), (7, 'MK'), (8, 'SA'), (9, 'CS'), (10, 'OP'),
        (11, 'PM'), (12, 'HR'), (13, 'FN'), (14, 'LG'), (15, 'PR'),
        (16, 'BD'), (17, 'IT'), (18, 'AI'), (19, 'ML'), (20, 'DS')
    ) as item(org_no, short_code)
),
employee_numbers as (
    select generate_series(1, 50) as employee_no
),
profiles as (
    select
        orgs.org_no,
        orgs.short_code,
        employee_numbers.employee_no,
        case ((employee_numbers.employee_no - 1) % 10)
            when 0 then 'Frontend Developer'
            when 1 then 'Backend Developer'
            when 2 then 'QA Engineer'
            when 3 then 'UI/UX Designer'
            when 4 then 'Data Analyst'
            when 5 then 'Marketing Specialist'
            when 6 then 'Sales Executive'
            when 7 then 'Customer Support'
            when 8 then 'Operations Coordinator'
            else 'Project Coordinator'
        end as job_title,
        case ((employee_numbers.employee_no - 1) % 5)
            when 0 then 'INTERN'
            when 1 then 'JUNIOR'
            when 2 then 'MIDDLE'
            when 3 then 'SENIOR'
            else 'LEAD'
        end as seniority_level,
        ((employee_numbers.employee_no - 1) % 5) + 1 as skill_rating,
        ((employee_numbers.employee_no + orgs.org_no) % 9) as years_of_experience
    from orgs
    cross join employee_numbers
)
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
    ('00000000-0000-0000-0003-' || lpad((org_no * 1000 + employee_no)::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0001-' || lpad(org_no::text, 12, '0'))::uuid,
    'Employee ' || lpad(org_no::text, 2, '0') || '-' || lpad(employee_no::text, 2, '0'),
    'employee' || lpad(org_no::text, 2, '0') || '.' || lpad(employee_no::text, 2, '0') || '@sample.forep.local',
    '091' || lpad((org_no * 100 + employee_no)::text, 7, '0'),
    lower('employee' || lpad(org_no::text, 2, '0') || lpad(employee_no::text, 2, '0') || short_code || lpad(employee_no::text, 4, '0')),
    short_code || lpad(employee_no::text, 4, '0'),
    short_code || lpad(employee_no::text, 4, '0'),
    crypt(short_code || lpad(employee_no::text, 4, '0'), gen_salt('bf')),
    'EMPLOYEE',
    null,
    'ACTIVE',
    job_title,
    seniority_level,
    skill_rating,
    years_of_experience,
    case job_title
        when 'Frontend Developer' then 'React, TypeScript, UI, CSS'
        when 'Backend Developer' then 'Java, Spring Boot, PostgreSQL, API'
        when 'QA Engineer' then 'Testing, Automation, Regression, Bug tracking'
        when 'UI/UX Designer' then 'Figma, UX research, Wireframe, Prototype'
        when 'Data Analyst' then 'SQL, Dashboard, Reporting, Excel'
        when 'Marketing Specialist' then 'Campaign, Content, SEO, Social media'
        when 'Sales Executive' then 'Sales, CRM, Proposal, Negotiation'
        when 'Customer Support' then 'Support, Ticket, Customer success, Communication'
        when 'Operations Coordinator' then 'Operations, Inventory, Process, Coordination'
        else 'Planning, Coordination, Documentation, Follow-up'
    end,
    now(),
    now()
from profiles
on conflict (id) do update set
    full_name = excluded.full_name,
    email = excluded.email,
    phone = excluded.phone,
    username = excluded.username,
    employee_code = excluded.employee_code,
    initial_password = excluded.initial_password,
    password_hash = excluded.password_hash,
    role = excluded.role,
    status = excluded.status,
    job_title = excluded.job_title,
    seniority_level = excluded.seniority_level,
    skill_rating = excluded.skill_rating,
    years_of_experience = excluded.years_of_experience,
    skills = excluded.skills,
    updated_at = excluded.updated_at;

commit;
