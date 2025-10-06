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
            
            // 获取详细内容（如果配置了详情获取）
            String enhancedDescription = enhanceJobDescription(job, profile);
            String finalDescription = enhancedDescription != null ? enhancedDescription : job.description();
            
            Job domainJob = Job.builder()
                    .source(resolveSourceName(context))
                    .externalId(job.externalId().isBlank() ? job.title() : job.externalId())
                    .title(job.title())
                    .company(resolveCompany(context, job))
                    .location(job.location())
                    .level(job.level())
                    .postedAt(job.postedAt() == null ? Instant.now() : job.postedAt())
                    .url(job.url())
                    .tags(normalizeTags(job.tags()))
                    .build();
            results.add(new CrawlResult(new FetchedJob(domainJob, finalDescription), finalDescription, Map.of()));
        }
        return results;
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
     * 解析详情内容
     */
    private String parseDetailContent(String pageContent, ParserProfile.DetailFetchConfig config, String url) {
        try {
            Document doc = Jsoup.parse(pageContent);
            StringBuilder content = new StringBuilder();
            
            // 使用配置的选择器提取内容
            for (String selector : config.getContentSelectors()) {
                try {
                    var elements = doc.select(selector);
                    for (Element element : elements) {
                        String text = element.text().trim();
                        if (!text.isBlank() && text.length() > 50) {
                            // 避免重复内容
                            if (content.length() == 0 || !content.toString().contains(text.substring(0, Math.min(50, text.length())))) {
                                if (content.length() > 0) {
                                    content.append("\n\n");
                                }
                                content.append(text);
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
