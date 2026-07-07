alter table workspace_registrations
    add column registration_token varchar(160);

alter table workspace_registrations
    add column expired_at timestamp with time zone;

update workspace_registrations
set registration_token = replace(cast(id as varchar), '-', '') || 'legacy'
where registration_token is null;

alter table workspace_registrations
    alter column registration_token set not null;

alter table workspace_registrations
    add constraint workspace_registrations_token_unique unique (registration_token);

alter table payment_transactions
    add column payment_code varchar(80);

update payment_transactions
set payment_code = order_code
where payment_code is null;

alter table payment_transactions
    alter column payment_code set not null;

alter table payment_transactions
    add constraint payment_transactions_payment_code_unique unique (payment_code);

create index idx_workspace_registrations_token on workspace_registrations(registration_token);
create index idx_payment_transactions_payment_code on payment_transactions(payment_code);

alter table payment_transactions
    drop constraint if exists payment_transactions_status_valid;

alter table payment_transactions
    add constraint payment_transactions_status_valid check (status in ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED', 'REFUNDED', 'MANUAL_REVIEW'));
