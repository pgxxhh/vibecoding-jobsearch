package com.vibe.jobs.crawler.domain;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Instant;
import java.util.*;

public class ParserProfile {

    private final String listSelector;
    private final Map<String, ParserField> fields;
    private final Set<String> tagFields;
    private final String descriptionField;

    private ParserProfile(String listSelector,
                          Map<String, ParserField> fields,
                          Set<String> tagFields,
                          String descriptionField) {
        this.listSelector = listSelector == null ? "" : listSelector.trim();
        this.fields = fields == null ? Map.of() : Map.copyOf(fields);
        this.tagFields = tagFields == null ? Set.of() : Set.copyOf(tagFields);
        this.descriptionField = descriptionField == null ? "" : descriptionField.trim();
    }

    public static ParserProfile empty() {
        return new ParserProfile("", Map.of(), Set.of(), "");
    }

    public static ParserProfile of(String listSelector,
                                   Map<String, ParserField> fields,
                                   Set<String> tagFields,
                                   String descriptionField) {
        return new ParserProfile(listSelector, fields, tagFields, descriptionField);
    }

    public String listSelector() {
        return listSelector;
    }

    public Map<String, ParserField> fields() {
        return fields;
    }

    public boolean isConfigured() {
        return !listSelector.isBlank() && fields.containsKey("title");
    }

    public List<ParsedJob> parse(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Elements elements = org.jsoup.Jsoup.parse(html).select(listSelector);
        if (elements == null || elements.isEmpty()) {
            return List.of();
        }
        List<ParsedJob> result = new ArrayList<>();
        for (Element element : elements) {
            ParsedJob job = parse(element);
            if (job != null) {
                result.add(job);
            }
        }
        return result;
    }

    public ParsedJob parse(Element element) {
        if (element == null) {
            return null;
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, ParserField> entry : fields.entrySet()) {
            Object value = entry.getValue().extract(element);
            if (value == null && entry.getValue().required()) {
                return null;
            }
            if (value != null) {
                values.put(entry.getKey(), value);
            }
        }
        if (!values.containsKey("title")) {
            return null;
        }
        String description = descriptionField.isBlank() ? element.html() : Objects.toString(values.get(descriptionField), element.html());
        Set<String> tags = new LinkedHashSet<>();
        for (String tagField : tagFields) {
            Object raw = values.get(tagField);
            if (raw instanceof Collection<?> collection) {
                for (Object value : collection) {
                    if (value != null) {
                        String text = value.toString().trim();
                        if (!text.isBlank()) {
                            tags.add(text.toLowerCase(Locale.ROOT));
                        }
                    }
                }
            } else if (raw != null) {
                String text = raw.toString().trim();
                if (!text.isBlank()) {
                    tags.add(text.toLowerCase(Locale.ROOT));
                }
            }
        }
        return new ParsedJob(
                Objects.toString(values.getOrDefault("externalId", UUID.randomUUID().toString()), ""),
                Objects.toString(values.get("title"), ""),
                Objects.toString(values.getOrDefault("company", ""), ""),
                Objects.toString(values.getOrDefault("location", ""), ""),
                Objects.toString(values.getOrDefault("url", ""), ""),
                Objects.toString(values.getOrDefault("level", ""), ""),
                values.get("postedAt") instanceof Instant instant ? instant : null,
                tags,
                description
        );
    }

    public record ParsedJob(String externalId,
                             String title,
                             String company,
                             String location,
                             String url,
                             String level,
                             Instant postedAt,
                             Set<String> tags,
                             String description) {
    }
}
