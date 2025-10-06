package com.vibe.jobs.crawler.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Strongly typed representation of browser automation metadata attached to a {@link CrawlBlueprint}.
 */
public class AutomationSettings {

    private final boolean enabled;
    private final boolean javascriptEnabled;
    private final String waitForSelector;
    private final int waitForMilliseconds;
    private final SearchSettings search;

    public AutomationSettings(boolean enabled,
                              boolean javascriptEnabled,
                              String waitForSelector,
                              Integer waitForMilliseconds,
                              SearchSettings search) {
        this.enabled = enabled;
        this.javascriptEnabled = javascriptEnabled;
        this.waitForSelector = waitForSelector == null ? "" : waitForSelector.trim();
        this.waitForMilliseconds = waitForMilliseconds == null ? 0 : Math.max(0, waitForMilliseconds);
        this.search = search == null ? SearchSettings.disabled() : search;
    }

    public static AutomationSettings disabled() {
        return new AutomationSettings(false, false, "", 0, SearchSettings.disabled());
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean javascriptEnabled() {
        return javascriptEnabled;
    }

    public String waitForSelector() {
        return waitForSelector;
    }

    public int waitForMilliseconds() {
        return waitForMilliseconds;
    }

    public SearchSettings search() {
        return search;
    }

    public boolean requiresBrowser() {
        return javascriptEnabled || (enabled && (search != null && search.enabled()));
    }

    public static class SearchSettings {
        private final boolean enabled;
        private final List<SearchField> fields;
        private final String submitSelector;
        private final String waitForSelector;
        private final int waitAfterSubmitMs;

        public SearchSettings(boolean enabled,
                              List<SearchField> fields,
                              String submitSelector,
                              String waitForSelector,
                              Integer waitAfterSubmitMs) {
            this.enabled = enabled;
            if (fields == null || fields.isEmpty()) {
                this.fields = List.of();
            } else {
                List<SearchField> normalized = new ArrayList<>();
                for (SearchField field : fields) {
                    if (field != null) {
                        normalized.add(field);
                    }
                }
                this.fields = Collections.unmodifiableList(normalized);
            }
            this.submitSelector = submitSelector == null ? "" : submitSelector.trim();
            this.waitForSelector = waitForSelector == null ? "" : waitForSelector.trim();
            this.waitAfterSubmitMs = waitAfterSubmitMs == null ? 0 : Math.max(0, waitAfterSubmitMs);
        }

        public static SearchSettings disabled() {
            return new SearchSettings(false, List.of(), "", "", 0);
        }

        public boolean enabled() {
            return enabled;
        }

        public List<SearchField> fields() {
            return fields;
        }

        public String submitSelector() {
            return submitSelector;
        }

        public String waitForSelector() {
            return waitForSelector;
        }

        public int waitAfterSubmitMs() {
            return waitAfterSubmitMs;
        }
    }

    public static class SearchField {
        private final String selector;
        private final String optionKey;
        private final String constantValue;
        private final FillStrategy strategy;
        private final boolean clearBefore;
        private final boolean required;

        public SearchField(String selector,
                           String optionKey,
                           String constantValue,
                           FillStrategy strategy,
                           Boolean clearBefore,
                           Boolean required) {
            this.selector = Objects.requireNonNullElse(selector, "").trim();
            this.optionKey = optionKey == null ? "" : optionKey.trim();
            this.constantValue = constantValue == null ? "" : constantValue.trim();
            this.strategy = strategy == null ? FillStrategy.FILL : strategy;
            this.clearBefore = Boolean.TRUE.equals(clearBefore);
            this.required = Boolean.TRUE.equals(required);
        }

        public String selector() {
            return selector;
        }

        public String optionKey() {
            return optionKey;
        }

        public String constantValue() {
            return constantValue;
        }

        public FillStrategy strategy() {
            return strategy;
        }

        public boolean clearBefore() {
            return clearBefore;
        }

        public boolean required() {
            return required;
        }
    }

    public enum FillStrategy {
        /** Use Playwright's fill() */
        FILL,
        /** Use selectOption() */
        SELECT,
        /** Click the selector without providing value */
        CLICK
    }
}
