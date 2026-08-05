-- A MoMo transId is the external receipt identifier. It must never pay for two
-- different FOREP invoices.
create unique index if not exists ux_payment_transactions_momo_trans_id
    on payment_transactions(provider_transaction_id)
    where provider_transaction_id is not null;

create index if not exists idx_workspace_registrations_workspace_lookup
    on workspace_registrations(workspace_id)
    where workspace_id is not null;
