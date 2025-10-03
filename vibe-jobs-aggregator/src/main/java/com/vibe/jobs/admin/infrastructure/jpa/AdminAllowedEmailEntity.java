package com.vibe.jobs.admin.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_allowed_email")
public class AdminAllowedEmailEntity {

    @Id
    @Column(name = "email", nullable = false)
    private String email;

    public AdminAllowedEmailEntity() {
    }

    public AdminAllowedEmailEntity(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
