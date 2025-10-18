package com.vibe.jobs.admin.infrastructure.jpa;

import com.vibe.jobs.admin.domain.AdminChangeLogEntry;
import com.vibe.jobs.admin.domain.AdminChangeLogRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAdminChangeLogRepository implements AdminChangeLogRepository {

    private final ObjectProvider<SpringDataAdminChangeLogRepository> delegateProvider;

    public JpaAdminChangeLogRepository(ObjectProvider<SpringDataAdminChangeLogRepository> delegateProvider) {
        this.delegateProvider = delegateProvider;
    }

    @Override
    public void save(AdminChangeLogEntry entry) {
        SpringDataAdminChangeLogRepository delegate = delegateProvider.getIfAvailable();
        if (delegate == null) {
            return;
        }
        AdminChangeLogEntity entity = new AdminChangeLogEntity();
        entity.setActorEmail(entry.actorEmail());
        entity.setAction(entry.action());
        entity.setResourceType(entry.resourceType());
        entity.setResourceId(entry.resourceId());
        entity.setDiffJson(entry.diffJson());
        delegate.save(entity);
    }
}
