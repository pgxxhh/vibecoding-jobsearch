package com.vibe.jobs.companydiscovery.infrastructure.jpa;

import com.vibe.jobs.companydiscovery.domain.CompanyDiscoveryResult;
import com.vibe.jobs.companydiscovery.domain.CompanyDiscoveryResultStatus;
import com.vibe.jobs.companydiscovery.domain.spi.CompanyDiscoveryResultRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaCompanyDiscoveryResultRepository implements CompanyDiscoveryResultRepositoryPort {

    private final SpringDataCompanyDiscoveryResultRepository repository;

    public JpaCompanyDiscoveryResultRepository(SpringDataCompanyDiscoveryResultRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompanyDiscoveryResult save(CompanyDiscoveryResult result) {
        CompanyDiscoveryResultEntity entity = result.id() == null
                ? new CompanyDiscoveryResultEntity()
                : repository.findById(result.id()).orElse(new CompanyDiscoveryResultEntity());
        entity.setRunId(result.runId());
        entity.setDataSourceCode(result.dataSourceCode());
        entity.setCompanyReference(result.companyReference());
        entity.setDisplayName(result.displayName());
        entity.setProvider(result.provider());
        entity.setStatus(result.status().name());
        entity.setReason(result.reason());
        CompanyDiscoveryResultEntity saved = repository.save(entity);
        return new CompanyDiscoveryResult(
                saved.getId(),
                saved.getRunId(),
                saved.getDataSourceCode(),
                saved.getCompanyReference(),
                saved.getDisplayName(),
                saved.getProvider(),
                CompanyDiscoveryResultStatus.valueOf(saved.getStatus()),
                saved.getReason(),
                saved.getCreatedTime()
        );
    }

    @Override
    public List<CompanyDiscoveryResult> fetchRecent(int limit) {
        List<CompanyDiscoveryResultEntity> entities = repository.findTop100ByDeletedFalseOrderByCreatedTimeDesc();
        return entities.stream()
                .limit(Math.max(0, limit))
                .map(entity -> new CompanyDiscoveryResult(
                        entity.getId(),
                        entity.getRunId(),
                        entity.getDataSourceCode(),
                        entity.getCompanyReference(),
                        entity.getDisplayName(),
                        entity.getProvider(),
                        CompanyDiscoveryResultStatus.valueOf(entity.getStatus()),
                        entity.getReason(),
                        entity.getCreatedTime()
                ))
                .toList();
    }
}
