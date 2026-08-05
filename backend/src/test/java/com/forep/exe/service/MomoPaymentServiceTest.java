package com.forep.exe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MomoPaymentServiceTest {
    private final MomoPaymentService service = new MomoPaymentService(new ObjectMapper());

    @Test
    void createRawSignatureUsesMomoFixedFieldOrderAndHmacSha256() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("amount", "1000");
        request.put("extraData", "");
        request.put("ipnUrl", "https://merchant.test/ipn");
        request.put("orderId", "ORDER-1");
        request.put("orderInfo", "FOREP");
        request.put("partnerCode", "PARTNER");
        request.put("redirectUrl", "https://merchant.test/return");
        request.put("requestId", "REQUEST-1");
        request.put("requestType", "captureWallet");

        String raw = service.createRawSignature(request, "access");

        assertEquals("accessKey=access&amount=1000&extraData=&ipnUrl=https://merchant.test/ipn&orderId=ORDER-1&orderInfo=FOREP&partnerCode=PARTNER&redirectUrl=https://merchant.test/return&requestId=REQUEST-1&requestType=captureWallet", raw);
        assertEquals("a5a8cfd64ce5c185821051b0518fe096ccbbdb7d2c9d9fe745b773f9b44df5e1",
                service.hmacSha256(raw, "secret"));
    }

    @Test
    void wrongIpnSignatureIsRejected() {
        Map<String, Object> payload = validIpn();
        var config = config("https://test-payment.momo.vn");

        assertFalse(service.verifyIpnSignature(payload, "not-a-valid-signature", config));
    }

    @Test
    void providerBaseUrlIsNormalizedForCreateAndQuery() {
        assertEquals("https://test-payment.momo.vn/v2/gateway/api/create",
                service.createEndpoint("https://test-payment.momo.vn///"));
        assertEquals("https://payment.momo.vn/v2/gateway/api/query",
                service.queryEndpoint("https://payment.momo.vn/v2/gateway/api/create"));
    }

    @Test
    void nonPositiveOrFractionalAmountIsRejectedBeforeProviderCall() {
        PaymentTransactionEntity payment = payment(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> service.createPayment(payment, config("https://localhost")));

        payment.setAmount(new BigDecimal("1000.50"));
        assertThrows(IllegalArgumentException.class, () -> service.createPayment(payment, config("https://localhost")));
    }

    @Test
    void providerTimeoutIsReportedWithoutReturningPaymentUrl() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(MomoPaymentService.CREATE_PATH, exchange -> {
            try {
                Thread.sleep(200);
                byte[] response = "{\"resultCode\":0,\"payUrl\":\"https://pay.test\"}".getBytes();
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            MomoPaymentService timeoutService = new MomoPaymentService(
                    new ObjectMapper(), HttpClient.newHttpClient(), Duration.ofMillis(30));
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

            MomoPaymentService.MomoProviderException error = assertThrows(
                    MomoPaymentService.MomoProviderException.class,
                    () -> timeoutService.createPayment(payment(BigDecimal.valueOf(1000)), config(baseUrl)));

            assertEquals("MoMo payment creation timed out.", error.getMessage());
        } finally {
            server.stop(0);
        }
    }

    private Map<String, Object> validIpn() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", "PARTNER");
        payload.put("orderId", "ORDER-1");
        payload.put("requestId", "REQUEST-1");
        payload.put("amount", 1000L);
        payload.put("orderInfo", "FOREP");
        payload.put("orderType", "momo_wallet");
        payload.put("transId", 123L);
        payload.put("resultCode", 0);
        payload.put("message", "Successful.");
        payload.put("payType", "qr");
        payload.put("responseTime", 1721720663942L);
        payload.put("extraData", "");
        return payload;
    }

    private PaymentTransactionEntity payment(BigDecimal amount) {
        PaymentTransactionEntity payment = new PaymentTransactionEntity();
        payment.setAmount(amount);
        payment.setOrderCode("ORDER-1");
        payment.setRequestId("REQUEST-1");
        return payment;
    }

    private MomoPaymentService.MomoProviderConfig config(String baseUrl) {
        return new MomoPaymentService.MomoProviderConfig(baseUrl, "PARTNER", "access", "secret",
                "https://merchant.test/return", "https://merchant.test/ipn");
    }
}
