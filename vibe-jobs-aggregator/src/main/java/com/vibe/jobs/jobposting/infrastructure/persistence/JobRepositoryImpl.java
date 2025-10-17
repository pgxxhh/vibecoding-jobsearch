package com.vibe.jobs.jobposting.infrastructure.persistence;

import com.vibe.jobs.jobposting.domain.Job;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JobRepositoryImpl implements JobRepositoryCustom {

    private final EntityManager entityManager;
    private final boolean supportsFullText;

    public JobRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.supportsFullText = detectFullTextSupport(entityManager);
    }

    @Override
    public List<Job> searchAfter(String q,
                                 String company,
                                 String location,
                                 String level,
                                 Instant postedAfter,
                                 Instant cursorPostedAt,
                                 Long cursorId,
                                 boolean searchDetail,
                                 Pageable pageable) {
        String normalizedQuery = normalize(q);
        boolean hasQuery = normalizedQuery != null;
        boolean detailEnabled = searchDetail && hasQuery;
        boolean hasCursor = cursorPostedAt != null && cursorId != null;
        Query query = entityManager.createNativeQuery(
                buildSearchSql(false, detailEnabled, hasCursor, hasQuery), Job.class);
        Map<String, Object> params = new HashMap<>();
        String fullTextQuery = supportsFullText && hasQuery ? buildFullTextQuery(normalizedQuery) : null;
        populateCommonParameters(params, normalizedQuery, fullTextQuery, company, location, level, postedAfter, detailEnabled);
        if (hasCursor) {
            params.put("cursorPostedAt", Timestamp.from(cursorPostedAt));
            params.put("cursorId", cursorId);
        }
        applyParameters(query, params);
        if (pageable != null) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
        @SuppressWarnings("unchecked")
        List<Job> jobs = query.getResultList();
        return jobs;
    }

    @Override
    public long countSearch(String q,
                            String company,
                            String location,
                            String level,
                            Instant postedAfter,
                            boolean searchDetail) {
        String normalizedQuery = normalize(q);
        boolean hasQuery = normalizedQuery != null;
        boolean detailEnabled = searchDetail && hasQuery;
        Query query = entityManager.createNativeQuery(buildSearchSql(true, detailEnabled, false, hasQuery));
        Map<String, Object> params = new HashMap<>();
        String fullTextQuery = supportsFullText && hasQuery ? buildFullTextQuery(normalizedQuery) : null;
        populateCommonParameters(params, normalizedQuery, fullTextQuery, company, location, level, postedAfter, detailEnabled);
        applyParameters(query, params);
        Number result = (Number) query.getSingleResult();
        return result.longValue();
    }

    private void populateCommonParameters(Map<String, Object> params,
                                          String normalizedQuery,
                                          String fullTextQuery,
                                          String company,
                                          String location,
                                          String level,
                                          Instant postedAfter,
                                          boolean detailEnabled) {
        if (normalizedQuery != null) {
            params.put("q", normalizedQuery);
        }
        if (fullTextQuery != null) {
            params.put("mainFullTextQuery", fullTextQuery);
            if (detailEnabled) {
                params.put("detailFullTextQuery", fullTextQuery);
            }
        }
        params.put("company", normalize(company));
        params.put("location", normalize(location));
        params.put("level", normalize(level));
        params.put("postedAfter", postedAfter != null ? Timestamp.from(postedAfter) : null);
        if (!supportsFullText && detailEnabled && normalizedQuery != null) {
            params.put("fallbackDetailQuery", normalizedQuery);
        }
    }

    private void applyParameters(Query query, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private String buildSearchSql(boolean count, boolean detailEnabled, boolean includeCursor, boolean hasQuery) {
        StringBuilder sql = new StringBuilder();
        if (count) {
            sql.append("select count(*) from (");
        }
        sql.append("select j.* from jobs j ");
        if (detailEnabled) {
            sql.append("left join job_details jd on jd.job_id = j.id and jd.deleted = false ");
        }
        sql.append("where j.deleted = false ");
        if (hasQuery) {
            sql.append("and (");
            boolean hasPreviousClause = false;
            if (supportsFullText) {
                hasPreviousClause = appendOrClause(sql, hasPreviousClause,
                        "MATCH(j.title, j.company, j.location) AGAINST (:mainFullTextQuery IN BOOLEAN MODE)");
            } else {
                hasPreviousClause = appendOrClause(sql, hasPreviousClause,
                        "lower(j.title) like lower(concat('%', :q, '%'))");
                hasPreviousClause = appendOrClause(sql, hasPreviousClause,
                        "lower(j.company) like lower(concat('%', :q, '%'))");
                hasPreviousClause = appendOrClause(sql, hasPreviousClause,
                        "lower(j.location) like lower(concat('%', :q, '%'))");
            }
            hasPreviousClause = appendOrClause(sql, hasPreviousClause,
                    "exists (select 1 from job_tags jt where jt.job_id = j.id and lower(jt.tag) like lower(concat('%', :q, '%')))");
            if (detailEnabled) {
                if (supportsFullText) {
                    hasPreviousClause = appendOrClause(sql, hasPreviousClause,
                            "(jd.content_text is not null and MATCH(jd.content_text) AGAINST (:detailFullTextQuery IN BOOLEAN MODE))");
                } else {
                    hasPreviousClause = appendOrClause(sql, hasPreviousClause,
                            "(jd.content_text is not null and lower(jd.content_text) like lower(concat('%', :fallbackDetailQuery, '%')))");
                }
            }
            sql.append(") ");
        }
        sql.append("and (:company is null or lower(j.company) like lower(concat('%', :company, '%'))) ");
        sql.append("and (:location is null or lower(j.location) like lower(concat('%', :location, '%'))) ");
        sql.append("and (:level is null or lower(j.level) = lower(:level)) ");
        sql.append("and (:postedAfter is null or j.posted_at >= :postedAfter) ");
        if (!count && includeCursor) {
            sql.append("and (j.posted_at < :cursorPostedAt or (j.posted_at = :cursorPostedAt and j.id < :cursorId)) ");
        }
        if (!count) {
            sql.append("order by j.posted_at desc, j.id desc");
        } else {
            sql.append(") as count_query");
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

    private boolean appendOrClause(StringBuilder sql, boolean hasPreviousClause, String clause) {
        if (hasPreviousClause) {
            sql.append(" or ");
        }
        sql.append(clause);
        return true;
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
