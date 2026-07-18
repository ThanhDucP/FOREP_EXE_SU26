create table if not exists payment_qr_settings (
    id uuid primary key,
    payment_method varchar(30) not null,
    qr_code_url text not null,
    payment_url text,
    deeplink text,
    bank_code varchar(50),
    bank_name varchar(255),
    bank_account_number varchar(120),
    bank_account_name varchar(255),
    transfer_content_prefix text,
    enabled boolean not null default false,
    updated_by uuid references users(id),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint payment_qr_settings_method_unique unique (payment_method),
    constraint payment_qr_settings_method_check check (payment_method in ('MOMO','BANK_TRANSFER')),
    constraint payment_qr_settings_qr_required check (length(trim(qr_code_url)) > 0)
);

create index if not exists idx_payment_qr_settings_method_enabled
    on payment_qr_settings(payment_method, enabled);
