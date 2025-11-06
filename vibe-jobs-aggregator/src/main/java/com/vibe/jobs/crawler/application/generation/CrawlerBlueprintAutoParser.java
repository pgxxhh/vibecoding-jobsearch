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

    public AutoParseResult parse(String entryUrl, String html) {
        Document document = Jsoup.parse(html == null ? "" : html);
        if (document.body() == null) {
            throw new IllegalArgumentException("Empty HTML document");
        }

        // 检测网站类型和是否需要浏览器引擎
        SiteAnalysis siteAnalysis = analyzeSite(document, entryUrl);
        
        Element listElement = findBestListElement(document);
        if (listElement == null && siteAnalysis.requiresBrowser()) {
            // 对于SPA网站，可能需要JavaScript渲染后才能找到职位列表
            log.info("No job list found in initial HTML for {}, generating browser-enabled config", entryUrl);
            return generateBrowserConfig(entryUrl, siteAnalysis);
        }
        
        if (listElement == null) {
            throw new IllegalStateException("Unable to determine repeating job element");
        }
        
        String listSelector = safeCssSelector(listElement)
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
        AutomationSettings automation = siteAnalysis.requiresBrowser() ? 
            generateAutomationSettings(siteAnalysis) : AutomationSettings.disabled();
        CrawlFlow flow = siteAnalysis.requiresBrowser() ? 
            generateBrowserFlow(siteAnalysis) : CrawlFlow.empty();
        Map<String, Object> metadata = Map.of("siteType", siteAnalysis.siteType());

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
        String tag = element.tagName();
        if (tag.equalsIgnoreCase("nav") || tag.equalsIgnoreCase("header") || tag.equalsIgnoreCase("footer")) {
            return false;
        }
        String className = element.className().toLowerCase(Locale.ROOT);
        if (className.contains("breadcrumb") || className.contains("header") || className.contains("footer")) {
            return false;
        }
        int anchorCount = element.select("a[href]").size();
        return anchorCount >= 2;
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

    private SiteAnalysis analyzeSite(Document document, String entryUrl) {
        // 检测是否为SPA应用
        boolean hasSpaIndicators = hasSpaFramework(document);
        boolean hasMinimalContent = hasMinimalJobContent(document);
        boolean needsBrowser = hasSpaIndicators || hasMinimalContent;
        
        String siteType = determineSiteType(document, entryUrl);
        
        return new SiteAnalysis(siteType, needsBrowser, hasSpaIndicators);
    }
    
    private boolean hasSpaFramework(Document document) {
        // 检测常见的SPA框架标识
        String html = document.html().toLowerCase(Locale.ROOT);
        return html.contains("ng-app") ||              // Angular
               html.contains("data-reactroot") ||      // React  
               html.contains("__vue") ||               // Vue
               html.contains("ember-") ||              // Ember
               html.contains("backbone") ||            // Backbone
               document.select("script[src*=angular]").size() > 0 ||
               document.select("script[src*=react]").size() > 0 ||
               document.select("script[src*=vue]").size() > 0;
    }
    
    private boolean hasMinimalJobContent(Document document) {
        // 检查是否已有足够的职位内容
        Elements jobLinks = document.select("a[href*=job], a[href*=position], a[href*=career]");
        Elements jobElements = document.select("[class*=job], [class*=position], [class*=career]");
        
        // 如果找到的职位相关元素很少，可能需要JavaScript加载
        return jobLinks.size() < 3 && jobElements.size() < 5;
    }
    
    private String determineSiteType(Document document, String entryUrl) {
        if (entryUrl == null) return "unknown";
        
        String host = "";
        try {
            URI uri = new URI(entryUrl);
            host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase(Locale.ROOT);
        } catch (URISyntaxException ignored) {}
        
        // 根据域名和内容特征判断网站类型
        if (host.contains("workday")) return "workday";
        if (host.contains("greenhouse")) return "greenhouse";
        if (host.contains("lever")) return "lever";
        if (host.contains("bamboohr")) return "bamboohr";
        if (host.contains("smartrecruiters")) return "smartrecruiters";
        if (host.contains("icims")) return "icims";
        if (host.contains("jobvite")) return "jobvite";
        
        // 检测常见的职位网站模式
        if (hasSpaFramework(document)) return "spa";
        if (document.select("table tr").size() > 10) return "table-based";
        
        return "standard";
    }
    
    private AutoParseResult generateBrowserConfig(String entryUrl, SiteAnalysis analysis) {
        // 为SPA网站生成适合的配置
        String listSelector = generateSpaJobSelector(analysis.siteType());
        
        Map<String, ParserField> fields = generateSpaFields(entryUrl, analysis.siteType());
        
        ParserProfile profile = ParserProfile.of(
                listSelector,
                fields,
                Set.of(),
                "",
                ParserProfile.DetailFetchConfig.disabled()
        );
        
        PagingStrategy paging = PagingStrategy.disabled(); // SPA通常用无限滚动
        AutomationSettings automation = generateAutomationSettings(analysis);
        CrawlFlow flow = generateBrowserFlow(analysis);
        Map<String, Object> metadata = Map.of("siteType", analysis.siteType());
        
        return new AutoParseResult(profile, paging, automation, flow, metadata);
    }
    
    private String generateSpaJobSelector(String siteType) {
        return switch (siteType) {
            case "workday" -> "a[href*='/job/'], .css-19uc56f, .css-1psffb1";
            case "greenhouse" -> "a[data-mapped='true'], .opening";
            case "lever" -> "a[href*='/jobs/'], .posting";
            case "icims" -> "a[href*='/job/'], .iCIMS_JobsTable a, .job-link";
            case "spa" -> "a[href*='/job'], a[href*='/position'], .job-item, .position-item, .career-item";
            default -> "a[href*='/job'], a[href*='/position'], a[href*='/career'], .job, .position, .opening";
        };
    }
    
    private Map<String, ParserField> generateSpaFields(String entryUrl, String siteType) {
        Map<String, ParserField> fields = new LinkedHashMap<>();
        
        // URL字段
        String urlSelector = switch (siteType) {
            case "workday" -> "a[href*='/job/']";
            case "greenhouse" -> "a[data-mapped='true']";
            case "lever" -> "a[href*='/jobs/']";
            case "icims" -> "a[href*='/job/']";
            default -> "a[href*='/job'], a[href*='/position'], a[href*='/career']";
        };
        
        fields.put("url", new ParserField(
                "url",
                ParserFieldType.ATTRIBUTE,
                urlSelector,
                "href",
                null,
                null,
                ",",
                true,
                baseUrl(entryUrl)
        ));
        
        // 标题字段 - 使用多个备选选择器
        String titleSelector = switch (siteType) {
            case "workday" -> "h3, .css-19uc56f h3, [data-automation-id='jobTitle']";
            case "greenhouse" -> ".opening a, h4 a, .job-title";
            case "lever" -> ".posting-name, h5 a, .posting-title";
            case "icims" -> ".iCIMS_JobsTable a, .job-title, h3 a";
            default -> "h1, h2, h3, h4, .job-title, .position-title, .title, a";
        };
        
        fields.put("title", new ParserField(
                "title",
                ParserFieldType.TEXT,
                titleSelector,
                null,
                null,
                null,
                ",",
                true,
                null
        ));
        
        // 位置字段
        fields.put("location", new ParserField(
                "location",
                ParserFieldType.TEXT,
                ".location, [class*=location], [class*=city], .office, [data-automation-id='jobLocation']",
                null,
                null,
                null,
                ",",
                false,
                null
        ));
        
        return fields;
    }
    
    private AutomationSettings generateAutomationSettings(SiteAnalysis analysis) {
        if (!analysis.requiresBrowser()) {
            return AutomationSettings.disabled();
        }
        
        // 为不同类型的网站生成合适的等待时间和选择器
        int waitTime = analysis.siteType().equals("spa") ? 8000 : 5000;
        String waitSelector = switch (analysis.siteType()) {
            case "workday" -> ".css-19uc56f, [data-automation-id='jobTitle']";
            case "greenhouse" -> ".opening, .opening-list";
            case "lever" -> ".posting, .postings-group";
            case "icims" -> ".iCIMS_JobsTable, .job-results";
            default -> ".job, .position, .career, .job-list, .position-list, .careers-list";
        };
        
        return new AutomationSettings(
                true,
                true,
                waitSelector,
                waitTime,
                null  // 不设置搜索功能，简化配置
        );
    }
    
    private CrawlFlow generateBrowserFlow(SiteAnalysis analysis) {
        if (!analysis.requiresBrowser()) {
            return CrawlFlow.empty();
        }
        
        List<CrawlStep> steps = new ArrayList<>();
        
        // 等待页面加载
        Map<String, Object> waitOptions = new LinkedHashMap<>();
        String waitSelector = switch (analysis.siteType()) {
            case "workday" -> ".css-19uc56f, [data-automation-id='jobTitle']";
            case "greenhouse" -> ".opening, .opening-list";
            case "lever" -> ".posting, .postings-group";
            case "icims" -> ".iCIMS_JobsTable, .job-results";
            default -> ".job, .position, .career, .job-list, .position-list";
        };
        
        waitOptions.put("selector", waitSelector);
        waitOptions.put("durationMs", 10000);
        steps.add(new CrawlStep(CrawlStepType.WAIT, waitOptions));
        
        // 对于某些SPA，尝试滚动加载更多内容
        if (analysis.siteType().equals("spa") || analysis.siteType().equals("workday")) {
            Map<String, Object> scrollOptions = new LinkedHashMap<>();
            scrollOptions.put("to", "bottom");
            steps.add(new CrawlStep(CrawlStepType.SCROLL, scrollOptions));
            
            // 滚动后再等待
            Map<String, Object> waitAfterScroll = new LinkedHashMap<>();
            waitAfterScroll.put("durationMs", 3000);
            steps.add(new CrawlStep(CrawlStepType.WAIT, waitAfterScroll));
        }
        
        // 提取列表
        steps.add(new CrawlStep(CrawlStepType.EXTRACT_LIST, Map.of()));
        
        return CrawlFlow.of(steps);
    }

    private record SiteAnalysis(String siteType, boolean requiresBrowser, boolean hasSpaFramework) {}

    public record AutoParseResult(ParserProfile profile,
                                  PagingStrategy pagingStrategy,
                                  AutomationSettings automation,
                                  CrawlFlow flow,
                                  Map<String, Object> metadata) {
    }
}
