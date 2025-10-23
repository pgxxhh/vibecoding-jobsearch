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

    @Test
    void parsesJobCardsDetectedThroughDataAttributes() {
        String html = """
                <html>
                  <body>
                    <div id='app'>
                      <div data-automation-id='jobResults'>
                        <ul role='listbox'>
                          <li data-automation-id='jobListItem'>
                            <div class='css-1 job-card' data-automation-id='jobCard'>
                              <div data-automation-id='jobTitle'>
                                <a href='/en-us/careers/job/123'>Programmatic Specialist</a>
                              </div>
                              <div data-automation-id='locations'>Singapore</div>
                            </div>
                          </li>
                        </ul>
                      </div>
                    </div>
                  </body>
                </html>
                """;

        CrawlerBlueprintAutoParser.AutoParseResult result = parser.parse("https://careers.example.com/search", html);

        ParserProfile profile = result.profile();
        assertThat(profile.listSelector()).isNotBlank();
        assertThat(profile.fields()).containsKeys("title", "url");
        assertThat(profile.fields().get("title").selector()).contains("a");
        assertThat(profile.fields().get("url").attribute()).isEqualTo("href");
    }
}
