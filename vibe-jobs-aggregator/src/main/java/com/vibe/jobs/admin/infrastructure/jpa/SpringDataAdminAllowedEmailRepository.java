package com.vibe.jobs.admin.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAdminAllowedEmailRepository extends JpaRepository<AdminAllowedEmailEntity, String> {
}
