package com.vibe.jobs.crawler.application.generation;

import com.vibe.jobs.crawler.domain.PagingStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlerPagingDetectionTest {

    @Test
    void detectsStartRowOffsetPaging() {
        String html = """
                <html>
                <body>
                  <table id=\"jobs\"><tbody>
                    <tr class=\"row\"><td><a href=\"/job/1\">Job 1</a></td></tr>
                    <tr class=\"row\"><td><a href=\"/job/2\">Job 2</a></td></tr>
                  </tbody></table>
                  <a href=\"/search?q=&startrow=25\">Next</a>
                </body>
                </html>
                """;
        CrawlerBlueprintAutoParser parser = new CrawlerBlueprintAutoParser();
        CrawlerBlueprintAutoParser.AutoParseResult result = parser.parse("https://example.com/search", html);

        assertThat(result.pagingStrategy().mode()).isEqualTo(PagingStrategy.Mode.OFFSET);
        assertThat(result.pagingStrategy().parameter()).isEqualTo("startrow");
    }

    @Test
    void fallsBackWhenNoPagingLinkFound() {
        String html = """
                <html>
                <body>
                  <table id=\"jobs\"><tbody>
                    <tr class=\"row\"><td><a href=\"/job/1\">Job 1</a></td></tr>
                    <tr class=\"row\"><td><a href=\"/job/2\">Job 2</a></td></tr>
                  </tbody></table>
                </body>
                </html>
                """;
        CrawlerBlueprintAutoParser parser = new CrawlerBlueprintAutoParser();

        CrawlerBlueprintAutoParser.AutoParseResult result = parser.parse("https://example.com/search", html);

        assertThat(result.pagingStrategy().mode()).isEqualTo(PagingStrategy.Mode.NONE);
    }
}
