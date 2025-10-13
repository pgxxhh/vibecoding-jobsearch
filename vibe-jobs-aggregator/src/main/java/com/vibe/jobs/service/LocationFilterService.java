package com.vibe.jobs.service;

import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.sources.FetchedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationFilterService {
    
    private static final Logger log = LoggerFactory.getLogger(LocationFilterService.class);
    
    private final IngestionProperties properties;
    
    public LocationFilterService(IngestionProperties properties) {
        this.properties = properties;
    }
    
    /**
     * 根据配置的location过滤器过滤job列表
     */
    public List<FetchedJob> filterJobs(List<FetchedJob> jobs) {
        IngestionProperties.LocationFilter filter = properties.getLocationFilter();
        
        if (!filter.isEnabled()) {
            log.info("Location filter is disabled, returning all {} jobs", jobs.size());
            return jobs;
        }
        
        List<FetchedJob> filteredJobs = jobs.stream()
                .filter(job -> filter.matches(job.job().getLocation()))
                .collect(Collectors.toList());
        
        int originalSize = jobs.size();
        int filteredSize = filteredJobs.size();
        
        if (originalSize > filteredSize) {
            log.info("Location filter: {} jobs -> {} jobs (filtered out {})", 
                    originalSize, filteredSize, originalSize - filteredSize);
        }
        
        return filteredJobs;
    }
    
    /**
     * 检查单个job是否符合location过滤条件
     */
    public boolean matchesLocationFilter(String location) {
        IngestionProperties.LocationFilter filter = properties.getLocationFilter();
        return filter.matches(location);
    }
    
    /**
     * 获取当前location过滤器的配置状态
     */
    public String getFilterStatus() {
        IngestionProperties.LocationFilter filter = properties.getLocationFilter();
        if (!filter.isEnabled()) {
            return "Location filter: DISABLED";
        }
        
        StringBuilder status = new StringBuilder("Location filter: ENABLED\n");
        
        if (!filter.getIncludeCountries().isEmpty()) {
            status.append("  Include countries: ").append(filter.getIncludeCountries()).append("\n");
        }
        if (!filter.getIncludeCities().isEmpty()) {
            status.append("  Include cities: ").append(filter.getIncludeCities()).append("\n");
        }
        if (!filter.getIncludeKeywords().isEmpty()) {
            status.append("  Include keywords: ").append(filter.getIncludeKeywords()).append("\n");
        }
        if (!filter.getExcludeCountries().isEmpty()) {
            status.append("  Exclude countries: ").append(filter.getExcludeCountries()).append("\n");
        }
        if (!filter.getExcludeCities().isEmpty()) {
            status.append("  Exclude cities: ").append(filter.getExcludeCities()).append("\n");
        }
        if (!filter.getExcludeKeywords().isEmpty()) {
            status.append("  Exclude keywords: ").append(filter.getExcludeKeywords()).append("\n");
        }
        
        return status.toString().trim();
    }
}