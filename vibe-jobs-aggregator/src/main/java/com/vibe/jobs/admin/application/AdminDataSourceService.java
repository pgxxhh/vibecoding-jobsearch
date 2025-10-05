package com.vibe.jobs.admin.application;

import com.vibe.jobs.admin.domain.event.DataSourceConfigurationChangedEvent;
import com.vibe.jobs.admin.web.dto.BulkUploadResult;
import com.vibe.jobs.admin.web.dto.DataSourceRequest;
import com.vibe.jobs.datasource.application.DataSourceCommandService;
import com.vibe.jobs.datasource.application.DataSourceQueryService;
import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.infrastructure.jpa.JobDataSourceCompanyEntity;
import com.vibe.jobs.datasource.infrastructure.jpa.SpringDataJobDataSourceCompanyRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminDataSourceService {

    private static final Logger log = LoggerFactory.getLogger(AdminDataSourceService.class);

    private final DataSourceQueryService queryService;
    private final DataSourceCommandService commandService;
    private final ApplicationEventPublisher eventPublisher;
    private final SpringDataJobDataSourceCompanyRepository companyRepository;

    public AdminDataSourceService(DataSourceQueryService queryService,
                                  DataSourceCommandService commandService,
                                  ApplicationEventPublisher eventPublisher,
                                  SpringDataJobDataSourceCompanyRepository companyRepository) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.eventPublisher = eventPublisher;
        this.companyRepository = companyRepository;
    }

    public List<JobDataSource> listAll() {
        return queryService.fetchAll();
    }

    public JobDataSource getById(Long id) {
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

    public JobDataSource update(Long id, JobDataSource source) {
        JobDataSource withId = source.withId(id).normalized();
        JobDataSource saved = commandService.save(withId);
        publishChange(saved.getCode());
        return saved;
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
        
        // 检查重复的reference
        boolean referenceExists = companyRepository.findByDataSourceCodeOrderByReference(dataSourceCode)
                .stream()
                .anyMatch(existing -> existing.getReference().equals(company.reference().trim()));
                
        if (referenceExists) {
            throw new IllegalArgumentException("Company reference '" + company.reference() + "' already exists");
        }

        // 直接创建并保存到数据库
        JobDataSourceCompanyEntity entity = new JobDataSourceCompanyEntity();
        entity.setDataSourceCode(dataSourceCode);
        entity.setReference(company.reference().trim());
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

    public com.vibe.jobs.admin.web.dto.BulkCompanyResult bulkCreateCompanies(
            String dataSourceCode,
            List<JobDataSource.DataSourceCompany> companiesToCreate,
            String actorEmail) {
        
        if (companiesToCreate == null || companiesToCreate.isEmpty()) {
            return com.vibe.jobs.admin.web.dto.BulkCompanyResult.success(List.of());
        }
        
        // 验证数据源存在
        queryService.getByCode(dataSourceCode);
        
        // 获取现有公司引用
        Set<String> existingReferences = companyRepository.findByDataSourceCodeOrderByReference(dataSourceCode)
                .stream()
                .map(JobDataSourceCompanyEntity::getReference)
                .collect(Collectors.toSet());
        
        List<com.vibe.jobs.admin.web.dto.CompanyResponse> successful = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (JobDataSource.DataSourceCompany companyRequest : companiesToCreate) {
            try {
                // 验证必填字段
                if (companyRequest.reference() == null || companyRequest.reference().trim().isEmpty()) {
                    errors.add("Reference is required for company entry");
                    continue;
                }
                
                String reference = companyRequest.reference().trim();
                
                // 检查重复引用
                if (existingReferences.contains(reference)) {
                    errors.add("Company reference '" + reference + "' already exists");
                    continue;
                }
                
                // 创建新公司实体
                JobDataSourceCompanyEntity entity = new JobDataSourceCompanyEntity();
                entity.setDataSourceCode(dataSourceCode);
                entity.setReference(reference);
                entity.setDisplayName(companyRequest.displayName());
                entity.setSlug(companyRequest.slug());
                entity.setEnabled(Boolean.TRUE.equals(companyRequest.enabled()));
                entity.setPlaceholderOverrides(companyRequest.placeholderOverrides() != null ? 
                        new LinkedHashMap<>(companyRequest.placeholderOverrides()) : new LinkedHashMap<>());
                entity.setOverrideOptions(companyRequest.overrideOptions() != null ? 
                        new LinkedHashMap<>(companyRequest.overrideOptions()) : new LinkedHashMap<>());

                JobDataSourceCompanyEntity saved = companyRepository.save(entity);
                existingReferences.add(reference); // 避免后续重复

                JobDataSource.DataSourceCompany domainCompany = new JobDataSource.DataSourceCompany(
                        saved.getId(),
                        saved.getReference(),
                        saved.getDisplayName(),
                        saved.getSlug(),
                        saved.isEnabled(),
                        saved.getPlaceholderOverrides() != null ? saved.getPlaceholderOverrides() : Map.of(),
                        saved.getOverrideOptions() != null ? saved.getOverrideOptions() : Map.of()
                );
                
                successful.add(com.vibe.jobs.admin.web.dto.CompanyResponse.fromDomain(domainCompany));

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

        return com.vibe.jobs.admin.web.dto.BulkCompanyResult.withErrors(successful, errors);
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

    public com.vibe.jobs.admin.web.dto.PagedDataSourceResponse.PagedCompanyResponse getCompaniesPaged(
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
            
            return new com.vibe.jobs.admin.web.dto.PagedDataSourceResponse.PagedCompanyResponse(
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
            
            return new com.vibe.jobs.admin.web.dto.PagedDataSourceResponse.PagedCompanyResponse(
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
        
        // 更新字段
        entity.setReference(updatedCompany.reference());
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
}
