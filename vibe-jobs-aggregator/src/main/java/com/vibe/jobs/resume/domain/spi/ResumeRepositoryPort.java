package com.vibe.jobs.resume.domain.spi;

import com.vibe.jobs.resume.domain.Resume;

import java.util.Optional;

public interface ResumeRepositoryPort {
    Resume save(Resume resume);

    Optional<Resume> findById(Long id);
}
