package me.pinkysha.easychat.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class PlaceholderService {
    private final Plugin plugin;
    private final boolean enabled;

    public PlaceholderService(Plugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("placeholders.enabled", true)
                && plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String parse(Player player, String text) {
        if (!enabled || player == null || text == null || text.isEmpty()) {
            return text;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Throwable ignored) {
            plugin.getLogger().warning("Failed to process PlaceholderAPI placeholders.");
            return text;
        }
    }
}
