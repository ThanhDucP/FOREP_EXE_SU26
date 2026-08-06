package com.forep.exe.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PayosCredentialCipherTest {
    private final PayosCredentialCipher cipher = new PayosCredentialCipher("0123456789abcdef0123456789abcdef");

    @Test
    void credentialsAreEncryptedAndDecryptable() {
        String encrypted = cipher.encrypt("payos-secret-value");
        assertTrue(encrypted.startsWith("enc:v1:"));
        assertFalse(encrypted.contains("payos-secret-value"));
        assertEquals("payos-secret-value", cipher.decrypt(encrypted));
    }
}
