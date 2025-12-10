package com.vibe.jobs.resume.application;

import com.vibe.jobs.resume.domain.ResumeProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeParsingServiceTest {

    private final ResumeParsingService parsingService = buildService();

    private ResumeParsingService buildService() {
        var props = new com.vibe.jobs.resume.config.ResumeParsingProperties();
        props.setSkillSectionHeaders(java.util.List.of("skills"));
        props.setExperienceSectionHeaders(java.util.List.of("experience", "projects"));
        props.setSummarySectionHeaders(java.util.List.of("summary"));
        props.setBulletPrefixes(java.util.List.of("-", "*", "•"));
        props.setStopWords(java.util.List.of("and", "with", "the", "a"));
        props.setTokenSplitRegex("[^\\p{L}\\p{N}+]+");
        props.setSkillsSeparatorRegex("[,;、/|]\\s*");
        props.setTopTokenLimit(12);
        props.setExperiencesMaxLines(6);
        props.setSummaryMaxChars(240);
        props.setEnableFrequencyFallback(true);
        return new ResumeParsingService(props);
    }

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
