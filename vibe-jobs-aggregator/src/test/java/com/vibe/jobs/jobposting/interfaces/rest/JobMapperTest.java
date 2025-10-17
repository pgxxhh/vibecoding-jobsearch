package com.vibe.jobs.jobposting.interfaces.rest;

import com.vibe.jobs.jobposting.application.dto.JobDetailEnrichmentsDto;
import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;
import com.vibe.jobs.jobposting.interfaces.rest.dto.JobDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JobMapperTest {

    @Test
    void toDtoUsesEnrichmentProjection() {
        Job job = Job.builder()
                .id(10L)
                .source("test")
                .externalId("ext-10")
                .title("Data Engineer")
                .company("Data Corp")
                .location("Remote")
                .level("mid")
                .postedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .tags(Set.of("python", "spark"))
                .url("https://example.com/jobs/10")
                .checksum("checksum-10")
                .build();

        Map<JobEnrichmentKey, String> values = new EnumMap<>(JobEnrichmentKey.class);
        values.put(JobEnrichmentKey.SUMMARY, "\"Great role\"");
        values.put(JobEnrichmentKey.SKILLS, "[\"Python\", \"Spark\", \"  \"]");
        values.put(JobEnrichmentKey.HIGHLIGHTS, "[\"Remote\"]");
        values.put(JobEnrichmentKey.STATUS, "{\"state\":\"SUCCESS\"}");
        JobDetailEnrichmentsDto enrichmentsDto = new JobDetailEnrichmentsDto(job.getId(), values);

        JobDto dto = JobMapper.toDto(job, true, enrichmentsDto);

        assertThat(dto.summary()).isEqualTo("Great role");
        assertThat(dto.skills()).containsExactly("Python", "Spark");
        assertThat(dto.highlights()).containsExactly("Remote");
        assertThat(dto.enrichments()).containsEntry("summary", "Great role");
        assertThat(dto.enrichments()).containsEntry("skills", dto.skills());
        assertThat(dto.detailMatch()).isTrue();
    }
}
