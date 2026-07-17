alter table job_positions drop constraint if exists job_positions_permission_group_check;
alter table job_positions add constraint job_positions_permission_group_check
    check (permission_group in ('EMPLOYEE','MANAGER','EXECUTIVE'));

alter table job_positions drop constraint if exists job_positions_department_required_check;
alter table job_positions add constraint job_positions_department_required_check
    check (department_id is not null) not valid;

comment on table job_positions is 'Workspace-scoped business/job positions. Not system authorization roles.';
comment on column job_positions.permission_group is 'Fixed workspace permission group: EMPLOYEE, MANAGER, or EXECUTIVE. HR cannot create PLATFORM_ADMIN or BUSINESS_OWNER here.';
