package com.vibe.jobs.crawler.application.generation;

import com.vibe.jobs.crawler.domain.AutomationSettings;
import com.vibe.jobs.crawler.domain.CrawlFlow;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
            "职位名称",
            "岗位",
            "职缺",
            "招聘",
            "机会",
            "工作"
    );

    private static final List<String> JOB_ATTRIBUTE_NAMES = List.of(
            "data-automation-id",
            "data-testid",
            "data-cy",
            "data-component",
            "data-qa",
            "data-ph-at-id",
            "aria-label",
            "class",
            "id",
            "name"
    );

    private static final List<String> TITLE_PRIORITY_SELECTORS = List.of(
            "a[data-job-id]",
            "[data-automation-id*=jobtitle i]",
            "[data-testid*=jobtitle i]",
            "[data-component*=jobtitle i]",
            "[data-qa*=jobtitle i]",
            "[role=link]",
            "button[role=link]",
            "button[data-url]",
            "button[data-href]"
    );

    private static final List<String> TITLE_FALLBACK_SELECTORS = List.of(
            "h1",
            "h2",
            "h3",
            "h4",
            "span[class*=title i]",
            "div[class*=title i]",
            "p[class*=title i]"
    );

    private static final List<String> URL_ATTRIBUTE_CANDIDATES = List.of(
            "href",
            "data-url",
            "data-href",
            "data-link",
            "data-target-url",
            "data-job-url",
            "data-apply-url",
            "data-redirect-url"
    );

    private static final int MAX_PARENT_DEPTH = 8;
    private static final int MAX_METADATA_CANDIDATES = 20;

    public AutoParseResult parse(String entryUrl, String html) {
        Document document = Jsoup.parse(html == null ? "" : html);
        if (document.body() == null) {
            throw new IllegalArgumentException("Empty HTML document");
        }

        if (isChallengePage(document)) {
            throw new IllegalStateException("Encountered challenge page instead of job listings");
        }

        List<CandidateEvaluation> candidates = findCandidateListCandidates(document);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Unable to determine repeating job element");
        }

        if (log.isDebugEnabled()) {
            for (CandidateEvaluation candidate : candidates) {
                log.debug("Detected parser candidate {} with score {} (reasons: {})",
                        candidate.selector(), candidate.score().totalScore(), candidate.reasons());
            }
        }

        List<Map<String, Object>> candidateSummaries = new ArrayList<>();
        for (CandidateEvaluation candidate : candidates) {
            Element normalized = candidate.element() == null ? null : normalizeRepeatingElement(candidate.element());
            candidateSummaries.add(buildCandidateSummary(candidate, normalized));
        }
        List<String> attemptErrors = new ArrayList<>();
        for (CandidateEvaluation candidate : candidates) {
            Element rawElement = candidate.element();
            if (rawElement == null) {
                attemptErrors.add("Candidate %s resolved to null element".formatted(candidate.selector()));
                continue;
            }

            Element listElement = normalizeRepeatingElement(rawElement);
            if (listElement == null || isRootNode(listElement)) {
                attemptErrors.add("Candidate %s normalized to invalid root".formatted(candidate.selector()));
                continue;
            }

            if (!isLikelyJobList(listElement)) {
                attemptErrors.add("Candidate %s rejected by job list heuristics".formatted(candidate.selector()));
                continue;
            }

            Optional<String> listSelectorOpt = safeCssSelector(listElement).map(this::generalizeSelector);
            if (listSelectorOpt.isEmpty() || listSelectorOpt.get().isBlank()) {
                attemptErrors.add("Candidate %s missing CSS selector".formatted(candidate.selector()));
                continue;
            }

            String listSelector = listSelectorOpt.get();
            FieldBuildResult fieldResult = buildFields(listElement, entryUrl);
            Map<String, ParserField> fields = fieldResult.fields();
            if (!fields.containsKey("title")) {
                attemptErrors.add("Candidate %s missing title field".formatted(candidate.selector()));
                continue;
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
                    attemptErrors.add("Selector %s produced no job entries".formatted(listSelector));
                    continue;
                }
            } catch (RuntimeException ex) {
                attemptErrors.add("Selector %s failed: %s".formatted(listSelector, ex.getMessage()));
                continue;
            }

            PagingStrategy paging = detectPagingStrategy(document, entryUrl);
            AutomationSettings automation = AutomationSettings.disabled();
            CrawlFlow flow = CrawlFlow.empty();

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("selectedCandidate", buildCandidateSummary(candidate, listElement));
            metadata.put("candidateSummaries", candidateSummaries);
            metadata.put("fieldSources", fieldResult.fieldSources());
            metadata.put("listSelector", listSelector);

            return new AutoParseResult(
                    profile,
                    paging,
                    automation,
                    flow,
                    Map.copyOf(metadata)
            );
        }

        String message = attemptErrors.isEmpty()
                ? "Unable to determine repeating job element"
                : "Unable to build parser from detected candidates: " + attemptErrors.stream()
                .limit(5)
                .collect(Collectors.joining("; "));
        throw new IllegalStateException(message);
    }

    private FieldBuildResult buildFields(Element listElement, String entryUrl) {
        Map<String, ParserField> fields = new LinkedHashMap<>();
        List<Map<String, Object>> fieldSources = new ArrayList<>();

        TitleDetection titleDetection = detectTitle(listElement, entryUrl);
        if (titleDetection != null) {
            fields.put("title", titleDetection.titleField());
            fieldSources.add(buildFieldSource("title", titleDetection.titleField(), titleDetection.titleReason()));
            if (titleDetection.urlField() != null) {
                fields.put("url", titleDetection.urlField());
                fieldSources.add(buildFieldSource("url", titleDetection.urlField(), titleDetection.urlReason()));
            }
        }

        if (!fields.containsKey("url")) {
            UrlDetection fallbackUrl = detectUrl(listElement, listElement, entryUrl);
            if (fallbackUrl.field() != null) {
                fields.put("url", fallbackUrl.field());
                fieldSources.add(buildFieldSource(
                        "url",
                        fallbackUrl.field(),
                        fallbackUrl.reason() == null ? "fallback-url-search" : fallbackUrl.reason()
                ));
            }
        }

        Element company = findFirst(listElement, List.of(
                "[class*=company]",
                "[class*=employer]",
                "[data-company]",
                "[data-testid*=company i]",
                "[data-automation-id*=company i]",
                "[aria-label*=company i]",
                "span:matches((?i)company|employer|公司|企业)",
                "p:matches((?i)company|employer|公司|企业)",
                "div:matches((?i)company|employer|公司|企业)"
        ));
        if (company != null && !fields.containsKey("company")) {
            ParserField field = textField("company", listElement, company, false);
            fields.put("company", field);
            fieldSources.add(buildFieldSource("company", field, "company-selector-match"));
        }

        Element location = findFirst(listElement, List.of(
                "[class*=location]",
                "[class*=city]",
                "[data-testid*=location i]",
                "[data-automation-id*=location i]",
                "[aria-label*=location i]",
                "span:matches((?i)location|city|地点|城市)",
                "p:matches((?i)location|city|地点|城市)",
                "div:matches((?i)location|city|地点|城市)"
        ));
        if (location != null && !fields.containsKey("location")) {
            ParserField field = textField("location", listElement, location, false);
            fields.put("location", field);
            fieldSources.add(buildFieldSource("location", field, "location-selector-match"));
        }

        return new FieldBuildResult(Map.copyOf(fields), List.copyOf(fieldSources));
    }

    private Map<String, Object> buildFieldSource(String name, ParserField field, String reason) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("name", name);
        source.put("selector", field.selector());
        source.put("attribute", field.attribute());
        source.put("type", field.type().name());
        if (reason != null && !reason.isBlank()) {
            source.put("reason", reason);
        }
        return source;
    }

    private ParserField textField(String name, Element parent, Element target, boolean required) {
        String selector = relativeSelector(parent, target);
        return new ParserField(
                name,
                ParserFieldType.TEXT,
                selector,
                null,
                null,
                null,
                ",",
                required,
                null
        );
    }

    private ParserField createUrlField(Element parent, Element target, String attribute, String entryUrl) {
        String selector = relativeSelector(parent, target);
        String base = "href".equalsIgnoreCase(attribute) ? baseUrl(entryUrl) : "";
        return new ParserField(
                "url",
                ParserFieldType.ATTRIBUTE,
                selector,
                attribute,
                null,
                null,
                ",",
                true,
                base
        );
    }
    private TitleDetection detectTitle(Element listElement, String entryUrl) {
        if (listElement == null) {
            return null;
        }
        Element anchor = listElement.selectFirst("a[href]");
        if (anchor != null && isMeaningfulTitleElement(anchor)) {
            ParserField titleField = textField("title", listElement, anchor, true);
            UrlDetection url = detectUrl(listElement, anchor, entryUrl);
            return new TitleDetection(titleField, url.field(), "anchor[href]", url.reason());
        }

        for (String selector : TITLE_PRIORITY_SELECTORS) {
            Element candidate = listElement.selectFirst(selector);
            if (candidate == null || candidate == listElement) {
                continue;
            }
            if (!isMeaningfulTitleElement(candidate)) {
                continue;
            }
            ParserField titleField = textField("title", listElement, candidate, true);
            UrlDetection url = detectUrl(listElement, candidate, entryUrl);
            return new TitleDetection(titleField, url.field(), "selector:" + selector, url.reason());
        }

        for (String selector : TITLE_FALLBACK_SELECTORS) {
            Element candidate = listElement.selectFirst(selector);
            if (candidate == null || candidate == listElement) {
                continue;
            }
            if (!isMeaningfulTitleElement(candidate)) {
                continue;
            }
            ParserField titleField = textField("title", listElement, candidate, true);
            UrlDetection url = detectUrl(listElement, candidate, entryUrl);
            return new TitleDetection(titleField, url.field(), "selector:" + selector, url.reason());
        }

        Element fallback = findFallbackTitleCandidate(listElement);
        if (fallback != null) {
            ParserField titleField = textField("title", listElement, fallback, true);
            UrlDetection url = detectUrl(listElement, fallback, entryUrl);
            return new TitleDetection(titleField, url.field(), "fallback-text", url.reason());
        }

        return null;
    }

    private Element findFallbackTitleCandidate(Element listElement) {
        if (listElement == null) {
            return null;
        }
        for (Element element : listElement.getAllElements()) {
            if (element == listElement) {
                continue;
            }
            if (!isMeaningfulTitleElement(element)) {
                continue;
            }
            if (containsJobKeyword(element.className()) || containsJobKeyword(element.id())) {
                return element;
            }
        }
        for (Element element : listElement.getAllElements()) {
            if (element == listElement) {
                continue;
            }
            if (isMeaningfulTitleElement(element)) {
                return element;
            }
        }
        return null;
    }

    private boolean isMeaningfulTitleElement(Element element) {
        if (element == null) {
            return false;
        }
        if (element == element.ownerDocument().body()) {
            return false;
        }
        return isMeaningfulTitleText(element.text());
    }

    private boolean isMeaningfulTitleText(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.trim();
        if (normalized.isBlank()) {
            return false;
        }
        if (normalized.length() < 3 || normalized.length() > 160) {
            return false;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("apply") || lower.contains("申请") || lower.contains("提交") || lower.contains("投递")) {
            return false;
        }
        return true;
    }

    private UrlDetection detectUrl(Element listElement, Element candidate, String entryUrl) {
        if (listElement == null) {
            return UrlDetection.empty();
        }
        if (candidate != null) {
            for (String attr : URL_ATTRIBUTE_CANDIDATES) {
                if (!candidate.hasAttr(attr)) {
                    continue;
                }
                String value = candidate.attr(attr);
                if (value != null && !value.isBlank()) {
                    return new UrlDetection(createUrlField(listElement, candidate, attr, entryUrl), attr + "-candidate");
                }
            }
            Element descendantAnchor = candidate.selectFirst("a[href]");
            if (descendantAnchor != null) {
                return new UrlDetection(createUrlField(listElement, descendantAnchor, "href", entryUrl), "descendant-anchor");
            }
            Element ancestor = candidate.parent();
            int depth = 0;
            while (ancestor != null && depth < MAX_PARENT_DEPTH) {
                for (String attr : URL_ATTRIBUTE_CANDIDATES) {
                    if (ancestor.hasAttr(attr) && !ancestor.attr(attr).isBlank()) {
                        return new UrlDetection(createUrlField(listElement, ancestor, attr, entryUrl), "ancestor-" + attr);
                    }
                }
                ancestor = ancestor.parent();
                depth++;
            }
        }

        Element anchor = listElement.selectFirst("a[href]");
        if (anchor != null) {
            return new UrlDetection(createUrlField(listElement, anchor, "href", entryUrl), "fallback-anchor");
        }

        Element dataUrl = findFirst(listElement, List.of(
                "[data-url]",
                "[data-href]",
                "[data-link]",
                "[data-target-url]",
                "[data-job-url]",
                "[data-apply-url]",
                "[data-redirect-url]"
        ));
        if (dataUrl != null) {
            for (String attr : URL_ATTRIBUTE_CANDIDATES) {
                if (dataUrl.hasAttr(attr) && !dataUrl.attr(attr).isBlank()) {
                    return new UrlDetection(createUrlField(listElement, dataUrl, attr, entryUrl), "fallback-" + attr);
                }
            }
        }

        return UrlDetection.empty();
    }
    private List<CandidateEvaluation> findCandidateListCandidates(Document document) {
        Map<String, CandidateScore> scores = new LinkedHashMap<>();
        Map<String, Element> samples = new LinkedHashMap<>();
        Map<String, List<String>> reasons = new LinkedHashMap<>();

        Element jobListContainer = document.selectFirst(
                "ul#search-job-list, ul[id*=search-job-list i], ul[class*=rc-accordion], ul[data-automation-id=jobResults]"
        );
        if (jobListContainer != null) {
            Element item = jobListContainer.selectFirst(
                    "li[data-core-accordion-item], li[data-job-id], li[data-jobid], li, div[data-job-id], div[data-jobid]"
            );
            if (item != null) {
                registerCandidate(item, scores, samples, reasons, 6, "structured-container", CandidateScore::addAnchorHit);
            }
        }

        Element structured = document.selectFirst(
                "li[data-core-accordion-item], li[data-job-id], li[data-jobid], li[data-automation-id=jobListItem]"
        );
        if (structured != null) {
            registerCandidate(structured, scores, samples, reasons, 4, "structured-element", CandidateScore::addAnchorHit);
        }

        Element structuredContainer = document.selectFirst(
                "ul[id*=search-job-list i], ul[class*=rc-accordion], ul[data-automation-id=jobResults],"
                        + "section[id*=search-results i], div[id*=search-results i]"
        );
        if (structuredContainer != null) {
            Element firstItem = structuredContainer.selectFirst(
                    "li[data-core-accordion-item], li[data-job-id], li[data-jobid], li, div[data-job-id], div[data-jobid]"
            );
            if (firstItem != null) {
                registerCandidate(firstItem, scores, samples, reasons, 5, "structured-container-child", CandidateScore::addAnchorHit);
            }
        }

        Elements interactiveElements = document.select("a[href], button[role=link], [role=link]");
        for (Element interactive : interactiveElements) {
            Element candidate = interactive;
            for (int depth = 0; depth < MAX_PARENT_DEPTH && candidate != null; depth++) {
                if (candidate.tagName().equalsIgnoreCase("a")) {
                    candidate = candidate.parent();
                    continue;
                }
                registerCandidate(candidate, scores, samples, reasons, 1, "interactive-parent", CandidateScore::addAnchorHit);
                candidate = candidate.parent();
            }
        }

        for (Element element : document.getAllElements()) {
            if (!matchesJobAttribute(element)) {
                continue;
            }
            Element candidate = element.tagName().equalsIgnoreCase("a") ? element.parent() : element;
            registerCandidate(candidate, scores, samples, reasons, 2, "job-attribute", null);
        }

        Elements listRoleElements = document.select("[role=listitem]");
        for (Element listRole : listRoleElements) {
            registerCandidate(listRole, scores, samples, reasons, 1, "role=listitem", null);
        }

        return scores.entrySet().stream()
                .map(entry -> new CandidateEvaluation(
                        samples.get(entry.getKey()),
                        entry.getKey(),
                        entry.getValue(),
                        List.copyOf(reasons.getOrDefault(entry.getKey(), List.of()))
                ))
                .filter(candidate -> candidate.element() != null && !isRootNode(candidate.element()))
                .sorted(Comparator.comparingInt((CandidateEvaluation eval) -> eval.score().totalScore()).reversed())
                .toList();
    }

    private void registerCandidate(Element element,
                                   Map<String, CandidateScore> scores,
                                   Map<String, Element> samples,
                                   Map<String, List<String>> reasons,
                                   int baseBoost,
                                   String reason,
                                   Consumer<CandidateScore> extraScore) {
        if (element == null || isRootNode(element) || isWithinNavigation(element)) {
            return;
        }
        Optional<String> selectorOpt = safeCssSelector(element);
        if (selectorOpt.isEmpty()) {
            return;
        }
        String selector = generalizeSelector(selectorOpt.get());
        if (selector.isBlank()) {
            return;
        }

        samples.putIfAbsent(selector, element);
        CandidateScore score = scores.computeIfAbsent(selector, key -> new CandidateScore());
        if (baseBoost != 0) {
            score.addBase(baseBoost);
        }

        List<String> reasonList = reasons.computeIfAbsent(selector, key -> new ArrayList<>());
        if (reason != null && !reason.isBlank() && !reasonList.contains(reason)) {
            reasonList.add(reason);
        }

        applySemanticHints(element, score, reasonList);
        if (extraScore != null) {
            extraScore.accept(score);
        }
    }

    private void applySemanticHints(Element element, CandidateScore score, List<String> reasons) {
        if (element == null) {
            return;
        }
        String role = element.attr("role");
        if (role != null && role.equalsIgnoreCase("listitem")) {
            if (score.markListRole() && !reasons.contains("role=listitem")) {
                reasons.add("role=listitem");
            }
        }
        for (String attr : JOB_ATTRIBUTE_NAMES) {
            String value = element.attr(attr);
            if (value != null && !value.isBlank() && containsJobKeyword(value)) {
                score.addKeywordHit();
                String attrReason = "attribute:" + attr;
                if (!reasons.contains(attrReason)) {
                    reasons.add(attrReason);
                }
            }
        }
        String idAndClass = (element.id() + " " + element.className()).trim();
        if (!idAndClass.isBlank() && containsJobKeyword(idAndClass)) {
            score.addKeywordHit();
            if (!reasons.contains("id/class keyword")) {
                reasons.add("id/class keyword");
            }
        }
        if (containsJobKeyword(element.text())) {
            if (score.markKeywordText() && !reasons.contains("text keyword")) {
                reasons.add("text keyword");
            }
        }
    }
    private Map<String, Object> buildCandidateSummary(CandidateEvaluation candidate, Element normalizedElement) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("selector", candidate.selector());
        summary.put("score", candidate.score().totalScore());
        summary.put("metrics", candidate.score().asMetrics());
        summary.put("reasons", candidate.reasons());
        summary.put("sampleText", snippet(candidate.element()));
        safeCssSelector(candidate.element())
                .map(this::generalizeSelector)
                .ifPresent(sel -> summary.put("rawSelector", sel));
        if (normalizedElement != null) {
            safeCssSelector(normalizedElement)
                    .map(this::generalizeSelector)
                    .ifPresent(sel -> summary.put("normalizedSelector", sel));
        }
        if (candidate.element() != null) {
            String role = candidate.element().attr("role");
            if (role != null && !role.isBlank()) {
                summary.put("role", role);
            }
        }
        return summary;
    }

    private String snippet(Element element) {
        if (element == null) {
            return "";
        }
        String text = element.text();
        if (text == null) {
            return "";
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 160) {
            return normalized.substring(0, 157) + "...";
        }
        return normalized;
    }

    private boolean matchesJobAttribute(Element element) {
        if (element == null) {
            return false;
        }
        for (String attr : JOB_ATTRIBUTE_NAMES) {
            String value = element.attr(attr);
            if (value != null && !value.isBlank() && containsJobKeyword(value)) {
                return true;
            }
        }
        String role = element.attr("role");
        return role != null && role.equalsIgnoreCase("listitem");
    }

    private boolean isRootNode(Element element) {
        if (element == null) {
            return true;
        }
        String tag = element.tagName().toLowerCase(Locale.ROOT);
        return tag.equals("html") || tag.equals("body") || tag.equals("#root");
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

    private String generalizeSelector(String selector) {
        if (selector == null || selector.isBlank()) {
            return "";
        }
        String normalized = selector;
        normalized = normalized.replaceAll(
                ":(nth-of-type|nth-child|nth-last-of-type|nth-last-child)\\((?:\\d+|odd|even)\\)",
                ""
        );
        normalized = normalized.replaceAll(":first-child|:last-child|:first-of-type|:last-of-type", "");
        normalized = normalized.replaceAll("\\s*>\\s*", " > ");
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized.trim();
    }

    private boolean isLikelyJobList(Element element) {
        if (element == null) {
            return false;
        }
        if (isWithinNavigation(element)) {
            return false;
        }
        String role = element.attr("role");
        if (role != null && role.equalsIgnoreCase("listitem")) {
            return true;
        }
        String tag = element.tagName().toLowerCase(Locale.ROOT);
        if (tag.equals("nav") || tag.equals("header") || tag.equals("footer")) {
            return false;
        }
        int interactiveCount = element.select("a[href], button[role=link], [role=link]").size();
        if (interactiveCount >= 2) {
            return true;
        }
        if (interactiveCount == 1) {
            Element interactive = element.selectFirst("a[href], button[role=link], [role=link]");
            if (interactive != null) {
                if (containsJobKeyword(interactive.text()) || containsJobKeyword(interactive.attr("href"))) {
                    return true;
                }
            }
        }
        if (containsJobKeyword(element.id()) || containsJobKeyword(element.className())) {
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
            if (className.contains("globalnav") || className.contains("globalheader")
                    || className.contains("breadcrumb") || className.contains("footer")) {
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

    private record CandidateEvaluation(Element element,
                                       String selector,
                                       CandidateScore score,
                                       List<String> reasons) {
    }

    private static final class CandidateScore {
        private int baseScore;
        private int anchorHits;
        private int keywordHits;
        private int semanticBoost;
        private boolean listRole;
        private boolean keywordText;

        void addBase(int value) {
            baseScore += value;
        }

        void addAnchorHit() {
            anchorHits++;
        }

        void addKeywordHit() {
            keywordHits++;
        }

        boolean markListRole() {
            if (!listRole) {
                listRole = true;
                semanticBoost += 2;
                return true;
            }
            return false;
        }

        boolean markKeywordText() {
            if (!keywordText) {
                keywordText = true;
                semanticBoost += 1;
                return true;
            }
            return false;
        }

        int totalScore() {
            return baseScore + anchorHits + keywordHits + semanticBoost;
        }

        Map<String, Object> asMetrics() {
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("baseScore", baseScore);
            metrics.put("anchorHits", anchorHits);
            metrics.put("keywordHits", keywordHits);
            metrics.put("semanticBoost", semanticBoost);
            metrics.put("listRole", listRole);
            metrics.put("keywordText", keywordText);
            metrics.put("total", totalScore());
            return metrics;
        }
    }

    private record FieldBuildResult(Map<String, ParserField> fields,
                                    List<Map<String, Object>> fieldSources) {
    }

    private record TitleDetection(ParserField titleField,
                                  ParserField urlField,
                                  String titleReason,
                                  String urlReason) {
    }

    private record UrlDetection(ParserField field, String reason) {
        static UrlDetection empty() {
            return new UrlDetection(null, null);
        }
    }

    public record AutoParseResult(ParserProfile profile,
                                  PagingStrategy pagingStrategy,
                                  AutomationSettings automation,
                                  CrawlFlow flow,
                                  Map<String, Object> metadata) {
    }
}
