package com.vibe.jobs.resume.application;

import com.vibe.jobs.resume.domain.ResumeProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeParsingServiceTest {

    private final ResumeParsingService parsingService = new ResumeParsingService();

    @Test
    void parseExtractsSkillsAndSummary() {
        String content = "Java developer with Spring and SQL experience\nBuilt services and APIs";
        ResumeProfile profile = parsingService.parse(content.getBytes());
        assertThat(profile.getSkills()).contains("java", "developer");
        assertThat(profile.getSummary()).contains("Java developer");
    }

    @Test
    void detectLanguageDetectsChinese() {
        String text = "熟悉 Java 和分布式";
        assertThat(parsingService.detectLanguage(text)).isEqualTo("zh");
    }
}
