package me.pinkysha.easychat;

import me.pinkysha.easychat.util.DurationParser;

public final class DurationParserSmokeTest {
    public static void main(String[] args) {
        if (DurationParser.parse("30s").toSeconds() != 30) throw new AssertionError();
        if (DurationParser.parse("2h").toHours() != 2) throw new AssertionError();
        if (DurationParser.parse("7d").toDays() != 7) throw new AssertionError();
        if (DurationParser.parse("perm") != null) throw new AssertionError();
        System.out.println("DurationParser smoke test passed");
    }
}
