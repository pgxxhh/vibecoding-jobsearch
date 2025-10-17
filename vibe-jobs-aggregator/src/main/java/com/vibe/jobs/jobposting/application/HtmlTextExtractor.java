package com.vibe.jobs.jobposting.application;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Safelist;

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

        String decoded = trimmed;
        for (int i = 0; i < 3; i++) {
            String candidate = Entities.unescape(decoded);
            if (candidate.equals(decoded)) {
                break;
            }
            decoded = candidate;
        }

        if (!decoded.equals(trimmed)) {
            trimmed = decoded;
        }

        Document document = Jsoup.parse(trimmed);
        preserveLineBreaks(document);

        Element body = document.body();
        if (body == null) {
            return null;
        }

        Document.OutputSettings outputSettings = new Document.OutputSettings()
                .prettyPrint(false)
                .escapeMode(Entities.EscapeMode.xhtml);
        String stripped = Jsoup.clean(body.html(), "", Safelist.none(), outputSettings);
        if (stripped == null) {
            return null;
        }

        String normalized = normalizeWhitespace(stripped.replace('\u00A0', ' '));
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
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
