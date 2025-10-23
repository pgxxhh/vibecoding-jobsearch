package com.vibe.jobs.crawler.application.generation;

import com.vibe.jobs.crawler.domain.ParserProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlerBlueprintAutoParserTest {

    private final CrawlerBlueprintAutoParser parser = new CrawlerBlueprintAutoParser();

    @Test
    void parsesSingleJobListingWithMinimalAnchors() {
        String html = """
                <html>
                  <body>
                    <main>
                      <div class='job-card'>
                        <a href='/jobs/123'>Senior Trader, APAC</a>
                        <p class='job-card__location'>Singapore</p>
                      </div>
                    </main>
                  </body>
                </html>
                """;

        CrawlerBlueprintAutoParser.AutoParseResult result = parser.parse("https://example.com/careers", html);

        ParserProfile profile = result.profile();
        assertThat(profile.listSelector()).isNotBlank();
        assertThat(profile.fields()).containsKeys("title", "url");
        assertThat(profile.fields().get("title").selector()).isEqualTo("a");
        assertThat(profile.fields().get("url").selector()).isEqualTo("a");
        assertThat(profile.fields().get("url").attribute()).isEqualTo("href");
    }
}
