package com.vibe.jobs.admin.application;

import com.vibe.jobs.admin.infrastructure.jpa.SpringDataAdminAllowedEmailRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminAccessService {

    private final SpringDataAdminAllowedEmailRepository repository;

    public AdminAccessService(SpringDataAdminAllowedEmailRepository repository) {
        this.repository = repository;
    }

    public boolean isAllowed(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return repository.existsById(email.trim().toLowerCase());
    }
}
