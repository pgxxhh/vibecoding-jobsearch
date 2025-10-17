package com.vibe.jobs.admin.infrastructure.jpa;

import com.vibe.jobs.admin.domain.AdminAllowedEmailRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAdminAllowedEmailRepository implements AdminAllowedEmailRepository {

    private final ObjectProvider<SpringDataAdminAllowedEmailRepository> delegateProvider;

    public JpaAdminAllowedEmailRepository(ObjectProvider<SpringDataAdminAllowedEmailRepository> delegateProvider) {
        this.delegateProvider = delegateProvider;
    }

    @Override
    public boolean exists(String email) {
        SpringDataAdminAllowedEmailRepository delegate = delegateProvider.getIfAvailable();
        if (delegate == null) {
            return false;
        }
        return delegate.existsById(email);
    }
}
