package com.vibe.jobs.web;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.domain.JobDetailEnrichment;
import com.vibe.jobs.domain.JobEnrichmentKey;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobEnrichmentExtractorTest {

    @Test
    void shouldExtractAllFieldsWhenStatusSuccess() {
        JobDetail detail = newDetail();
        addEnrichment(detail, JobEnrichmentKey.STATUS, "{\"state\":\"SUCCESS\",\"source\":\"test\"}");
        addEnrichment(detail, JobEnrichmentKey.SUMMARY, "\"  Senior Developer  \"");
        addEnrichment(detail, JobEnrichmentKey.SKILLS, "[\"Java\",\"\", \" Kotlin \"]");
        addEnrichment(detail, JobEnrichmentKey.HIGHLIGHTS, "[\"Perk A\",\"Perk B\"]");
        addEnrichment(detail, JobEnrichmentKey.STRUCTURED_DATA, "{\"foo\":1,\"bar\":\"baz\"}");

        JobEnrichmentExtractor.EnrichmentView view = JobEnrichmentExtractor.extract(detail);

        assertThat(view.summary()).contains("Senior Developer");
        assertThat(view.skills()).containsExactly("Java", "Kotlin");
        assertThat(view.highlights()).containsExactly("Perk A", "Perk B");
        assertThat(view.structured()).contains("{\"foo\":1,\"bar\":\"baz\"}");
        assertThat(view.status()).isPresent();
        assertThat(view.status().orElseThrow())
                .containsEntry("state", "SUCCESS")
                .containsEntry("source", "test");
        assertThat(view.enrichments())
                .containsKeys("summary", "skills", "highlights", "structured_data", "status");
        assertThat(view.enrichments().get("summary")).isEqualTo("  Senior Developer  ");
        assertThat(view.enrichments().get("status")).isInstanceOf(Map.class);

        assertThatThrownBy(() -> view.skills().add("Python"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> view.highlights().add("New"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> view.enrichments().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> view.status().orElseThrow().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(JobEnrichmentExtractor.summary(detail)).isEqualTo(view.summary());
        assertThat(JobEnrichmentExtractor.skills(detail)).isEqualTo(view.skills());
        assertThat(JobEnrichmentExtractor.highlights(detail)).isEqualTo(view.highlights());
        assertThat(JobEnrichmentExtractor.structured(detail)).isEqualTo(view.structured());
        assertThat(JobEnrichmentExtractor.status(detail)).isEqualTo(view.status());
        assertThat(JobEnrichmentExtractor.enrichments(detail)).isEqualTo(view.enrichments());
    }

    @Test
    void shouldSkipSummaryAndListsWhenStatusNotSuccessful() {
        JobDetail detail = newDetail();
        addEnrichment(detail, JobEnrichmentKey.STATUS, "{\"state\":\"FAILED\"}");
        addEnrichment(detail, JobEnrichmentKey.SUMMARY, "\"Role summary\"");
        addEnrichment(detail, JobEnrichmentKey.SKILLS, "[\"Go\"]");

        JobEnrichmentExtractor.EnrichmentView view = JobEnrichmentExtractor.extract(detail);

        assertThat(view.summary()).isEmpty();
        assertThat(view.skills()).isEmpty();
        assertThat(view.highlights()).isEmpty();
        assertThat(view.structured()).isEmpty();
        assertThat(view.status()).isPresent();
        assertThat(view.status().orElseThrow()).containsEntry("state", "FAILED");
        assertThat(view.enrichments()).containsEntry("summary", "Role summary");

        assertThat(JobEnrichmentExtractor.summary(detail)).isEmpty();
        assertThat(JobEnrichmentExtractor.skills(detail)).isEmpty();
        assertThat(JobEnrichmentExtractor.highlights(detail)).isEmpty();
        assertThat(JobEnrichmentExtractor.structured(detail)).isEmpty();
    }

    @Test
    void shouldHandleMissingAndInvalidPayloads() {
        JobDetail detail = newDetail();
        addEnrichment(detail, JobEnrichmentKey.STATUS, "{\"state\":\"SUCCESS\"}");
        addEnrichment(detail, JobEnrichmentKey.SUMMARY, "\"  \"");
        addEnrichment(detail, JobEnrichmentKey.SKILLS, "\"plain\"");
        addEnrichment(detail, JobEnrichmentKey.STRUCTURED_DATA, "{invalid");
        addEnrichment(detail, JobEnrichmentKey.HIGHLIGHTS, null);

        JobEnrichmentExtractor.EnrichmentView view = JobEnrichmentExtractor.extract(detail);

        assertThat(view.summary()).isEmpty();
        assertThat(view.skills()).isEmpty();
        assertThat(view.highlights()).isEmpty();
        assertThat(view.structured()).isEmpty();
        assertThat(view.status()).isPresent();
        assertThat(view.enrichments()).containsEntry("summary", "  ");
        assertThat(view.enrichments()).containsEntry("skills", "plain");
        assertThat(view.enrichments()).doesNotContainKey("structured_data");

        JobEnrichmentExtractor.EnrichmentView emptyView = JobEnrichmentExtractor.extract(newDetail());
        assertThat(emptyView.summary()).isEmpty();
        assertThat(emptyView.skills()).isEmpty();
        assertThat(emptyView.enrichments()).isEmpty();

        assertThat(JobEnrichmentExtractor.extract(null)).isSameAs(JobEnrichmentExtractor.EnrichmentView.empty());
    }

    private static JobDetail newDetail() {
        return new JobDetail(new Job(), "", "");
    }

    private static void addEnrichment(JobDetail detail, JobEnrichmentKey key, String json) {
        JobDetailEnrichment enrichment = new JobDetailEnrichment(detail, key);
        if (json != null) {
            enrichment.updateValue(json, null, null, null, null);
        }
        detail.getEnrichments().add(enrichment);
    }
}
