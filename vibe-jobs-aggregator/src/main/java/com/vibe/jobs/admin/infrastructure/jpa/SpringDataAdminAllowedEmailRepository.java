package com.vibe.jobs.admin.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAdminAllowedEmailRepository extends JpaRepository<AdminAllowedEmailEntity, String> {
}

