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

        log.info("Fetching crawl page {} for blueprint {}", pagination.page(), blueprint.code());
        try {
            String body = client.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));
            return new CrawlPageSnapshot(body == null ? "" : body, java.util.List.of(), Map.of("url", url));
        } catch (Throwable ex) {
            if (isNotSslRecordException(ex)) {
                log.warn("Received non-TLS response while fetching {}: {}", url, extractPlainHttpMessage(ex));
                return new CrawlPageSnapshot("", java.util.List.of(), Map.of("url", url, "error", "NON_TLS_RESPONSE"));
            }
            if (ex instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new RuntimeException(ex);
        }
    }

    private String randomUserAgent() {
        String[] agents = new String[]{
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_2) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Safari/605.1.15",
                "Mozilla/5.0 (X11; Linux x86_64) Gecko/20100101 Firefox/124.0"
        };
        return agents[ThreadLocalRandom.current().nextInt(agents.length)];
    }

    private boolean isNotSslRecordException(Throwable ex) {
        while (ex != null) {
            if (ex.getClass().getName().equals("io.netty.handler.ssl.NotSslRecordException")) {
                return true;
            }
            ex = ex.getCause();
        }
        return false;
    }

    private String extractPlainHttpMessage(Throwable ex) {
        while (ex != null) {
            if (ex instanceof io.netty.handler.ssl.NotSslRecordException notSsl) {
                String message = notSsl.getMessage();
                String decoded = decodeHexPayload(message);
                return decoded != null ? decoded : message;
            }
            ex = ex.getCause();
        }
        return "not an SSL/TLS record";
    }

    private String decodeHexPayload(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();
        int idx = trimmed.lastIndexOf(':');
        if (idx != -1) {
            trimmed = trimmed.substring(idx + 1).trim();
        }
        if (trimmed.isEmpty() || (trimmed.length() % 2 != 0)) {
            return null;
        }
        if (!trimmed.chars().allMatch(ch -> Character.digit(ch, 16) != -1)) {
            return null;
        }
        byte[] bytes = new byte[trimmed.length() / 2];
        for (int i = 0; i < trimmed.length(); i += 2) {
            int hi = Character.digit(trimmed.charAt(i), 16);
            int lo = Character.digit(trimmed.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                return null;
            }
            bytes[i / 2] = (byte) ((hi << 4) + lo);
        }
        return new String(bytes);
    }
}
