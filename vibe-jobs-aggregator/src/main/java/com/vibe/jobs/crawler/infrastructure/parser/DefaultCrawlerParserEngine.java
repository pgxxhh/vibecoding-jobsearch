package com.vibe.jobs.crawler.infrastructure.parser;

import com.vibe.jobs.crawler.domain.CrawlContext;
import com.vibe.jobs.crawler.domain.CrawlResult;
import com.vibe.jobs.crawler.domain.CrawlSession;
import com.vibe.jobs.crawler.domain.ParserProfile;
import com.vibe.jobs.crawler.domain.ParserProfile.ParsedJob;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.sources.FetchedJob;
import com.vibe.jobs.crawler.infrastructure.engine.CrawlPageSnapshot;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DefaultCrawlerParserEngine implements CrawlerParserEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultCrawlerParserEngine.class);
    private final WebClient.Builder webClientBuilder;

    public DefaultCrawlerParserEngine(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public List<CrawlResult> parse(CrawlSession session, CrawlPageSnapshot snapshot) {
        ParserProfile profile = session.blueprint().parserProfile();
        List<ParsedJob> parsed = profile.parse(snapshot.pageContent());
        List<String> detailSnapshots = snapshot.detailContents();
        int detailIndex = 0;
        List<CrawlResult> results = new ArrayList<>();
        CrawlContext context = session.context();
        
        for (ParsedJob job : parsed) {
            if (job.title() == null || job.title().isBlank()) {
                continue;
            }
            
            // 获取详细内容和增强的职位信息（如果配置了详情获取）
            String inlineDetail = null;
            if (detailIndex < detailSnapshots.size()) {
                inlineDetail = detailSnapshots.get(detailIndex++);
                if (inlineDetail != null && inlineDetail.isBlank()) {
                    inlineDetail = null;
                }
            }

            EnhancedJobResult enhanced = enhanceJobWithDetails(job, profile, inlineDetail);
            ParsedJob finalJob = enhanced.job();
            String finalDescription = enhanced.description();
            
            Job domainJob = Job.builder()
                    .source(resolveSourceName(context))
                    .externalId(cleanExternalId(finalJob.externalId(), finalJob.url(), finalJob.title()))
                    .title(finalJob.title())
                    .company(resolveCompany(context, finalJob))
                    .location(finalJob.location())
                    .level(finalJob.level())
                    .postedAt(finalJob.postedAt() == null ? Instant.now() : finalJob.postedAt())
                    .url(finalJob.url())
                    .tags(normalizeTags(finalJob.tags()))
                    .build();
            results.add(new CrawlResult(new FetchedJob(domainJob, finalDescription), finalDescription, Map.of()));
        }
        return results;
    }

    /**
     * 增强职位信息，包括详情内容和更好的external_id
     */
    private EnhancedJobResult enhanceJobWithDetails(ParsedJob job, ParserProfile profile, String inlineDetailPage) {
        ParserProfile.DetailFetchConfig detailConfig = profile.getDetailFetchConfig();

        if (!detailConfig.isEnabled() && inlineDetailPage == null) {
            return new EnhancedJobResult(job, job.description());
        }

        try {
            // 构建详情URL
            String detailUrl = buildDetailUrl(job, detailConfig);
            if ((detailUrl == null || detailUrl.isBlank()) && inlineDetailPage == null) {
                log.debug("Cannot build detail URL for job: {}", job.title());
                return new EnhancedJobResult(job, job.description());
            }

            // 获取详情页面内容
            String pageContent = inlineDetailPage;
            if (pageContent == null || pageContent.isBlank()) {
                pageContent = fetchDetailPage(detailUrl);
            }
            if (pageContent == null || pageContent.isBlank()) {
                log.debug("No content fetched from: {}", detailUrl != null ? detailUrl : "inline-detail");
                return new EnhancedJobResult(job, job.description());
            }

            // 解析详情内容
            String detailContent = parseDetailContent(pageContent, detailConfig, detailUrl != null ? detailUrl : job.url());
            detailContent = stripRedundantHeader(job, detailContent);

            // 尝试提取更好的职位编号
            String betterJobId = extractJobId(pageContent, job.externalId(), detailConfig);
            
            // 创建增强后的职位信息
            ParsedJob enhancedJob = job;
            if (!betterJobId.equals(job.externalId())) {
                log.debug("Updated external ID from '{}' to '{}' for job '{}'", 
                         job.externalId(), betterJobId, job.title());
                enhancedJob = new ParsedJob(
                    betterJobId,  // 使用更好的职位编号
                    job.title(),
                    job.company(), 
                    job.location(),
                    job.url(),
                    job.level(),
                    job.postedAt(),
                    job.tags(),
                    job.description()
                );
            }
            
            String finalDescription = detailContent != null && !detailContent.isBlank() ?
                detailContent : job.description();
            
            if (detailContent != null && detailContent.length() > 100) {
                log.debug("Enhanced job description for '{}' from: {}", job.title(), detailUrl);
            }
            
            return new EnhancedJobResult(enhancedJob, finalDescription);
            
        } catch (Exception e) {
            log.warn("Failed to enhance job details for '{}': {}", job.title(), e.getMessage());
            return new EnhancedJobResult(job, job.description());
        }
    }

    /**
     * 增强职位结果的包装类
     */
    private record EnhancedJobResult(ParsedJob job, String description) {
    }

    private String resolveSourceName(CrawlContext context) {
        if (context.sourceName() != null && !context.sourceName().isBlank()) {
            return context.sourceName();
        }
        return context.dataSourceCode().isBlank() ? "crawler" : "crawler:" + context.dataSourceCode();
    }

    private String resolveCompany(CrawlContext context, ParsedJob parsed) {
        if (parsed.company() != null && !parsed.company().isBlank()) {
            return parsed.company();
        }
        return context.company().isBlank() ? parsed.company() : context.company();
    }

    private Set<String> normalizeTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }
        return tags.stream()
                .map(tag -> tag == null ? "" : tag.trim().toLowerCase())
                .filter(tag -> !tag.isBlank())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    /**
     * 构建详情URL
     */
    private String buildDetailUrl(ParsedJob job, ParserProfile.DetailFetchConfig config) {
        String baseUrl = config.getBaseUrl();
        String urlField = config.getUrlField();
        
        if (baseUrl.isBlank()) {
            return null;
        }
        
        // 根据字段名获取URL部分
        String urlPart;
        switch (urlField.toLowerCase()) {
            case "url" -> urlPart = job.url();
            case "externalid" -> urlPart = job.externalId();
            default -> {
                log.warn("Unknown URL field: {}", urlField);
                return null;
            }
        }
        
        if (urlPart == null || urlPart.isBlank()) {
            return null;
        }
        
        // 如果urlPart已经是完整URL，直接返回
        if (urlPart.startsWith("http://") || urlPart.startsWith("https://")) {
            return urlPart;
        }
        
        // 拼接baseUrl和urlPart
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String cleanUrlPart = urlPart.startsWith("/") ? urlPart : "/" + urlPart;
        
        return cleanBaseUrl + cleanUrlPart;
    }

    /**
     * 获取详情页面内容
     */
    private String fetchDetailPage(String url) {
        try {
            WebClient client = webClientBuilder
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                    .defaultHeader(HttpHeaders.USER_AGENT, getRandomUserAgent())
                    .build();

            return client.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));

        } catch (WebClientResponseException e) {
            log.debug("HTTP error {} when fetching {}", e.getStatusCode(), url);
            return null;
        } catch (Exception e) {
            log.debug("Error fetching {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 解析详情内容 - 优化版本，支持HTML格式保留和职位编号提取
     */
    private String parseDetailContent(String pageContent, ParserProfile.DetailFetchConfig config, String url) {
        try {
            Document doc = Jsoup.parse(pageContent);
            
            // 检查是否需要HTML格式
            boolean preserveHtml = shouldPreserveHtmlFormat(config, url);
            
            StringBuilder content = new StringBuilder();
            
            // 使用配置的选择器提取内容
            for (String selector : config.getContentSelectors()) {
                try {
                    var elements = doc.select(selector);
                    for (Element element : elements) {
                        String extractedContent;
                        
                        if (preserveHtml) {
                            // 保留HTML格式，但清理不必要的标签和属性
                            extractedContent = cleanHtmlContent(element);
                        } else {
                            // 纯文本提取
                            extractedContent = element.text().trim();
                        }
                        
                        if (!extractedContent.isBlank() && extractedContent.length() > 50) {
                            // 避免重复内容
                            String preview = extractedContent.length() > 50 ? 
                                extractedContent.substring(0, 50).replaceAll("<[^>]*>", "") : extractedContent;
                            
                            if (content.length() == 0 || !content.toString().contains(preview)) {
                                if (content.length() > 0) {
                                    content.append(preserveHtml ? "\n\n" : "\n\n");
                                }
                                content.append(extractedContent);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error with selector '{}' on {}: {}", selector, url, e.getMessage());
                }
            }
            
            if (content.length() == 0) {
                content.append(doc.body().html().trim());
            }
            if (content.length() == 0) {
                content.append(doc.body().text().trim());
            }
            return content.length() > 0 ? content.toString() : null;
            
        } catch (Exception e) {
            log.debug("Error parsing detail content from {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 判断是否应该保留HTML格式 - 通用化版本
     */
    private boolean shouldPreserveHtmlFormat(ParserProfile.DetailFetchConfig config, String url) {
        // 优先检查配置中的明确设置
        if (config.shouldPreserveHtml() != null) {
            return config.shouldPreserveHtml();
        }
        
        // 基于URL模式的智能判断
        String[] htmlFormattedSites = {
            "jobs.apple.com",
            "careers.airbnb.com", 
            "careers.google.com",
            "careers.microsoft.com",
            "jobs.netflix.com"
        };
        
        for (String site : htmlFormattedSites) {
            if (url.contains(site)) {
                return true;
            }
        }
        
        // 基于选择器特征的判断
        return config.getContentSelectors().stream()
                .anyMatch(s -> s.contains("section") || s.contains("div[class*") || s.contains("#description"));
    }

    /**
     * 清理HTML内容，保留格式但移除不必要的标签和属性
     */
    private String cleanHtmlContent(Element element) {
        // 克隆元素以避免修改原始DOM
        Element cleaned = element.clone();
        
        // 移除不必要的属性，保留基本格式标签
        cleaned.select("*").forEach(el -> {
            // 保留的标签
            String tagName = el.tagName().toLowerCase();
            if (!isAllowedTag(tagName)) {
                el.unwrap(); // 移除标签但保留内容
                return;
            }
            
            // 清理属性，只保留必要的
            el.attributes().asList().forEach(attr -> {
                if (!isAllowedAttribute(attr.getKey())) {
                    el.removeAttr(attr.getKey());
                }
            });
        });
        
        // 清理空白和格式化
        String html = cleaned.html()
                .replaceAll("(?m)^\\s*$", "") // 移除空行
                .replaceAll("\\s{2,}", " ") // 压缩多余空格
                .trim();
        
        return html.isBlank() ? cleaned.text().trim() : html;
    }

    /**
     * 允许的HTML标签（保留格式用）
     */
    private boolean isAllowedTag(String tagName) {
        return switch (tagName) {
            case "h1", "h2", "h3", "h4", "h5", "h6" -> true; // 标题
            case "p", "div", "section" -> true;               // 段落和容器
            case "ul", "ol", "li" -> true;                    // 列表
            case "strong", "b", "em", "i" -> true;           // 强调
            case "br" -> true;                                // 换行
            default -> false;
        };
    }

    /**
     * 允许的HTML属性
     */
    private boolean isAllowedAttribute(String attrName) {
        return switch (attrName.toLowerCase()) {
            case "id", "class" -> true;  // 基本属性
            default -> false;
        };
    }

    private String stripRedundantHeader(ParsedJob job, String detailContent) {
        if (detailContent == null || detailContent.isBlank()) {
            return detailContent;
        }
        String title = job.title() == null ? "" : job.title().trim();
        String location = job.location() == null ? "" : job.location().trim();
        String cleaned = detailContent;
        try {
            if (detailContent.contains("<")) {
                Document fragment = Jsoup.parseBodyFragment(detailContent);
                Element body = fragment.body();
                stripMatchingChildren(body, title, location);
                cleaned = body.html().trim();
                if (cleaned.isBlank()) {
                    cleaned = body.text().trim();
                }
            } else {
                cleaned = removeLeadingLines(detailContent, title, location);
            }
        } catch (Exception ex) {
            log.debug("Failed to strip header from detail content: {}", ex.getMessage());
        }
        return cleaned.isBlank() ? detailContent : cleaned;
    }

    private void stripMatchingChildren(Element body, String title, String location) {
        if (body == null) {
            return;
        }
        List<Element> children = new ArrayList<>(body.children());
        int removed = 0;
        for (Element child : children) {
            if (removed >= 3) {
                break;
            }
            String text = child.text().trim();
            if (matchesHeaderText(text, title, location)) {
                child.remove();
                removed++;
            } else {
                break;
            }
        }
    }

    private String removeLeadingLines(String detailContent, String title, String location) {
        String[] lines = detailContent.split("\n");
        List<String> remaining = new ArrayList<>();
        boolean skipping = true;
        int removed = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (skipping && removed < 3 && matchesHeaderText(trimmed, title, location)) {
                removed++;
                continue;
            }
            skipping = false;
            remaining.add(line);
        }
        return String.join("\n", remaining).trim();
    }

    private boolean matchesHeaderText(String text, String title, String location) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.strip();
        if (!title.isBlank() && normalized.equalsIgnoreCase(title)) {
            return true;
        }
        if (!location.isBlank() && normalized.equalsIgnoreCase(location)) {
            return true;
        }
        if (!title.isBlank() && !location.isBlank()) {
            String combined = (title + " " + location).trim();
            if (normalized.equalsIgnoreCase(combined)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从页面内容中提取职位编号作为更好的external_id - 通用化版本
     */
    private String extractJobId(String pageContent, String fallbackExternalId, ParserProfile.DetailFetchConfig config) {
        try {
            // 使用配置中的提取模式，如果没有则使用默认模式
            var patterns = getJobIdPatterns(config);
            
            for (String pattern : patterns) {
                var matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(pageContent);
                if (matcher.find()) {
                    String jobId = matcher.group(1).trim();
                    if (!jobId.isBlank()) {
                        log.debug("Extracted job ID '{}' using pattern '{}'", jobId, pattern);
                        return jobId;
                    }
                }
            }
            
            // 如果没找到，从URL中提取
            if (fallbackExternalId != null && fallbackExternalId.contains("/")) {
                // 通用的URL ID提取模式
                var urlPatterns = new String[]{
                    "/(?:details|job|position|career)/([A-Za-z0-9-]+)",
                    "/([A-Za-z0-9-]+)/?(?:\\?|$)",
                    "id=([A-Za-z0-9-]+)"
                };
                
                for (String urlPattern : urlPatterns) {
                    var urlMatcher = java.util.regex.Pattern.compile(urlPattern).matcher(fallbackExternalId);
                    if (urlMatcher.find()) {
                        String extractedId = urlMatcher.group(1);
                        if (!extractedId.isBlank() && extractedId.length() > 3) {
                            log.debug("Extracted job ID '{}' from URL using pattern '{}'", extractedId, urlPattern);
                            return extractedId;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.debug("Failed to extract job ID: {}", e.getMessage());
        }
        
        return fallbackExternalId; // 回退到原始值
    }

    /**
     * 获取职位ID提取模式 - 支持配置化
     */
    private String[] getJobIdPatterns(ParserProfile.DetailFetchConfig config) {
        // 通用的职位ID提取模式
        return new String[]{
            // Apple Jobs
            "Role Number[:\\s]*([A-Za-z0-9-]+)",
            // Google Careers  
            "Job ID[:\\s]*([A-Za-z0-9-]+)",
            // Microsoft Careers
            "Job number[:\\s]*([A-Za-z0-9-]+)",
            // Airbnb
            "Requisition[:\\s]*(?:ID[:\\s]*)?([A-Za-z0-9-]+)",
            // Netflix
            "Position ID[:\\s]*([A-Za-z0-9-]+)",
            // Amazon
            "Job ID[:\\s]*([A-Za-z0-9-]+)",
            // 通用模式
            "(?:Job|Position|Role|Req)\\s*(?:ID|Number|#)[:\\s]*([A-Za-z0-9-]+)",
            "ID[:\\s]+([A-Za-z0-9]{5,})",
        };
    }

    /**
     * 清理和优化external_id - 从URL中提取最佳标识符
     */
    private String cleanExternalId(String rawExternalId, String jobUrl, String title) {
        // 尝试从URL中提取数字ID
        if (jobUrl != null && !jobUrl.isBlank()) {
            // 匹配常见的职位ID模式
            var patterns = new String[]{
                "/positions/(\\d+)/",           // Airbnb: /positions/7174313/
                "/job/([A-Za-z0-9-]+)",         // 通用: /job/abc-123
                "/career/([A-Za-z0-9-]+)",      // 通用: /career/def-456
                "/details/([A-Za-z0-9-]+)",     // Apple: /details/200619104
                "\\?id=([A-Za-z0-9-]+)",       // 查询参数: ?id=xyz789
                "/([A-Za-z0-9-]{5,})/?$"       // URL末尾的ID
            };
            
            for (String pattern : patterns) {
                try {
                    var matcher = java.util.regex.Pattern.compile(pattern).matcher(jobUrl);
                    if (matcher.find()) {
                        String extractedId = matcher.group(1);
                        if (!extractedId.isBlank() && extractedId.length() >= 3) {
                            log.debug("Extracted clean external ID '{}' from URL: {}", extractedId, jobUrl);
                            return extractedId;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Pattern matching error: {}", e.getMessage());
                }
            }
        }
        
        // 如果rawExternalId是有效的数字或短字符串，直接使用
        if (rawExternalId != null && !rawExternalId.isBlank()) {
            // 如果是纯数字或合理长度的字符串
            if (rawExternalId.matches("\\d+") || (rawExternalId.length() >= 3 && rawExternalId.length() <= 20)) {
                return rawExternalId;
            }
            
            // 如果是URL，尝试从中提取
            if (rawExternalId.contains("/")) {
                for (String pattern : new String[]{"/(\\d+)/?", "/([A-Za-z0-9-]{3,})/?$"}) {
                    try {
                        var matcher = java.util.regex.Pattern.compile(pattern).matcher(rawExternalId);
                        if (matcher.find()) {
                            return matcher.group(1);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        
        // 最后回退到title的hash或截断版本
        if (title != null && !title.isBlank()) {
            return String.valueOf(Math.abs(title.hashCode()));
        }
        
        // 完全失败的情况，生成随机ID
        return "job-" + System.currentTimeMillis() % 1000000;
    }

    /**
     * 获取随机User-Agent
     */
    private String getRandomUserAgent() {
        String[] agents = {
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
        };
        return agents[ThreadLocalRandom.current().nextInt(agents.length)];
    }
}
