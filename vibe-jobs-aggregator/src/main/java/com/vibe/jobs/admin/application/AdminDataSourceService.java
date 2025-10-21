package com.vibe.jobs.admin.application;

import com.vibe.jobs.admin.domain.event.DataSourceConfigurationChangedEvent;
import com.vibe.jobs.admin.interfaces.dto.BulkUploadResult;
import com.vibe.jobs.admin.interfaces.dto.DataSourceRequest;
import com.vibe.jobs.datasource.application.DataSourceCommandService;
import com.vibe.jobs.datasource.application.DataSourceQueryService;
import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.infrastructure.jpa.JobDataSourceCompanyEntity;
import com.vibe.jobs.datasource.infrastructure.jpa.SpringDataJobDataSourceCompanyRepository;
import com.vibe.jobs.datasource.infrastructure.jpa.JpaJobDataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminDataSourceService {

    private static final Logger log = LoggerFactory.getLogger(AdminDataSourceService.class);

    private final DataSourceQueryService queryService;
    private final DataSourceCommandService commandService;
    private final ApplicationEventPublisher eventPublisher;
    private final SpringDataJobDataSourceCompanyRepository companyRepository;
    private final JpaJobDataSourceRepository jpaRepository;

    public AdminDataSourceService(DataSourceQueryService queryService,
                                  DataSourceCommandService commandService,
                                  ApplicationEventPublisher eventPublisher,
                                  SpringDataJobDataSourceCompanyRepository companyRepository,
                                  JpaJobDataSourceRepository jpaRepository) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.eventPublisher = eventPublisher;
        this.companyRepository = companyRepository;
        this.jpaRepository = jpaRepository;
    }

    public List<JobDataSource> listAll() {
        return queryService.fetchAll();
    }

    public JobDataSource getById(Long id) {
        // 在获取数据源之前，尝试清理可能的重复数据
        try {
            cleanupDuplicateCompanies(id);
        } catch (Exception e) {
            log.warn("Failed to cleanup duplicate companies for data source {}: {}", id, e.getMessage());
        }
        return queryService.getById(id);
    }

    public JobDataSource getByCode(String code) {
        return queryService.getByCode(code);
    }

    public JobDataSource create(JobDataSource source) {
        JobDataSource saved = commandService.save(source.normalized());
        publishChange(saved.getCode());
        return saved;
    }

    @Transactional
    public JobDataSource update(Long id, JobDataSource source) {
        // 获取更新前的数据源信息
        JobDataSource existingSource = queryService.getById(id);
        boolean wasEnabled = existingSource.isEnabled();
        boolean willBeEnabled = source.isEnabled();
        
        JobDataSource withId = source.withId(id).normalized();
        
        // 判断是否只是简单的启用/禁用操作（不涉及公司和分类数据的变更）
        boolean isSimpleEnableDisableOperation = isOnlyEnableStatusChange(existingSource, withId);
        
        JobDataSource saved;
        if (isSimpleEnableDisableOperation) {
            // 只更新数据源本身，不触碰公司和分类
            saved = jpaRepository.updateDataSourceOnly(withId);
            
            // 如果数据源从启用变为禁用，自动禁用所有关联的公司
            if (wasEnabled && !willBeEnabled) {
                log.info("Data source '{}' is being disabled, disabling all associated companies", saved.getCode());
                companyRepository.findByDataSourceCodeOrderByReference(saved.getCode())
                    .stream()
                    .filter(JobDataSourceCompanyEntity::isEnabled)
                    .forEach(company -> {
                        company.setEnabled(false);
                        companyRepository.save(company);
                        log.info("Disabled company '{}' in data source '{}'", company.getReference(), saved.getCode());
                    });
            }
            // 如果数据源从禁用变为启用，可以选择性地启用公司（这里暂时不自动启用，保持现有状态）
            else if (!wasEnabled && willBeEnabled) {
                log.info("Data source '{}' is being enabled, companies remain in their current state", saved.getCode());
            }
        } else {
            // 完整更新，包括公司和分类数据
            saved = commandService.save(withId);
        }
        
        publishChange(saved.getCode());
        return saved;
    }
    
    /**
     * 判断是否只是启用/禁用状态的变更，而没有其他数据的变更
     */
    private boolean isOnlyEnableStatusChange(JobDataSource existing, JobDataSource updated) {
        // 检查除了enabled字段外的其他关键字段是否有变化
        return existing.getCode().equals(updated.getCode()) &&
               existing.getType().equals(updated.getType()) &&
               existing.isRunOnStartup() == updated.isRunOnStartup() &&
               existing.isRequireOverride() == updated.isRequireOverride() &&
               existing.getFlow() == updated.getFlow() &&
               existing.getBaseOptions().equals(updated.getBaseOptions()) &&
               existing.getCategories().equals(updated.getCategories()) &&
               existing.getCompanies().equals(updated.getCompanies());
    }

    public void delete(Long id) {
        JobDataSource existing = queryService.getById(id);
        commandService.delete(id);
        publishChange(existing.getCode());
    }

    public BulkUploadResult bulkCreate(List<DataSourceRequest> requests, String operatorEmail) {
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (DataSourceRequest request : requests) {
            try {
                JobDataSource source = request.toDomain(null);
                JobDataSource saved = commandService.save(source.normalized());
                publishChange(saved.getCode());
                success++;
                log.info("Bulk created data source: {}", saved.getCode());
            } catch (Exception e) {
                failed++;
                String errorMessage = String.format("Failed to create data source '%s': %s", 
                    request.code(), e.getMessage());
                errors.add(errorMessage);
                log.error("Bulk create failed for data source: {}", request.code(), e);
            }
        }

        return new BulkUploadResult(success, failed, errors);
    }

    public JobDataSource.DataSourceCompany createCompany(String dataSourceCode, JobDataSource.DataSourceCompany company) {
        // 验证数据源存在
        JobDataSource dataSource = queryService.getByCode(dataSourceCode);
        
        // 检查 data_source_code + reference 且 deleted = false 的唯一性
        String trimmedReference = company.reference().trim();
        Optional<JobDataSourceCompanyEntity> existingCompany = companyRepository
                .findActiveByDataSourceCodeAndReference(dataSourceCode, trimmedReference);
                
        if (existingCompany.isPresent()) {
            throw new IllegalArgumentException("Company reference '" + trimmedReference + "' already exists in data source '" + dataSourceCode + "'");
        }

        // 直接创建并保存到数据库
        JobDataSourceCompanyEntity entity = new JobDataSourceCompanyEntity();
        entity.setDataSourceCode(dataSourceCode);
        entity.setReference(trimmedReference);
        entity.setDisplayName(company.displayName());
        entity.setSlug(company.slug());
        entity.setEnabled(company.enabled());
        entity.setPlaceholderOverrides(new LinkedHashMap<>(company.placeholderOverrides()));
        entity.setOverrideOptions(new LinkedHashMap<>(company.overrideOptions()));
        
        JobDataSourceCompanyEntity saved = companyRepository.save(entity);
        publishChange(dataSourceCode);

        return new JobDataSource.DataSourceCompany(
                saved.getId(),
                saved.getReference(),
                saved.getDisplayName(),
                saved.getSlug(),
                saved.isEnabled(),
                saved.getPlaceholderOverrides() != null ? saved.getPlaceholderOverrides() : Map.of(),
                saved.getOverrideOptions() != null ? saved.getOverrideOptions() : Map.of()
        );
    }

    public com.vibe.jobs.admin.interfaces.dto.BulkCompanyResult bulkCreateCompanies(
            String dataSourceCode,
            List<JobDataSource.DataSourceCompany> companiesToCreate,
            String actorEmail) {
        
        if (companiesToCreate == null || companiesToCreate.isEmpty()) {
            return com.vibe.jobs.admin.interfaces.dto.BulkCompanyResult.success(List.of());
        }
        
        // 验证数据源存在
        queryService.getByCode(dataSourceCode);
        
        List<com.vibe.jobs.admin.interfaces.dto.CompanyResponse> successful = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> processedReferences = new java.util.HashSet<>(); // 跟踪本批次已处理的引用

        for (JobDataSource.DataSourceCompany companyRequest : companiesToCreate) {
            try {
                // 验证必填字段
                if (companyRequest.reference() == null || companyRequest.reference().trim().isEmpty()) {
                    errors.add("Reference is required for company entry");
                    continue;
                }
                
                String trimmedReference = companyRequest.reference().trim();
                
                // 检查本批次内部重复
                if (processedReferences.contains(trimmedReference)) {
                    errors.add("Duplicate reference '" + trimmedReference + "' in current batch");
                    continue;
                }
                
                // 检查数据库中是否存在活跃的相同引用
                Optional<JobDataSourceCompanyEntity> existingCompany = companyRepository
                        .findActiveByDataSourceCodeAndReference(dataSourceCode, trimmedReference);
                        
                if (existingCompany.isPresent()) {
                    errors.add("Company reference '" + trimmedReference + "' already exists in data source '" + dataSourceCode + "'");
                    continue;
                }
                
                // 创建新公司实体
                JobDataSourceCompanyEntity entity = new JobDataSourceCompanyEntity();
                entity.setDataSourceCode(dataSourceCode);
                entity.setReference(trimmedReference);
                entity.setDisplayName(companyRequest.displayName());
                entity.setSlug(companyRequest.slug());
                entity.setEnabled(Boolean.TRUE.equals(companyRequest.enabled()));
                entity.setPlaceholderOverrides(companyRequest.placeholderOverrides() != null ? 
                        new LinkedHashMap<>(companyRequest.placeholderOverrides()) : new LinkedHashMap<>());
                entity.setOverrideOptions(companyRequest.overrideOptions() != null ? 
                        new LinkedHashMap<>(companyRequest.overrideOptions()) : new LinkedHashMap<>());

                JobDataSourceCompanyEntity saved = companyRepository.save(entity);
                processedReferences.add(trimmedReference); // 标记为已处理

                JobDataSource.DataSourceCompany domainCompany = new JobDataSource.DataSourceCompany(
                        saved.getId(),
                        saved.getReference(),
                        saved.getDisplayName(),
                        saved.getSlug(),
                        saved.isEnabled(),
                        saved.getPlaceholderOverrides() != null ? saved.getPlaceholderOverrides() : Map.of(),
                        saved.getOverrideOptions() != null ? saved.getOverrideOptions() : Map.of()
                );
                
                successful.add(com.vibe.jobs.admin.interfaces.dto.CompanyResponse.fromDomain(domainCompany));

            } catch (Exception e) {
                String errorMsg = "Failed to create company '" +
                    (companyRequest.reference() != null ? companyRequest.reference() : "unknown") +
                    "': " + e.getMessage();
                errors.add(errorMsg);
                log.error("Bulk company creation failed", e);
            }
        }
        
        if (!successful.isEmpty()) {
            publishChange(dataSourceCode);
            log.info("Bulk created {} companies for data source '{}' by user '{}'",
                successful.size(), dataSourceCode, actorEmail);
        }

        return com.vibe.jobs.admin.interfaces.dto.BulkCompanyResult.withErrors(successful, errors);
    }

    public JobDataSource.DataSourceCompany getCompanyById(Long companyId) {
        // 直接从数据库查找company，包括软删除的记录
        try {
            var companyEntity = companyRepository.findById(companyId);
            if (companyEntity.isEmpty()) {
                throw new IllegalArgumentException("Company not found: " + companyId);
            }
            var entity = companyEntity.get();
            return new JobDataSource.DataSourceCompany(
                    entity.getId(),
                    entity.getReference(),
                    entity.getDisplayName(),
                    entity.getSlug(),
                    entity.isEnabled(),
                    entity.getPlaceholderOverrides() != null ? entity.getPlaceholderOverrides() : Map.of(),
                    entity.getOverrideOptions() != null ? entity.getOverrideOptions() : Map.of()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Company not found: " + companyId, e);
        }
    }

    public com.vibe.jobs.admin.interfaces.dto.PagedDataSourceResponse.PagedCompanyResponse getCompaniesPaged(
            String dataSourceCode, int page, int size) {
        // 验证数据源存在
        queryService.getByCode(dataSourceCode);
        
        log.info("Fetching companies for dataSourceCode: '{}', page: {}, size: {}", dataSourceCode, page, size);
        
        try {
            // 使用Spring Data分页
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            org.springframework.data.domain.Page<JobDataSourceCompanyEntity> pageResult = 
                companyRepository.findByDataSourceCodeOrderByReference(dataSourceCode, pageable);
            
            List<JobDataSource.DataSourceCompany> companies = pageResult.getContent().stream()
                    .map(entity -> new JobDataSource.DataSourceCompany(
                            entity.getId(),
                            entity.getReference(),
                            entity.getDisplayName(),
                            entity.getSlug(),
                            entity.isEnabled(),
                            entity.getPlaceholderOverrides() != null ? entity.getPlaceholderOverrides() : Map.of(),
                            entity.getOverrideOptions() != null ? entity.getOverrideOptions() : Map.of()
                    ))
                    .toList();
            
            log.info("Successfully fetched {} companies (page {}) for dataSourceCode: '{}'", 
                    companies.size(), page, dataSourceCode);
            
            return new com.vibe.jobs.admin.interfaces.dto.PagedDataSourceResponse.PagedCompanyResponse(
                companies,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalPages(),
                pageResult.getTotalElements(),
                pageResult.hasNext(),
                pageResult.hasPrevious()
            );
            
        } catch (Exception e) {
            log.warn("Failed to use pageable query for dataSourceCode '{}', falling back to manual pagination: {}", 
                    dataSourceCode, e.getMessage());
            
            // 备用方案：手动分页
            List<JobDataSourceCompanyEntity> allCompanies = companyRepository.findByDataSourceCodeOrderByReference(dataSourceCode);
            
            int totalElements = allCompanies.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            int start = page * size;
            int end = Math.min(start + size, totalElements);
            
            List<JobDataSourceCompanyEntity> pagedEntities = start >= totalElements ? 
                List.of() : allCompanies.subList(start, end);
            
            List<JobDataSource.DataSourceCompany> companies = pagedEntities.stream()
                    .map(entity -> new JobDataSource.DataSourceCompany(
                            entity.getId(),
                            entity.getReference(),
                            entity.getDisplayName(),
                            entity.getSlug(),
                            entity.isEnabled(),
                            entity.getPlaceholderOverrides() != null ? entity.getPlaceholderOverrides() : Map.of(),
                            entity.getOverrideOptions() != null ? entity.getOverrideOptions() : Map.of()
                    ))
                    .toList();
            
            log.info("Fallback: Returning {} companies (page {}/{}) for dataSourceCode: '{}'", 
                    companies.size(), page + 1, Math.max(totalPages, 1), dataSourceCode);
            
            return new com.vibe.jobs.admin.interfaces.dto.PagedDataSourceResponse.PagedCompanyResponse(
                companies,
                page,
                size,
                totalPages,
                totalElements,
                page < totalPages - 1,
                page > 0
            );
        }
    }

    public JobDataSource.DataSourceCompany updateCompany(String dataSourceCode, Long companyId, JobDataSource.DataSourceCompany updatedCompany) {
        // 验证数据源存在
        queryService.getByCode(dataSourceCode);
        
        // 直接更新数据库中的公司记录
        JobDataSourceCompanyEntity entity = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
        
        // 检查是否属于指定的数据源
        if (!entity.getDataSourceCode().equals(dataSourceCode)) {
            throw new IllegalArgumentException("Company " + companyId + " does not belong to data source " + dataSourceCode);
        }
        
        // 检查软删除状态
        if (entity.isDeleted()) {
            throw new IllegalArgumentException("Cannot update deleted company: " + companyId);
        }
        
        // 检查 data_source_code + reference 且 deleted = false 的唯一性（排除当前记录）
        String trimmedReference = updatedCompany.reference().trim();
        Optional<JobDataSourceCompanyEntity> existingCompany = companyRepository
                .findActiveByDataSourceCodeAndReferenceExcludingId(dataSourceCode, trimmedReference, companyId);
                
        if (existingCompany.isPresent()) {
            throw new IllegalArgumentException("Company reference '" + trimmedReference + "' already exists in data source '" + dataSourceCode + "'");
        }
        
        // 更新字段
        entity.setReference(trimmedReference);
        entity.setDisplayName(updatedCompany.displayName());
        entity.setSlug(updatedCompany.slug());
        entity.setEnabled(updatedCompany.enabled());
        entity.setPlaceholderOverrides(new LinkedHashMap<>(updatedCompany.placeholderOverrides()));
        entity.setOverrideOptions(new LinkedHashMap<>(updatedCompany.overrideOptions()));
        
        JobDataSourceCompanyEntity saved = companyRepository.save(entity);
        publishChange(dataSourceCode);
        
        return new JobDataSource.DataSourceCompany(
                saved.getId(),
                saved.getReference(),
                saved.getDisplayName(),
                saved.getSlug(),
                saved.isEnabled(),
                saved.getPlaceholderOverrides() != null ? saved.getPlaceholderOverrides() : Map.of(),
                saved.getOverrideOptions() != null ? saved.getOverrideOptions() : Map.of()
        );
    }

    @Transactional
    public void deleteCompany(String dataSourceCode, Long companyId) {
        // 验证数据源存在
        queryService.getByCode(dataSourceCode);
        
        // 验证公司存在并且属于指定的数据源
        JobDataSourceCompanyEntity entity = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
        
        if (!entity.getDataSourceCode().equals(dataSourceCode)) {
            throw new IllegalArgumentException("Company " + companyId + " does not belong to data source " + dataSourceCode);
        }
        
        // 执行软删除
        Instant now = Instant.now();
        companyRepository.softDeleteById(companyId, now);

        publishChange(dataSourceCode);
    }

    private void publishChange(String code) {
        eventPublisher.publishEvent(new DataSourceConfigurationChangedEvent(code));
    }
    
    /**
     * 清理指定数据源的重复公司记录（保留ID最大的活跃记录）
     */
    @Transactional
    public void cleanupDuplicateCompanies(Long dataSourceId) {
        try {
            // 首先获取数据源信息
            JobDataSource dataSource = queryService.getById(dataSourceId);
            String dataSourceCode = dataSource.getCode();
            
            // 查找所有活跃的公司记录
            List<JobDataSourceCompanyEntity> allActiveCompanies = companyRepository.findByDataSourceCodeOrderByReference(dataSourceCode);
            
            // 按reference分组，找出重复的记录
            Map<String, List<JobDataSourceCompanyEntity>> companiesByReference = allActiveCompanies.stream()
                    .collect(Collectors.groupingBy(JobDataSourceCompanyEntity::getReference));
            
            Instant now = Instant.now();
            int duplicatesRemoved = 0;
            
            for (Map.Entry<String, List<JobDataSourceCompanyEntity>> entry : companiesByReference.entrySet()) {
                List<JobDataSourceCompanyEntity> companies = entry.getValue();
                if (companies.size() > 1) {
                    // 有重复记录，保留ID最大的（最新的），软删除其他的
                    companies.sort((c1, c2) -> Long.compare(c2.getId(), c1.getId())); // 按ID降序排列
                    JobDataSourceCompanyEntity keepCompany = companies.get(0);
                    
                    log.warn("Found {} duplicate companies with reference '{}' in data source '{}'. Keeping company with ID {}, soft-deleting others.",
                            companies.size(), entry.getKey(), dataSourceCode, keepCompany.getId());
                    
                    // 软删除其他重复记录
                    for (int i = 1; i < companies.size(); i++) {
                        JobDataSourceCompanyEntity duplicate = companies.get(i);
                        companyRepository.softDeleteById(duplicate.getId(), now);
                        duplicatesRemoved++;
                        log.info("Soft-deleted duplicate company with ID {} (reference: '{}') in data source '{}'",
                                duplicate.getId(), duplicate.getReference(), dataSourceCode);
                    }
                }
            }
            
            if (duplicatesRemoved > 0) {
                log.info("Cleaned up {} duplicate company records for data source '{}' (ID: {})", 
                        duplicatesRemoved, dataSourceCode, dataSourceId);
                publishChange(dataSourceCode);
            } else {
                log.info("No duplicate company records found for data source '{}' (ID: {})", dataSourceCode, dataSourceId);
            }
            
        } catch (Exception e) {
            log.error("Failed to cleanup duplicate companies for data source ID {}: {}", dataSourceId, e.getMessage(), e);
            throw new RuntimeException("Failed to cleanup duplicate companies", e);
        }
    }
}
