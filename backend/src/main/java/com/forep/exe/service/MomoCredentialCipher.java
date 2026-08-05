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
public class MomoCredentialCipher {
    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String encryptionKey;

    public MomoCredentialCipher(@Value("${forep.momo.config-encryption-key:}") String encryptionKey) {
        this.encryptionKey = encryptionKey == null ? "" : encryptionKey.trim();
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        requireConfiguredKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] packed = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return PREFIX + Base64.getEncoder().encodeToString(packed);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not encrypt MoMo credential.", exception);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return null;
        }
        if (!isEncrypted(ciphertext)) {
            throw new IllegalStateException("Legacy plaintext MoMo secret must be replaced by an administrator.");
        }
        requireConfiguredKey();
        try {
            byte[] packed = Base64.getDecoder().decode(ciphertext.substring(PREFIX.length()));
            if (packed.length <= IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted credential.");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[packed.length - IV_LENGTH];
            System.arraycopy(packed, 0, iv, 0, iv.length);
            System.arraycopy(packed, iv.length, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not decrypt MoMo credential.", exception);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    private SecretKeySpec key() throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(bytes, "AES");
    }

    private void requireConfiguredKey() {
        if (encryptionKey.length() < 32) {
            throw new IllegalStateException("MOMO_CONFIG_ENCRYPTION_KEY must contain at least 32 characters.");
        }
    }
}
