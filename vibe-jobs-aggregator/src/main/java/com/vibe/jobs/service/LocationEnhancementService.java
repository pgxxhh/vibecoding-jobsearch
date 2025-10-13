package com.vibe.jobs.service;

import com.vibe.jobs.sources.FetchedJob;
import com.vibe.jobs.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

/**
 * Location字段增强服务 - 通用能力
 * 对解析后仍然缺失location信息的job进行智能补全
 */
@Service
public class LocationEnhancementService {

    private static final Logger log = LoggerFactory.getLogger(LocationEnhancementService.class);

    /**
     * 批量增强jobs的location信息
     */
    public List<FetchedJob> enhanceLocationFields(List<FetchedJob> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return jobs;
        }

        List<FetchedJob> enhanced = new ArrayList<>();
        int enhancedCount = 0;

        for (FetchedJob fetchedJob : jobs) {
            Job job = fetchedJob.job();
            String originalLocation = job.getLocation();
            
            // 如果location为空或无效，尝试智能提取
            if (isLocationEmpty(originalLocation)) {
                String enhancedLocation = extractLocationFromJobData(job);
                if (enhancedLocation != null) {
                    Job enhancedJob = Job.builder()
                            .source(job.getSource())
                            .externalId(job.getExternalId())
                            .title(job.getTitle())
                            .company(job.getCompany())
                            .location(enhancedLocation)
                            .level(job.getLevel())
                            .postedAt(job.getPostedAt())
                            .url(job.getUrl())
                            .checksum(job.getChecksum())
                            .tags(job.getTags())
                            .build();
                    
                    enhanced.add(new FetchedJob(enhancedJob, fetchedJob.content()));
                    enhancedCount++;
                    
                    log.info("Enhanced location for job '{}': '{}' -> '{}'", 
                             job.getTitle(), originalLocation, enhancedLocation);
                } else {
                    enhanced.add(fetchedJob);
                }
            } else {
                enhanced.add(fetchedJob);
            }
        }

        if (enhancedCount > 0) {
            log.info("Enhanced location information for {} out of {} jobs", enhancedCount, jobs.size());
        }

        return enhanced;
    }

    /**
     * 从job的各个字段中智能提取location信息
     */
    private String extractLocationFromJobData(Job job) {
        // 策略1: 从job title中提取
        String locationFromTitle = extractLocationFromText(job.getTitle());
        if (locationFromTitle != null) {
            return locationFromTitle;
        }

        // 策略2: 从URL中推断
        String locationFromUrl = extractLocationFromUrl(job.getUrl());
        if (locationFromUrl != null) {
            return locationFromUrl;
        }

        // 策略3: 从company信息推断
        String locationFromCompany = inferLocationFromCompany(job.getCompany(), job.getUrl());
        if (locationFromCompany != null) {
            return locationFromCompany;
        }

        return null;
    }

    /**
     * 从文本中提取location信息
     */
    private String extractLocationFromText(String text) {
        if (text == null || text.length() < 3) {
            return null;
        }

        // 模式1: "Title, Location" 格式
        if (text.contains(",")) {
            String[] parts = text.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (isValidLocationText(trimmed)) {
                    return trimmed;
                }
            }
        }

        // 模式2: 直接包含location关键词
        String[] locationKeywords = {
            "China", "Beijing", "Shanghai", "Shenzhen", "Guangzhou", "Hangzhou", "Chengdu",
            "中国", "北京", "上海", "深圳", "广州", "杭州", "成都",
            "Singapore", "Hong Kong", "Taiwan", "Macau", "Japan", "Korea"
        };

        for (String keyword : locationKeywords) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }

        return null;
    }

    /**
     * 从URL中推断location
     */
    private String extractLocationFromUrl(String url) {
        if (url == null) {
            return null;
        }

        String lowerUrl = url.toLowerCase();
        
        // URL参数模式
        if (lowerUrl.contains("_offices=china") || lowerUrl.contains("location=china")) {
            return "China";
        }
        if (lowerUrl.contains("region=asia") || lowerUrl.contains("apac")) {
            return "Asia Pacific";
        }

        // 域名模式
        if (lowerUrl.contains(".cn/") || lowerUrl.contains("china")) {
            return "China";
        }
        if (lowerUrl.contains(".sg/") || lowerUrl.contains("singapore")) {
            return "Singapore";
        }
        if (lowerUrl.contains(".hk/") || lowerUrl.contains("hongkong")) {
            return "Hong Kong";
        }

        return null;
    }

    /**
     * 根据公司信息推断location
     */
    private String inferLocationFromCompany(String company, String url) {
        if (company == null) {
            return null;
        }

        String lowerCompany = company.toLowerCase();
        String lowerUrl = url != null ? url.toLowerCase() : "";

        // 已知的公司location映射
        if ("airbnb".equals(lowerCompany) && lowerUrl.contains("china")) {
            return "China";
        }
        if ("apple".equals(lowerCompany) && lowerUrl.contains("zh-cn")) {
            return "China";
        }
        if (lowerCompany.contains("singapore") || lowerUrl.contains(".sg")) {
            return "Singapore";
        }

        return null;
    }

    /**
     * 检查location是否为空或无效
     */
    private boolean isLocationEmpty(String location) {
        return location == null || location.trim().isEmpty() || 
               "unknown".equalsIgnoreCase(location) || 
               "n/a".equalsIgnoreCase(location);
    }

    /**
     * 验证文本是否为有效的location信息
     */
    private boolean isValidLocationText(String text) {
        if (text == null || text.isBlank() || text.length() < 2) {
            return false;
        }

        String lower = text.toLowerCase();
        
        // 排除明显不是location的文本
        if (lower.matches("^[0-9]+$") || // 纯数字
            lower.contains("engineer") || lower.contains("manager") || 
            lower.contains("lead") || lower.contains("senior") ||
            lower.contains("support") || lower.contains("community") ||
            lower.contains("software") || lower.contains("data") ||
            lower.contains("product") || lower.contains("design") ||
            lower.length() > 50) { // 太长的文本通常不是location
            return false;
        }

        // 常见location关键词
        String[] locationKeywords = {
            "china", "beijing", "shanghai", "shenzhen", "guangzhou", "hangzhou",
            "singapore", "hong kong", "taiwan", "macau", "japan", "korea",
            "remote", "hybrid", "office", "onsite"
        };

        for (String keyword : locationKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }

        // 检查是否包含城市/国家的常见模式
        if (text.matches(".*[A-Z][a-z]+.*") && text.length() < 20) {
            return true;
        }

        return false;
    }
}