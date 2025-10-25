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

@Component
public class CrawlerBlueprintAutoParser {

    private static final Logger log = LoggerFactory.getLogger(CrawlerBlueprintAutoParser.class);
    private static final List<String> JOB_KEYWORD_HINTS = List.of(
            "job",
            "career",
            "position",
            "role",
            "opening",
            "vacancy",
            "opportunity",
            "职位",
            "招聘",
            "机会"
    );
    private static final List<String> JOB_ATTRIBUTE_NAMES = List.of(
            "data-automation-id",
            "data-testid",
            "data-cy",
            "data-component",
            "data-qa",
            "aria-label",
            "class",
            "id",
            "name"
    );
    private static final int MAX_PARENT_DEPTH = 8;

    public AutoParseResult parse(String entryUrl, String html) {
        Document document = Jsoup.parse(html == null ? "" : html);
        if (document.body() == null) {
            throw new IllegalArgumentException("Empty HTML document");
        }

        if (isChallengePage(document)) {
            throw new IllegalStateException("Encountered challenge page instead of job listings");
        }

        Element listElement = findBestListElement(document);
        if (listElement == null || isRootNode(listElement)) {
            throw new IllegalStateException("Unable to determine repeating job element");
        }
        listElement = normalizeRepeatingElement(listElement);
        String listSelector = safeCssSelector(listElement)
                .orElseThrow(() -> new IllegalStateException("Unable to build CSS selector for job list element"));
        listSelector = generalizeSelector(listSelector);
        if (listSelector.isBlank()) {
            throw new IllegalStateException("Unable to generalize selector for job list element");
        }

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

        try {
            if (profile.parse(html).isEmpty()) {
                throw new IllegalStateException("Auto-generated parser returned no job entries");
            }
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to evaluate generated parser", ex);
        }

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
        Element jobListContainer = document.selectFirst(
                "ul#search-job-list, ul[id*=search-job-list i], ul[class*=rc-accordion], ul[data-automation-id=jobResults]"
        );
        if (jobListContainer != null) {
            Element candidateItem = jobListContainer.selectFirst(
                    "li[data-core-accordion-item], li[data-job-id], li[data-jobid], li, div[data-job-id], div[data-jobid]"
            );
            if (candidateItem != null && candidateItem.selectFirst("a[href]") != null) {
                return candidateItem;
            }
        }

        Element structured = document.selectFirst(
                "li[data-core-accordion-item], li[data-job-id], li[data-jobid], li[data-automation-id=jobListItem]");
        if (structured != null) {
            return structured;
        }
        Element structuredContainer = document.selectFirst(
                "ul[id*=search-job-list i], ul[class*=rc-accordion], ul[data-automation-id=jobResults]," +
                        "section[id*=search-results i], div[id*=search-results i]");
        if (structuredContainer != null) {
            Element firstItem = structuredContainer.selectFirst("li[data-core-accordion-item], li, div[data-job-id], div[data-jobid]");
            if (firstItem != null) {
                return firstItem;
            }
        }

        Elements anchors = document.select("a[href]");
        Map<String, Integer> scores = new LinkedHashMap<>();
        Map<String, Element> samples = new LinkedHashMap<>();
        for (Element anchor : anchors) {
            Element candidate = anchor;
            for (int depth = 0; depth < MAX_PARENT_DEPTH && candidate != null; depth++) {
                if (candidate.tagName().equalsIgnoreCase("a")) {
                    candidate = candidate.parent();
                    continue;
                }
                if (candidate.tagName().equalsIgnoreCase("body")
                        || candidate.tagName().equalsIgnoreCase("html")
                        || candidate.tagName().equalsIgnoreCase("#root")) {
                    candidate = candidate.parent();
                    continue;
                }
                Optional<String> selectorOpt = safeCssSelector(candidate);
                if (selectorOpt.isEmpty()) {
                    candidate = candidate.parent();
                    continue;
                }
                String selector = generalizeSelector(selectorOpt.get());
                if (selector.isBlank()) {
                    candidate = candidate.parent();
                    continue;
                }
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

        Element byAttributes = findByJobAttributes(document);
        if (byAttributes != null) {
            return byAttributes;
        }

        return scores.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(entry -> samples.get(entry.getKey()))
                .filter(this::isLikelyJobList)
                .findFirst()
                .orElse(null);
    }

    private boolean isRootNode(Element element) {
        if (element == null) {
            return true;
        }
        String tag = element.tagName().toLowerCase(Locale.ROOT);
        return tag.equals("html") || tag.equals("body") || tag.equals("#root");
    }

    private Element findByJobAttributes(Document document) {
        for (Element element : document.getAllElements()) {
            if (!matchesJobAttribute(element)) {
                continue;
            }
            Element candidate = element;
            if (candidate.tagName().equalsIgnoreCase("a")) {
                candidate = candidate.parent();
            }
            if (candidate == null) {
                continue;
            }
            Element anchor = candidate.selectFirst("a[href]");
            if (anchor == null) {
                continue;
            }
            Element current = candidate;
            for (int depth = 0; depth < MAX_PARENT_DEPTH && current != null; depth++) {
                if (isLikelyJobList(current)) {
                    return current;
                }
                current = current.parent();
            }
        }
        return null;
    }

    private boolean matchesJobAttribute(Element element) {
        for (String attr : JOB_ATTRIBUTE_NAMES) {
            if (containsJobKeyword(element.attr(attr))) {
                return true;
            }
        }
        return false;
    }

    private boolean isLikelyJobList(Element element) {
        if (element == null) {
            return false;
        }
        if (isWithinNavigation(element)) {
            return false;
        }
        String tag = element.tagName();
        if (tag.equalsIgnoreCase("nav") || tag.equalsIgnoreCase("header") || tag.equalsIgnoreCase("footer")) {
            return false;
        }
        String className = element.className().toLowerCase(Locale.ROOT);
        if (className.contains("breadcrumb") || className.contains("header") || className.contains("footer")) {
            return false;
        }
        int anchorCount = element.select("a[href]").size();
        if (anchorCount >= 2) {
            return true;
        }
        if (anchorCount == 0) {
            return false;
        }
        Element anchor = element.selectFirst("a[href]");
        if (anchor != null) {
            if (containsJobKeyword(anchor.attr("href"))) {
                return true;
            }
            if (containsJobKeyword(anchor.text())) {
                return true;
            }
            if (containsJobKeyword(anchor.className())) {
                return true;
            }
            if (containsJobKeyword(anchor.id())) {
                return true;
            }
        }
        if (containsJobKeyword(element.id())) {
            return true;
        }
        if (containsJobKeyword(className)) {
            return true;
        }
        return containsJobKeyword(element.text());
    }

    private boolean isWithinNavigation(Element element) {
        Element current = element;
        int depth = 0;
        while (current != null && depth < MAX_PARENT_DEPTH + 2) {
            String tag = current.tagName().toLowerCase(Locale.ROOT);
            if (tag.equals("header") || tag.equals("footer") || tag.equals("nav")) {
                return true;
            }
            String role = current.attr("role");
            if (role != null && role.equalsIgnoreCase("navigation")) {
                return true;
            }
            String className = current.className().toLowerCase(Locale.ROOT);
            if (className.contains("globalnav") || className.contains("globalheader") || className.contains("breadcrumb") || className.contains("footer")) {
                return true;
            }
            current = current.parent();
            depth++;
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
        String child = generalizeSelector(childSelector.get());
        String parentPath = parentSelector.map(this::generalizeSelector).orElse("");
        if (!parentPath.isBlank() && child.startsWith(parentPath)) {
            String stripped = child.substring(parentPath.length());
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

    private Element normalizeRepeatingElement(Element element) {
        if (element == null) {
            return null;
        }
        if (element.tagName().equalsIgnoreCase("ul") || element.tagName().equalsIgnoreCase("ol")) {
            Element listItem = element.selectFirst("> li");
            if (listItem != null) {
                return listItem;
            }
        }
        if (element.tagName().equalsIgnoreCase("table")) {
            Element row = element.selectFirst("> tbody > tr, > tr");
            if (row != null) {
                return row;
            }
        }
        if (element.tagName().equalsIgnoreCase("tbody")) {
            Element row = element.selectFirst("> tr");
            if (row != null) {
                return row;
            }
        }
        if ((element.tagName().equalsIgnoreCase("div") || element.tagName().equalsIgnoreCase("section"))
                && element.attr("role").equalsIgnoreCase("list")) {
            Element item = element.selectFirst("> div, > section, > article, > li");
            if (item != null) {
                return item;
            }
        }
        return element;
    }

    private String generalizeSelector(String selector) {
        if (selector == null || selector.isBlank()) {
            return "";
        }
        String normalized = selector;
        normalized = normalized.replaceAll(
                ":(nth-of-type|nth-child|nth-last-of-type|nth-last-child)\\((?:\\d+|odd|even)\\)",
                "");
        normalized = normalized.replaceAll(":first-child|:last-child|:first-of-type|:last-of-type", "");
        normalized = normalized.replaceAll("\\s*>\\s*", " > ");
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized.trim();
    }

    private boolean isChallengePage(Document document) {
        String title = document.title() == null ? "" : document.title().toLowerCase(Locale.ROOT);
        if (title.contains("just a moment") || title.contains("attention required")) {
            return true;
        }
        if (document.selectFirst("#challenge-form, #challenge-error-text, script[src*='challenge-platform']") != null) {
            return true;
        }
        return document.selectFirst("meta[http-equiv=refresh][content*='__cf_chl']") != null;
    }

    private boolean containsJobKeyword(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String normalized = content.toLowerCase(Locale.ROOT);
        for (String keyword : JOB_KEYWORD_HINTS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public record AutoParseResult(ParserProfile profile,
                                  PagingStrategy pagingStrategy,
                                  AutomationSettings automation,
                                  CrawlFlow flow,
                                  Map<String, Object> metadata) {
    }
}
