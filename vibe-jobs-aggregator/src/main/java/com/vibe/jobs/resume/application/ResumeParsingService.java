package com.vibe.jobs.resume.application;

import com.vibe.jobs.resume.domain.ResumeProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ResumeParsingService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParsingService.class);
    private final com.vibe.jobs.resume.config.ResumeParsingProperties props;

    public ResumeParsingService(com.vibe.jobs.resume.config.ResumeParsingProperties props) {
        this.props = props;
    }

    public ResumeProfile parse(byte[] content) {
        if (content == null || content.length == 0) {
            log.warn("Received empty resume content for parsing");
            return ResumeProfile.builder().build();
        }
        String text;
        try {
            org.apache.tika.Tika tika = new org.apache.tika.Tika();
            text = tika.parseToString(new java.io.ByteArrayInputStream(content));
        } catch (Exception ex) {
            log.warn("Tika failed to parse content, fallback to UTF-8 raw bytes", ex);
            text = new String(content, detectEncoding(content));
        }
        String normalized = normalizeWhitespace(text);

        String skillsSection = extractSection(normalized, props.getSkillSectionHeaders());
        List<String> skills = (skillsSection != null && !skillsSection.isBlank())
                ? splitSkills(skillsSection)
                : (props.isEnableFrequencyFallback() ? topFrequentTokens(normalized, props.getTopTokenLimit()) : new ArrayList<>());

        if (skills.isEmpty() && props.isEnableFrequencyFallback()) {
            skills = topFrequentTokens(normalized, props.getTopTokenLimit());
        }

        String expSection = extractSection(normalized, props.getExperienceSectionHeaders());
        List<String> experiences = (expSection != null && !expSection.isBlank())
                ? extractBullets(expSection, props.getExperiencesMaxLines())
                : extractLines(normalized, props.getExperiencesMaxLines());

        String summarySection = extractSection(normalized, props.getSummarySectionHeaders());
        String baseSummary = (summarySection != null && !summarySection.isBlank()) ? summarySection : normalized;
        int maxChars = props.getSummaryMaxChars() > 0 ? props.getSummaryMaxChars() : baseSummary.length();
        String summary = baseSummary.length() > maxChars ? baseSummary.substring(0, maxChars) : baseSummary;

        return ResumeProfile.builder()
                .rawText(normalized)
                .skills(new ArrayList<>(skills))
                .experiences(experiences)
                .summary(summary)
                .build();
    }

    public String detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            log.warn("Cannot detect language for blank resume text");
            return "unknown";
        }
        long han = text.chars().filter(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN).count();
        long latin = text.chars().filter(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.LATIN).count();
        if (han > latin) {
            return "zh";
        }
        return Locale.ENGLISH.getLanguage();
    }

    private Charset detectEncoding(byte[] content) {
        return StandardCharsets.UTF_8;
    }

    private String extractSection(String text, List<String> headers) {
        if (headers == null || headers.isEmpty() || text == null || text.isBlank()) {
            return null;
        }
        // Build a simple regex to capture content after a header until the next header or end
        String alternation = headers.stream()
                .filter(h -> h != null && !h.isBlank())
                .map(java.util.regex.Pattern::quote)
                .collect(Collectors.joining("|"));
        if (alternation.isBlank()) return null;
        String pattern = "(?ims)^(?:" + alternation + ")\\b[\\s:：-]*\\n?(.*?)(?=^(?:" + alternation + ")\\b|\\z)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private List<String> splitSkills(String section) {
        String sep = props.getSkillsSeparatorRegex();
        if (sep == null || sep.isBlank()) return List.of(section.trim());
        return java.util.Arrays.stream(section.split(sep))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private List<String> extractBullets(String section, int maxLines) {
        List<String> bullets = new ArrayList<>();
        List<String> prefixes = props.getBulletPrefixes();
        for (String line : section.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            boolean isBullet = false;
            if (prefixes != null && !prefixes.isEmpty()) {
                for (String p : prefixes) {
                    if (p != null && !p.isEmpty() && trimmed.startsWith(p + " ")) { isBullet = true; break; }
                }
            }
            if (isBullet || !trimmed.isEmpty()) {
                bullets.add(trimmed);
            }
            if (maxLines > 0 && bullets.size() >= maxLines) break;
        }
        return bullets;
    }

    private List<String> topFrequentTokens(String text, int limit) {
        List<String> tokens = tokenize(text);
        java.util.Set<String> stop = new java.util.HashSet<>(props.getStopWords() == null ? List.of() : props.getStopWords());
        Map<String, Long> freq = tokens.stream()
                .filter(token -> token.length() > 2)
                .filter(token -> !stop.contains(token))
                .collect(Collectors.groupingBy(token -> token, Collectors.counting()));
        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<String> tokenize(String text) {
        String regex = props.getTokenSplitRegex();
        if (regex == null || regex.isBlank()) {
            regex = "\\s+"; // fallback to whitespace if not configured
        }
        return List.of(text.toLowerCase(Locale.ROOT).split(regex));
    }

    private String normalizeWhitespace(String text) {
        return text.replaceAll("\r", " ").replaceAll("\n+", "\n").trim();
    }

    private List<String> extractLines(String text, int maxLines) {
        String[] lines = text.split("\n");
        List<String> distinct = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                distinct.add(trimmed);
            }
            if (maxLines > 0 && distinct.size() >= maxLines) {
                break;
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(distinct));
    }
}
