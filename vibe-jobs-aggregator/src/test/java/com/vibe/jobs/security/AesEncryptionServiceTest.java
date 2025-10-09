package com.vibe.jobs.security;

import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesEncryptionServiceTest {

    @Test
    void encryptAndDecryptRoundTrip() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        SecretKey key = generator.generateKey();
        AesEncryptionService service = new AesEncryptionService(key);

        String ciphertext = service.encrypt("super-secret");
        String plaintext = service.decrypt(ciphertext);

        assertThat(plaintext).isEqualTo("super-secret");
    }

    @Test
    void wrapsAndUnwrapsEncryptedValues() {
        String wrapped = AesEncryptionService.wrapEncryptedValue("abc123");

        assertThat(AesEncryptionService.isWrappedEncryptedValue(wrapped)).isTrue();
        assertThat(AesEncryptionService.unwrapEncryptedValue(wrapped)).isEqualTo("abc123");
    }

    @Test
    void rejectsInvalidWrappedValues() {
        assertThat(AesEncryptionService.isWrappedEncryptedValue("abc123")).isFalse();
        assertThatThrownBy(() -> AesEncryptionService.unwrapEncryptedValue("abc123"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

