package com.vibe.jobs.crawler.application.generation;

import com.vibe.jobs.crawler.domain.ParserProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlerBlueprintAutoParserTest {

    private static final String SAP_SAMPLE_HTML = """
            <html class=\"html5\">
            <body class=\"coreCSB search-page\">
              <div class=\"pagination-top clearfix\">
                <div class=\"paginationShell clearfix\">
                  <div class=\"pagination-well pagination\">
                    <ul class=\"pagination\">
                      <li><a class=\"paginationItemFirst\" aria-label=\"First\" href=\"?startrow=0\">&laquo;</a></li>
                      <li><a href=\"?startrow=25\">2</a></li>
                      <li><a aria-label=\"Next\" href=\"?startrow=50\">Next</a></li>
                    </ul>
                  </div>
                </div>
              </div>
              <div class=\"searchResultsShell\">
                <table id=\"searchresults\" class=\"searchResults\">
                  <tbody>
                    <tr class=\"data-row\">
                      <td class=\"colTitle\">
                        <span class=\"jobTitle\">
                          <a class=\"jobTitle-link\" href=\"/job/123\">Project Lead</a>
                        </span>
                      </td>
                      <td class=\"colLocation\">Tokyo, Japan</td>
                    </tr>
                    <tr class=\"data-row\">
                      <td class=\"colTitle\">
                        <span class=\"jobTitle\">
                          <a class=\"jobTitle-link\" href=\"/job/456\">Software Engineer</a>
                        </span>
                      </td>
                      <td class=\"colLocation\">Berlin, Germany</td>
                    </tr>
                    <tr class=\"data-row\">
                      <td class=\"colTitle\">
                        <span class=\"jobTitle\">
                          <a class=\"jobTitle-link\" href=\"/job/789\">Data Analyst</a>
                        </span>
                      </td>
                      <td class=\"colLocation\">New York, USA</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </body>
            </html>
            """;

    @Test
    void parsePrefersJobTableOverPagination() {
        CrawlerBlueprintAutoParser parser = new CrawlerBlueprintAutoParser();

        CrawlerBlueprintAutoParser.AutoParseResult result = parser.parse(
                "https://jobs.sap.com/search",
                SAP_SAMPLE_HTML
        );

        ParserProfile profile = result.profile();

        assertThat(profile.listSelector()).isEqualTo("div.searchResultsShell > table.searchResults > tbody");
        assertThat(profile.fields()).containsKeys("title", "url", "company");
        assertThat(profile.fields().get("title").selector()).isEqualTo("tr.data-row > td.colTitle > span.jobTitle > a.jobTitle-link");
        assertThat(profile.fields().get("url").selector()).isEqualTo("tr.data-row > td.colTitle > span.jobTitle > a.jobTitle-link");
        assertThat(profile.fields().get("company").constant()).isEqualTo("SAP");
    }
}
