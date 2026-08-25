package me.pinkysha.easychat.util;

import net.kyori.adventure.text.Component;

public final class MessageUtil {
    private MessageUtil() {}
    public static Component replace(Component component, String placeholder, String value) {
        return component.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral(placeholder).replacement(value).build());
    }
}
