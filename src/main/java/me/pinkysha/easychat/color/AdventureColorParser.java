package me.pinkysha.easychat.color;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AdventureColorParser implements ColorParser {
    private static final Pattern HEX = Pattern.compile("(?i)(?:&#|#|<#)([0-9a-f]{6})>?");
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
            .hexColors()
            .character('&')
            .build();

    private final LegacyComponentSerializer sectionSerializer = LegacyComponentSerializer.builder()
            .hexColors()
            .character('§')
            .build();

    @Override
    public Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        Matcher matcher = HEX.matcher(input);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("&x&" + String.join("&", matcher.group(1).split(""))));
        }
        matcher.appendTail(buffer);
        return serializer.deserialize(buffer.toString());
    }

    @Override
    public Component parseFiltered(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return sectionSerializer.deserialize(input);
    }
}
