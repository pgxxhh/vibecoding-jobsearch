package com.vibe.jobs.repo;

import java.util.Collection;
import java.util.Set;

public interface JobDetailRepositoryCustom {

    Set<Long> findMatchingJobIds(Collection<Long> jobIds, String query);
}

