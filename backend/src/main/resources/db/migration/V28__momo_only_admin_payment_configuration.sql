alter table payment_qr_settings
    add column if not exists provider_endpoint text;

alter table payment_qr_settings
    add column if not exists partner_code varchar(120);

alter table payment_qr_settings
    add column if not exists access_key varchar(255);

alter table payment_qr_settings
    add column if not exists secret_key text;

alter table payment_qr_settings
    add column if not exists return_url text;

alter table payment_qr_settings
    add column if not exists notify_url text;

delete from payment_qr_settings
where payment_method = 'BANK_TRANSFER';

insert into payment_qr_settings (
    id, payment_method, enabled, created_at, updated_at
)
select '39000000-0000-0000-0000-000000000001', 'MOMO', false, current_timestamp, current_timestamp
where not exists (
    select 1 from payment_qr_settings where payment_method = 'MOMO'
);

update payment_qr_settings
set qr_code_url = null,
    qr_file_id = null,
    payment_url = null,
    deeplink = null,
    bank_code = null,
    bank_name = null,
    bank_account_number = null,
    bank_account_name = null
where payment_method = 'MOMO';

alter table payment_qr_settings
    drop constraint if exists payment_qr_settings_method_check;

alter table payment_qr_settings
    add constraint payment_qr_settings_method_check check (payment_method in ('MOMO'));
