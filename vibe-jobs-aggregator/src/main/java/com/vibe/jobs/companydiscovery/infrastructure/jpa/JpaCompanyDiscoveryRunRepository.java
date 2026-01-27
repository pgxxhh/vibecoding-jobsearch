package com.vibe.jobs.companydiscovery.infrastructure.jpa;

import com.vibe.jobs.companydiscovery.domain.CompanyDiscoveryRun;
import com.vibe.jobs.companydiscovery.domain.CompanyDiscoveryRunStatus;
import com.vibe.jobs.companydiscovery.domain.spi.CompanyDiscoveryRunRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaCompanyDiscoveryRunRepository implements CompanyDiscoveryRunRepositoryPort {

    private final SpringDataCompanyDiscoveryRunRepository repository;

    public JpaCompanyDiscoveryRunRepository(SpringDataCompanyDiscoveryRunRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompanyDiscoveryRun save(CompanyDiscoveryRun run) {
        CompanyDiscoveryRunEntity entity = run.id() == null
                ? new CompanyDiscoveryRunEntity()
                : repository.findById(run.id()).orElse(new CompanyDiscoveryRunEntity());
        entity.setStatus(run.status().name());
        entity.setProvider(run.provider());
        entity.setDryRun(run.dryRun());
        entity.setTotalCandidates(run.totalCandidates());
        entity.setTotalValid(run.totalValid());
        entity.setStartedAt(run.startedAt());
        entity.setCompletedAt(run.completedAt());
        CompanyDiscoveryRunEntity saved = repository.save(entity);
        return new CompanyDiscoveryRun(
                saved.getId(),
                CompanyDiscoveryRunStatus.valueOf(saved.getStatus()),
                saved.getProvider(),
                saved.isDryRun(),
                saved.getTotalCandidates(),
                saved.getTotalValid(),
                saved.getStartedAt(),
                saved.getCompletedAt()
        );
    }

    @Override
    public List<CompanyDiscoveryRun> fetchRecent(int limit) {
        List<CompanyDiscoveryRunEntity> entities = repository.findTop50ByDeletedFalseOrderByStartedAtDesc();
        return entities.stream()
                .limit(Math.max(0, limit))
                .map(entity -> new CompanyDiscoveryRun(
                        entity.getId(),
                        CompanyDiscoveryRunStatus.valueOf(entity.getStatus()),
                        entity.getProvider(),
                        entity.isDryRun(),
                        entity.getTotalCandidates(),
                        entity.getTotalValid(),
                        entity.getStartedAt(),
                        entity.getCompletedAt()
                ))
                .toList();
    }
}
