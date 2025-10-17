package com.vibe.jobs.admin.application;

import com.vibe.jobs.admin.domain.AdminAllowedEmailRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminAccessService {

    private final AdminAllowedEmailRepository repository;

    public AdminAccessService(AdminAllowedEmailRepository repository) {
        this.repository = repository;
    }

    public boolean isAllowed(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return repository.exists(email.trim().toLowerCase());
    }
}
