package com.vibe.jobs.crawler.application;

import com.vibe.jobs.crawler.domain.CrawlBlueprint;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintRepository;
import com.vibe.jobs.crawler.domain.CrawlerParserTemplateRepository;
import com.vibe.jobs.crawler.domain.ParserProfile;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CrawlerCommandService {

    private final CrawlerBlueprintRepository blueprintRepository;
    private final CrawlerParserTemplateRepository templateRepository;

    public CrawlerCommandService(CrawlerBlueprintRepository blueprintRepository,
                                 CrawlerParserTemplateRepository templateRepository) {
        this.blueprintRepository = blueprintRepository;
        this.templateRepository = templateRepository;
    }

    public Optional<CrawlBlueprint> findBlueprint(String code) {
        return blueprintRepository.findByCode(code);
    }

    public ParserProfile resolveParser(String templateCode, ParserProfile fallback) {
        if (templateCode == null || templateCode.isBlank()) {
            return fallback;
        }
        return templateRepository.findByCode(templateCode).orElse(fallback);
    }
}
