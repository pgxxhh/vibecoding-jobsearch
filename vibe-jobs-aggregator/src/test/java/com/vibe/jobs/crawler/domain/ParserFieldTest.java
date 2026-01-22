package com.vibe.jobs.crawler.domain;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParserFieldTest {

    @Test
    void normalizesRelativeHrefWithMissingSlash() {
        Element element = Jsoup.parse("<a href='/job/123'>Job</a>").selectFirst("a");
        ParserField field = new ParserField(
                "url",
                ParserFieldType.ATTRIBUTE,
                ".",
                "href",
                null,
                null,
                ",",
                true,
                "https://jobs.sap.com"
        );

        Object value = field.extract(element);

        assertThat(value).isEqualTo("https://jobs.sap.com/job/123");
    }
}
