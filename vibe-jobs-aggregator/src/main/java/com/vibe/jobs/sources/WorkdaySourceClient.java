package com.vibe.jobs.sources;

import com.vibe.jobs.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fetch jobs from Workday "cxs" endpoints:
 * {baseUrl}/wday/cxs/{tenant}/{site}/jobs
 */
public class WorkdaySourceClient implements SourceClient {

    private static final Logger log = LoggerFactory.getLogger(WorkdaySourceClient.class);

    /** 常见的 site 取值（大小写敏感，顺序会影响探测优先级） */
    private static final List<String> COMMON_SITES = List.of(
            "External", "external", "Careers", "careers", "Career", "career", "Jobs", "jobs"
    );

    /** 允许的 facet key（实际仍应以租户返回为准，这里做最小白名单防止 422） */
    private static final Set<String> ALLOWED_FACETS = Set.of(
            "locations", "locationsCountry",
            "timeType", "workerSubType",
            "jobFamily", "jobFamilyGroup", "jobProfile",
            "postingChannel"
    );

    private final String company;
    private final String baseUrl;       // e.g. https://micron.wd1.myworkdayjobs.com
    private final String origin;        // e.g. https://micron.wd1.myworkdayjobs.com (no path component)
    private final String tenant;        // e.g. micron
    private final String initialSite;   // e.g. External / careers / ...
    private final AtomicReference<String> activeSite;

    private final WebClient client;
    private final AtomicReference<String> lastAppliedReferer = new AtomicReference<>();

    private final Map<String, String> cookieStore = new ConcurrentHashMap<>();
    private final AtomicReference<String> csrfToken = new AtomicReference<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public WorkdaySourceClient(String company, String baseUrl, String tenant, String site) {
        this.company = company;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.origin = deriveOrigin(this.baseUrl);
        this.tenant = tenant;
        this.initialSite = site == null ? "" : site.trim();
        this.activeSite = new AtomicReference<>(this.initialSite);

        this.client = WebClient.builder()
                .baseUrl(this.baseUrl)
                // 2024年Workday要求更精确的浏览器模拟
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9") // 改回英文避免地区检测
                .defaultHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
                .defaultHeader(HttpHeaders.CONNECTION, "keep-alive")
                .defaultHeader(HttpHeaders.USER_AGENT, modernBrowserUserAgent()) // 使用最新UA
                .defaultHeader("Sec-Ch-Ua", "\"Google Chrome\";v=\"129\", \"Not=A?Brand\";v=\"8\", \"Chromium\";v=\"129\"")
                .defaultHeader("Sec-Ch-Ua-Mobile", "?0")
                .defaultHeader("Sec-Ch-Ua-Platform", "\"Windows\"") // Windows更通用
                .defaultHeader("Sec-Fetch-Dest", "empty")
                .defaultHeader("Sec-Fetch-Mode", "cors")
                .defaultHeader("Sec-Fetch-Site", "same-origin")
                .defaultHeader("X-Requested-With", "XMLHttpRequest")
                // 关键：添加Workday 2024年需要的新头部
                .defaultHeader("X-Workday-Client", "wd-prod")
                .defaultHeader("Content-Type", "application/json;charset=UTF-8")
                .filter(sessionCookies())
                .filter(addDynamicHeaders())
                .build();

        seedBrowserCookies();
    }

    @Override
    public String sourceName() {
        return "workday:" + tenant;
    }

    @Override
    public List<FetchedJob> fetchPage(int page, int size) {
        return fetchPage(page, size, null);
    }

    /** 支持自定义 facets 的分页抓取 */
    public List<FetchedJob> fetchPage(int page, int size, Map<String, List<String>> customFacets) {
        int limit = Math.max(1, Math.min(size, 100));
        int offset = Math.max(page - 1, 0) * limit;

        ensureSession();

        Map<String, Object> payload = buildRequestPayload(limit, offset, customFacets);
        Map<String, Object> response = executeRequest(limit, offset, payload);

        if (response == null || !response.containsKey("jobPostings")) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jobPostings = (List<Map<String, Object>>) response.get("jobPostings");
        if (jobPostings == null || jobPostings.isEmpty()) {
            return List.of();
        }

        return jobPostings.stream().map(this::mapJob).toList();
    }

    /* ========================= 映射 ========================= */

    private FetchedJob mapJob(Map<String, Object> posting) {
        String jobId = stringValue(posting.get("jobPostingId"));
        String title = stringValue(posting.get("title"));

        String location = stringValue(posting.get("locationsText"));
        if (location.isEmpty()) {
            Object locations = posting.get("locations");
            if (locations instanceof List<?> locationList) {
                location = locationList.stream()
                        .map(this::stringValue)
                        .filter(v -> !v.isEmpty())
                        .findFirst()
                        .orElse("");
            }
        }

        String externalPath = stringValue(posting.get("externalPath"));
        String url = externalPath.isEmpty() ? baseUrl : baseUrl + externalPath;

        Instant postedAt = Instant.now();
        postedAt = parseInstant(posting.get("postedOn"), postedAt);
        postedAt = parseInstant(posting.get("postedOnTime"), postedAt);

        Set<String> tags = new HashSet<>();
        addTags(tags, posting.get("jobFamilies"));
        addTags(tags, posting.get("jobFamilyGroup"));
        addTags(tags, posting.get("jobFamily"));

        Job job = Job.builder()
                .source(sourceName())
                .externalId(jobId.isEmpty() ? title : jobId)
                .title(title)
                .company(company)
                .location(location)
                .url(url)
                .postedAt(postedAt)
                .tags(tags)
                .build();

        String content = extractContent(posting);
        return new FetchedJob(job, content);
    }

    private Instant parseInstant(Object value, Instant fallback) {
        if (value instanceof String text) {
            String t = text.trim();
            if (!t.isEmpty()) {
                try { return Instant.parse(t); }
                catch (DateTimeParseException ignored) { /* continue */ }
            }
        }
        if (value instanceof Number n) {
            return Instant.ofEpochMilli(n.longValue());
        }
        return fallback;
    }

    private void addTags(Set<String> tags, Object candidate) {
        if (candidate instanceof List<?> list) {
            list.forEach(it -> addTags(tags, it));
            return;
        }
        if (candidate instanceof Map<?, ?> map) {
            Object label = map.get("label");
            if (label != null) addTags(tags, label);
            Object value = map.get("value");
            if (value != null) addTags(tags, value);
            return;
        }
        String v = stringValue(candidate);
        if (!v.isEmpty()) tags.add(v);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String extractContent(Map<String, Object> posting) {
        String content = stringValue(posting.get("jobPostingDescription"));
        if (!content.isEmpty()) return content;

        content = stringValue(posting.get("description"));
        if (!content.isEmpty()) return content;

        content = stringValue(posting.get("jobDescription"));
        if (!content.isEmpty()) return content;

        Object mapping = posting.get("jobPostingText");
        if (mapping instanceof Map<?, ?> map) {
            Object longText = map.get("longDescription");
            content = stringValue(longText);
            if (!content.isEmpty()) return content;
        }
        return content;
    }

    /* ========================= 请求构造 ========================= */

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String buildReferer(String baseUrl, String site) {
        if (baseUrl == null || baseUrl.isBlank()) return "";
        String normalized = trimTrailingSlash(baseUrl);
        String s = site == null ? "" : site.trim();
        if (s.isEmpty()) {
            return normalized;
        }
        if (normalized.endsWith("/" + s)) {
            return normalized;
        }
        return normalized + "/" + s;
    }

    private String deriveOrigin(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            java.net.URI uri = java.net.URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return trimTrailingSlash(url);
            }
            int port = uri.getPort();
            if (port == -1) {
                return scheme + "://" + host;
            }
            return scheme + "://" + host + ":" + port;
        } catch (IllegalArgumentException ex) {
            return trimTrailingSlash(url);
        }
    }

    /** 基于2024年Workday API最新格式构建请求payload */
    private Map<String, Object> buildRequestPayload(int limit, int offset, Map<String, List<String>> customFacets) {
        Map<String, Object> payload = new LinkedHashMap<>();
        
        // 2024年新的必需字段结构
        payload.put("limit", limit);
        payload.put("offset", offset);
        
        // 搜索配置 - 新的2024格式
        Map<String, Object> searchOptions = new LinkedHashMap<>();
        searchOptions.put("searchText", "");
        searchOptions.put("locations", Collections.emptyList());
        searchOptions.put("locationHierarchy", Collections.emptyList());
        searchOptions.put("facets", Collections.emptyMap());
        payload.put("searchOptions", searchOptions);
        
        // 应用facets（如果提供）
        if (customFacets != null && !customFacets.isEmpty()) {
            Map<String, Object> appliedFacets = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : customFacets.entrySet()) {
                if (ALLOWED_FACETS.contains(entry.getKey()) && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    appliedFacets.put(entry.getKey(), entry.getValue());
                }
            }
            if (!appliedFacets.isEmpty()) {
                searchOptions.put("facets", appliedFacets);
            }
        }
        
        // 2024年新增的排序和过滤配置
        Map<String, Object> sortConfig = new LinkedHashMap<>();
        sortConfig.put("sortOrder", List.of(Map.of("field", "postedOn", "sortDirection", "DESC")));
        payload.put("sortOrder", sortConfig.get("sortOrder"));
        
        // 添加客户端信息 - 2024年反爬虫要求
        Map<String, Object> clientInfo = new LinkedHashMap<>();
        clientInfo.put("source", "WEB");
        clientInfo.put("version", "2024.44.0"); // 当前Workday版本
        clientInfo.put("locale", "en_US"); // 固定英文避免地区检测
        payload.put("clientApplicationInfo", clientInfo);
        
        return payload;
    }

    private String desktopUserAgent() {
        return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/128.0.0.0 Safari/537.36";
    }
    
    // 2024年最新的现代浏览器UA
    private String modernBrowserUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/129.0.0.0 Safari/537.36";
    }
    
    // 添加动态请求头过滤器
    private ExchangeFilterFunction addDynamicHeaders() {
        return (request, next) -> {
            ClientRequest.Builder builder = ClientRequest.from(request);
            
            // 添加时间戳相关头部 - Workday 2024反爬虫检测
            long timestamp = System.currentTimeMillis();
            builder.header("X-Request-Timestamp", String.valueOf(timestamp));
            
            // 添加随机化的窗口尺寸 - 模拟真实浏览器
            int screenWidth = 1920 + (int)(Math.random() * 100);
            int screenHeight = 1080 + (int)(Math.random() * 100);
            builder.header("X-Screen-Resolution", screenWidth + "x" + screenHeight);
            
            // 根据请求方法添加不同的头部
            if ("POST".equals(request.method().name())) {
                builder.header("Origin", origin);
                builder.header("DNT", "1"); // Do Not Track
            }
            
            return next.exchange(builder.build());
        };
    }

    /* ========================= 执行请求（含 422/404 处理 & GET 回退） ========================= */

    private Map<String, Object> executeRequest(int limit, int offset, Map<String, Object> payload) {
        String site = activeSite.get();
        String path = buildPath(site);
        String fallbackPath = path;

        try {
            return postJson(path, payload);
        } catch (WebClientResponseException ex) {
            int status = ex.getStatusCode() != null ? ex.getStatusCode().value() : -1;
            WebClientResponseException failure = ex;
            boolean minimalRetried = false;

            // 422：用最小 payload 再试，不带 facets/sortOrder
            if (status == 422) {
                minimalRetried = true;
                log.warn("Workday 422 for {}/{} -> retry with minimal payload", tenant, site);
                Map<String, Object> minimal = minimalPayload(limit, offset);
                try {
                    return postJson(path, minimal);
                } catch (WebClientResponseException ex2) {
                    failure = ex2;
                    status = ex2.getStatusCode() != null ? ex2.getStatusCode().value() : -1;
                    log.warn("POST minimal still failed ({}), will attempt alternate recovery. body={}",
                            ex2.getStatusCode(), safeBody(ex2));
                }
            }

            // 404：site 可能写错；或者在 400/422（minimal 重试后）时也尝试探测常见 site 并切换
            if (status == 404 || (minimalRetried && (status == 400 || status == 422))) {
                String found = tryDiscoverSite(limit, offset);
                if (found != null) {
                    activeSite.set(found);
                    String newPath = buildPath(found);
                    fallbackPath = newPath;
                    log.warn("Workday site '{}' failed with {} -> auto-switched to '{}'", site, status, found);
                    // 切换后用原 payload 再发一次
                    try {
                        return postJson(newPath, payload);
                    } catch (WebClientResponseException ex3) {
                        failure = ex3;
                        status = ex3.getStatusCode() != null ? ex3.getStatusCode().value() : -1;
                        log.warn("POST after site switch failed ({}), body={}", ex3.getStatusCode(), safeBody(ex3));
                    }
                }
            }

            if (shouldFallback(failure.getStatusCode())) {
                log.info("POST failed ({}), fallback GET for {}", failure.getStatusCode(), fallbackPath);
                return getJson(fallbackPath, limit, offset);
            }

            log.error("Workday request failed {} body={}", failure.getStatusCode(), safeBody(failure));
            throw failure;
        }
    }

    private String buildPath(String site) {
        String s = (site == null || site.isBlank()) ? "" : site.trim();
        if (s.isEmpty()) {
            return "/wday/cxs/" + tenant + "/jobs";
        }
        return "/wday/cxs/" + tenant + "/" + s + "/jobs";
    }

    private Map<String, Object> postJson(String path, Map<String, Object> body) {
        return client.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    /** 2024年增强的GET方法 - 当POST被拒绝时的备选方案 */
    private Map<String, Object> getJson(String path, int limit, int offset) {
        try {
            // 使用GET方式访问，模拟浏览器直接访问
            return client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("limit", limit)
                            .queryParam("offset", offset)
                            .queryParam("searchText", "")
                            .queryParam("appliedFacets", "{}")
                            .queryParam("sortOrder", "postedOn:desc")
                            // 2024年新增参数
                            .queryParam("source", "WEB")
                            .queryParam("version", "2024.44.0")
                            .queryParam("locale", "en_US")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Cache-Control", "max-age=0") // 强制刷新
                    .header("Pragma", "no-cache")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .doOnError(ex -> log.warn("GET fallback also failed for {}: {}", path, ex.getMessage()))
                    .onErrorReturn(Collections.emptyMap())
                    .block();
        } catch (Exception e) {
            log.warn("GET fallback failed for {}: {}", path, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** 探测常见 site，返回可用的一个（最小 payload），失败则返回 null */
    private String tryDiscoverSite(int limit, int offset) {
        for (String candidate : COMMON_SITES) {
            try {
                Map<String, Object> resp = postJson(buildPath(candidate), minimalPayload(limit, offset));
                if (resp != null && resp.containsKey("jobPostings")) {
                    return candidate;
                }
            } catch (WebClientResponseException ignore) {
                // 继续尝试下一个
            }
        }
        return null;
    }

    private Map<String, Object> minimalPayload(int limit, int offset) {
        Map<String, Object> minimal = new LinkedHashMap<>();
        minimal.put("limit", limit);
        minimal.put("offset", offset);
        
        // 2024年最小化payload结构
        Map<String, Object> searchOptions = new LinkedHashMap<>();
        searchOptions.put("searchText", "");
        searchOptions.put("locations", Collections.emptyList());
        searchOptions.put("facets", Collections.emptyMap());
        minimal.put("searchOptions", searchOptions);
        
        // 必需的客户端信息
        Map<String, Object> clientInfo = new LinkedHashMap<>();
        clientInfo.put("source", "WEB");
        clientInfo.put("version", "2024.44.0");
        clientInfo.put("locale", "en_US");
        minimal.put("clientApplicationInfo", clientInfo);
        
        return minimal;
    }

    private String safeBody(WebClientResponseException ex) {
        try { return ex.getResponseBodyAsString(); }
        catch (Exception ignored) { return "<no-body>"; }
    }

    private boolean shouldFallback(HttpStatusCode statusCode) {
        if (statusCode == null) return false;
        int s = statusCode.value();
        return s == 405 || s == 409 || s == 422 || s == 500 || s == 503;
    }

    /* ========================= 会话/CSRF ========================= */

    /** 2024年增强的会话初始化 - 必须先获取正确的会话状态 */
    private void ensureSession() {
        if (initialized.compareAndSet(false, true)) {
            String site = activeSite.get();
            
            // Step 1: 访问主页建立基础会话
            warmUpMainPage();
            
            // Step 2: 访问careers页面获取应用状态
            warmUpLandingPage(site);
            
            // Step 3: 预热API端点获取CSRF令牌
            warmUpApiSession(site);
            
            // Step 4: 验证会话有效性
            validateSession(site);
        }
    }
    
    /** 新增：访问Workday主页建立基础会话 */
    private void warmUpMainPage() {
        try {
            String mainPageUrl = baseUrl.replace("/wday/cxs", "");
            client.get()
                    .uri(mainPageUrl)
                    .accept(MediaType.TEXT_HTML)
                    .retrieve()
                    .toBodilessEntity()
                    .onErrorResume(ex -> {
                        log.info("Main page warm-up ignored: {}", ex.getMessage());
                        return Mono.empty();
                    })
                    .block();
            
            // 短暂延迟模拟真实用户行为
            Thread.sleep(500 + (long)(Math.random() * 1000));
            
        } catch (Exception e) {
            log.info("Main page warm-up failed but continues: {}", e.toString());
        }
    }
    
    /** 新增：验证会话有效性 */
    private void validateSession(String site) {
        try {
            // 发送一个简单的OPTIONS请求验证CORS配置
            String path = buildPath(site);
            client.options()
                    .uri(path)
                    .retrieve()
                    .toBodilessEntity()
                    .onErrorResume(ex -> {
                        log.info("Session validation ignored: {}", ex.getMessage());
                        return Mono.empty();
                    })
                    .block();
                    
        } catch (Exception e) {
            log.info("Session validation completed: {}", e.getMessage());
        }
    }

    private void warmUpLandingPage(String site) {
        String landing = landingPath(site);
        try {
            String response = client.get()
                    .uri(landing)
                    .accept(MediaType.TEXT_HTML)
                    .header("Upgrade-Insecure-Requests", "1") // 2024年新增安全头
                    .header("Sec-Purpose", "prefetch") // 预取标识
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.info("Landing warm-up ignored: {} body={}", ex.getStatusCode(), safeBody(ex));
                        return Mono.just("");
                    })
                    .onErrorResume(t -> {
                        log.info("Landing warm-up ignored: {}", t.toString());
                        return Mono.just("");
                    })
                    .block();
                    
            // 增强的CSRF令牌提取 - 2024年模式
            if (response != null && !response.isEmpty()) {
                extractEnhancedCsrfFromHtml(response);
            }
            
            // 模拟用户停留时间
            Thread.sleep(1000 + (long)(Math.random() * 2000));
            
        } catch (Exception e) {
            log.info("Landing warm-up threw but continues: {}", e.toString());
        }
    }
    
    /** 2024年增强的CSRF令牌提取 - 支持多种新格式 */
    private void extractEnhancedCsrfFromHtml(String html) {
        // 2024年新的CSRF令牌模式
        String[] patterns = {
            "\"csrfToken\"\\s*:\\s*\"([^\"]+)\"",
            "csrf[\"']?\\s*[:=]\\s*[\"']([^\"']+)[\"']",
            "CSRF_TOKEN[\"']?\\s*[:=]\\s*[\"']([^\"']+)[\"']",
            "_token[\"']?\\s*[:=]\\s*[\"']([^\"']+)[\"']",
            "authenticity_token[\"']?\\s*[:=]\\s*[\"']([^\"']+)[\"']",
            "data-csrf-token[\"']?\\s*[:=]\\s*[\"']([^\"']+)[\"']",
            // 2024年新增的Workday特定模式
            "wd-csrf[\"']?\\s*[:=]\\s*[\"']([^\"']+)[\"']",
            "workday-token[\"']?\\s*[:=]\\s*[\"']([^\"']+)[\"']"
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(html);
            if (m.find()) {
                String token = m.group(1);
                if (token != null && token.length() > 10) { // 最小长度验证
                    csrfToken.set(token);
                    log.info("Extracted enhanced CSRF token ({}chars): {}...", token.length(), token.substring(0, Math.min(8, token.length())));
                    break;
                }
            }
        }
        
        // 备用：查找隐藏的input字段
        if (csrfToken.get() == null) {
            java.util.regex.Pattern inputPattern = java.util.regex.Pattern.compile(
                "<input[^>]+name=[\"']([^\"']*(?:csrf|token)[^\"']*)[\"'][^>]+value=[\"']([^\"']+)[\"'][^>]*>",
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher m = inputPattern.matcher(html);
            if (m.find()) {
                String token = m.group(2);
                if (token != null && token.length() > 10) {
                    csrfToken.set(token);
                    log.info("Extracted CSRF from input field: {}...", token.substring(0, Math.min(8, token.length())));
                }
            }
        }
    }

    private void warmUpApiSession(String site) {
        String path = buildPath(site);
        try {
            client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("limit", 1)
                            .queryParam("offset", 0)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity()
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.info("Warm-up GET ignored: {} body={}", ex.getStatusCode(), safeBody(ex));
                        return Mono.empty();
                    })
                    .onErrorResume(t -> {
                        log.info("Warm-up GET ignored: {}", t.toString());
                        return Mono.empty();
                    })
                    .block();
        } catch (Exception e) {
            log.info("Warm-up GET threw but continues: {}", e.toString());
        }
    }

    private ExchangeFilterFunction sessionCookies() {
        return (request, next) -> {
            ClientRequest.Builder builder = ClientRequest.from(request);
            cookieStore.forEach(builder::cookie);
            
            String token = csrfToken.get();
            if (token != null && !token.isBlank()) {
                // 尝试多种CSRF头字段名称
                builder.header("x-calypso-csrf-token", token);
                builder.header("X-CSRF-Token", token);
                builder.header("csrf-token", token);
            }
            
            String site = activeSite.get();
            String referer = buildReferer(baseUrl, site);
            builder.headers(headers -> {
                headers.set(HttpHeaders.REFERER, referer);
                if (!origin.isBlank()) {
                    headers.set("Origin", origin);
                }
                // 添加额外的安全头
                headers.set("Sec-Ch-Ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"");
                headers.set("Sec-Ch-Ua-Mobile", "?0");
                headers.set("Sec-Ch-Ua-Platform", "\"macOS\"");
            });
            
            String previous = lastAppliedReferer.getAndSet(referer);
            if (!Objects.equals(previous, referer) && log.isInfoEnabled()) {
                log.info("Updated Workday referer header to '{}' for site '{}'", referer, site);
            }
            return next.exchange(builder.build())
                    .flatMap(response -> captureCookies(response).thenReturn(response));
        };
    }

    private Mono<Void> captureCookies(ClientResponse response) {
        response.cookies().forEach((name, values) -> {
            if (!values.isEmpty()) {
                String value = values.get(0).getValue();
                cookieStore.put(name, value);
                
                // 2024年Workday新的CSRF令牌cookie模式
                String lowerName = name.toLowerCase();
                if (lowerName.contains("csrf") || lowerName.contains("token") || 
                    "CALYPSO_CSRF_TOKEN".equalsIgnoreCase(name) ||
                    "XSRF-TOKEN".equalsIgnoreCase(name) ||
                    "X-CSRF-TOKEN".equalsIgnoreCase(name) ||
                    // 2024年新增的Workday特定cookie
                    "WD_CSRF_TOKEN".equalsIgnoreCase(name) ||
                    "WORKDAY_CSRF".equalsIgnoreCase(name) ||
                    "wd-session-token".equalsIgnoreCase(name) ||
                    "wday_vps_cookie".equalsIgnoreCase(name)) {
                    
                    // 验证token有效性（长度、格式等）
                    if (value != null && value.length() >= 10 && !value.equals("deleted")) {
                        csrfToken.set(value);
                        log.info("Captured CSRF token from cookie '{}' ({}chars): {}...", 
                                name, value.length(), value.substring(0, Math.min(8, value.length())));
                    }
                }
                
                // 记录重要的会话cookie
                if (lowerName.contains("session") || lowerName.contains("jsessionid") || 
                    lowerName.contains("wday") || lowerName.startsWith("wd_")) {
                    log.info("Captured session cookie: {} = {}...", name, 
                            value.substring(0, Math.min(10, value.length())));
                }
            }
        });
        return Mono.empty();
    }

    private void seedBrowserCookies() {
        cookieStore.putIfAbsent("wd-browser-id", UUID.randomUUID().toString());
        cookieStore.putIfAbsent("timezoneOffset", computeTimezoneOffsetCookie());
    }

    private String computeTimezoneOffsetCookie() {
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            int minutes = now.getOffset().getTotalSeconds() / 60;
            return Integer.toString(minutes);
        } catch (Exception e) {
            return "0";
        }
    }

    private String landingPath(String site) {
        String s = site == null ? "" : site.trim();
        if (s.isEmpty()) {
            return "/";
        }
        if (s.startsWith("/")) {
            return s;
        }
        return "/" + s;
    }
}
