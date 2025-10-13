package com.vibe.jobs.security;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CredentialEncryptionTool {

    private static final Logger log = LoggerFactory.getLogger(CredentialEncryptionTool.class);

    private CredentialEncryptionTool() {
    }

    public static void main(String[] args) {
        if (args.length == 0 || "--help".equals(args[0])) {
            printUsage();
            return;
        }

        try {
            switch (args[0]) {
                case "generate-key" -> generateKey(args);
                case "encrypt" -> encrypt(args);
                case "decrypt" -> decrypt(args);
                default -> {
                    log.error("Unknown command: {}", args[0]);
                    printUsage();
                }
            }
        } catch (Exception ex) {
            log.error("Command failed", ex);
        }
    }

    private static void generateKey(String[] args) {
        int keySize = 256;
        if (args.length > 1) {
            keySize = Integer.parseInt(args[1]);
        }
        log.info(AesEncryptionService.generateKeyBase64(keySize));
    }

    private static void encrypt(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: encrypt <key-path> <plaintext>");
        }
        Path keyPath = Path.of(args[1]);
        String plaintext = args[2];
        AesEncryptionService aes = new AesEncryptionService(AesEncryptionService.loadKey(keyPath));
        log.info(AesEncryptionService.wrapEncryptedValue(aes.encrypt(plaintext)));
    }

    private static void decrypt(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: decrypt <key-path> <ciphertext>");
        }
        Path keyPath = Path.of(args[1]);
        String cipherText = args[2];
        if (AesEncryptionService.isWrappedEncryptedValue(cipherText)) {
            cipherText = AesEncryptionService.unwrapEncryptedValue(cipherText);
        }
        AesEncryptionService aes = new AesEncryptionService(AesEncryptionService.loadKey(keyPath));
        log.info(aes.decrypt(cipherText));
    }

    private static void printUsage() {
        log.info("Usage: <command> [args]\n" +
                "Commands:\n" +
                "  generate-key [key-size]   Generates a new Base64 encoded AES key (default 256 bits).\n" +
                "  encrypt <key-path> <plaintext>  Encrypts the provided plaintext using the key.\n" +
                "  decrypt <key-path> <ciphertext> Decrypts the provided ciphertext using the key.\n");
    }
}

