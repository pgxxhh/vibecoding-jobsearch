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
        List<String> tokens = tokenize(normalized);
        Map<String, Long> freq = tokens.stream()
                .filter(token -> token.length() > 2)
                .collect(Collectors.groupingBy(token -> token, Collectors.counting()));
        List<String> skills = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(12)
                .map(Map.Entry::getKey)
                .toList();

        List<String> experiences = extractLines(normalized, 6);
        String summary = normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
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

    private List<String> tokenize(String text) {
        return List.of(text.toLowerCase(Locale.ROOT).split("[^a-zA-Z0-9+一-龥]+"));
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
            if (distinct.size() >= maxLines) {
                break;
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(distinct));
    }
}
