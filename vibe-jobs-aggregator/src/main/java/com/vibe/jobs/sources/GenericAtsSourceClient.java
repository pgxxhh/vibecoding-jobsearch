package com.vibe.jobs.sources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用ATS适配器 - 支持中国本土ATS系统
 * 包括：Moka智聘、北森(Beisen)、SAP SuccessFactors、Oracle Taleo、iCIMS、SmartRecruiters等
 */
public class GenericAtsSourceClient implements SourceClient {
    
    private static final Logger log = LoggerFactory.getLogger(GenericAtsSourceClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    private final String company;
    private final String baseUrl;
    private final String searchPath;
    private final Map<String, String> queryParams;
    private final String atsType;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> payloadParams;
    private final Map<String, String> extraHeaders;

    /**
     * @param company 公司名称
     * @param baseUrl ATS基础URL
     * @param searchPath 搜索API路径 (可选，默认自动检测)
     * @param queryParams 查询参数映射
     * @param atsType ATS类型标识 (moka, beisen, successfactors, taleo, icims, smartrecruiters)
     */
    public GenericAtsSourceClient(String company, String baseUrl, String searchPath,
                                  Map<String, String> queryParams,
                                  Map<String, String> payloadParams,
                                  Map<String, String> headers,
                                  String atsType) {
        this.company = company;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.searchPath = searchPath != null ? searchPath : detectSearchPath(atsType);
        this.queryParams = queryParams != null ? queryParams : Map.of();
        this.payloadParams = payloadParams != null ? payloadParams : Map.of();
        this.extraHeaders = headers != null ? headers : Map.of();
        this.atsType = atsType != null ? atsType.toLowerCase() : "generic";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String sourceName() {
        return "Generic ATS (" + company + " - " + atsType + ")";
    }
    
    @Override
    public List<FetchedJob> fetchPage(int page, int size) throws Exception {
        String apiUrl = buildApiUrl(page, size);
        log.debug("Fetching {} jobs from: {}", atsType, apiUrl);
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

        extraHeaders.forEach(requestBuilder::header);

        // 根据ATS类型设置特定请求头
        switch (atsType) {
            case "moka":
                requestBuilder.header("X-Requested-With", "XMLHttpRequest");
                requestBuilder.header("Referer", baseUrl);
                break;
            case "beisen":
                requestBuilder.header("X-Requested-With", "XMLHttpRequest");
                requestBuilder.header("Content-Type", "application/json;charset=UTF-8");
                break;
            case "successfactors":
                requestBuilder.header("Accept", "application/json, text/javascript, */*; q=0.01");
                break;
        }
        
        HttpRequest request;
        if (requiresPost()) {
            String payload = buildPostPayload(page, size);
            request = requestBuilder
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
        } else {
            request = requestBuilder.GET().build();
        }
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException(atsType + " API error: " + response.statusCode() + " - " + response.body());
        }
        
        return parseResponse(response.body());
    }
    
    private String detectSearchPath(String atsType) {
        return switch (atsType) {
            case "moka" -> "/api/jobs/search";
            case "beisen" -> "/sf/job-search";
            case "successfactors" -> "/career-search/job_search_api";
            case "taleo" -> "/careersection/2/jobsearch.ftl";
            case "icims" -> "/jobs/search";
            case "smartrecruiters" -> "/api/more_postings";
            default -> "/api/jobs";
        };
    }
    
    private boolean requiresPost() {
        return Set.of("moka", "beisen", "successfactors", "smartrecruiters").contains(atsType);
    }
    
    private String buildApiUrl(int page, int size) {
        StringBuilder url = new StringBuilder(baseUrl + searchPath);
        
        Map<String, String> combinedParams = new java.util.LinkedHashMap<>();
        if (!requiresPost()) {
            combinedParams.putAll(getDefaultParams(page, size));
        }
        combinedParams.putAll(queryParams);

        if (!combinedParams.isEmpty()) {
            boolean firstParam = !searchPath.contains("?");
            for (Map.Entry<String, String> param : combinedParams.entrySet()) {
                url.append(firstParam ? "?" : "&");
                url.append(param.getKey()).append("=").append(encode(param.getValue()));
                firstParam = false;
            }
        }

        return url.toString();
    }
    
    private Map<String, String> getDefaultParams(int page, int size) {
        return switch (atsType) {
            case "taleo" -> Map.of(
                    "keyword", "财务|金融|financial|analyst|工程师|engineer|软件|software|developer|程序员",
                    "location", "中国|上海|北京|深圳|广州",
                    "startIndex", String.valueOf(page * size),
                    "maxResults", String.valueOf(size)
            );
            case "icims" -> Map.of(
                    "searchText", "financial analyst 财务分析师 software engineer 软件工程师",
                    "location", "China;Shanghai;Beijing",
                    "offset", String.valueOf(page * size),
                    "limit", String.valueOf(size)
            );
            default -> Map.of(
                    "page", String.valueOf(page),
                    "size", String.valueOf(size),
                    "keyword", "financial analyst engineer 财务 工程师"
            );
        };
    }
    
    private String buildPostPayload(int page, int size) {
        Map<String, Object> payload = switch (atsType) {
            case "moka" -> Map.of(
                    "keyword", "财务分析师 financial analyst 软件工程师 software engineer",
                    "location", "上海,北京,深圳,广州",
                    "page", page,
                    "size", size,
                    "limit", size,
                    "department", "",
                    "jobType", ""
            );
            case "beisen" -> Map.of(
                    "searchText", "财务|金融|投资|分析师|工程师|软件|开发|程序员",
                    "location", "中国",
                    "pageIndex", page,
                    "pageSize", size,
                    "sortType", "1"
            );
            case "successfactors" -> Map.of(
                    "appliedFacets", Map.of(),
                    "searchText", "financial analyst engineer 财务分析 软件工程师",
                    "location", "China",
                    "offset", page * size,
                    "limit", size
            );
            case "smartrecruiters" -> Map.of(
                    "query", "financial analyst OR software engineer",
                    "location.name", "China",
                    "offset", page * size,
                    "limit", size,
                    "department.name", "Finance,Engineering,Technology"
            );
            default -> Map.of(
                    "query", "financial analyst engineer 财务分析师 软件工程师",
                    "page", page,
                    "size", size
            );
        };
        
        try {
            java.util.Map<String, Object> merged = new java.util.LinkedHashMap<>(payload);
            payloadParams.forEach(merged::put);
            merged.putIfAbsent("page", page);
            merged.putIfAbsent("pageIndex", page);
            merged.putIfAbsent("offset", page * size);
            merged.putIfAbsent("limit", size);
            merged.putIfAbsent("size", size);
            merged.putIfAbsent("pageSize", size);
            return objectMapper.writeValueAsString(merged);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build POST payload", e);
        }
    }
    
    private List<FetchedJob> parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            List<FetchedJob> jobs = new ArrayList<>();
            
            JsonNode jobsNode = findJobsNode(root);
            if (jobsNode == null || !jobsNode.isArray()) {
                return jobs;
            }
            
            for (JsonNode jobNode : jobsNode) {
                try {
                    FetchedJob job = parseJob(jobNode);
                    if (job != null && isTargetJob(job.job().getTitle())) {
                        jobs.add(job);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse job from {}: {}", atsType, e.getMessage());
                }
            }
            
            return jobs;
        } catch (Exception e) {
            log.error("Failed to parse response from {}: {}", atsType, e.getMessage());
            return List.of();
        }
    }
    
    private JsonNode findJobsNode(JsonNode root) {
        // 尝试常见的jobs数组路径
        String[] jobsPaths = {"jobs", "data.jobs", "result.jobs", "jobPostings", "items", "data", "results"};
        
        for (String path : jobsPaths) {
            JsonNode node = root;
            for (String segment : path.split("\\.")) {
                node = node.get(segment);
                if (node == null) break;
            }
            if (node != null && node.isArray()) {
                return node;
            }
        }
        
        // 如果根节点就是数组
        if (root.isArray()) {
            return root;
        }
        
        return null;
    }
    
    private FetchedJob parseJob(JsonNode jobNode) throws Exception {
        String id = getTextValue(jobNode, "id", "jobId", "positionId", "req_id");
        String title = getTextValue(jobNode, "title", "jobTitle", "positionName", "name");
        String location = getTextValue(jobNode, "location", "city", "workplace", "jobLocation");
        String url = getTextValue(jobNode, "url", "jobUrl", "applyUrl", "detailUrl");
        String description = getTextValue(jobNode, "description", "jobDescription", "summary", "content");
        
        if (id == null) {
            id = generateJobId(title, company);
        }
        
        if (title == null || title.isEmpty()) {
            return null;
        }
        
        Set<String> tags = new HashSet<>();
        tags.add(atsType);
        tags.add("china");
        
        // 根据职位标题添加相应标签
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("analyst") || title.contains("分析师")) {
            tags.add("analyst");
        }
        if (lowerTitle.contains("financial") || title.contains("财务")) {
            tags.add("financial");
        }
        if (lowerTitle.contains("engineer") || title.contains("工程师")) {
            tags.add("engineer");
        }
        if (lowerTitle.contains("software") || lowerTitle.contains("developer") || title.contains("软件") || title.contains("开发")) {
            tags.add("software");
            tags.add("engineer");
        }
        if (lowerTitle.contains("backend") || lowerTitle.contains("后端")) {
            tags.add("backend");
        }
        if (lowerTitle.contains("frontend") || lowerTitle.contains("前端")) {
            tags.add("frontend");
        }
        if (lowerTitle.contains("fullstack") || lowerTitle.contains("full stack") || title.contains("全栈")) {
            tags.add("fullstack");
        }
        
        Job job = Job.builder()
                .source(atsType)
                .externalId(id)
                .title(title)
                .company(company)
                .location(location != null ? location : "中国")
                .postedAt(Instant.now()) // 大多数ATS不提供精确时间
                .url(url != null ? url : baseUrl)
                .tags(tags)
                .build();
        
        return new FetchedJob(job, description != null ? description : "");
    }
    
    private String getTextValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode fieldNode = node.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                return fieldNode.asText().trim();
            }
        }
        return null;
    }
    
    private boolean isTargetJob(String title) {
        if (title == null) return false;
        String lowerTitle = title.toLowerCase();
        
        // 财务相关岗位
        boolean isFinancial = lowerTitle.contains("financial") ||
               lowerTitle.contains("finance") ||
               lowerTitle.contains("analyst") ||
               lowerTitle.contains("investment") ||
               title.contains("财务") ||
               title.contains("金融") ||
               title.contains("投资") ||
               title.contains("分析师");
        
        // 工程师相关岗位
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
               lowerTitle.contains("cloud") ||
               lowerTitle.contains("blockchain") ||
               title.contains("工程师") ||
               title.contains("软件") ||
               title.contains("开发") ||
               title.contains("程序员") ||
               title.contains("后端") ||
               title.contains("前端") ||
               title.contains("全栈") ||
               title.contains("算法") ||
               title.contains("人工智能") ||
               title.contains("云计算") ||
               title.contains("区块链") ||
               title.contains("移动端") ||
               title.contains("架构师");
        
        return isFinancial || isEngineer;
    }
    
    private String generateJobId(String title, String company) {
        return (company + "-" + (title != null ? title : "job")).toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-|-$", "");
    }
    
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}