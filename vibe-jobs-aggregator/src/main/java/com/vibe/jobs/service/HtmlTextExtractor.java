package com.vibe.jobs.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class HtmlTextExtractor {

    private HtmlTextExtractor() {
    }

    public static String toPlainText(String html) {
        if (html == null) {
            return null;
        }
        String trimmed = html.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        Document document = Jsoup.parse(trimmed);
        preserveLineBreaks(document);
        String text = document.text()
                .replace('\u00A0', ' ')
                .trim();
        if (text.isEmpty()) {
            return null;
        }
        return normalizeWhitespace(text);
    }

    private static void preserveLineBreaks(Document document) {
        for (Element element : document.select("br")) {
            element.append("\n");
        }
        document.select("p, li, div").forEach(node -> node.append("\n"));
    }

    private static String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}
