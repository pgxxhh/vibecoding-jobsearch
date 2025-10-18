package com.vibe.jobs.shared.infrastructure.config;

import com.vibe.jobs.shared.infrastructure.security.AesEncryptionService;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.nio.file.Path;

@Slf4j
@Configuration
@Profile("prod")
@EnableConfigurationProperties(DbCredentialEncryptionProperties.class)
public class ProdDataSourceConfig {

    private final DataSourceProperties dataSourceProperties;
    private final DbCredentialEncryptionProperties encryptionProperties;

    public ProdDataSourceConfig(DataSourceProperties dataSourceProperties,
                                DbCredentialEncryptionProperties encryptionProperties) {
        this.dataSourceProperties = dataSourceProperties;
        this.encryptionProperties = encryptionProperties;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        var builder = dataSourceProperties.initializeDataSourceBuilder();
        builder.type(HikariDataSource.class);

        if (encryptionProperties.isEnabled()) {
            if (!StringUtils.hasText(encryptionProperties.getKeyPath())) {
                throw new IllegalStateException("db.credentials.encryption.key-path must be configured in production");
            }
            AesEncryptionService aes = new AesEncryptionService(
                    AesEncryptionService.loadKey(Path.of(encryptionProperties.getKeyPath())));
            String username = decryptIfNecessary(dataSourceProperties.getUsername(), aes);
            String password = decryptIfNecessary(dataSourceProperties.getPassword(), aes);
            builder.username(username);
            builder.password(password);
        }

        return builder.build();
    }

    private String decryptIfNecessary(String value, AesEncryptionService aes) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (AesEncryptionService.isWrappedEncryptedValue(value)) {
            String cipherText = AesEncryptionService.unwrapEncryptedValue(value);
            return aes.decrypt(cipherText);
        }
        return value;
    }
}
