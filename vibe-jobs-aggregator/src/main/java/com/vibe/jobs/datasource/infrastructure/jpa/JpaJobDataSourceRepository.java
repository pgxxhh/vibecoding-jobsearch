package com.vibe.jobs.datasource.infrastructure.jpa;

import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSourceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JpaJobDataSourceRepository implements JobDataSourceRepository {

    private final SpringDataJobDataSourceRepository delegate;

    public JpaJobDataSourceRepository(SpringDataJobDataSourceRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public List<JobDataSource> findAllEnabled() {
        return delegate.findAllEnabled().stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(JobDataSource::getCode))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<JobDataSource> findAll() {
        return delegate.findAll().stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(JobDataSource::getCode))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<JobDataSource> findById(Long id) {
        return delegate.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public JobDataSource save(JobDataSource dataSource) {
        JobDataSourceEntity entity = toEntity(dataSource);
        JobDataSourceEntity saved = delegate.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void saveAll(Collection<JobDataSource> sources) {
        List<JobDataSourceEntity> entities = sources.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        delegate.saveAll(entities);
    }

    @Override
    public boolean existsAny() {
        return delegate.count() > 0;
    }

    private JobDataSource toDomain(JobDataSourceEntity entity) {
        List<JobDataSource.DataSourceCompany> companies = entity.getCompanies().stream()
                .map(company -> new JobDataSource.DataSourceCompany(
                        company.getId(),
                        company.getReference(),
                        company.getDisplayName(),
                        company.getSlug(),
                        company.isEnabled(),
                        safeMap(company.getPlaceholderOverrides()),
                        safeMap(company.getOverrideOptions())
                ))
                .sorted(Comparator.comparing(JobDataSource.DataSourceCompany::reference))
                .toList();

        List<JobDataSource.CategoryQuotaDefinition> categories = entity.getCategories().stream()
                .sorted(Comparator.comparing(JobDataSourceCategoryEntity::getName))
                .map(category -> new JobDataSource.CategoryQuotaDefinition(
                        category.getName(),
                        category.getLimit(),
                        safeList(category.getTags()),
                        safeMapList(category.getFacets())
                ))
                .toList();

        return new JobDataSource(
                entity.getId(),
                entity.getCode(),
                entity.getType(),
                entity.isEnabled(),
                entity.isRunOnStartup(),
                entity.isRequireOverride(),
                entity.getFlow(),
                safeMap(entity.getBaseOptions()),
                categories,
                companies
        );
    }

    private JobDataSourceEntity toEntity(JobDataSource dataSource) {
        JobDataSourceEntity entity = dataSource.getId() == null
                ? new JobDataSourceEntity()
                : delegate.findById(dataSource.getId()).orElse(new JobDataSourceEntity());
        entity.setId(dataSource.getId());
        entity.setCode(dataSource.getCode());
        entity.setType(dataSource.getType());
        entity.setEnabled(dataSource.isEnabled());
        entity.setRunOnStartup(dataSource.isRunOnStartup());
        entity.setRequireOverride(dataSource.isRequireOverride());
        entity.setFlow(dataSource.getFlow());
        entity.setBaseOptions(new LinkedHashMap<>(dataSource.getBaseOptions()));

        entity.getCompanies().clear();
        for (JobDataSource.DataSourceCompany company : dataSource.getCompanies()) {
            JobDataSourceCompanyEntity companyEntity = new JobDataSourceCompanyEntity();
            companyEntity.setId(null);
            companyEntity.setDataSource(entity);
            companyEntity.setReference(company.reference());
            companyEntity.setDisplayName(company.displayName());
            companyEntity.setSlug(company.slug());
            companyEntity.setEnabled(company.enabled());
            companyEntity.setPlaceholderOverrides(new LinkedHashMap<>(company.placeholderOverrides()));
            companyEntity.setOverrideOptions(new LinkedHashMap<>(company.overrideOptions()));
            entity.getCompanies().add(companyEntity);
        }

        entity.getCategories().clear();
        for (JobDataSource.CategoryQuotaDefinition category : dataSource.getCategories()) {
            JobDataSourceCategoryEntity categoryEntity = new JobDataSourceCategoryEntity();
            categoryEntity.setId(null);
            categoryEntity.setDataSource(entity);
            categoryEntity.setName(category.name());
            categoryEntity.setLimit(category.limit());
            categoryEntity.setTags(category.tags());
            categoryEntity.setFacets(category.facets());
            entity.getCategories().add(categoryEntity);
        }

        return entity;
    }

    private Map<String, String> safeMap(Map<String, String> source) {
        return source == null ? Map.of() : new LinkedHashMap<>(source);
    }

    private Map<String, List<String>> safeMapList(Map<String, List<String>> source) {
        if (source == null) {
            return Map.of();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, value == null ? List.of() : List.copyOf(value)));
        return copy;
    }

    private List<String> safeList(List<String> source) {
        return source == null ? List.of() : List.copyOf(source);
    }
}
