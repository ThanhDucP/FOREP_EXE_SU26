alter table users drop constraint if exists users_role_check;
alter table users add constraint users_role_check
    check (role in ('PLATFORM_ADMIN','BUSINESS_OWNER','HR','EXECUTIVE','MANAGER','EMPLOYEE','SYSTEM','SYSTEM_ADMIN','OWNER'));
