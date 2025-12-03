package com.vibe.jobs.resume.infrastructure.persistence;

import com.vibe.jobs.resume.domain.Resume;
import com.vibe.jobs.resume.domain.spi.ResumeRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class ResumeRepositoryAdapter implements ResumeRepositoryPort {
    private final ResumeJpaRepository resumeJpaRepository;

    public ResumeRepositoryAdapter(ResumeJpaRepository resumeJpaRepository) {
        this.resumeJpaRepository = resumeJpaRepository;
    }

    @Override
    @Transactional
    public Resume save(Resume resume) {
        ResumeEntity entity = resume.getId() != null
                ? resumeJpaRepository.findById(resume.getId()).orElseGet(() -> ResumeEntity.fromDomain(resume))
                : ResumeEntity.fromDomain(resume);
        entity.updateFromDomain(resume);
        ResumeEntity saved = resumeJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Resume> findById(Long id) {
        return resumeJpaRepository.findById(id).map(ResumeEntity::toDomain);
    }
}
