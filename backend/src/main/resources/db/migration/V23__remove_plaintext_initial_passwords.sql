update users set initial_password = null where initial_password is not null;
alter table users drop column if exists initial_password;
