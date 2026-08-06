package com.forep.exe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PayosPaymentServiceTest {
    private final PayosPaymentService service = new PayosPaymentService(new ObjectMapper());

    @Test
    void createSignatureUsesOfficialFieldOrderAndHmacSha256() {
        String raw = service.createRawSignature(123456789L, 100000L, "Thanh toan", "https://shop/return", "https://shop/cancel");
        assertEquals("amount=100000&cancelUrl=https://shop/cancel&description=Thanh toan&orderCode=123456789&returnUrl=https://shop/return", raw);
        assertEquals("c18f7ebe1978913288b4df2f1c267df52ebd58dc0a1ef1be6de5f11a5861eb88",
                service.hmacSha256(raw, "checksum"));
    }

    @Test
    void webhookSignatureSortsKeysAndRejectsWrongSignature() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentLinkId", "link-1");
        data.put("orderCode", 123L);
        data.put("amount", 3000L);
        data.put("code", "00");
        assertEquals("amount=3000&code=00&orderCode=123&paymentLinkId=link-1", service.webhookRawSignature(data));
        String signature = service.hmacSha256(service.webhookRawSignature(data), "checksum");
        assertTrue(service.verifyWebhook(data, signature, "checksum"));
        assertFalse(service.verifyWebhook(data, "bad", "checksum"));
    }

    @Test
    void providerBaseUrlIsNormalized() {
        assertEquals("https://api.payos.vn/v2/payment-requests", service.createEndpoint("https://api.payos.vn///"));
        assertEquals("https://api.payos.vn/v2/payment-requests", service.createEndpoint("https://api.payos.vn/v2/payment-requests"));
    }

    @Test
    void nonPositiveAmountIsRejectedBeforeNetworkCall() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createPayment(123, 0, "FOREP 123", List.of(), config("https://localhost")));
    }

    @Test
    void providerTimeoutDoesNotReturnCheckoutUrl() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(PayosPaymentService.CREATE_PATH, exchange -> {
            try {
                Thread.sleep(200);
                byte[] response = "{\"code\":\"00\",\"data\":{\"checkoutUrl\":\"https://pay\",\"paymentLinkId\":\"id\"}}".getBytes();
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally { exchange.close(); }
        });
        server.start();
        try {
            PayosPaymentService timeoutService = new PayosPaymentService(new ObjectMapper(), HttpClient.newHttpClient(), Duration.ofMillis(30));
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            var error = assertThrows(PayosPaymentService.PayosProviderException.class,
                    () -> timeoutService.createPayment(123, 1000, "FOREP 123", List.of(), config(baseUrl)));
            assertEquals("PayOS payment creation timed out.", error.getMessage());
        } finally { server.stop(0); }
    }

    private PayosPaymentService.PayosProviderConfig config(String baseUrl) {
        return new PayosPaymentService.PayosProviderConfig(baseUrl, "client", "api", "checksum",
                "https://merchant/return", "https://merchant/cancel");
    }
}
