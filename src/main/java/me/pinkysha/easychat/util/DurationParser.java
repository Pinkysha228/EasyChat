package me.pinkysha.easychat.util;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern PATTERN = Pattern.compile("^(\\d+)(s|m|h|d|w)$", Pattern.CASE_INSENSITIVE);
    private DurationParser() {}
    public static Duration parse(String input) {
        if (input == null || input.equalsIgnoreCase("perm") || input.equalsIgnoreCase("permanent")) return null;
        Matcher m = PATTERN.matcher(input.trim().toLowerCase(Locale.ROOT));
        if (!m.matches()) throw new IllegalArgumentException();
        long value = Long.parseLong(m.group(1));
        return switch (m.group(2)) {
            case "s" -> Duration.ofSeconds(value);
            case "m" -> Duration.ofMinutes(value);
            case "h" -> Duration.ofHours(value);
            case "d" -> Duration.ofDays(value);
            case "w" -> Duration.ofDays(Math.multiplyExact(value, 7));
            default -> throw new IllegalArgumentException();
        };
    }
}
