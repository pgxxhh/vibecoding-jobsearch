package com.vibe.jobs.sources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ashby ATS Client for fetching job listings
 * Many modern tech companies use Ashby ATS: https://ashbyhq.com/
 * 
 * Example companies using Ashby:
 * - Notion, Figma, Linear, Webflow, Airtable, etc.
 */
public class AshbySourceClient implements SourceClient {
    
    private static final Logger log = LoggerFactory.getLogger(AshbySourceClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    private final String company;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    /**
     * @param company Company name
     * @param baseUrl Base URL for Ashby careers page (e.g., https://jobs.ashbyhq.com/company)
     */
    public AshbySourceClient(String company, String baseUrl) {
        this.company = company;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String sourceName() {
        return "Ashby (" + company + ")";
    }
    
    @Override
    public List<FetchedJob> fetchPage(int page, int size) throws Exception {
        // Ashby typically loads all jobs in one API call
        if (page > 0) {
            return List.of(); // No more pages
        }
        
        String apiUrl = baseUrl + "/api/non-user-graphql?op=ApiJobBoardWithTeams";
        log.debug("Fetching Ashby jobs from: {}", apiUrl);
        
        // GraphQL query for Ashby jobs API
        String graphqlQuery = """
            {
              "query": "query ApiJobBoardWithTeams($organizationHostedJobsPageName: String!) { jobBoard: jobBoardWithTeams(organizationHostedJobsPageName: $organizationHostedJobsPageName) { teams { name uuid jobs { id title locationName isListed publishedAt updatedAt jobPostingUrls { url } } } } }",
              "variables": {
                "organizationHostedJobsPageName": "%s"
              }
            }""".formatted(extractOrgName(baseUrl));
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("User-Agent", "VibeCoding-JobAggregator/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(graphqlQuery))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            // Fallback to HTML scraping if GraphQL fails
            return fetchJobsFromHtml();
        }
        
        AshbyResponse ashbyResponse = objectMapper.readValue(response.body(), AshbyResponse.class);
        return convertToFetchedJobs(ashbyResponse);
    }
    
    private List<FetchedJob> fetchJobsFromHtml() throws Exception {
        // Fallback: Parse HTML careers page
        String htmlUrl = baseUrl;
        log.debug("Falling back to HTML parsing for: {}", htmlUrl);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(htmlUrl))
                .timeout(TIMEOUT)
                .header("User-Agent", "VibeCoding-JobAggregator/1.0")
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ashby HTML error: " + response.statusCode());
        }
        
        return parseHtmlJobs(response.body());
    }
    
    private List<FetchedJob> parseHtmlJobs(String html) {
        List<FetchedJob> jobs = new ArrayList<>();
        
        // Simple HTML parsing for job listings
        // Look for common patterns in Ashby job pages
        String[] lines = html.split("\n");
        for (String line : lines) {
            if ((line.contains("financial") && (line.contains("analyst") || line.contains("Analyst"))) ||
                (line.contains("engineer") || line.contains("Engineer")) ||
                (line.contains("developer") || line.contains("Developer")) ||
                (line.contains("software") || line.contains("Software"))) {
                // Extract job information from HTML line
                try {
                    String title = extractTitleFromHtml(line);
                    String location = extractLocationFromHtml(line);
                    String url = extractUrlFromHtml(line, baseUrl);
                    
                    if (!title.isEmpty()) {
                        Set<String> tags = new HashSet<>();
                        tags.add("ashby");
                        
                        // 根据职位标题添加标签
                        String lowerTitle = title.toLowerCase();
                        if (lowerTitle.contains("analyst")) {
                            tags.add("analyst");
                        }
                        if (lowerTitle.contains("financial")) {
                            tags.add("financial");
                        }
                        if (lowerTitle.contains("engineer")) {
                            tags.add("engineer");
                        }
                        if (lowerTitle.contains("software") || lowerTitle.contains("developer")) {
                            tags.add("software");
                        }
                        
                        Job job = Job.builder()
                                .source("ashby")
                                .externalId(generateJobId(title, company))
                                .title(title)
                                .company(company)
                                .location(location)
                                .postedAt(Instant.now())
                                .url(url)
                                .tags(tags)
                                .build();
                        
                        jobs.add(new FetchedJob(job, ""));
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse HTML job line: {}", e.getMessage());
                }
            }
        }
        
        return jobs;
    }
    
    private String extractTitleFromHtml(String line) {
        // Extract job title from HTML
        if (line.contains("title=\"") || line.contains("title='")) {
            int start = line.indexOf("title=\"");
            if (start == -1) start = line.indexOf("title='");
            if (start != -1) {
                start += 7; // Skip 'title="'
                int end = line.indexOf("\"", start);
                if (end == -1) end = line.indexOf("'", start);
                if (end != -1) {
                    return line.substring(start, end).trim();
                }
            }
        }
        
        // Try other patterns
        if (line.contains(">") && line.contains("<")) {
            int start = line.indexOf(">") + 1;
            int end = line.indexOf("<", start);
            if (end > start) {
                String title = line.substring(start, end).trim();
                if (title.toLowerCase().contains("analyst")) {
                    return title;
                }
            }
        }
        
        return "";
    }
    
    private String extractLocationFromHtml(String line) {
        // Try to find location information
        String[] locationPatterns = {"Remote", "New York", "San Francisco", "London", "Singapore", "Hong Kong"};
        for (String pattern : locationPatterns) {
            if (line.toLowerCase().contains(pattern.toLowerCase())) {
                return pattern;
            }
        }
        return "Remote";
    }
    
    private String extractUrlFromHtml(String line, String baseUrl) {
        if (line.contains("href=\"")) {
            int start = line.indexOf("href=\"") + 6;
            int end = line.indexOf("\"", start);
            if (end > start) {
                String url = line.substring(start, end);
                if (url.startsWith("/")) {
                    return baseUrl + url;
                }
                return url;
            }
        }
        return baseUrl;
    }
    
    private String extractOrgName(String url) {
        // Extract organization name from Ashby URL
        // Example: https://jobs.ashbyhq.com/notion -> "notion"
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }
    
    private String generateJobId(String title, String company) {
        return (company + "-" + title).toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
    
    private List<FetchedJob> convertToFetchedJobs(AshbyResponse response) {
        List<FetchedJob> jobs = new ArrayList<>();
        
        if (response.data == null || response.data.jobBoard == null || response.data.jobBoard.teams == null) {
            return jobs;
        }
        
        for (AshbyTeam team : response.data.jobBoard.teams) {
            if (team.jobs == null) continue;
            
            for (AshbyJob ashbyJob : team.jobs) {
                if (!ashbyJob.isListed || !isFinancialAnalystRole(ashbyJob.title)) {
                    continue;
                }
                
                try {
                    String jobUrl = ashbyJob.jobPostingUrls != null && !ashbyJob.jobPostingUrls.isEmpty() 
                            ? ashbyJob.jobPostingUrls.get(0).url 
                            : baseUrl + "/jobs/" + ashbyJob.id;
                    
                    Set<String> tags = new HashSet<>();
                    tags.add("ashby");
                    
                    // 根据职位标题添加标签
                    if (ashbyJob.title != null) {
                        String lowerTitle = ashbyJob.title.toLowerCase();
                        if (lowerTitle.contains("analyst")) {
                            tags.add("analyst");
                        }
                        if (lowerTitle.contains("financial")) {
                            tags.add("financial");
                        }
                        if (lowerTitle.contains("engineer")) {
                            tags.add("engineer");
                        }
                        if (lowerTitle.contains("software") || lowerTitle.contains("developer")) {
                            tags.add("software");
                        }
                        if (lowerTitle.contains("backend")) {
                            tags.add("backend");
                        }
                        if (lowerTitle.contains("frontend")) {
                            tags.add("frontend");
                        }
                        if (lowerTitle.contains("fullstack") || lowerTitle.contains("full stack")) {
                            tags.add("fullstack");
                        }
                    }
                    
                    Job job = Job.builder()
                            .source("ashby")
                            .externalId(ashbyJob.id)
                            .title(ashbyJob.title)
                            .company(company)
                            .location(ashbyJob.locationName != null ? ashbyJob.locationName : "Remote")
                            .postedAt(parseAshbyDate(ashbyJob.publishedAt))
                            .url(jobUrl)
                            .tags(tags)
                            .build();
                    
                    jobs.add(new FetchedJob(job, ""));
                    
                } catch (Exception e) {
                    log.warn("Failed to parse Ashby job: {}", e.getMessage());
                }
            }
        }
        
        return jobs;
    }
    
    private boolean isFinancialAnalystRole(String title) {
        if (title == null) return false;
        String lowerTitle = title.toLowerCase();
        
        // 财务分析师岗位
        boolean isFinancial = lowerTitle.contains("financial") && lowerTitle.contains("analyst") ||
               lowerTitle.contains("investment analyst") ||
               lowerTitle.contains("research analyst") ||
               lowerTitle.contains("business analyst") ||
               lowerTitle.contains("data analyst") ||
               lowerTitle.contains("quantitative analyst");
        
        // 工程师岗位
        boolean isEngineer = lowerTitle.contains("engineer") ||
               lowerTitle.contains("developer") ||
               lowerTitle.contains("software") ||
               lowerTitle.contains("backend") ||
               lowerTitle.contains("frontend") ||
               lowerTitle.contains("fullstack") ||
               lowerTitle.contains("full stack") ||
               lowerTitle.contains("devops") ||
               lowerTitle.contains("sre") ||
               lowerTitle.contains("platform") ||
               lowerTitle.contains("infrastructure") ||
               lowerTitle.contains("security") ||
               lowerTitle.contains("mobile") ||
               lowerTitle.contains("ios") ||
               lowerTitle.contains("android") ||
               lowerTitle.contains("machine learning") ||
               lowerTitle.contains("data engineer") ||
               lowerTitle.contains("ai") ||
               lowerTitle.contains("ml engineer") ||
               lowerTitle.contains("cloud") ||
               lowerTitle.contains("blockchain");
        
        return isFinancial || isEngineer;
    }
    
    private Instant parseAshbyDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return Instant.now();
        }
        
        try {
            // Ashby typically uses ISO format
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
                    .toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            log.debug("Failed to parse Ashby date '{}', using current time", dateStr);
            return Instant.now();
        }
    }
    
    // JSON response classes
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AshbyResponse {
        @JsonProperty("data")
        public AshbyData data;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AshbyData {
        @JsonProperty("jobBoard")
        public AshbyJobBoard jobBoard;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AshbyJobBoard {
        @JsonProperty("teams")
        public List<AshbyTeam> teams;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AshbyTeam {
        @JsonProperty("name")
        public String name;
        
        @JsonProperty("uuid")
        public String uuid;
        
        @JsonProperty("jobs")
        public List<AshbyJob> jobs;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AshbyJob {
        @JsonProperty("id")
        public String id;
        
        @JsonProperty("title")
        public String title;
        
        @JsonProperty("locationName")
        public String locationName;
        
        @JsonProperty("isListed")
        public boolean isListed;
        
        @JsonProperty("publishedAt")
        public String publishedAt;
        
        @JsonProperty("updatedAt")
        public String updatedAt;
        
        @JsonProperty("jobPostingUrls")
        public List<AshbyJobUrl> jobPostingUrls;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AshbyJobUrl {
        @JsonProperty("url")
        public String url;
    }
}