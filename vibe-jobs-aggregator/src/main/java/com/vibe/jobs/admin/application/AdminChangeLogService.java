package com.vibe.jobs.admin.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.admin.infrastructure.jpa.AdminChangeLogEntity;
import com.vibe.jobs.admin.infrastructure.jpa.SpringDataAdminChangeLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminChangeLogService {

    private static final Logger log = LoggerFactory.getLogger(AdminChangeLogService.class);

    private final SpringDataAdminChangeLogRepository repository;
    private final ObjectMapper objectMapper;

    public AdminChangeLogService(SpringDataAdminChangeLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void record(String actorEmail,
                       String action,
                       String resourceType,
                       String resourceId,
                       Object diff) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return;
        }
        AdminChangeLogEntity entity = new AdminChangeLogEntity();
        entity.setActorEmail(actorEmail);
        entity.setAction(action == null ? "UNKNOWN" : action);
        entity.setResourceType(resourceType == null ? "UNKNOWN" : resourceType);
        entity.setResourceId(resourceId);
        entity.setDiffJson(serialize(diff));
        repository.save(entity);
    }

    private String serialize(Object diff) {
        if (diff == null) {
            return null;
        }
        if (diff instanceof String str) {
            return str;
        }
        if (diff instanceof Map<?, ?> map && map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(diff);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize admin diff: {}", e.getMessage());
            return null;
        }
    }
}
