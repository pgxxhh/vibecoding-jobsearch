package com.vibe.jobs.auth.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class EmailAddress {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final String value;

    private EmailAddress(String value) {
        this.value = value;
    }

    public static EmailAddress of(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Email must not be null");
        }
        String trimmed = raw.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid email address format");
        }
        return new EmailAddress(trimmed.toLowerCase(Locale.ROOT));
    }

    public static EmailAddress restored(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or blank");
        }
        return new EmailAddress(value);
    }

    public String value() {
        return value;
    }

    public String masked() {
        int atIdx = value.indexOf('@');
        if (atIdx <= 1) {
            return "***" + value.substring(Math.max(atIdx, 0));
        }
        String local = value.substring(0, atIdx);
        String domain = value.substring(atIdx);
        int visible = Math.min(2, local.length());
        return local.substring(0, visible) + "***" + domain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailAddress that = (EmailAddress) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
