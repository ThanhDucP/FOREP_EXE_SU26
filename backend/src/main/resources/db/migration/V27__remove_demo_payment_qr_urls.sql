update payment_transactions
set provider_payment_url = null,
    provider_deeplink = null,
    provider_qr_code_url = null,
    qr_display_data = null,
    raw_provider_response = '{"seed":true,"normalized":true,"reason":"URL-based demo payment data removed; bank QR must come from admin upload and MoMo data from real provider callback."}'
where raw_provider_request = '{"seed":true}'
  and (
      provider_payment_url is not null
      or provider_deeplink is not null
      or provider_qr_code_url is not null
      or qr_display_data is not null
  );
