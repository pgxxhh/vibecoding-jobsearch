package com.vibe.jobs.auth.domain.spi;

import com.vibe.jobs.auth.domain.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepositoryPort {
    UserAccount save(UserAccount userAccount);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(UUID id);
}
