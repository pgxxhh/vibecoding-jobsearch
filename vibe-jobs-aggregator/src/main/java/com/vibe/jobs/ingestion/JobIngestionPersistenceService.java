package com.vibe.jobs.ingestion;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.service.JobDetailService;
import com.vibe.jobs.service.JobService;
import com.vibe.jobs.sources.FetchedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class JobIngestionPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(JobIngestionPersistenceService.class);

    private final JobService jobService;
    private final JobDetailService jobDetailService;
    private final int chunkSize;

    public JobIngestionPersistenceService(JobService jobService,
                                          JobDetailService jobDetailService,
                                          @Value("${ingestion.persistence.chunk-size:100}") int chunkSize) {
        this.jobService = jobService;
        this.jobDetailService = jobDetailService;
        this.chunkSize = Math.max(1, chunkSize);
    }

    public JobBatchPersistenceResult persistBatch(List<FetchedJob> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return JobBatchPersistenceResult.empty();
        }
        List<FetchedJob> safeJobs = new ArrayList<>(jobs);
        int persisted = 0;
        Job lastJob = null;
        for (List<FetchedJob> chunk : splitIntoChunks(safeJobs)) {
            JobBatchPersistenceResult result = persistChunk(chunk);
            persisted += result.persisted();
            if (result.lastJob() != null) {
                lastJob = result.lastJob();
            }
        }
        return new JobBatchPersistenceResult(persisted, lastJob, persisted > 0);
    }

    private JobBatchPersistenceResult persistChunk(List<FetchedJob> chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return JobBatchPersistenceResult.empty();
        }
        int persisted = 0;
        Job lastJob = null;
        for (FetchedJob fetched : chunk) {
            if (fetched == null) {
                continue;
            }
            try {
                Job persistedJob = jobService.upsert(fetched.job());
                jobDetailService.saveContent(persistedJob, fetched.content());
                lastJob = persistedJob;
                persisted++;
            } catch (Exception ex) {
                log.warn("Failed to persist job {} from source {}: {}",
                        fetched.job() == null ? "unknown" : fetched.job().getTitle(),
                        fetched.job() == null ? "unknown" : fetched.job().getSource(),
                        ex.getMessage());
                log.info("Job persistence error", ex);
            }
        }
        return new JobBatchPersistenceResult(persisted, lastJob, persisted > 0);
    }

    private List<List<FetchedJob>> splitIntoChunks(List<FetchedJob> jobs) {
        if (jobs.size() <= chunkSize) {
            return Collections.singletonList(jobs);
        }
        List<List<FetchedJob>> partitions = new ArrayList<>();
        int start = 0;
        while (start < jobs.size()) {
            int end = Math.min(start + chunkSize, jobs.size());
            partitions.add(new ArrayList<>(jobs.subList(start, end)));
            start = end;
        }
        return partitions;
    }

    public static final class JobBatchPersistenceResult {
        private static final JobBatchPersistenceResult EMPTY = new JobBatchPersistenceResult(0, null, false);

        private final int persisted;
        private final Job lastJob;
        private final boolean advanced;

        JobBatchPersistenceResult(int persisted, Job lastJob, boolean advanced) {
            this.persisted = persisted;
            this.lastJob = lastJob;
            this.advanced = advanced;
        }

        public static JobBatchPersistenceResult empty() {
            return EMPTY;
        }

        public int persisted() {
            return persisted;
        }

        public Job lastJob() {
            return lastJob;
        }

        public boolean advanced() {
            return advanced;
        }
    }
}
