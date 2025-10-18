package com.vibe.jobs.ingestion.infrastructure.sourceclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ATS系统自动检测器
 * 根据careers URL自动识别使用的ATS类型
 */
public class AtsDetector {
    
    private static final Logger log = LoggerFactory.getLogger(AtsDetector.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    
    private final HttpClient httpClient;
    
    // ATS检测模式
    private static final Map<String, Pattern[]> ATS_PATTERNS;
    
    static {
        Map<String, Pattern[]> patterns = new java.util.HashMap<>();
        patterns.put("workday", new Pattern[]{
            Pattern.compile("myworkdayjobs\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("workday", Pattern.CASE_INSENSITIVE),
            Pattern.compile("/wday/", Pattern.CASE_INSENSITIVE)
        });
        patterns.put("greenhouse", new Pattern[]{
            Pattern.compile("greenhouse\\.io", Pattern.CASE_INSENSITIVE),
            Pattern.compile("boards\\.greenhouse", Pattern.CASE_INSENSITIVE),
            Pattern.compile("greenhouse", Pattern.CASE_INSENSITIVE)
        });
        patterns.put("lever", new Pattern[]{
            Pattern.compile("lever\\.co", Pattern.CASE_INSENSITIVE),
            Pattern.compile("jobs\\.lever", Pattern.CASE_INSENSITIVE),
            Pattern.compile("lever", Pattern.CASE_INSENSITIVE)
        });
        patterns.put("ashby", new Pattern[]{
            Pattern.compile("ashbyhq\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("jobs\\.ashbyhq", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ashby", Pattern.CASE_INSENSITIVE)
        });
        patterns.put("moka", new Pattern[]{
            Pattern.compile("mokahr\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("moka", Pattern.CASE_INSENSITIVE),
            Pattern.compile("智聘", Pattern.CASE_INSENSITIVE),
            Pattern.compile("MOKA", Pattern.CASE_INSENSITIVE)
        });
        patterns.put("beisen", new Pattern[]{
            Pattern.compile("beisen\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("北森", Pattern.CASE_INSENSITIVE),
            Pattern.compile("beisen", Pattern.CASE_INSENSITIVE),
            Pattern.compile("BeiSen", Pattern.CASE_INSENSITIVE)
        });
        patterns.put("successfactors", new Pattern[]{
            Pattern.compile("successfactors", Pattern.CASE_INSENSITIVE),
            Pattern.compile("sap\\.com.*careers", Pattern.CASE_INSENSITIVE),
            Pattern.compile("sfcareer", Pattern.CASE_INSENSITIVE)
        });
        patterns.put("taleo", new Pattern[]{
            Pattern.compile("taleo", Pattern.CASE_INSENSITIVE),
            Pattern.compile("oracle.*careers", Pattern.CASE_INSENSITIVE),
            Pattern.compile("careersection", Pattern.CASE_INSENSITIVE)
        });
        patterns.put("icims", new Pattern[]{
            Pattern.compile("icims", Pattern.CASE_INSENSITIVE),
            Pattern.compile("iCIMS", Pattern.CASE_INSENSITIVE),
            Pattern.compile("jobs\\.icims", Pattern.CASE_INSENSITIVE)
        });
        patterns.put("smartrecruiters", new Pattern[]{
            Pattern.compile("smartrecruiters", Pattern.CASE_INSENSITIVE),
            Pattern.compile("jobs\\.smartrecruiters", Pattern.CASE_INSENSITIVE),
            Pattern.compile("SmartRecruiters", Pattern.CASE_INSENSITIVE)
        });
        patterns.put("avature", new Pattern[]{
            Pattern.compile("avature", Pattern.CASE_INSENSITIVE),
            Pattern.compile("careers\\.avature", Pattern.CASE_INSENSITIVE)
        });
        ATS_PATTERNS = Map.copyOf(patterns);
    }
    
    public AtsDetector() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }
    
    /**
     * 检测careers URL使用的ATS类型
     * @param careersUrl 公司careers页面URL
     * @return 检测到的ATS类型，未检测到则返回"generic"
     */
    public AtsDetectionResult detectAts(String careersUrl) {
        if (careersUrl == null || careersUrl.isEmpty()) {
            return new AtsDetectionResult("generic", careersUrl, null);
        }
        
        log.info("Detecting ATS type for URL: {}", careersUrl);
        
        // 1. 首先通过URL模式检测
        String atsType = detectByUrl(careersUrl);
        if (!"generic".equals(atsType)) {
            return new AtsDetectionResult(atsType, careersUrl, extractApiInfo(atsType, careersUrl));
        }
        
        // 2. 通过HTTP响应检测
        try {
            String htmlContent = fetchHtmlContent(careersUrl);
            if (htmlContent != null) {
                atsType = detectByContent(htmlContent);
                if (!"generic".equals(atsType)) {
                    return new AtsDetectionResult(atsType, careersUrl, extractApiInfo(atsType, careersUrl));
                }
            }
        } catch (Exception e) {
            log.info("Failed to fetch content for ATS detection: {}", e.getMessage());
        }
        
        return new AtsDetectionResult("generic", careersUrl, null);
    }
    
    private String detectByUrl(String url) {
        for (Map.Entry<String, Pattern[]> entry : ATS_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(url).find()) {
                    log.info("Detected ATS '{}' by URL pattern: {}", entry.getKey(), pattern.pattern());
                    return entry.getKey();
                }
            }
        }
        return "generic";
    }
    
    private String fetchHtmlContent(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        }
        
        return null;
    }
    
    private String detectByContent(String htmlContent) {
        for (Map.Entry<String, Pattern[]> entry : ATS_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(htmlContent).find()) {
                    log.info("Detected ATS '{}' by content pattern: {}", entry.getKey(), pattern.pattern());
                    return entry.getKey();
                }
            }
        }
        
        // 额外的内容特征检测
        if (htmlContent.contains("greenhouse") || htmlContent.contains("boards.greenhouse")) {
            return "greenhouse";
        }
        if (htmlContent.contains("lever.co") || htmlContent.contains("jobs.lever")) {
            return "lever";
        }
        if (htmlContent.contains("workday") || htmlContent.contains("myworkdayjobs")) {
            return "workday";
        }
        if (htmlContent.contains("moka") || htmlContent.contains("智聘") || htmlContent.contains("mokahr")) {
            return "moka";
        }
        if (htmlContent.contains("beisen") || htmlContent.contains("北森")) {
            return "beisen";
        }
        
        return "generic";
    }
    
    private AtsApiInfo extractApiInfo(String atsType, String careersUrl) {
        return switch (atsType) {
            case "workday" -> extractWorkdayInfo(careersUrl);
            case "greenhouse" -> extractGreenhouseInfo(careersUrl);
            case "lever" -> extractLeverInfo(careersUrl);
            case "moka" -> extractMokaInfo(careersUrl);
            case "beisen" -> extractBeisenInfo(careersUrl);
            default -> new AtsApiInfo(null, null, Map.of());
        };
    }
    
    private AtsApiInfo extractWorkdayInfo(String url) {
        // 提取Workday的tenant和site信息
        // URL格式: https://company.wd1.myworkdayjobs.com/site
        if (url.contains("myworkdayjobs.com")) {
            String[] parts = url.split("[./]");
            for (int i = 0; i < parts.length; i++) {
                if ("myworkdayjobs".equals(parts[i]) && i > 0) {
                    String tenant = parts[i-1];
                    String site = (i + 2 < parts.length) ? parts[i + 2] : "External";
                    String baseUrl = "https://" + tenant + ".wd1.myworkdayjobs.com";
                    
                    return new AtsApiInfo(
                        baseUrl + "/wday/cxs/" + tenant + "/" + site + "/jobs",
                        "POST",
                        Map.of("tenant", tenant, "site", site, "baseUrl", baseUrl)
                    );
                }
            }
        }
        return new AtsApiInfo(null, null, Map.of());
    }
    
    private AtsApiInfo extractGreenhouseInfo(String url) {
        // 提取Greenhouse的组织信息
        if (url.contains("greenhouse.io")) {
            String org = url.replaceAll(".*boards\\.greenhouse\\.io/([^/?]+).*", "$1");
            return new AtsApiInfo(
                "https://boards.greenhouse.io/" + org + ".json",
                "GET",
                Map.of("org", org)
            );
        }
        return new AtsApiInfo(null, null, Map.of());
    }
    
    private AtsApiInfo extractLeverInfo(String url) {
        // 提取Lever的组织信息
        if (url.contains("lever.co") || url.contains("jobs.lever")) {
            String org = url.replaceAll(".*jobs\\.lever\\.co/([^/?]+).*", "$1");
            return new AtsApiInfo(
                "https://api.lever.co/v0/postings/" + org + "?mode=json",
                "GET",
                Map.of("org", org)
            );
        }
        return new AtsApiInfo(null, null, Map.of());
    }
    
    private AtsApiInfo extractMokaInfo(String url) {
        return new AtsApiInfo(
            url + "/api/jobs/search",
            "POST",
            Map.of("baseUrl", url)
        );
    }
    
    private AtsApiInfo extractBeisenInfo(String url) {
        return new AtsApiInfo(
            url + "/sf/job-search",
            "POST",
            Map.of("baseUrl", url)
        );
    }
    
    /**
     * ATS检测结果
     */
    public static class AtsDetectionResult {
        private final String atsType;
        private final String careersUrl;
        private final AtsApiInfo apiInfo;
        
        public AtsDetectionResult(String atsType, String careersUrl, AtsApiInfo apiInfo) {
            this.atsType = atsType;
            this.careersUrl = careersUrl;
            this.apiInfo = apiInfo;
        }
        
        public String getAtsType() { return atsType; }
        public String getCareersUrl() { return careersUrl; }
        public AtsApiInfo getApiInfo() { return apiInfo; }
    }
    
    /**
     * ATS API信息
     */
    public static class AtsApiInfo {
        private final String apiUrl;
        private final String method;
        private final Map<String, String> params;
        
        public AtsApiInfo(String apiUrl, String method, Map<String, String> params) {
            this.apiUrl = apiUrl;
            this.method = method;
            this.params = params;
        }
        
        public String getApiUrl() { return apiUrl; }
        public String getMethod() { return method; }
        public Map<String, String> getParams() { return params; }
    }
}