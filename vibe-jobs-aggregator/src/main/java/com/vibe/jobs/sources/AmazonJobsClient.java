package com.vibe.jobs.sources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vibe.jobs.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Amazon Jobs API客户端
 * 使用官方API: https://www.amazon.jobs/en/search.json
 */
public class AmazonJobsClient implements SourceClient {
    
    private static final Logger log = LoggerFactory.getLogger(AmazonJobsClient.class);
    private static final String BASE_URL = "https://www.amazon.jobs/en/search.json";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    private final HttpClient httpClient;
    
    public AmazonJobsClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }
    
    @Override
    public String sourceName() {
        return "Amazon Jobs API";
    }
    
    @Override
    public List<FetchedJob> fetchPage(int page, int size) throws Exception {
        int offset = (page - 1) * size;
        
        String apiUrl = buildApiUrl(offset, size);
        log.debug("Fetching Amazon jobs from: {}", apiUrl);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://www.amazon.jobs/en/")
                .GET()
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 301 || response.statusCode() == 302) {
                log.warn("Amazon API redirected ({}), API endpoint may have changed", response.statusCode());
                return new ArrayList<>(); // 返回空列表而不是抛出异常
            }
            
            if (response.statusCode() != 200) {
                log.warn("Amazon API returned status: {}, skipping this fetch", response.statusCode());
                return new ArrayList<>(); // 返回空列表而不是抛出异常
            }
            
            return parseAmazonResponse(response.body());
            
        } catch (Exception e) {
            log.warn("Error fetching Amazon jobs: {}, continuing without this source", e.getMessage());
            return new ArrayList<>(); // 返回空列表而不是抛出异常
        }
    }
    
    private String buildApiUrl(int offset, int size) {
        StringBuilder url = new StringBuilder(BASE_URL);
        url.append("?offset=").append(offset);
        url.append("&result_limit=").append(size);
        
        // 搜索关键词
        String keywords = "financial analyst OR software engineer OR data engineer OR cloud engineer";
        url.append("&q=").append(URLEncoder.encode(keywords, StandardCharsets.UTF_8));
        
        // 亚太地区
        String locations = "CN,SG,HK,JP,AU,IN";
        url.append("&loc_query=").append(URLEncoder.encode(locations, StandardCharsets.UTF_8));
        
        // 排序
        url.append("&sort=recent");
        
        return url.toString();
    }
    
    private List<FetchedJob> parseAmazonResponse(String responseBody) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            AmazonApiResponse apiResponse = mapper.readValue(responseBody, AmazonApiResponse.class);
            
            List<FetchedJob> jobs = new ArrayList<>();
            
            if (apiResponse.jobs != null) {
                for (AmazonJob amazonJob : apiResponse.jobs) {
                    try {
                        FetchedJob job = convertToFetchedJob(amazonJob);
                        if (job != null && isTargetJob(amazonJob.title)) {
                            jobs.add(job);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse Amazon job: {}", e.getMessage());
                    }
                }
            }
            
            return jobs;
        } catch (Exception e) {
            log.error("Failed to parse Amazon API response: {}", e.getMessage());
            return List.of();
        }
    }
    
    private FetchedJob convertToFetchedJob(AmazonJob amazonJob) {
        Set<String> tags = new HashSet<>();
        tags.add("amazon");
        tags.add("official-api");
        
        // 根据职位标题添加标签
        if (amazonJob.title != null) {
            String lowerTitle = amazonJob.title.toLowerCase();
            if (lowerTitle.contains("financial") || lowerTitle.contains("analyst")) {
                tags.add("financial");
                tags.add("analyst");
            }
            if (lowerTitle.contains("engineer")) {
                tags.add("engineer");
            }
            if (lowerTitle.contains("software")) {
                tags.add("software");
            }
            if (lowerTitle.contains("aws") || lowerTitle.contains("cloud")) {
                tags.add("cloud");
                tags.add("aws");
            }
            if (lowerTitle.contains("data")) {
                tags.add("data");
            }
        }
        
        String jobUrl = "https://www.amazon.jobs/en/jobs/" + amazonJob.id_icims;
        
        Job job = Job.builder()
                .source("amazon-api")
                .externalId(amazonJob.id_icims)
                .title(amazonJob.title)
                .company("Amazon")
                .location(amazonJob.location != null ? amazonJob.location : "Global")
                .postedAt(parseAmazonDate(amazonJob.posted_date))
                .url(jobUrl)
                .tags(tags)
                .build();
        
        return new FetchedJob(job, amazonJob.basic_qualifications != null ? amazonJob.basic_qualifications : "");
    }
    
    private boolean isTargetJob(String title) {
        if (title == null) return false;
        String lowerTitle = title.toLowerCase();
        
        boolean isFinancial = lowerTitle.contains("financial") ||
               lowerTitle.contains("analyst") ||
               lowerTitle.contains("finance");
        
        boolean isEngineer = lowerTitle.contains("engineer") ||
               lowerTitle.contains("developer") ||
               lowerTitle.contains("software") ||
               lowerTitle.contains("technical") ||
               lowerTitle.contains("architect") ||
               lowerTitle.contains("sde");
        
        return isFinancial || isEngineer;
    }
    
    private Instant parseAmazonDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return Instant.now();
        }
        
        try {
            // Amazon日期格式通常是 "January 1, 2024" 或 "2024-01-01"
            if (dateStr.contains(",")) {
                // 处理 "January 1, 2024" 格式
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
            } else {
                // 处理 "2024-01-01" 格式
                LocalDate date = LocalDate.parse(dateStr);
                return date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
            }
        } catch (Exception e) {
            return Instant.now();
        }
    }
    
    // Amazon API响应模型
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AmazonApiResponse {
        @JsonProperty("jobs")
        public List<AmazonJob> jobs;
        
        @JsonProperty("hits")
        public Integer hits;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AmazonJob {
        @JsonProperty("id_icims")
        public String id_icims;
        
        @JsonProperty("title")
        public String title;
        
        @JsonProperty("location")
        public String location;
        
        @JsonProperty("basic_qualifications")
        public String basic_qualifications;
        
        @JsonProperty("posted_date")
        public String posted_date;
        
        @JsonProperty("job_category")
        public String job_category;
        
        @JsonProperty("company_name")
        public String company_name;
        
        @JsonProperty("business_category")
        public String business_category;
    }
}