package com.vibe.jobs.resume.infrastructure.persistence;

import com.vibe.jobs.resume.domain.ResumeFeedback;
import com.vibe.jobs.resume.domain.spi.ResumeFeedbackRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class ResumeFeedbackRepositoryAdapter implements ResumeFeedbackRepositoryPort {
    private final ResumeFeedbackJpaRepository resumeFeedbackJpaRepository;

    public ResumeFeedbackRepositoryAdapter(ResumeFeedbackJpaRepository resumeFeedbackJpaRepository) {
        this.resumeFeedbackJpaRepository = resumeFeedbackJpaRepository;
    }

    @Override
    @Transactional
    public List<ResumeFeedback> saveAll(List<ResumeFeedback> feedback) {
        List<ResumeFeedbackEntity> entities = new ArrayList<>();
        for (ResumeFeedback item : feedback) {
            entities.add(ResumeFeedbackEntity.fromDomain(item));
        }
        return resumeFeedbackJpaRepository.saveAll(entities).stream()
                .map(ResumeFeedbackEntity::toDomain)
                .toList();
    }
}
