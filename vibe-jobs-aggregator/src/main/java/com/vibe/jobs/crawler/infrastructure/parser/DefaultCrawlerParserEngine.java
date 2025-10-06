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
        List<CrawlResult> results = new ArrayList<>();
        CrawlContext context = session.context();
        
        for (ParsedJob job : parsed) {
            if (job.title() == null || job.title().isBlank()) {
                continue;
            }
            
            // 获取详细内容和增强的职位信息（如果配置了详情获取）
            EnhancedJobResult enhanced = enhanceJobWithDetails(job, profile);
            ParsedJob finalJob = enhanced.job();
            String finalDescription = enhanced.description();
            
            Job domainJob = Job.builder()
                    .source(resolveSourceName(context))
                    .externalId(finalJob.externalId().isBlank() ? finalJob.title() : finalJob.externalId())
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
    private EnhancedJobResult enhanceJobWithDetails(ParsedJob job, ParserProfile profile) {
        ParserProfile.DetailFetchConfig detailConfig = profile.getDetailFetchConfig();
        
        if (!detailConfig.isEnabled()) {
            return new EnhancedJobResult(job, job.description());
        }
        
        try {
            // 构建详情URL
            String detailUrl = buildDetailUrl(job, detailConfig);
            if (detailUrl == null || detailUrl.isBlank()) {
                log.debug("Cannot build detail URL for job: {}", job.title());
                return new EnhancedJobResult(job, job.description());
            }
            
            // 获取详情页面内容
            String pageContent = fetchDetailPage(detailUrl);
            if (pageContent == null || pageContent.isBlank()) {
                log.debug("No content fetched from: {}", detailUrl);
                return new EnhancedJobResult(job, job.description());
            }
            
            // 解析详情内容
            String detailContent = parseDetailContent(pageContent, detailConfig, detailUrl);
            
            // 尝试提取更好的职位编号
            String betterJobId = extractJobId(pageContent, job.externalId());
            
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
            
            String finalDescription = detailContent != null && detailContent.length() > 100 ? 
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
     * 增强职位描述 - 如果配置了详情获取，则访问详情页面获取完整内容
     */
    private String enhanceJobDescription(ParsedJob job, ParserProfile profile) {
        ParserProfile.DetailFetchConfig detailConfig = profile.getDetailFetchConfig();
        
        if (!detailConfig.isEnabled()) {
            return null;  // 返回null表示不需要增强
        }
        
        try {
            // 构建详情URL
            String detailUrl = buildDetailUrl(job, detailConfig);
            if (detailUrl == null || detailUrl.isBlank()) {
                log.debug("Cannot build detail URL for job: {}", job.title());
                return null;
            }
            
            // 获取详情页面内容
            String pageContent = fetchDetailPage(detailUrl);
            if (pageContent == null || pageContent.isBlank()) {
                log.debug("No content fetched from: {}", detailUrl);
                return null;
            }
            
            // 解析详情内容
            String detailContent = parseDetailContent(pageContent, detailConfig, detailUrl);
            if (detailContent != null && detailContent.length() > 100) {
                log.debug("Enhanced job description for '{}' from: {}", job.title(), detailUrl);
                
                // 尝试提取更好的职位编号并更新
                String betterJobId = extractJobId(pageContent, job.externalId());
                if (!betterJobId.equals(job.externalId())) {
                    log.debug("Updated external ID from '{}' to '{}' for job '{}'", 
                             job.externalId(), betterJobId, job.title());
                    // 这里我们需要创建一个新的ParsedJob实例来更新external_id
                    // 但由于ParsedJob是record，我们需要在调用方处理这个逻辑
                }
                
                return detailContent;
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("Failed to enhance job description for '{}': {}", job.title(), e.getMessage());
            return null;
        }
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
            
            return content.length() > 0 ? content.toString() : null;
            
        } catch (Exception e) {
            log.debug("Error parsing detail content from {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 判断是否应该保留HTML格式
     */
    private boolean shouldPreserveHtmlFormat(ParserProfile.DetailFetchConfig config, String url) {
        // 可以根据URL或配置决定是否保留HTML
        // 对于Apple Jobs，保留HTML以获得更好的格式
        return url.contains("jobs.apple.com") || 
               config.getContentSelectors().stream().anyMatch(s -> s.contains("section") || s.contains("div"));
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

    /**
     * 从页面内容中提取职位编号作为更好的external_id
     */
    private String extractJobId(String pageContent, String fallbackExternalId) {
        try {
            // 尝试提取Role Number
            var roleNumberPatterns = new String[]{
                "Role Number[:\\s]*([0-9-]+)",
                "role[\\s\\-_]*number[:\\s]*([0-9-]+)", 
                "job[\\s\\-_]*id[:\\s]*([0-9-]+)",
                "position[\\s\\-_]*id[:\\s]*([0-9-]+)"
            };
            
            for (String pattern : roleNumberPatterns) {
                var matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(pageContent);
                if (matcher.find()) {
                    String roleNumber = matcher.group(1).trim();
                    if (!roleNumber.isBlank()) {
                        log.debug("Extracted job ID '{}' from page content", roleNumber);
                        return roleNumber;
                    }
                }
            }
            
            // 如果没找到，从URL中提取数字部分
            if (fallbackExternalId != null && fallbackExternalId.contains("/")) {
                var urlMatcher = java.util.regex.Pattern.compile("/details/([0-9-]+)")
                        .matcher(fallbackExternalId);
                if (urlMatcher.find()) {
                    return urlMatcher.group(1);
                }
            }
            
        } catch (Exception e) {
            log.debug("Failed to extract job ID: {}", e.getMessage());
        }
        
        return fallbackExternalId; // 回退到原始值
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
