package com.vibe.jobs.datasource.infrastructure.jpa;

import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSourceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JpaJobDataSourceRepository implements JobDataSourceRepository {

    private final SpringDataJobDataSourceRepository delegate;
    private final SpringDataJobDataSourceCompanyRepository companyRepository;
    private final SpringDataJobDataSourceCategoryRepository categoryRepository;

    public JpaJobDataSourceRepository(SpringDataJobDataSourceRepository delegate,
                                      SpringDataJobDataSourceCompanyRepository companyRepository,
                                      SpringDataJobDataSourceCategoryRepository categoryRepository) {
        this.delegate = delegate;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<JobDataSource> findAllEnabled() {
        return delegate.findAllEnabled().stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(JobDataSource::getCode))
                .collect(Collectors.toList());
    }

    @Override
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
        // Save the main entity first
        JobDataSourceEntity entity = toEntity(dataSource);
        JobDataSourceEntity saved = delegate.save(entity);

        // Delete existing companies and categories
        companyRepository.deleteByDataSourceCode(saved.getCode());
        categoryRepository.deleteByDataSourceCode(saved.getCode());

        // Save new companies
        for (JobDataSource.DataSourceCompany company : dataSource.getCompanies()) {
            JobDataSourceCompanyEntity companyEntity = new JobDataSourceCompanyEntity();
            companyEntity.setDataSourceCode(saved.getCode());
            companyEntity.setReference(company.reference());
            companyEntity.setDisplayName(company.displayName());
            companyEntity.setSlug(company.slug());
            companyEntity.setEnabled(company.enabled());
            companyEntity.setPlaceholderOverrides(new LinkedHashMap<>(company.placeholderOverrides()));
            companyEntity.setOverrideOptions(new LinkedHashMap<>(company.overrideOptions()));
            companyRepository.save(companyEntity);
        }

        // Save new categories
        for (JobDataSource.CategoryQuotaDefinition category : dataSource.getCategories()) {
            JobDataSourceCategoryEntity categoryEntity = new JobDataSourceCategoryEntity();
            categoryEntity.setDataSourceCode(saved.getCode());
            categoryEntity.setName(category.name());
            categoryEntity.setLimit(category.limit());
            categoryEntity.setTags(category.tags());
            categoryEntity.setFacets(category.facets());
            categoryRepository.save(categoryEntity);
        }

        return toDomain(saved);
    }

    @Override
    public boolean existsByCode(String code) {
        return delegate.existsByCode(code);
    }

    @Override
    public Optional<JobDataSource> findByCode(String code) {
        return delegate.findByCode(code).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        delegate.deleteById(id);
    }

    private JobDataSource toDomain(JobDataSourceEntity entity) {
        // Load companies and categories separately
        List<JobDataSourceCompanyEntity> companyEntities = companyRepository.findByDataSourceCodeOrderByReference(entity.getCode());
        List<JobDataSourceCategoryEntity> categoryEntities = categoryRepository.findByDataSourceCodeOrderByName(entity.getCode());

        List<JobDataSource.DataSourceCompany> companies = companyEntities.stream()
                .map(company -> new JobDataSource.DataSourceCompany(
                        company.getId(),
                        company.getReference(),
                        company.getDisplayName(),
                        company.getSlug(),
                        company.isEnabled(),
                        safeMap(company.getPlaceholderOverrides()),
                        safeMap(company.getOverrideOptions())
                ))
                .toList();

        List<JobDataSource.CategoryQuotaDefinition> categories = categoryEntities.stream()
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
