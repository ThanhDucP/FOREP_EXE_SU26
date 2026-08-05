package com.forep.exe.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MomoCredentialCipherTest {
    private final MomoCredentialCipher cipher = new MomoCredentialCipher("0123456789abcdef0123456789abcdef");

    @Test
    void secretIsEncryptedAtRestAndCanBeDecrypted() {
        String encrypted = cipher.encrypt("momo-secret-value");

        assertTrue(encrypted.startsWith("enc:v1:"));
        assertFalse(encrypted.contains("momo-secret-value"));
        assertEquals("momo-secret-value", cipher.decrypt(encrypted));
    }
}
