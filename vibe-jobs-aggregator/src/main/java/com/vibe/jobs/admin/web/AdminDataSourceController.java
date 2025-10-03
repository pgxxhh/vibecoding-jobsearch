package com.vibe.jobs.admin.web;

import com.vibe.jobs.admin.application.AdminChangeLogService;
import com.vibe.jobs.admin.application.AdminDataSourceService;
import com.vibe.jobs.admin.domain.AdminPrincipal;
import com.vibe.jobs.admin.web.dto.DataSourceRequest;
import com.vibe.jobs.admin.web.dto.DataSourceResponse;
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

    @GetMapping("/{id}")
    public DataSourceResponse get(@PathVariable Long id) {
        try {
            JobDataSource source = dataSourceService.getById(id);
            return DataSourceResponse.fromDomain(source);
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
}
