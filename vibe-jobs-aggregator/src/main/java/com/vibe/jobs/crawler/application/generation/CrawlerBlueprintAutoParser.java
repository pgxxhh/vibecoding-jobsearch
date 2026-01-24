package com.vibe.jobs.crawler.application.generation;

import com.vibe.jobs.crawler.domain.AutomationSettings;
import com.vibe.jobs.crawler.domain.CrawlFlow;
import com.vibe.jobs.crawler.domain.CrawlStep;
import com.vibe.jobs.crawler.domain.CrawlStepType;
import com.vibe.jobs.crawler.domain.PagingStrategy;
import com.vibe.jobs.crawler.domain.ParserField;
import com.vibe.jobs.crawler.domain.ParserFieldType;
import com.vibe.jobs.crawler.domain.ParserProfile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class CrawlerBlueprintAutoParser {

    private static final Logger log = LoggerFactory.getLogger(CrawlerBlueprintAutoParser.class);

    private static final Set<String> DISQUALIFYING_TAGS = Set.of("nav", "header", "footer", "aside");
    private static final Set<String> DISQUALIFYING_CLASS_KEYWORDS = Set.of(
            "breadcrumb",
            "header",
            "footer",
            "nav",
            "menu",
            "locale",
            "language",
            "share",
            "social",
            "pagination",
            "pager"
    );
    private static final Set<String> PAGINATION_TEXT_KEYWORDS = Set.of(
            "next",
            "previous",
            "prev",
            "first",
            "last",
            "older",
            "newer",
            "more",
            "less",
            "weiter",
            "zuruck",
            "suivant",
            "precedent",
            "nachste",
            "vorherige",
            "siguiente",
            "anterior",
            "weiterlesen",
            "\u4e0a\u4e00\u9875",
            "\u4e0b\u4e00\u9875",
            "\u9996\u9875",
            "\u672b\u9875"
    );
    private static final List<String> COMPANY_FIELD_SELECTORS = List.of(
            "[class*=company]",
            "[class*=employer]",
            "[data-company]",
            "span:matches((?i)company)",
            "p:matches((?i)company)",
            "div:matches((?i)company)"
    );
    private static final List<String> LOCATION_FIELD_SELECTORS = List.of(
            "[class*=location]",
            "[class*=city]",
            "span:matches((?i)location)",
            "p:matches((?i)location)",
            "div:matches((?i)location)"
    );

    public AutoParseResult parse(String entryUrl, String html) {
        Document document = Jsoup.parse(html == null ? "" : html);
        if (document.body() == null) {
            throw new IllegalArgumentException("Empty HTML document");
        }

        Element listElement = findBestListElement(document);
        if (listElement == null) {
            throw new IllegalStateException("Unable to determine repeating job element");
        }
        String listSelector = safeCssSelector(listElement)
                .map(this::simplifySelector)
                .orElseThrow(() -> new IllegalStateException("Unable to build CSS selector for job list element"));

        Map<String, ParserField> fields = buildFields(listElement, entryUrl);
        if (!fields.containsKey("title")) {
            throw new IllegalStateException("Failed to detect title field in job listing");
        }

        ParserProfile profile = ParserProfile.of(
                listSelector,
                fields,
                Set.of(),
                "",
                ParserProfile.DetailFetchConfig.disabled()
        );

        PagingStrategy paging = detectPagingStrategy(document, entryUrl);
        AutomationSettings automation = AutomationSettings.disabled();
        CrawlFlow flow = CrawlFlow.empty();
        Map<String, Object> metadata = Map.of();

        return new AutoParseResult(profile, paging, automation, flow, metadata);
    }

    private Map<String, ParserField> buildFields(Element listElement, String entryUrl) {
        Map<String, ParserField> fields = new LinkedHashMap<>();
        addAnchorFields(listElement, entryUrl, fields);
        addOptionalField(listElement, fields, "company", ParserFieldType.TEXT, COMPANY_FIELD_SELECTORS);
        addOptionalField(listElement, fields, "location", ParserFieldType.TEXT, LOCATION_FIELD_SELECTORS);
        addCompanyConstantIfMissing(listElement, entryUrl, fields);
        return fields;
    }

    private void addAnchorFields(Element listElement, String entryUrl, Map<String, ParserField> fields) {
        Element anchor = listElement.selectFirst("a[href]");
        if (anchor == null) {
            return;
        }
        String relativeAnchor = cleanFieldSelector(relativeSelector(listElement, anchor));
        fields.put("title", newField("title", ParserFieldType.TEXT, relativeAnchor, null, null, true, null));
        fields.put("url", newField("url", ParserFieldType.ATTRIBUTE, relativeAnchor, "href", null, true, baseUrl(entryUrl)));
    }

    private void addOptionalField(Element listElement,
                                  Map<String, ParserField> fields,
                                  String fieldName,
                                  ParserFieldType type,
                                  List<String> selectors) {
        Element candidate = findFirst(listElement, selectors);
        if (candidate == null) {
            return;
        }
        String relative = cleanFieldSelector(relativeSelector(listElement, candidate));
        fields.put(fieldName, newField(fieldName, type, relative, null, null, false, null));
    }

    private void addCompanyConstantIfMissing(Element listElement,
                                             String entryUrl,
                                             Map<String, ParserField> fields) {
        if (fields.containsKey("company")) {
            return;
        }
        inferCompanyConstant(listElement, entryUrl)
                .ifPresent(constant -> fields.put("company", newField(
                        "company",
                        ParserFieldType.CONSTANT,
                        "",
                        null,
                        constant,
                        false,
                        null
                )));
    }

    private ParserField newField(String name,
                                 ParserFieldType type,
                                 String selector,
                                 String attribute,
                                 String constant,
                                 boolean required,
                                 String baseUrl) {
        return new ParserField(
                name,
                type,
                selector,
                attribute,
                constant,
                null,
                ",",
                required,
                baseUrl
        );
    }

    private String simplifySelector(String selector) {
        if (selector == null) {
            return "";
        }
        String simplified = selector.trim();
        simplified = simplified.replaceFirst("^html[^>]*>\\s*", "");
        simplified = simplified.replaceFirst("^body[^>]*>\\s*", "");
        return simplified.isBlank() ? selector : simplified;
    }

    private String cleanFieldSelector(String selector) {
        if (selector == null || selector.isBlank()) {
            return selector;
        }
        String cleaned = selector;
        cleaned = cleaned.replaceFirst("^html[^>]*>\\s*", "");
        cleaned = cleaned.replaceFirst("^body[^>]*>\\s*", "");
        cleaned = cleaned.replaceAll("tr\\.data-row:nth-child\\(\\d+\\)", "tr.data-row");
        cleaned = cleaned.replaceAll(":nth-child\\(\\d+\\)", "");
        cleaned = cleaned.replaceAll(":first-child", "");
        cleaned = cleaned.replaceAll(":last-child", "");
        cleaned = cleaned.replaceAll(":nth-of-type\\(\\d+\\)", "");
        cleaned = cleaned.replaceAll(":first-of-type", "");
        return cleaned.trim();
    }

    private Optional<String> inferCompanyConstant(Element listElement, String entryUrl) {
        Document document = listElement == null ? null : listElement.ownerDocument();
        String candidate = extractMetaCompany(document);
        if (candidate.isBlank()) {
            candidate = extractTitleCompany(document);
        }
        if (candidate.isBlank()) {
            candidate = extractHostCompany(entryUrl);
        }
        String normalized = normalizeCompany(candidate);
        return normalized.isBlank() ? Optional.empty() : Optional.of(normalized);
    }

    private String extractMetaCompany(Document document) {
        if (document == null) {
            return "";
        }
        Element meta = document.selectFirst("meta[property=og:site_name], meta[name=application-name], meta[name=site_name], meta[name=og:site_name]");
        if (meta == null) {
            return "";
        }
        return meta.attr("content").trim();
    }

    private String extractTitleCompany(Document document) {
        if (document == null) {
            return "";
        }
        String title = document.title() == null ? "" : document.title().trim();
        if (title.isBlank()) {
            return "";
        }
        String lowered = title.toLowerCase(Locale.ROOT);
        int idx = firstKeywordIndex(lowered, List.of(" jobs", " careers", " openings"));
        if (idx > 0) {
            return title.substring(0, idx).trim();
        }
        return title;
    }

    private int firstKeywordIndex(String text, List<String> keywords) {
        int min = Integer.MAX_VALUE;
        for (String keyword : keywords) {
            int idx = text.indexOf(keyword);
            if (idx >= 0 && idx < min) {
                min = idx;
            }
        }
        return min == Integer.MAX_VALUE ? -1 : min;
    }

    private String extractHostCompany(String entryUrl) {
        if (entryUrl == null || entryUrl.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(entryUrl);
            String host = Optional.ofNullable(uri.getHost()).orElse("");
            if (host.isBlank()) {
                return "";
            }
            String[] parts = host.split("\\.");
            if (parts.length == 0) {
                return "";
            }
            List<String> blacklist = List.of("jobs", "job", "careers", "career", "www", "app", "apply");
            for (String part : parts) {
                String cleaned = part.trim().toLowerCase(Locale.ROOT);
                if (!cleaned.isBlank() && !blacklist.contains(cleaned)) {
                    return cleaned;
                }
            }
            return parts[0];
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private String normalizeCompany(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isBlank()) {
            return "";
        }
        String cleaned = Pattern.compile("[^a-zA-Z0-9\\s]").matcher(trimmed).replaceAll(" ").trim();
        if (cleaned.isBlank()) {
            cleaned = trimmed;
        }
        String[] tokens = cleaned.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            String normalized = token.length() <= 3
                    ? token.toUpperCase(Locale.ROOT)
                    : token.substring(0, 1).toUpperCase(Locale.ROOT) + token.substring(1).toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(normalized);
            }
        }
        return builder.toString().trim();
    }

    private Element findBestListElement(Document document) {
        Elements anchors = document.select("a[href]");
        Map<String, Integer> scores = new LinkedHashMap<>();
        Map<String, Element> samples = new LinkedHashMap<>();
        for (Element anchor : anchors) {
            Element candidate = anchor;
            for (int depth = 0; depth < 6 && candidate != null; depth++) {
                if (candidate.tagName().equalsIgnoreCase("a")) {
                    candidate = candidate.parent();
                    continue;
                }
                Optional<String> selectorOpt = safeCssSelector(candidate);
                if (selectorOpt.isEmpty()) {
                    candidate = candidate.parent();
                    continue;
                }
                String selector = selectorOpt.get();
                scores.merge(selector, 1, Integer::sum);
                samples.putIfAbsent(selector, candidate);
                candidate = candidate.parent();
            }
        }

        Element preferred = scores.entrySet().stream()
                .filter(entry -> entry.getValue() >= 3)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(entry -> samples.get(entry.getKey()))
                .filter(this::isLikelyJobList)
                .findFirst()
                .orElse(null);
        if (preferred != null) {
            return preferred;
        }

        return scores.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(entry -> samples.get(entry.getKey()))
                .filter(this::isLikelyJobList)
                .findFirst()
                .orElse(null);
    }

    private boolean isLikelyJobList(Element element) {
        if (element == null) {
            return false;
        }
        if (containsDisqualifyingContext(element)) {
            return false;
        }
        if (looksLikePagination(element)) {
            return false;
        }
        int anchorCount = element.select("a[href]").size();
        return anchorCount >= 2;
    }

    private boolean containsDisqualifyingContext(Element element) {
        Element current = element;
        int depth = 0;
        while (current != null && depth < 6) {
            String tag = current.tagName().toLowerCase(Locale.ROOT);
            if (DISQUALIFYING_TAGS.contains(tag)) {
                return true;
            }
            String className = current.className().toLowerCase(Locale.ROOT);
            if (!className.isBlank() && containsKeyword(className, DISQUALIFYING_CLASS_KEYWORDS)) {
                return true;
            }
            String id = current.id() == null ? "" : current.id().toLowerCase(Locale.ROOT);
            if (!id.isBlank() && containsKeyword(id, DISQUALIFYING_CLASS_KEYWORDS)) {
                return true;
            }
            current = current.parent();
            depth++;
        }
        return false;
    }

    private boolean looksLikePagination(Element element) {
        Elements anchors = element.select("a[href]");
        if (anchors.size() < 2) {
            return false;
        }
        int paginationLinks = 0;
        for (Element anchor : anchors) {
            if (isPaginationLink(anchor)) {
                paginationLinks++;
            }
        }
        if (paginationLinks == 0) {
            return false;
        }
        double ratio = (double) paginationLinks / anchors.size();
        return ratio >= 0.6;
    }

    private boolean isPaginationLink(Element anchor) {
        String text = anchor.text() == null ? "" : anchor.text().trim().toLowerCase(Locale.ROOT);
        if (!text.isBlank()) {
            if (PAGINATION_TEXT_KEYWORDS.contains(text)) {
                return true;
            }
            if (text.length() <= 4 && text.chars().allMatch(Character::isDigit)) {
                return true;
            }
            if (text.length() <= 3 && text.chars().allMatch(ch -> "\u00ab\u00bb<>".indexOf(ch) >= 0)) {
                return true;
            }
        }
        String ariaLabel = anchor.attr("aria-label").trim().toLowerCase(Locale.ROOT);
        if (!ariaLabel.isBlank() && PAGINATION_TEXT_KEYWORDS.contains(ariaLabel)) {
            return true;
        }
        String className = anchor.className().toLowerCase(Locale.ROOT);
        return containsKeyword(className, Set.of("pagination", "pager"));
    }

    private boolean containsKeyword(String value, Set<String> keywords) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private Element findFirst(Element root, List<String> selectors) {
        for (String selector : selectors) {
            if (selector == null || selector.isBlank()) {
                continue;
            }
            Element element = root.selectFirst(selector);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    private String relativeSelector(Element parent, Element target) {
        if (parent == null || target == null) {
            return "";
        }
        Optional<String> parentSelector = safeCssSelector(parent);
        Optional<String> childSelector = safeCssSelector(target);
        if (childSelector.isEmpty()) {
            return "";
        }
        String child = childSelector.get();
        if (parentSelector.isPresent() && child.startsWith(parentSelector.get())) {
            String stripped = child.substring(parentSelector.get().length());
            if (stripped.startsWith(" > ")) {
                stripped = stripped.substring(3);
            }
            return stripped.isBlank() ? "." : stripped.trim();
        }
        return child;
    }

    private PagingStrategy detectPagingStrategy(Document document, String entryUrl) {
        Element nextLink = document.select("a[href]").stream()
                .filter(el -> {
                    String text = el.text().toLowerCase(Locale.ROOT);
                    return text.contains("next") || text.contains("更多") || text.contains("下一") || text.contains("›");
                })
                .findFirst()
                .orElse(null);
        if (nextLink == null) {
            return PagingStrategy.disabled();
        }
        String href = nextLink.absUrl("href");
        if (href.isBlank()) {
            href = nextLink.attr("href");
            if (!href.startsWith("http")) {
                href = resolveRelative(entryUrl, href);
            }
        }
        if (href == null || href.isBlank()) {
            return PagingStrategy.disabled();
        }
        try {
            URI uri = new URI(href);
            String query = uri.getQuery();
            if (query != null) {
                for (String part : query.split("&")) {
                    String[] pair = part.split("=");
                    if (pair.length == 2) {
                        String key = pair[0].toLowerCase(Locale.ROOT);
                        if (isPageParameter(key)) {
                            return PagingStrategy.query(pair[0], 1, 1, null);
                        }
                        if (isOffsetParameter(key)) {
                            return PagingStrategy.offset(pair[0], 1, 1, null);
                        }
                    }
                }
            }
        } catch (URISyntaxException e) {
            log.info("Failed to parse paging URL: {}", e.getMessage());
        }
        return PagingStrategy.disabled();
    }

    private boolean isPageParameter(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("page") || normalized.equals("p");
    }

    private boolean isOffsetParameter(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("offset")
                || normalized.contains("startrow")
                || normalized.contains("rowstart")
                || normalized.contains("startindex")
                || normalized.equals("start")
                || normalized.equals("from");
    }

    private String baseUrl(String entryUrl) {
        if (entryUrl == null || entryUrl.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(entryUrl);
            String scheme = Optional.ofNullable(uri.getScheme()).orElse("http");
            String host = Optional.ofNullable(uri.getHost()).orElse("");
            if (host.isBlank()) {
                return entryUrl;
            }
            String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
            return scheme + "://" + host + port;
        } catch (URISyntaxException e) {
            return entryUrl;
        }
    }

    private String resolveRelative(String entryUrl, String relative) {
        if (relative == null || relative.isBlank()) {
            return "";
        }
        if (relative.startsWith("http://") || relative.startsWith("https://")) {
            return relative;
        }
        String base = baseUrl(entryUrl);
        if (base.endsWith("/") && relative.startsWith("/")) {
            return base + relative.substring(1);
        }
        if (!base.endsWith("/") && !relative.startsWith("/")) {
            return base + "/" + relative;
        }
        return base + relative;
    }

    private Optional<String> safeCssSelector(Element element) {
        if (element == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(element.cssSelector());
        } catch (Selector.SelectorParseException e) {
            log.warn("Failed to build css selector for element: {}", e.getMessage());
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("Failed to build css selector due to unexpected error", e);
            return Optional.empty();
        }
    }

    public record AutoParseResult(ParserProfile profile,
                                  PagingStrategy pagingStrategy,
                                  AutomationSettings automation,
                                  CrawlFlow flow,
                                  Map<String, Object> metadata) {
    }
}
