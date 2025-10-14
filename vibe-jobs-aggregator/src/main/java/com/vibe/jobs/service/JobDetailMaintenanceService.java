package com.vibe.jobs.service;

import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.repo.JobDetailRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class JobDetailMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(JobDetailMaintenanceService.class);

    public static final int DEFAULT_BATCH_SIZE = 500;

    private final JobDetailRepository jobDetailRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public JobDetailMaintenanceService(JobDetailRepository jobDetailRepository) {
        this.jobDetailRepository = jobDetailRepository;
    }

    @Transactional
    public MaintenanceResult normalizeContentText(Integer requestedBatchSize) {
        int batchSize = normalizeBatchSize(requestedBatchSize);
        long processed = 0L;
        long updated = 0L;
        int batches = 0;
        int pageNumber = 0;

        Sort sort = Sort.by(Sort.Direction.ASC, "id");

        while (true) {
            Page<JobDetail> page = jobDetailRepository.findAll(PageRequest.of(pageNumber, batchSize, sort));
            if (page.isEmpty()) {
                break;
            }

            List<JobDetail> toUpdate = new ArrayList<>();
            for (JobDetail detail : page.getContent()) {
                processed++;
                String normalized = HtmlTextExtractor.toPlainText(detail.getContent());
                if (!Objects.equals(normalized, detail.getContentText())) {
                    detail.setContentText(normalized);
                    toUpdate.add(detail);
                    updated++;
                }
            }

            if (!toUpdate.isEmpty()) {
                jobDetailRepository.saveAll(toUpdate);
                if (entityManager != null) {
                    entityManager.flush();
                    entityManager.clear();
                }
            } else if (entityManager != null) {
                entityManager.clear();
            }

            batches++;

            if (!page.hasNext()) {
                break;
            }
            pageNumber++;
        }

        log.info("Normalized content text for job details: processed={}, updated={}, batches={}, batchSize={}",
                processed, updated, batches, batchSize);
        return new MaintenanceResult(processed, updated, batches, batchSize);
    }

    private int normalizeBatchSize(Integer requestedBatchSize) {
        if (requestedBatchSize == null || requestedBatchSize < 1) {
            return DEFAULT_BATCH_SIZE;
        }
        int maxBatchSize = 2000;
        return Math.min(requestedBatchSize, maxBatchSize);
    }

    public record MaintenanceResult(long processed, long updated, int batches, int batchSize) {
    }
}
