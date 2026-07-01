alter table workspaces
    add column short_code varchar(2);

alter table workspaces
    add column next_employee_number integer not null default 1;

alter table workspaces
    add constraint workspaces_short_code_unique unique (short_code);

alter table workspaces
    add constraint workspaces_short_code_format check (short_code is null or (char_length(short_code) = 2 and short_code = upper(short_code)));

alter table workspaces
    add constraint workspaces_next_employee_number_range check (next_employee_number between 1 and 1001);

alter table users
    add column username varchar(120);

alter table users
    add column employee_code varchar(6);

alter table users
    add column initial_password varchar(20);

alter table users
    add constraint users_username_unique unique (username);

alter table users
    add constraint users_employee_code_unique unique (employee_code);

alter table users
    add constraint users_employee_code_format check (employee_code is null or (char_length(employee_code) = 6 and employee_code = upper(employee_code)));
