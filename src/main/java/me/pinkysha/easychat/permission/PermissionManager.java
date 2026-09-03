package me.pinkysha.easychat.permission;

import me.pinkysha.easychat.chat.ChatChannel;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PermissionManager {
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)(?:&#|#|<#)([0-9a-f]{6})>?");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("(?i)&([0-9a-fk-or])");

    private static final Map<Character, String> COLOR_NAMES = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white")
    );

    private static final Map<Character, String> STYLE_NAMES = Map.of(
            'k', "obfuscated",
            'l', "bold",
            'm', "strikethrough",
            'n', "underlined",
            'o', "italic",
            'r', "reset"
    );

    public boolean canUse(Player player, ChatChannel channel) {
        return player.hasPermission(channel.permission());
    }

    public boolean canBypassCooldown(Player player, String permission) {
        return player.hasPermission(permission);
    }

    public boolean canBypassMute(Player player, String permission) {
        return player.hasPermission(permission);
    }

    public boolean hasColorPermission(Player player, char code) {
        if (player.hasPermission("easychat.admin")
                || player.hasPermission("easychat.chat.color.*")
                || player.hasPermission("easychat.chat.color")) {
            return true;
        }

        char lower = Character.toLowerCase(code);
        if (player.hasPermission("easychat.chat.color." + lower)) {
            return true;
        }

        String name = COLOR_NAMES.get(lower);
        if (name != null && player.hasPermission("easychat.chat.color." + name)) {
            return true;
        }
        if (lower == 'd' && player.hasPermission("easychat.chat.color.pink")) {
            return true;
        }

        return false;
    }

    public boolean hasStylePermission(Player player, char code) {
        if (player.hasPermission("easychat.admin")
                || player.hasPermission("easychat.chat.style.*")
                || player.hasPermission("easychat.chat.style")) {
            return true;
        }

        char lower = Character.toLowerCase(code);
        if (player.hasPermission("easychat.chat.style." + lower)) {
            return true;
        }

        String name = STYLE_NAMES.get(lower);
        if (name != null && player.hasPermission("easychat.chat.style." + name)) {
            return true;
        }
        if (lower == 'n' && player.hasPermission("easychat.chat.style.underline")) {
            return true;
        }
        if (lower == 'k' && player.hasPermission("easychat.chat.style.magic")) {
            return true;
        }

        return false;
    }

    public boolean hasHexPermission(Player player, String hexCode) {
        if (player.hasPermission("easychat.admin")
                || player.hasPermission("easychat.chat.color.*")
                || player.hasPermission("easychat.chat.color.hex")
                || player.hasPermission("easychat.chat.color.hex.*")) {
            return true;
        }

        String clean = hexCode.replace("#", "").toLowerCase(Locale.ROOT);
        return player.hasPermission("easychat.chat.color.hex." + clean)
                || player.hasPermission("easychat.chat.color.hex.#" + clean);
    }

    public String applyColorPermissions(Player player, String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return "";
        }

        // 1. Process HEX colors
        Matcher hexMatcher = HEX_PATTERN.matcher(rawMessage);
        StringBuilder hexBuffer = new StringBuilder();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            if (hasHexPermission(player, hex)) {
                StringBuilder sectionHex = new StringBuilder("§x");
                for (char c : hex.toLowerCase(Locale.ROOT).toCharArray()) {
                    sectionHex.append('§').append(c);
                }
                hexMatcher.appendReplacement(hexBuffer, Matcher.quoteReplacement(sectionHex.toString()));
            } else {
                hexMatcher.appendReplacement(hexBuffer, Matcher.quoteReplacement(hexMatcher.group(0)));
            }
        }
        hexMatcher.appendTail(hexBuffer);

        // 2. Process legacy colors and styles
        Matcher legacyMatcher = LEGACY_PATTERN.matcher(hexBuffer.toString());
        StringBuilder result = new StringBuilder();
        while (legacyMatcher.find()) {
            char code = legacyMatcher.group(1).charAt(0);
            char lower = Character.toLowerCase(code);

            if (COLOR_NAMES.containsKey(lower)) {
                if (hasColorPermission(player, lower)) {
                    legacyMatcher.appendReplacement(result, "§" + lower);
                } else {
                    legacyMatcher.appendReplacement(result, Matcher.quoteReplacement(legacyMatcher.group(0)));
                }
            } else if (STYLE_NAMES.containsKey(lower)) {
                if (hasStylePermission(player, lower)) {
                    legacyMatcher.appendReplacement(result, "§" + lower);
                } else {
                    legacyMatcher.appendReplacement(result, Matcher.quoteReplacement(legacyMatcher.group(0)));
                }
            } else {
                legacyMatcher.appendReplacement(result, Matcher.quoteReplacement(legacyMatcher.group(0)));
            }
        }
        legacyMatcher.appendTail(result);

        return result.toString();
    }
}
