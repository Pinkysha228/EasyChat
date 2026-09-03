package me.pinkysha.easychat.config;

import me.pinkysha.easychat.EasyChat;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigManager {
    private final EasyChat plugin;
    private FileConfiguration config;

    public ConfigManager(EasyChat plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public FileConfiguration get() {
        return config;
    }
}
