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

@Component
public class CrawlerBlueprintAutoParser {

    private static final Logger log = LoggerFactory.getLogger(CrawlerBlueprintAutoParser.class);

    public AutoParseResult parse(String entryUrl, String html) {
        Document document = Jsoup.parse(html == null ? "" : html);
        if (document.body() == null) {
            throw new IllegalArgumentException("Empty HTML document");
        }

        Element listElement = findBestListElement(document);
        if (listElement == null) {
            throw new IllegalStateException("Unable to determine repeating job element");
        }
        String listSelector = listElement.cssSelector();

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
        Element anchor = listElement.selectFirst("a[href]");
        if (anchor != null) {
            String relativeAnchor = relativeSelector(listElement, anchor);
            fields.put("title", new ParserField(
                    "title",
                    ParserFieldType.TEXT,
                    relativeAnchor,
                    null,
                    null,
                    null,
                    ",",
                    true,
                    null
            ));
            fields.put("url", new ParserField(
                    "url",
                    ParserFieldType.ATTRIBUTE,
                    relativeAnchor,
                    "href",
                    null,
                    null,
                    ",",
                    true,
                    baseUrl(entryUrl)
            ));
        }

        Element company = findFirst(listElement, List.of(
                "[class*=company]",
                "[class*=employer]",
                "[data-company]",
                "span:matches((?i)company)",
                "p:matches((?i)company)",
                "div:matches((?i)company)"));
        if (company != null) {
            fields.put("company", new ParserField(
                    "company",
                    ParserFieldType.TEXT,
                    relativeSelector(listElement, company),
                    null,
                    null,
                    null,
                    ",",
                    false,
                    null
            ));
        }

        Element location = findFirst(listElement, List.of(
                "[class*=location]",
                "[class*=city]",
                "span:matches((?i)location)",
                "p:matches((?i)location)",
                "div:matches((?i)location)"));
        if (location != null) {
            fields.put("location", new ParserField(
                    "location",
                    ParserFieldType.TEXT,
                    relativeSelector(listElement, location),
                    null,
                    null,
                    null,
                    ",",
                    false,
                    null
            ));
        }

        if (!fields.containsKey("url") && anchor != null) {
            fields.put("url", new ParserField(
                    "url",
                    ParserFieldType.ATTRIBUTE,
                    relativeSelector(listElement, anchor),
                    "href",
                    null,
                    null,
                    ",",
                    true,
                    baseUrl(entryUrl)
            ));
        }

        return fields;
    }

    private Element findBestListElement(Document document) {
        Elements anchors = document.select("a[href]");
        Map<String, Integer> scores = new LinkedHashMap<>();
        Map<String, Element> samples = new LinkedHashMap<>();
        for (Element anchor : anchors) {
            Element candidate = anchor;
            for (int depth = 0; depth < 4 && candidate != null; depth++) {
                if (candidate.tagName().equalsIgnoreCase("a")) {
                    candidate = candidate.parent();
                    continue;
                }
                if (candidate == null) {
                    break;
                }
                String selector = candidate.cssSelector();
                scores.merge(selector, 1, Integer::sum);
                samples.putIfAbsent(selector, candidate);
                candidate = candidate.parent();
            }
        }
        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() > 2)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(entry -> samples.get(entry.getKey()))
                .findFirst()
                .orElse(null);
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
        String parentSelector = parent.cssSelector();
        String childSelector = target.cssSelector();
        if (childSelector.startsWith(parentSelector)) {
            String stripped = childSelector.substring(parentSelector.length());
            if (stripped.startsWith(" > ")) {
                stripped = stripped.substring(3);
            }
            return stripped.isBlank() ? "." : stripped.trim();
        }
        return childSelector;
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
                        if (key.contains("page") || key.contains("p")) {
                            return PagingStrategy.query(pair[0], 1, 1, null);
                        }
                        if (key.contains("offset")) {
                            return PagingStrategy.offset(pair[0], 0, 1, null);
                        }
                    }
                }
            }
        } catch (URISyntaxException e) {
            log.info("Failed to parse paging URL: {}", e.getMessage());
        }
        return PagingStrategy.disabled();
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

    public record AutoParseResult(ParserProfile profile,
                                  PagingStrategy pagingStrategy,
                                  AutomationSettings automation,
                                  CrawlFlow flow,
                                  Map<String, Object> metadata) {
    }
}
