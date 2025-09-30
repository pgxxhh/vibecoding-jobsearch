package com.vibe.jobs.sources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Apple Jobs API客户端
 * 注意：Apple没有公开的API，暂时返回空列表
 */
public class AppleJobsClient implements SourceClient {
    
    private static final Logger log = LoggerFactory.getLogger(AppleJobsClient.class);
    
    @Override
    public String sourceName() {
        return "Apple Jobs API";
    }
    
    @Override
    public List<FetchedJob> fetchPage(int page, int size) throws Exception {
        log.warn("Apple Jobs API is not publicly available - returning empty list");
        return new ArrayList<>();
    }
}