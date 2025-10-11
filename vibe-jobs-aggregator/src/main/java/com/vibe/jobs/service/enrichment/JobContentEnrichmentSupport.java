package com.vibe.jobs.service.enrichment;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared helpers for LLM backed job content enrichment providers.
 */
final class JobContentEnrichmentSupport {

    private static final int DEFAULT_CONTENT_TEXT_LIMIT = 6000;
    private static final int DEFAULT_RAW_CONTENT_LIMIT = 4000;

    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是一个帮助提炼职位描述信息的助手，需输出 JSON 对象，字段包括:",
            "- summary: 200 字以内的中文摘要，突出核心职责与要求。",
            "- skills: 字符串数组，列出 3~8 个关键技能，使用简体中文。",
            "- highlights: 字符串数组，列出亮点或福利，如无则返回空数组。",
            "- structured: JSON 对象，可包含 salary, experienceLevel, employmentType 等可选键，值保持原始语言。",
            "请勿额外输出解释或 markdown。"
    );

    private static final String SCHEMA_NAME = "job_detail_enrichment";

    private static final Map<String, Object> RESPONSE_SCHEMA = buildSchema();

    private JobContentEnrichmentSupport() {
    }

    static String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    static String schemaName() {
        return SCHEMA_NAME;
    }

    static Map<String, Object> responseSchema() {
        return RESPONSE_SCHEMA;
    }

    static String buildUserPrompt(JobSnapshot job, String rawContent, String contentText) {
        return buildUserPrompt(job, rawContent, contentText, DEFAULT_CONTENT_TEXT_LIMIT, DEFAULT_RAW_CONTENT_LIMIT);
    }

    static String buildUserPrompt(JobSnapshot job, String rawContent, String contentText, int contentLimit, int rawLimit) {
        StringBuilder userPrompt = new StringBuilder();
        if (job != null) {
            if (StringUtils.hasText(job.title())) {
                userPrompt.append("职位: ").append(job.title()).append('\n');
            }
            if (StringUtils.hasText(job.company())) {
                userPrompt.append("公司: ").append(job.company()).append('\n');
            }
            if (StringUtils.hasText(job.location())) {
                userPrompt.append("地点: ").append(job.location()).append('\n');
            }
        }
        userPrompt.append("\n职位描述（纯文本）:\n");
        userPrompt.append(truncate(contentText, contentLimit));
        if (StringUtils.hasText(rawContent)) {
            userPrompt.append("\n\n职位描述（原始 HTML，仅供参考）:\n");
            userPrompt.append(truncate(rawContent, rawLimit));
        }
        return userPrompt.toString();
    }

    static List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().replaceAll("\\s+", " "))
                .collect(Collectors.toList());
    }

    static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    static String truncate(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (maxChars <= 0) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildSchema() {
        Map<String, Object> structuredProperties = Map.ofEntries(
                Map.entry("summary", Map.of("type", "string")),
                Map.entry("details", Map.of("type", "string")),
                Map.entry("salary", Map.of("type", "string")),
                Map.entry("experienceLevel", Map.of("type", "string")),
                Map.entry("employmentType", Map.of("type", "string")),
                Map.entry("location", Map.of("type", "string")),
                Map.entry("remotePolicy", Map.of("type", "string")),
                Map.entry("compensation", Map.of("type", "string")),
                Map.entry("requirements", Map.of("type", "string")),
                Map.entry("benefits", Map.of("type", "string")),
                Map.entry("notes", Map.of("type", "string")),
                Map.entry("keywords", Map.of("type", "string")),
                Map.entry("tags", Map.of("type", "string")),
                Map.entry("industry", Map.of("type", "string")),
                Map.entry("level", Map.of("type", "string")),
                Map.entry("language", Map.of("type", "string")),
                Map.entry("education", Map.of("type", "string")),
                Map.entry("experience", Map.of("type", "string")),
                Map.entry("skills", Map.of("type", "string")),
                Map.entry("responsibilities", Map.of("type", "string")),
                Map.entry("requirementsSummary", Map.of("type", "string")),
                Map.entry("benefitsSummary", Map.of("type", "string")),
                Map.entry("company", Map.of("type", "string")),
                Map.entry("department", Map.of("type", "string")),
                Map.entry("team", Map.of("type", "string")),
                Map.entry("project", Map.of("type", "string")),
                Map.entry("mission", Map.of("type", "string")),
                Map.entry("vision", Map.of("type", "string")),
                Map.entry("culture", Map.of("type", "string")),
                Map.entry("values", Map.of("type", "string")),
                Map.entry("travelRequirements", Map.of("type", "string")),
                Map.entry("certifications", Map.of("type", "string")),
                Map.entry("securityClearance", Map.of("type", "string")),
                Map.entry("visaSponsorship", Map.of("type", "string")),
                Map.entry("contractLength", Map.of("type", "string")),
                Map.entry("workHours", Map.of("type", "string")),
                Map.entry("startDate", Map.of("type", "string")),
                Map.entry("endDate", Map.of("type", "string")),
                Map.entry("deadline", Map.of("type", "string")),
                Map.entry("applicationProcess", Map.of("type", "string")),
                Map.entry("other", Map.of("type", "string"))
        );
        List<String> structuredRequired = List.copyOf(structuredProperties.keySet());
        Map<String, Object> structuredSchema = Map.of(
                "type", "object",
                "properties", structuredProperties,
                "required", structuredRequired,
                "additionalProperties", false
        );

        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "summary", Map.of(
                                "type", "string",
                                "description", "中文摘要"
                        ),
                        "skills", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "关键技能列表"
                        ),
                        "highlights", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "职位亮点"
                        ),
                        "structured", structuredSchema
                ),
                "required", List.of("summary", "skills", "highlights", "structured"),
                "additionalProperties", false
        );
    }
}
