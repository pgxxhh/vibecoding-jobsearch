package com.vibe.jobs.crawler.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserField {

    private static final Logger log = LoggerFactory.getLogger(ParserField.class);

    private final String name;
    private final ParserFieldType type;
    private final String selector;
    private final String attribute;
    private final String constant;
    private final String format;
    private final String delimiter;
    private final boolean required;
    private final String baseUrl;

    public ParserField(String name,
                       ParserFieldType type,
                       String selector,
                       String attribute,
                       String constant,
                       String format,
                       String delimiter,
                       boolean required,
                       String baseUrl) {
        this.name = name == null ? "" : name.trim();
        this.type = type == null ? ParserFieldType.TEXT : type;
        this.selector = selector == null ? "" : selector.trim();
        this.attribute = attribute == null ? "" : attribute.trim();
        this.constant = constant == null ? "" : constant.trim();
        this.format = format == null ? "" : format.trim();
        this.delimiter = delimiter == null ? "," : delimiter;
        this.required = required;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    // 保持向后兼容的构造器
    public ParserField(String name,
                       ParserFieldType type,
                       String selector,
                       String attribute,
                       String constant,
                       String format,
                       String delimiter,
                       boolean required) {
        this(name, type, selector, attribute, constant, format, delimiter, required, null);
    }

    public static ParserField of(String name, ParserFieldType type, String selector) {
        return new ParserField(name, type, selector, null, null, null, ",", false, null);
    }

    public String name() {
        return name;
    }

    public ParserFieldType type() {
        return type;
    }

    public String selector() {
        return selector;
    }

    public String attribute() {
        return attribute;
    }

    public boolean required() {
        return required;
    }

    public Object extract(Element element) {
        if (element == null) {
            return null;
        }
        return switch (type) {
            case CONSTANT -> constant;
            case HTML -> select(element).stream().findFirst().map(Element::html).orElse(null);
            case ATTRIBUTE -> {
                String rawValue = select(element).stream().findFirst()
                        .map(el -> attribute.isBlank() ? el.text() : el.attr(attribute))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .orElse(null);

                if (rawValue != null && "href".equals(attribute)) {
                    if (!rawValue.startsWith("http://") && !rawValue.startsWith("https://")) {
                        String resolvedBaseUrl = !baseUrl.isBlank() ? baseUrl : extractBaseUrl(element);
                        if (resolvedBaseUrl != null) {
                            String separator = resolvedBaseUrl.endsWith("/") || rawValue.startsWith("/") ? "" : "/";
                            yield resolvedBaseUrl + separator + (rawValue.startsWith("/") ? rawValue.substring(1) : rawValue);
                        }
                    }
                }
                yield rawValue;
            }
            case LIST -> parseList(element);
            case DATE -> parseDate(element);
            case TEXT -> select(element).stream().findFirst()
                    .map(Element::text)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .orElse(null);
        };
    }

    private List<Element> select(Element element) {
        if (selector == null || selector.isBlank() || selector.trim().isEmpty()) {
            return Collections.singletonList(element);
        }
        
        // Clean the selector to avoid empty string issues
        String cleanSelector = selector.trim();
        if (cleanSelector.isEmpty()) {
            return Collections.singletonList(element);
        }
        
        try {
            Elements elements = element.select(cleanSelector);
            if (elements == null || elements.isEmpty()) {
                return List.of();
            }
            return elements.stream().toList();
        } catch (Exception e) {
            // If selector parsing fails, return the element itself
            log.debug("Selector parsing failed for '{}': {}", cleanSelector, e.getMessage());
            return Collections.singletonList(element);
        }
    }

    private Object parseDate(Element element) {
        List<Element> elements = select(element);
        if (elements.isEmpty()) {
            return null;
        }
        String value = elements.get(0).text();
        value = value == null ? null : value.trim();
        if (value == null || value.isBlank()) {
            return null;
        }
        if (format.isBlank()) {
            try {
                return Instant.parse(value);
            } catch (Exception ignored) {
                return null;
            }
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format, Locale.ROOT).withZone(ZoneOffset.UTC);
            return formatter.parse(value, Instant::from);
        } catch (Exception e) {
            return null;
        }
    }

    private Object parseList(Element element) {
        List<Element> elements = select(element);
        if (elements.isEmpty()) {
            return List.of();
        }
        String value = elements.get(0).text();
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] parts = value.split(delimiter == null || delimiter.isEmpty() ? "," : delimiter);
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }

    private String extractBaseUrl(Element element) {
        // 尝试从document的base标签获取
        if (element.ownerDocument() != null) {
            org.jsoup.nodes.Element base = element.ownerDocument().selectFirst("base[href]");
            if (base != null) {
                String baseHref = base.attr("href");
                if (!baseHref.isBlank()) {
                    return baseHref.endsWith("/") ? baseHref.substring(0, baseHref.length() - 1) : baseHref;
                }
            }
        }
        
        // 尝试从当前页面URL推断（如果有的话）
        String docLocation = element.ownerDocument() != null ? element.ownerDocument().location() : null;
        if (docLocation != null && !docLocation.isBlank()) {
            try {
                java.net.URL url = new java.net.URL(docLocation);
                return url.getProtocol() + "://" + url.getHost() + (url.getPort() != -1 && url.getPort() != 80 && url.getPort() != 443 ? ":" + url.getPort() : "");
            } catch (Exception ignored) {
                // Fallback to hardcoded for Apple Jobs (this is a compromise)
                if (docLocation.contains("jobs.apple.com")) {
                    return "https://jobs.apple.com";
                }
            }
        }
        
        // 最后的fallback - 如果能从element的HTML中推断出来
        String outerHtml = element.outerHtml();
        if (outerHtml.contains("jobs.apple.com")) {
            return "https://jobs.apple.com";
        }
        
        return null;
    }
}
