package com.vibe.jobs.admin.application;

import com.vibe.jobs.admin.domain.event.DataSourceConfigurationChangedEvent;
import com.vibe.jobs.admin.web.dto.BulkUploadResult;
import com.vibe.jobs.admin.web.dto.DataSourceRequest;
import com.vibe.jobs.datasource.application.DataSourceCommandService;
import com.vibe.jobs.datasource.application.DataSourceQueryService;
import com.vibe.jobs.datasource.domain.JobDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDataSourceService {

    private static final Logger log = LoggerFactory.getLogger(AdminDataSourceService.class);

    private final DataSourceQueryService queryService;
    private final DataSourceCommandService commandService;
    private final ApplicationEventPublisher eventPublisher;

    public AdminDataSourceService(DataSourceQueryService queryService,
                                  DataSourceCommandService commandService,
                                  ApplicationEventPublisher eventPublisher) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.eventPublisher = eventPublisher;
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
        JobDataSource dataSource = queryService.getByCode(dataSourceCode);
        List<JobDataSource.DataSourceCompany> companies = new ArrayList<>(dataSource.getCompanies());
        
        // Generate new ID for the company (this is simplified, in reality you might want a proper ID generation strategy)
        Long newId = companies.stream()
                .mapToLong(c -> c.id() != null ? c.id() : 0L)
                .max()
                .orElse(0L) + 1;
        
        JobDataSource.DataSourceCompany newCompany = new JobDataSource.DataSourceCompany(
                newId,
                company.reference(),
                company.displayName(),
                company.slug(),
                company.enabled(),
                company.placeholderOverrides(),
                company.overrideOptions()
        );
        
        companies.add(newCompany);
        
        JobDataSource updated = dataSource.withCompanies(companies);
        commandService.save(updated);
        publishChange(dataSourceCode);
        
        return newCompany;
    }

    public com.vibe.jobs.admin.web.dto.BulkCompanyResult bulkCreateCompanies(
            String dataSourceCode, 
            List<JobDataSource.DataSourceCompany> companiesToCreate,
            String actorEmail) {
        
        if (companiesToCreate == null || companiesToCreate.isEmpty()) {
            return com.vibe.jobs.admin.web.dto.BulkCompanyResult.success(List.of());
        }
        
        JobDataSource dataSource = queryService.getByCode(dataSourceCode);
        List<JobDataSource.DataSourceCompany> existingCompanies = new ArrayList<>(dataSource.getCompanies());
        
        // Find next available ID
        Long nextId = existingCompanies.stream()
                .mapToLong(c -> c.id() != null ? c.id() : 0L)
                .max()
                .orElse(0L) + 1;
        
        List<com.vibe.jobs.admin.web.dto.CompanyResponse> successful = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (JobDataSource.DataSourceCompany companyRequest : companiesToCreate) {
            try {
                // Validate required fields
                if (companyRequest.reference() == null || companyRequest.reference().trim().isEmpty()) {
                    errors.add("Reference is required for company entry");
                    continue;
                }
                
                // Check for duplicate reference within existing companies
                boolean referenceExists = existingCompanies.stream()
                        .anyMatch(existing -> existing.reference().equals(companyRequest.reference().trim()));
                
                if (referenceExists) {
                    errors.add("Company reference '" + companyRequest.reference() + "' already exists");
                    continue;
                }
                
                // Create new company with generated ID
                JobDataSource.DataSourceCompany newCompany = new JobDataSource.DataSourceCompany(
                        nextId++,
                        companyRequest.reference().trim(),
                        companyRequest.displayName(),
                        companyRequest.slug(),
                        Boolean.TRUE.equals(companyRequest.enabled()),
                        companyRequest.placeholderOverrides(),
                        companyRequest.overrideOptions()
                );
                
                existingCompanies.add(newCompany);
                successful.add(com.vibe.jobs.admin.web.dto.CompanyResponse.fromDomain(newCompany));
                
            } catch (Exception e) {
                String errorMsg = "Failed to create company '" + 
                    (companyRequest.reference() != null ? companyRequest.reference() : "unknown") + 
                    "': " + e.getMessage();
                errors.add(errorMsg);
                log.error("Bulk company creation failed", e);
            }
        }
        
        // Save updated data source if any companies were successfully created
        if (!successful.isEmpty()) {
            JobDataSource updated = dataSource.withCompanies(existingCompanies);
            commandService.save(updated);
            publishChange(dataSourceCode);
            
            log.info("Bulk created {} companies for data source '{}' by user '{}'", 
                successful.size(), dataSourceCode, actorEmail);
        }
        
        return com.vibe.jobs.admin.web.dto.BulkCompanyResult.withErrors(successful, errors);
    }

    public JobDataSource.DataSourceCompany getCompanyById(Long companyId) {
        return queryService.fetchAll().stream()
                .flatMap(ds -> ds.getCompanies().stream())
                .filter(company -> company.id() != null && company.id().equals(companyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
    }

    public com.vibe.jobs.admin.web.dto.PagedDataSourceResponse.PagedCompanyResponse getCompaniesPaged(
            String dataSourceCode, int page, int size) {
        JobDataSource dataSource = queryService.getByCode(dataSourceCode);
        List<JobDataSource.DataSourceCompany> allCompanies = dataSource.getCompanies();
        
        int totalElements = allCompanies.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        
        List<JobDataSource.DataSourceCompany> content = start >= totalElements ? 
            List.of() : allCompanies.subList(start, end);
        
        return new com.vibe.jobs.admin.web.dto.PagedDataSourceResponse.PagedCompanyResponse(
            content,
            page,
            size,
            totalPages,
            totalElements,
            page < totalPages - 1,
            page > 0
        );
    }

    public JobDataSource.DataSourceCompany updateCompany(String dataSourceCode, Long companyId, JobDataSource.DataSourceCompany updatedCompany) {
        JobDataSource dataSource = queryService.getByCode(dataSourceCode);
        List<JobDataSource.DataSourceCompany> companies = new ArrayList<>();
        
        boolean found = false;
        for (JobDataSource.DataSourceCompany existing : dataSource.getCompanies()) {
            if (existing.id() != null && existing.id().equals(companyId)) {
                JobDataSource.DataSourceCompany updated = new JobDataSource.DataSourceCompany(
                        companyId,
                        updatedCompany.reference(),
                        updatedCompany.displayName(),
                        updatedCompany.slug(),
                        updatedCompany.enabled(),
                        updatedCompany.placeholderOverrides(),
                        updatedCompany.overrideOptions()
                );
                companies.add(updated);
                found = true;
            } else {
                companies.add(existing);
            }
        }
        
        if (!found) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
        
        JobDataSource updatedDataSource = dataSource.withCompanies(companies);
        commandService.save(updatedDataSource);
        publishChange(dataSourceCode);
        
        return companies.stream()
                .filter(c -> c.id() != null && c.id().equals(companyId))
                .findFirst()
                .orElseThrow();
    }

    public void deleteCompany(String dataSourceCode, Long companyId) {
        JobDataSource dataSource = queryService.getByCode(dataSourceCode);
        List<JobDataSource.DataSourceCompany> companies = dataSource.getCompanies().stream()
                .filter(company -> company.id() == null || !company.id().equals(companyId))
                .toList();
        
        if (companies.size() == dataSource.getCompanies().size()) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
        
        JobDataSource updated = dataSource.withCompanies(companies);
        commandService.save(updated);
        publishChange(dataSourceCode);
    }

    private void publishChange(String code) {
        eventPublisher.publishEvent(new DataSourceConfigurationChangedEvent(code));
    }
}
