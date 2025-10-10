package com.vibe.jobs.admin.web;

import com.vibe.jobs.admin.application.AdminChangeLogService;
import com.vibe.jobs.admin.application.AdminDataSourceService;
import com.vibe.jobs.admin.domain.AdminPrincipal;
import com.vibe.jobs.admin.web.dto.DataSourceRequest;
import com.vibe.jobs.admin.web.dto.DataSourceResponse;
import com.vibe.jobs.admin.web.dto.PagedDataSourceResponse;
import com.vibe.jobs.admin.web.dto.BulkDataSourceRequest;
import com.vibe.jobs.admin.web.dto.BulkUploadResult;
import com.vibe.jobs.admin.web.dto.CompanyRequest;
import com.vibe.jobs.admin.web.dto.CompanyResponse;
import com.vibe.jobs.admin.web.dto.BulkCompanyRequest;
import com.vibe.jobs.admin.web.dto.BulkCompanyResult;
import com.vibe.jobs.datasource.domain.JobDataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/admin/data-sources", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminDataSourceController {

    private final AdminDataSourceService dataSourceService;
    private final AdminChangeLogService changeLogService;

    public AdminDataSourceController(AdminDataSourceService dataSourceService,
                                     AdminChangeLogService changeLogService) {
        this.dataSourceService = dataSourceService;
        this.changeLogService = changeLogService;
    }

    @GetMapping
    public List<DataSourceResponse> list() {
        return dataSourceService.listAll().stream()
                .map(DataSourceResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{code}")
    public DataSourceResponse get(@PathVariable String code) {
        try {
            JobDataSource source = dataSourceService.getByCode(code);
            return DataSourceResponse.fromDomain(source);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/by-id/{id}")
    public DataSourceResponse getById(@PathVariable Long id) {
        try {
            JobDataSource source = dataSourceService.getById(id);
            return DataSourceResponse.fromDomain(source);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/{code}/paged")
    public PagedDataSourceResponse getWithPagination(@PathVariable String code,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        try {
            JobDataSource source = dataSourceService.getByCode(code);
            var pagedCompanies = dataSourceService.getCompaniesPaged(code, page, size);
            return PagedDataSourceResponse.fromDomain(source, pagedCompanies);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DataSourceResponse create(@RequestBody DataSourceRequest request,
                                     AdminPrincipal principal) {
        JobDataSource created = dataSourceService.create(request.toDomain(null));
        changeLogService.record(
                principal != null ? principal.email() : null,
                "CREATE",
                "DATA_SOURCE",
                created.getId() == null ? created.getCode() : created.getId().toString(),
                Map.of("after", created)
        );
        return DataSourceResponse.fromDomain(created);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DataSourceResponse update(@PathVariable Long id,
                                     @RequestBody DataSourceRequest request,
                                     AdminPrincipal principal) {
        JobDataSource before;
        try {
            before = dataSourceService.getById(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
        JobDataSource updated = dataSourceService.update(id, request.toDomain(id));
        changeLogService.record(
                principal != null ? principal.email() : null,
                "UPDATE",
                "DATA_SOURCE",
                id.toString(),
                Map.of("before", before, "after", updated)
        );
        return DataSourceResponse.fromDomain(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, AdminPrincipal principal) {
        JobDataSource before;
        try {
            before = dataSourceService.getById(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
        dataSourceService.delete(id);
        changeLogService.record(
                principal != null ? principal.email() : null,
                "DELETE",
                "DATA_SOURCE",
                id.toString(),
                Map.of("before", before)
        );
    }

    @PostMapping(path = "/bulk", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BulkUploadResult bulkUpload(@RequestBody BulkDataSourceRequest request,
                                       AdminPrincipal principal) {
        return dataSourceService.bulkCreate(request.dataSources(), principal != null ? principal.email() : null);
    }

    @PostMapping(path = "/{code}/companies", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse createCompany(@PathVariable String code,
                                         @RequestBody CompanyRequest request,
                                         AdminPrincipal principal) {
        try {
            var company = dataSourceService.createCompany(code, request.toDomain());
            changeLogService.record(
                    principal != null ? principal.email() : null,
                    "CREATE",
                    "DATA_SOURCE_COMPANY", 
                    code + ":" + company.reference(),
                    Map.of("after", company)
            );
            return CompanyResponse.fromDomain(company);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping(path = "/{code}/companies/bulk", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BulkCompanyResult bulkCreateCompanies(@PathVariable String code,
                                                 @RequestBody BulkCompanyRequest request,
                                                 AdminPrincipal principal) {
        try {
            List<JobDataSource.DataSourceCompany> companies = request.companies().stream()
                    .map(CompanyRequest::toDomain)
                    .toList();
            
            BulkCompanyResult result = dataSourceService.bulkCreateCompanies(
                    code, 
                    companies,
                    principal != null ? principal.email() : null
            );
            
            // Record the bulk operation in change log
            if (result.successful() > 0) {
                changeLogService.record(
                        principal != null ? principal.email() : null,
                        "BULK_CREATE",
                        "DATA_SOURCE_COMPANY",
                        code,
                        Map.of(
                            "total", result.total(),
                            "successful", result.successful(),
                            "failed", result.failed(),
                            "companies", result.created().stream().map(CompanyResponse::reference).toList()
                        )
                );
            }
            
            return result;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping(path = "/{code}/companies/{companyId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CompanyResponse updateCompany(@PathVariable String code,
                                         @PathVariable Long companyId,
                                         @RequestBody CompanyRequest request,
                                         AdminPrincipal principal) {
        try {
            var before = dataSourceService.getCompanyById(companyId);
            var updated = dataSourceService.updateCompany(code, companyId, request.toDomain());
            changeLogService.record(
                    principal != null ? principal.email() : null,
                    "UPDATE",
                    "DATA_SOURCE_COMPANY",
                    code + ":" + companyId,
                    Map.of("before", before, "after", updated)
            );
            return CompanyResponse.fromDomain(updated);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/{code}/companies/{companyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompany(@PathVariable String code,
                              @PathVariable Long companyId,
                              AdminPrincipal principal) {
        try {
            var before = dataSourceService.getCompanyById(companyId);
            dataSourceService.deleteCompany(code, companyId);
            changeLogService.record(
                    principal != null ? principal.email() : null,
                    "DELETE",
                    "DATA_SOURCE_COMPANY",
                    code + ":" + companyId,
                    Map.of("before", before)
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/{id}/cleanup-duplicates")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cleanupDuplicateCompanies(@PathVariable Long id, AdminPrincipal principal) {
        try {
            dataSourceService.cleanupDuplicateCompanies(id);
            changeLogService.record(
                    principal != null ? principal.email() : null,
                    "CLEANUP",
                    "DATA_SOURCE_DUPLICATE_COMPANIES",
                    id.toString(),
                    Map.of("operation", "cleanup_duplicate_companies")
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}
