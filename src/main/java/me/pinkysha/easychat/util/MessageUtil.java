package me.pinkysha.easychat.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;

public final class MessageUtil {
    private MessageUtil() {}

    public static Component replace(Component component, String placeholder, String value) {
        return component.replaceText(TextReplacementConfig.builder()
                .matchLiteral(placeholder)
                .replacement(value)
                .build());
    }
}
