package com.vibe.jobs.sources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Microsoft Careers API客户端  
 * 注意：Microsoft没有公开的API，暂时返回空列表
 */
public class MicrosoftJobsClient implements SourceClient {
    
    private static final Logger log = LoggerFactory.getLogger(MicrosoftJobsClient.class);
    
    @Override
    public String sourceName() {
        return "Microsoft Careers API";
    }
    
    @Override
    public List<FetchedJob> fetchPage(int page, int size) throws Exception {
        log.warn("Microsoft Careers API is not publicly available - returning empty list");
        return new ArrayList<>();
    }
}