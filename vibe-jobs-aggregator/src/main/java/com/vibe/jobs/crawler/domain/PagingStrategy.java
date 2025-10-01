package com.vibe.jobs.crawler.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class PagingStrategy {

    public enum Mode {
        NONE,
        QUERY,
        PATH_SUFFIX,
        OFFSET
    }

    private final Mode mode;
    private final String parameter;
    private final int start;
    private final int step;
    private final String sizeParameter;

    private PagingStrategy(Mode mode, String parameter, int start, int step, String sizeParameter) {
        this.mode = mode;
        this.parameter = parameter == null ? "" : parameter.trim();
        this.start = start;
        this.step = step <= 0 ? 1 : step;
        this.sizeParameter = sizeParameter == null ? "" : sizeParameter.trim();
    }

    public static PagingStrategy disabled() {
        return new PagingStrategy(Mode.NONE, "", 1, 1, "");
    }

    public static PagingStrategy query(String parameter, int start, int step, String sizeParameter) {
        return new PagingStrategy(Mode.QUERY, parameter, start, step, sizeParameter);
    }

    public static PagingStrategy pathSuffix(int start, int step) {
        return new PagingStrategy(Mode.PATH_SUFFIX, null, start, step, "");
    }

    public static PagingStrategy offset(String parameter, int start, int step, String sizeParameter) {
        return new PagingStrategy(Mode.OFFSET, parameter, start, step, sizeParameter);
    }

    public String apply(String baseUrl, CrawlPagination pagination) {
        if (baseUrl == null || baseUrl.isBlank() || pagination == null) {
            return baseUrl;
        }
        try {
            return switch (mode) {
                case NONE -> baseUrl;
                case QUERY -> applyQuery(baseUrl, pagination.page());
                case PATH_SUFFIX -> applyPathSuffix(baseUrl, pagination.page());
                case OFFSET -> applyOffset(baseUrl, pagination);
            };
        } catch (URISyntaxException e) {
            return baseUrl;
        }
    }

    private String applyQuery(String baseUrl, int page) throws URISyntaxException {
        String param = parameter.isBlank() ? "page" : parameter;
        URI uri = new URI(baseUrl);
        String query = uri.getQuery();
        int normalized = normalizePage(page);
        String newQueryParam = param + "=" + normalized;
        String updated = query == null || query.isBlank() ? newQueryParam : query + "&" + newQueryParam;
        if (!sizeParameter.isBlank() && !updated.contains(sizeParameter + "=")) {
            updated = updated + "&" + sizeParameter + "=" + Math.max(1, pagination.size());
        }
        return rebuildUri(uri, updated);
    }

    private String applyOffset(String baseUrl, CrawlPagination pagination) throws URISyntaxException {
        String param = parameter.isBlank() ? "offset" : parameter;
        URI uri = new URI(baseUrl);
        String query = uri.getQuery();
        int offset = (normalizePage(pagination.page()) - 1) * Math.max(1, pagination.size());
        String newQueryParam = param + "=" + offset;
        String updated = query == null || query.isBlank() ? newQueryParam : query + "&" + newQueryParam;
        if (!sizeParameter.isBlank() && !updated.contains(sizeParameter + "=")) {
            updated = updated + "&" + sizeParameter + "=" + Math.max(1, pagination.size());
        }
        return rebuildUri(uri, updated);
    }

    private String applyPathSuffix(String baseUrl, int page) {
        int normalized = normalizePage(page);
        if (baseUrl.endsWith("/")) {
            return baseUrl + normalized;
        }
        return baseUrl + "/" + normalized;
    }

    private String rebuildUri(URI uri, String query) throws URISyntaxException {
        return new URI(
                uri.getScheme(),
                uri.getAuthority(),
                uri.getPath(),
                query,
                uri.getFragment())
                .toString();
    }

    private int normalizePage(int page) {
        int normalized = Math.max(1, page);
        int value = start + ((normalized - 1) * step);
        if (mode == Mode.OFFSET) {
            return value;
        }
        return Math.max(1, value);
    }

    private int normalizedStep() {
        return Math.max(1, step);
    }

    public Mode mode() {
        return mode;
    }

    public String parameter() {
        return parameter;
    }

    public int start() {
        return start;
    }

    public int step() {
        return step;
    }

    public String sizeParameter() {
        return sizeParameter;
    }

    public static PagingStrategy from(String mode, String parameter, int start, int step, String sizeParameter) {
        if (mode == null || mode.isBlank()) {
            return disabled();
        }
        Mode parsed = Mode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
        return switch (parsed) {
            case NONE -> disabled();
            case QUERY -> query(parameter, start, step, sizeParameter);
            case PATH_SUFFIX -> pathSuffix(start, step);
            case OFFSET -> offset(parameter, start, step, sizeParameter);
        };
    }
}
