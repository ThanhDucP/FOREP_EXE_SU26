alter table workspace_registrations
    add column owner_full_name varchar(255);

alter table workspace_registrations
    add column owner_email varchar(255);

alter table workspace_registrations
    add column owner_phone varchar(50);

alter table workspace_registrations
    add column owner_password_hash varchar(255);

alter table workspace_registrations
    add constraint workspace_registrations_owner_required check (
        owner_full_name is not null
        and owner_email is not null
        and owner_password_hash is not null
    );
