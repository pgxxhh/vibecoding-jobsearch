package com.vibe.jobs.shared.infrastructure.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Helper for encrypting and decrypting short secrets using AES/GCM.
 * <p>
 * The encrypted payload is encoded using Base64 and contains the IV followed by the ciphertext.
 */
public class AesEncryptionService {

    public static final String ENC_PREFIX = "ENC(";
    public static final String ENC_SUFFIX = ")";

    private static final String DEFAULT_ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes, recommended for GCM

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public AesEncryptionService(SecretKey secretKey) {
        this(secretKey, new SecureRandom());
    }

    public AesEncryptionService(SecretKey secretKey, SecureRandom secureRandom) {
        this.secretKey = secretKey;
        this.secureRandom = secureRandom;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to encrypt value", ex);
        }
    }

    public String decrypt(String encodedCipherText) {
        if (encodedCipherText == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encodedCipherText);
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to decrypt value", ex);
        }
    }

    public static SecretKey loadKey(Path keyPath) {
        try {
            byte[] encoded = Files.readAllBytes(keyPath);
            byte[] keyBytes = Base64.getDecoder().decode(new String(encoded).trim());
            return new javax.crypto.spec.SecretKeySpec(keyBytes, DEFAULT_ALGORITHM);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load AES key from " + keyPath, ex);
        }
    }

    public static String generateKeyBase64(int keySize) {
        try {
            KeyGenerator generator = KeyGenerator.getInstance(DEFAULT_ALGORITHM);
            generator.init(keySize);
            SecretKey key = generator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to generate AES key", ex);
        }
    }

    public static boolean isWrappedEncryptedValue(String value) {
        return value != null && value.startsWith(ENC_PREFIX) && value.endsWith(ENC_SUFFIX);
    }

    public static String wrapEncryptedValue(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        return ENC_PREFIX + cipherText + ENC_SUFFIX;
    }

    public static String unwrapEncryptedValue(String wrappedCipherText) {
        if (!isWrappedEncryptedValue(wrappedCipherText)) {
            throw new IllegalArgumentException("Value is not wrapped with ENC(...) prefix/suffix");
        }
        return wrappedCipherText.substring(ENC_PREFIX.length(), wrappedCipherText.length() - ENC_SUFFIX.length());
    }
}

