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
                // 模拟浏览器，避免部分租户对爬虫 UA 403
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, desktopUserAgent())
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
                .defaultHeader(HttpHeaders.REFERER, buildReferer(this.baseUrl, this.initialSite))
                .defaultHeader("Origin", this.origin)
                .defaultHeader("X-Workday-Client", "Workday Web Client") // ✅ 正确头
                .defaultHeader("X-Requested-With", "XMLHttpRequest")     // 避免部分租户反爬
                .filter(sessionCookies())
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

    /** 仅在 facet key 合法且 value 非空时才下发；不发 utm_source/空数组。 */
    private Map<String, Object> buildRequestPayload(int limit, int offset, Map<String, List<String>> customFacets) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("limit", limit);
        payload.put("offset", offset);
        payload.put("searchText", "");

        if (customFacets != null && !customFacets.isEmpty()) {
            Map<String, List<String>> applied = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> e : customFacets.entrySet()) {
                String k = e.getKey();
                List<String> v = e.getValue();
                if (ALLOWED_FACETS.contains(k) && v != null && !v.isEmpty()) {
                    applied.put(k, v);
                }
            }
            if (!applied.isEmpty()) {
                payload.put("appliedFacets", applied);
            }
        }

        // 通用排序；若触发 422，会在降级重试中移除
        payload.put("sortOrder", "MOST_RECENT");
        return payload;
    }

    private String desktopUserAgent() {
        return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/122.0.0.0 Safari/537.36";
    }

    /* ========================= 执行请求（含 422/404 处理 & GET 回退） ========================= */

    private Map<String, Object> executeRequest(int limit, int offset, Map<String, Object> payload) {
        String site = activeSite.get();
        String path = buildPath(site);

        try {
            return postJson(path, payload);
        } catch (WebClientResponseException ex) {
            int status = ex.getStatusCode() != null ? ex.getStatusCode().value() : -1;

            // 422：用最小 payload 再试，不带 facets/sortOrder
            if (status == 422) {
                log.warn("Workday 422 for {}/{} -> retry with minimal payload", tenant, site);
                Map<String, Object> minimal = minimalPayload(limit, offset);
                try {
                    return postJson(path, minimal);
                } catch (WebClientResponseException ex2) {
                    log.warn("POST minimal still failed ({}), fallback GET. body={}",
                            ex2.getStatusCode(), safeBody(ex2));
                }
            }

            // 404：site 可能写错；自动尝试常见 site 并切换
            if (status == 404) {
                String found = tryDiscoverSite(limit, offset);
                if (found != null) {
                    activeSite.set(found);
                    String newPath = buildPath(found);
                    log.warn("Workday site '{}' invalid, auto-switched to '{}'", site, found);
                    // 切换后用原 payload 再发一次
                    try {
                        return postJson(newPath, payload);
                    } catch (WebClientResponseException ex3) {
                        log.warn("POST after site switch failed ({}), body={}", ex3.getStatusCode(), safeBody(ex3));
                    }
                }
            }

            if (shouldFallback(ex.getStatusCode())) {
                log.debug("POST failed ({}), fallback GET for {}", ex.getStatusCode(), path);
                return getJson(path, limit, offset);
            }

            log.error("Workday request failed {} body={}", ex.getStatusCode(), safeBody(ex));
            throw ex;
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

    private Map<String, Object> getJson(String path, int limit, int offset) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
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
        minimal.put("searchText", "");
        // 不带 facets/sortOrder，最大兼容
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

    /** 预热：GET 一次拿 cookie/CSRF；使用当前 activeSite */
    private void ensureSession() {
        if (initialized.compareAndSet(false, true)) {
            String site = activeSite.get();
            warmUpLandingPage(site);
            warmUpApiSession(site);
        }
    }

    private void warmUpLandingPage(String site) {
        String landing = landingPath(site);
        try {
            client.get()
                    .uri(landing)
                    .accept(MediaType.TEXT_HTML)
                    .retrieve()
                    .toBodilessEntity()
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.debug("Landing warm-up ignored: {} body={}", ex.getStatusCode(), safeBody(ex));
                        return Mono.empty();
                    })
                    .onErrorResume(t -> {
                        log.debug("Landing warm-up ignored: {}", t.toString());
                        return Mono.empty();
                    })
                    .block();
        } catch (Exception e) {
            log.debug("Landing warm-up threw but continues: {}", e.toString());
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
                        log.debug("Warm-up GET ignored: {} body={}", ex.getStatusCode(), safeBody(ex));
                        return Mono.empty();
                    })
                    .onErrorResume(t -> {
                        log.debug("Warm-up GET ignored: {}", t.toString());
                        return Mono.empty();
                    })
                    .block();
        } catch (Exception e) {
            log.debug("Warm-up GET threw but continues: {}", e.toString());
        }
    }

    private ExchangeFilterFunction sessionCookies() {
        return (request, next) -> {
            ClientRequest.Builder builder = ClientRequest.from(request);
            cookieStore.forEach(builder::cookie);
            String token = csrfToken.get();
            if (token != null && !token.isBlank()) {
                builder.header("x-calypso-csrf-token", token);
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
                if ("CALYPSO_CSRF_TOKEN".equalsIgnoreCase(name)) {
                    csrfToken.set(value);
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
