package com.vibe.jobs.service.enrichment;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobContentEnrichmentTest {

    @Test
    void testRecordCreation() {
        JobContentEnrichment enrichment = new JobContentEnrichment(
                "职位摘要",
                List.of("Java", "Spring"),
                List.of("高薪", "福利好"),
                "{\"salary\": \"20k\"}"
        );

        assertThat(enrichment.summary()).isEqualTo("职位摘要");
        assertThat(enrichment.skills()).containsExactly("Java", "Spring");
        assertThat(enrichment.highlights()).containsExactly("高薪", "福利好");
        assertThat(enrichment.structuredData()).isEqualTo("{\"salary\": \"20k\"}");
    }

    @Test
    void testRecordWithNullValues() {
        JobContentEnrichment enrichment = new JobContentEnrichment(
                null,
                null,
                null,
                null
        );

        assertThat(enrichment.summary()).isNull();
        assertThat(enrichment.skills()).isEmpty();
        assertThat(enrichment.highlights()).isEmpty();
        assertThat(enrichment.structuredData()).isNull();
    }

    @Test
    void testRecordWithEmptyLists() {
        JobContentEnrichment enrichment = new JobContentEnrichment(
                "摘要",
                List.of(),
                List.of(),
                "{}"
        );

        assertThat(enrichment.summary()).isEqualTo("摘要");
        assertThat(enrichment.skills()).isEmpty();
        assertThat(enrichment.highlights()).isEmpty();
        assertThat(enrichment.structuredData()).isEqualTo("{}");
    }

    @Test
    void testListsAreImmutable() {
        List<String> originalSkills = new java.util.ArrayList<>(List.of("Java"));
        List<String> originalHighlights = new java.util.ArrayList<>(List.of("高薪"));
        
        JobContentEnrichment enrichment = new JobContentEnrichment(
                "摘要",
                originalSkills,
                originalHighlights,
                "{}"
        );

        // 验证返回的是不可变列表的副本
        assertThat(enrichment.skills()).isNotSameAs(originalSkills);
        assertThat(enrichment.highlights()).isNotSameAs(originalHighlights);
        
        // 验证内容相同
        assertThat(enrichment.skills()).isEqualTo(originalSkills);
        assertThat(enrichment.highlights()).isEqualTo(originalHighlights);
        
        // 验证列表是不可变的
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            enrichment.skills().add("Python");
        });
    }

    @Test
    void testEqualsAndHashCode() {
        JobContentEnrichment enrichment1 = new JobContentEnrichment(
                "摘要",
                List.of("Java"),
                List.of("高薪"),
                "{\"test\": true}"
        );
        
        JobContentEnrichment enrichment2 = new JobContentEnrichment(
                "摘要",
                List.of("Java"),
                List.of("高薪"),
                "{\"test\": true}"
        );
        
        JobContentEnrichment enrichment3 = new JobContentEnrichment(
                "不同摘要",
                List.of("Java"),
                List.of("高薪"),
                "{\"test\": true}"
        );

        assertThat(enrichment1).isEqualTo(enrichment2);
        assertThat(enrichment1.hashCode()).isEqualTo(enrichment2.hashCode());
        
        assertThat(enrichment1).isNotEqualTo(enrichment3);
        assertThat(enrichment1.hashCode()).isNotEqualTo(enrichment3.hashCode());
    }

    @Test
    void testToString() {
        JobContentEnrichment enrichment = new JobContentEnrichment(
                "职位摘要",
                List.of("Java", "Spring"),
                List.of("高薪"),
                "{\"salary\": \"20k\"}"
        );

        String toString = enrichment.toString();
        
        assertThat(toString).contains("职位摘要");
        assertThat(toString).contains("Java");
        assertThat(toString).contains("Spring");
        assertThat(toString).contains("高薪");
        assertThat(toString).contains("{\"salary\": \"20k\"}");
    }
}