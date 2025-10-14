package com.vibe.jobs.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class JobDetailRepositoryImpl implements JobDetailRepositoryCustom {

    private final EntityManager entityManager;
    private final boolean supportsFullText;

    public JobDetailRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.supportsFullText = detectFullTextSupport(entityManager);
    }

    @Override
    public Set<Long> findMatchingJobIds(Collection<Long> jobIds, String query) {
        if (jobIds == null || jobIds.isEmpty()) {
            return Set.of();
        }
        String normalizedQuery = normalize(query);
        if (normalizedQuery == null) {
            return Set.of();
        }
        List<Long> distinctIds = jobIds.stream()
                .filter(Objects::nonNull)
                .map(Long::longValue)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            return Set.of();
        }

        Query nativeQuery = entityManager.createNativeQuery(buildSql());
        nativeQuery.setParameter("jobIds", distinctIds);
        if (supportsFullText) {
            nativeQuery.setParameter("fulltextQuery", buildFullTextQuery(normalizedQuery));
        } else {
            nativeQuery.setParameter("fallbackDetailQuery", normalizedQuery);
        }

        @SuppressWarnings("unchecked")
        List<Number> result = nativeQuery.getResultList();
        return result.stream()
                .filter(Objects::nonNull)
                .map(Number::longValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String buildSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("select distinct jd.job_id from job_details jd ");
        sql.append("where jd.deleted = false ");
        sql.append("and jd.job_id in (:jobIds) ");
        sql.append("and jd.content_text is not null ");
        if (supportsFullText) {
            sql.append("and MATCH(jd.content_text) AGAINST (:fulltextQuery IN BOOLEAN MODE)");
        } else {
            sql.append("and lower(jd.content_text) like lower(concat('%', :fallbackDetailQuery, '%'))");
        }
        return sql.toString();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String buildFullTextQuery(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String[] tokens = value.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            String cleaned = token.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "");
            if (cleaned.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append('+').append(cleaned).append('*');
        }
        return builder.length() == 0 ? value : builder.toString();
    }

    private boolean detectFullTextSupport(EntityManager entityManager) {
        try {
            SessionFactoryImplementor sessionFactory = entityManager.getEntityManagerFactory()
                    .unwrap(SessionFactoryImplementor.class);
            Dialect dialect = sessionFactory.getJdbcServices().getDialect();
            return dialect instanceof MySQLDialect
                    || dialect.getClass().getName().toLowerCase(Locale.ROOT).contains("maria");
        } catch (Exception ex) {
            return false;
        }
    }
}

