package com.vibe.jobs.admin.domain;

public interface AdminAllowedEmailRepository {

    boolean exists(String email);
}

