package com.vibe.jobs.crawler.application.generation;

import com.vibe.jobs.crawler.domain.ParserProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void throwsWhenChallengePageDetected() {
        String html = """
                <html>
                  <head>
                    <title>Just a moment...</title>
                    <meta http-equiv="refresh" content="0;url=/jobs?__cf_chl=abc" />
                  </head>
                  <body>
                    <form id="challenge-form"></form>
                  </body>
                </html>
                """;

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                parser.parse("https://example.com/jobs", html)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("challenge page");
    }

    @Test
    void throwsWhenOnlyNavigationAnchorsDetected() {
        String html = """
                <html>
                  <body>
                    <footer class='global-footer'>
                      <nav role='navigation'>
                        <ul>
                          <li><a href='/zh-cn/jobs'>Apple 职位</a></li>
                          <li><a href='/zh-cn/careers'>更多职位</a></li>
                          <li><a href='/zh-cn/opportunities'>探索机会</a></li>
                        </ul>
                      </nav>
                    </footer>
                  </body>
                </html>
                """;

        assertThatThrownBy(() -> parser.parse("https://jobs.apple.com/zh-cn/search", html))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void detectsAppleJobListStructure() {
        String html = """
                <html>
                  <body>
                    <ul id="search-job-list">
                      <li class="rc-accordion-item" data-core-accordion-item>
                        <div>
                          <h3><a href="/job/1">Store Leader</a></h3>
                        </div>
                      </li>
                      <li class="rc-accordion-item" data-core-accordion-item>
                        <div>
                          <h3><a href="/job/2">Senior Manager</a></h3>
                        </div>
                      </li>
                    </ul>
                  </body>
                </html>
                """;

        CrawlerBlueprintAutoParser.AutoParseResult result = parser.parse("https://jobs.apple.com/zh-cn/search", html);

        ParserProfile profile = result.profile();
        assertThat(profile.listSelector()).contains("li").contains("rc-accordion-item");
        assertThat(profile.fields().get("title").selector()).contains("a");
        assertThat(profile.fields().get("url").attribute()).isEqualTo("href");
    }
}
