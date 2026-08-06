package com.forep.exe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PayosCredentialCipher {
    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private final SecureRandom random = new SecureRandom();
    private final String encryptionKey;

    public PayosCredentialCipher(@Value("${forep.payos.config-encryption-key:}") String encryptionKey) {
        this.encryptionKey = encryptionKey == null ? "" : encryptionKey.trim();
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return null;
        requireKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not encrypt PayOS credential.", exception);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) return null;
        if (!ciphertext.startsWith(PREFIX)) {
            throw new IllegalStateException("Plaintext PayOS credential must be replaced by an administrator.");
        }
        requireKey();
        try {
            byte[] packed = Base64.getDecoder().decode(ciphertext.substring(PREFIX.length()));
            if (packed.length <= IV_LENGTH) throw new IllegalArgumentException("Invalid encrypted credential.");
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[packed.length - IV_LENGTH];
            System.arraycopy(packed, 0, iv, 0, iv.length);
            System.arraycopy(packed, iv.length, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not decrypt PayOS credential.", exception);
        }
    }

    private SecretKeySpec key() throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(bytes, "AES");
    }

    private void requireKey() {
        if (encryptionKey.length() < 32) {
            throw new IllegalStateException("PAYOS_CONFIG_ENCRYPTION_KEY must contain at least 32 characters.");
        }
    }
}
