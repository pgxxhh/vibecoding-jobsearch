package com.vibe.jobs.admin.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAdminChangeLogRepository extends JpaRepository<AdminChangeLogEntity, Long> {
}

