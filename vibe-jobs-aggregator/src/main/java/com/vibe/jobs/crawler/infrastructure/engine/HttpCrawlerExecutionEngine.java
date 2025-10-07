package com.vibe.jobs.crawler.infrastructure.engine;

import com.vibe.jobs.crawler.domain.CrawlBlueprint;
import com.vibe.jobs.crawler.domain.CrawlPagination;
import com.vibe.jobs.crawler.domain.CrawlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class HttpCrawlerExecutionEngine implements CrawlerExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(HttpCrawlerExecutionEngine.class);
    private static final int MAX_BUFFER_SIZE_BYTES = 2 * 1024 * 1024; // allow ~2MB HTML payloads
    private static final ExchangeStrategies CUSTOM_STRATEGIES = ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_BUFFER_SIZE_BYTES))
            .build();
    private final WebClient.Builder webClientBuilder;

    public HttpCrawlerExecutionEngine(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public CrawlPageSnapshot fetch(CrawlSession session, CrawlPagination pagination) {
        CrawlBlueprint blueprint = session.blueprint();
        String url = blueprint.resolveEntryUrl(session.context(), pagination);
        if (url == null || url.isBlank()) {
            return new CrawlPageSnapshot("", java.util.List.of(), Map.of("status", 400));
        }
        WebClient client = webClientBuilder.clone()
                .exchangeStrategies(CUSTOM_STRATEGIES)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, randomUserAgent())
                .build();

        log.debug("Fetching crawl page {} for blueprint {}", pagination.page(), blueprint.code());
        String body = client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));

        return new CrawlPageSnapshot(body == null ? "" : body, java.util.List.of(), Map.of("url", url));
    }

    private String randomUserAgent() {
        String[] agents = new String[]{
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_2) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Safari/605.1.15",
                "Mozilla/5.0 (X11; Linux x86_64) Gecko/20100101 Firefox/124.0"
        };
        return agents[ThreadLocalRandom.current().nextInt(agents.length)];
    }
}
