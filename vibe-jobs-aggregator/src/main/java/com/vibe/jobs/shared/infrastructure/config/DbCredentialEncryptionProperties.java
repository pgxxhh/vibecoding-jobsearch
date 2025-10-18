package com.vibe.jobs.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "db.credentials.encryption")
public class DbCredentialEncryptionProperties {

    /**
     * Enables AES decryption for database credentials.
     */
    private boolean enabled = false;

    /**
     * Path to the Base64 encoded AES key used for decrypting credentials.
     */
    private String keyPath;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }
}

