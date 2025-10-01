package com.vibe.jobs.crawler.infrastructure.parser;

import com.vibe.jobs.crawler.domain.CrawlContext;
import com.vibe.jobs.crawler.domain.CrawlResult;
import com.vibe.jobs.crawler.domain.CrawlSession;
import com.vibe.jobs.crawler.domain.ParserProfile;
import com.vibe.jobs.crawler.domain.ParserProfile.ParsedJob;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.sources.FetchedJob;
import com.vibe.jobs.crawler.infrastructure.engine.CrawlPageSnapshot;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DefaultCrawlerParserEngine implements CrawlerParserEngine {

    @Override
    public List<CrawlResult> parse(CrawlSession session, CrawlPageSnapshot snapshot) {
        ParserProfile profile = session.blueprint().parserProfile();
        List<ParsedJob> parsed = profile.parse(snapshot.pageContent());
        List<CrawlResult> results = new ArrayList<>();
        CrawlContext context = session.context();
        for (ParsedJob job : parsed) {
            if (job.title() == null || job.title().isBlank()) {
                continue;
            }
            Job domainJob = Job.builder()
                    .source(resolveSourceName(context))
                    .externalId(job.externalId().isBlank() ? job.title() : job.externalId())
                    .title(job.title())
                    .company(resolveCompany(context, job))
                    .location(job.location())
                    .level(job.level())
                    .postedAt(job.postedAt() == null ? Instant.now() : job.postedAt())
                    .url(job.url())
                    .tags(normalizeTags(job.tags()))
                    .build();
            results.add(new CrawlResult(new FetchedJob(domainJob, job.description()), job.description(), Map.of()));
        }
        return results;
    }

    private String resolveSourceName(CrawlContext context) {
        if (context.sourceName() != null && !context.sourceName().isBlank()) {
            return context.sourceName();
        }
        return context.dataSourceCode().isBlank() ? "crawler" : "crawler:" + context.dataSourceCode();
    }

    private String resolveCompany(CrawlContext context, ParsedJob parsed) {
        if (parsed.company() != null && !parsed.company().isBlank()) {
            return parsed.company();
        }
        return context.company().isBlank() ? parsed.company() : context.company();
    }

    private Set<String> normalizeTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }
        return tags.stream()
                .map(tag -> tag == null ? "" : tag.trim().toLowerCase())
                .filter(tag -> !tag.isBlank())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
