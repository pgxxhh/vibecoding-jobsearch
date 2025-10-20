package com.vibe.jobs.datasource.infrastructure.jpa;

import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSourceRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class JpaJobDataSourceRepository implements JobDataSourceRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaJobDataSourceRepository.class);

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
        return delegate.findAllNotDeleted().stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(JobDataSource::getCode))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<JobDataSource> findById(Long id) {
        return delegate.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<JobDataSource> findByIdIncludingDeleted(Long id) {
        return delegate.findByIdIncludingDeleted(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public JobDataSource save(JobDataSource dataSource) {
        return save(dataSource, true);
    }

    /**
     * 只更新数据源的基本属性，不触碰公司和分类数据
     * @param dataSource 要更新的数据源
     * @return 更新后的数据源
     */
    @Transactional
    public JobDataSource updateDataSourceOnly(JobDataSource dataSource) {
        // 只保存数据源主体，不处理关联数据
        JobDataSourceEntity entity = toEntity(dataSource);
        JobDataSourceEntity saved = delegate.save(entity);
        return toDomain(saved);
    }

    /**
     * 保存数据源，可选择是否同时处理公司和分类数据
     * @param dataSource 要保存的数据源
     * @param handleRelations 是否处理关联的公司和分类数据
     * @return 保存后的数据源
     */
    @Transactional
    public JobDataSource save(JobDataSource dataSource, boolean handleRelations) {
        // Save the main entity first
        JobDataSourceEntity entity = toEntity(dataSource);
        JobDataSourceEntity saved = delegate.save(entity);

        if (handleRelations) {
            // Handle companies: preserve existing IDs where possible
            handleCompanySave(saved.getCode(), dataSource.getCompanies());

            // Handle categories: keep existing behavior for now
            Instant now = Instant.now();
            categoryRepository.softDeleteByDataSourceCode(saved.getCode(), now);
            
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
        }

        return toDomain(saved);
    }
    
    private void handleCompanySave(String dataSourceCode, List<JobDataSource.DataSourceCompany> companies) {
        // 获取现有的公司实体
        List<JobDataSourceCompanyEntity> existingEntities = companyRepository.findByDataSourceCodeOrderByReference(dataSourceCode);
        Map<Long, JobDataSourceCompanyEntity> existingById = existingEntities.stream()
                .collect(Collectors.toMap(JobDataSourceCompanyEntity::getId, e -> e));
        
        // 处理可能的重复reference，使用最新的实体（按ID降序）
        Map<String, JobDataSourceCompanyEntity> existingByReference = existingEntities.stream()
                .collect(Collectors.toMap(
                    JobDataSourceCompanyEntity::getReference, 
                    e -> e,
                    (existing, replacement) -> {
                        log.warn("Found duplicate reference '{}' in data source '{}'. Using entity with higher ID: {} instead of {}",
                                replacement.getReference(), dataSourceCode, 
                                Math.max(existing.getId(), replacement.getId()), 
                                Math.min(existing.getId(), replacement.getId()));
                        return existing.getId() > replacement.getId() ? existing : replacement;
                    }
                ));
        
        Set<Long> updatedIds = new HashSet<>();
        
        // 处理每个公司：如果有ID就更新，否则创建新的
        for (JobDataSource.DataSourceCompany company : companies) {
            JobDataSourceCompanyEntity entity;
            
            if (company.id() != null && existingById.containsKey(company.id())) {
                // 更新现有实体
                entity = existingById.get(company.id());
                updatedIds.add(company.id());
            } else if (existingByReference.containsKey(company.reference())) {
                // 基于reference找到现有实体并更新
                entity = existingByReference.get(company.reference());
                updatedIds.add(entity.getId());
            } else {
                // 创建新实体
                entity = new JobDataSourceCompanyEntity();
                entity.setDataSourceCode(dataSourceCode);
            }
            
            // 更新实体字段
            entity.setReference(company.reference());
            entity.setDisplayName(company.displayName());
            entity.setSlug(company.slug());
            entity.setEnabled(company.enabled());
            entity.setPlaceholderOverrides(new LinkedHashMap<>(company.placeholderOverrides()));
            entity.setOverrideOptions(new LinkedHashMap<>(company.overrideOptions()));
            entity.setDeleted(false); // 确保不是软删除状态
            
            companyRepository.save(entity);
        }
        
        // 软删除不在更新列表中的现有公司
        Instant now = Instant.now();
        for (JobDataSourceCompanyEntity existing : existingEntities) {
            if (!updatedIds.contains(existing.getId())) {
                companyRepository.softDeleteById(existing.getId(), now);
            }
        }
    }

    public boolean existsByCode(String code) {
        return delegate.existsByCode(code);
    }

    @Override
    public Optional<JobDataSource> findByCode(String code) {
        return delegate.findByCode(code).map(this::toDomain);
    }

    @Override
    public Optional<JobDataSource> findByCodeIncludingDeleted(String code) {
        return delegate.findByCodeIncludingDeleted(code).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        delegate.deleteAll(delegate.findAllById(List.of(id)));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Instant now = Instant.now();
        delegate.softDeleteById(id, now);
        
        // 同时软删除相关的companies和categories
        Optional<JobDataSourceEntity> entity = delegate.findByIdIncludingDeleted(id);
        if (entity.isPresent()) {
            String code = entity.get().getCode();
            companyRepository.softDeleteByDataSourceCode(code, now);
            categoryRepository.softDeleteByDataSourceCode(code, now);
        }
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
                companies,
                entity.getCrawlerBlueprintCode(),
                entity.isAutoGenerated()
        );
    }

    private JobDataSourceEntity toEntity(JobDataSource dataSource) {
        JobDataSourceEntity entity = dataSource.getId() == null
                ? new JobDataSourceEntity()
                : delegate.findByIdIncludingDeleted(dataSource.getId()).orElse(new JobDataSourceEntity());
        entity.setId(dataSource.getId());
        entity.setCode(dataSource.getCode());
        entity.setType(dataSource.getType());
        entity.setEnabled(dataSource.isEnabled());
        entity.setRunOnStartup(dataSource.isRunOnStartup());
        entity.setRequireOverride(dataSource.isRequireOverride());
        entity.setFlow(dataSource.getFlow());
        entity.setBaseOptions(new LinkedHashMap<>(dataSource.getBaseOptions()));
        entity.setCrawlerBlueprintCode(dataSource.getCrawlerBlueprintCode());
        entity.setAutoGenerated(dataSource.isAutoGenerated());

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
