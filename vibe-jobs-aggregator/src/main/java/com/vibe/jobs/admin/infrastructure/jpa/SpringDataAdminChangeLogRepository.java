package com.vibe.jobs.admin.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAdminChangeLogRepository extends JpaRepository<AdminChangeLogEntity, Long> {
}
