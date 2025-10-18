package com.vibe.jobs.admin.domain;

public interface AdminChangeLogRepository {

    void save(AdminChangeLogEntry entry);
}

