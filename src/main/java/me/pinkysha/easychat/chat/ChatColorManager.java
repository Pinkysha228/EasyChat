package me.pinkysha.easychat.chat;

import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.database.UserSettingsRepository;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class ChatColorManager {
    private static final Pattern HEX_PATTERN = Pattern.compile("^(?:&#|#)?([0-9a-fA-F]{6})$");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("^&?([0-9a-fA-F])$");

    private final EasyChat plugin;
    private final UserSettingsRepository repository;
    private final Map<UUID, String> playerColors = new ConcurrentHashMap<>();

    public ChatColorManager(EasyChat plugin, UserSettingsRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public CompletableFuture<Void> load() {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<UUID, String> loaded = repository.loadAllChatColors();
                playerColors.clear();
                playerColors.putAll(loaded);
                plugin.getLogger().info("Loaded " + loaded.size() + " user chat colors.");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load user chat colors: " + e.getMessage());
            }
        }, plugin.asyncExecutor());
    }

    public String getColor(UUID uuid) {
        String color = playerColors.get(uuid);
        if (color != null && !color.isBlank()) {
            return color;
        }
        return plugin.getConfig().getString("chatcolor.default", "");
    }

    public boolean hasCustomColor(UUID uuid) {
        return playerColors.containsKey(uuid);
    }

    public void setColor(UUID uuid, String color) {
        playerColors.put(uuid, color);
        plugin.asyncExecutor().execute(() -> {
            try {
                repository.saveChatColor(uuid, color);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save chat color for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void resetColor(UUID uuid) {
        playerColors.remove(uuid);
        plugin.asyncExecutor().execute(() -> {
            try {
                repository.removeChatColor(uuid);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to remove chat color for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public String normalizeColorInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String clean = input.trim();

        var hexMatcher = HEX_PATTERN.matcher(clean);
        if (hexMatcher.matches()) {
            return "&#" + hexMatcher.group(1).toLowerCase(Locale.ROOT);
        }

        var legacyMatcher = LEGACY_PATTERN.matcher(clean);
        if (legacyMatcher.matches()) {
            return "&" + legacyMatcher.group(1).toLowerCase(Locale.ROOT);
        }

        return null;
    }

    public String getColorDisplayName(String colorCode) {
        if (colorCode == null || colorCode.isBlank()) {
            return "Default";
        }
        if (colorCode.startsWith("&#") || colorCode.startsWith("#")) {
            return colorCode.toUpperCase(Locale.ROOT);
        }
        if (colorCode.startsWith("&") && colorCode.length() == 2) {
            char c = Character.toLowerCase(colorCode.charAt(1));
            return switch (c) {
                case '0' -> "Black";
                case '1' -> "Dark Blue";
                case '2' -> "Dark Green";
                case '3' -> "Dark Aqua";
                case '4' -> "Dark Red";
                case '5' -> "Dark Purple";
                case '6' -> "Gold";
                case '7' -> "Gray";
                case '8' -> "Dark Gray";
                case '9' -> "Blue";
                case 'a' -> "Green";
                case 'b' -> "Aqua";
                case 'c' -> "Red";
                case 'd' -> "Light Purple";
                case 'e' -> "Yellow";
                case 'f' -> "White";
                default -> colorCode;
            };
        }
        return colorCode;
    }
}
