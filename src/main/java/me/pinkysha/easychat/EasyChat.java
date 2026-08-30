package me.pinkysha.easychat;

import me.pinkysha.easychat.chat.*;
import me.pinkysha.easychat.color.AdventureColorParser;
import me.pinkysha.easychat.command.*;
import me.pinkysha.easychat.config.ConfigManager;
import me.pinkysha.easychat.database.*;
import me.pinkysha.easychat.group.GroupWeightProvider;
import me.pinkysha.easychat.group.LuckPermsGroupWeightProvider;
import me.pinkysha.easychat.moderation.*;
import me.pinkysha.easychat.permission.PermissionManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import me.pinkysha.easychat.util.PlaceholderService;
import me.pinkysha.easychat.discord.DiscordSRVBridge;
import me.pinkysha.easychat.network.NetworkBridge;

public final class EasyChat extends JavaPlugin {
    private ExecutorService asyncExecutor;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private MuteRepository muteRepository;
    private GroupWeightProvider weightProvider;
    private PermissionManager permissionManager;
    private MessageCooldownManager cooldownManager;
    private MuteManager muteManager;
    private MuteScheduler muteScheduler;
    private ChatManager chatManager;
    private PlaceholderService placeholderService;
    private DiscordSRVBridge discordSRVBridge;
    private NetworkBridge networkBridge;

    @Override public void onEnable() {
        saveDefaultConfig();
        getDataFolder().mkdirs();
        asyncExecutor = Executors.newFixedThreadPool(2, r -> { Thread t = new Thread(r, "EasyChat-DB"); t.setDaemon(true); return t; });
        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this);
        try { databaseManager.connect(); } catch (Exception e) { getLogger().severe("Database startup failed: " + e.getMessage()); getServer().getPluginManager().disablePlugin(this); return; }

        Map<String,Integer> fallback = new HashMap<>();
        var section = getConfig().getConfigurationSection("weights.permissions");
        if (section != null) for (String key: section.getKeys(false)) fallback.put(key, section.getInt(key));
        weightProvider = new LuckPermsGroupWeightProvider(fallback, getConfig().getInt("weights.default", 0));
        permissionManager = new PermissionManager();
        cooldownManager = new MessageCooldownManager();
        muteRepository = new MuteRepository(databaseManager);
        muteManager = new MuteManager(this, muteRepository, weightProvider);
        muteScheduler = new MuteScheduler(this, muteManager);
        placeholderService = new PlaceholderService(this);
        discordSRVBridge = new DiscordSRVBridge(this);
        getLogger().info("DiscordSRV: " + (discordSRVBridge.isAvailable() ? "enabled" : "disabled"));
        ChatFormatter chatFormatter = new ChatFormatter(new AdventureColorParser(), placeholderService);
        chatManager = new ChatManager(this, permissionManager, muteManager, chatFormatter, cooldownManager);
        getLogger().info("PlaceholderAPI: " + (placeholderService.isEnabled() ? "enabled" : "disabled"));
        chatManager.reload();

        networkBridge = new NetworkBridge(this, permissionManager, chatFormatter);
        networkBridge.reload();
        getLogger().info("Межсерверная адресация (Velocity): " + (getConfig().getBoolean("network.enabled", false) ? "enabled" : "disabled"));

        getServer().getPluginManager().registerEvents(new ChatListener(this, chatManager, permissionManager, muteManager, cooldownManager), this);
        registerCommands();
        muteManager.load().thenAccept(mutes -> getServer().getScheduler().runTask(this, () -> {
            for (Mute mute : mutes) {
                if (mute.isExpired(java.time.Instant.now())) {
                    muteManager.unmute(mute.playerUuid());
                } else if (mute.expiresAt() != null) {
                    muteScheduler.schedule(mute.playerUuid(), mute.expiresAt());
                }
            }
        }));
        getLogger().info("EasyChat enabled.");
    }

    private void registerCommands() {
        PluginCommand easy = getCommand("easychat"); if (easy != null) { var c=new EasyChatCommand(this); easy.setExecutor(c); easy.setTabCompleter(c); }
        PluginCommand mute = getCommand("mute"); if (mute != null) { var c=new MuteCommand(this, muteScheduler); mute.setExecutor(c); mute.setTabCompleter(c); }
        PluginCommand unmute = getCommand("unmute"); if (unmute != null) { var c=new UnmuteCommand(this, muteScheduler); unmute.setExecutor(c); unmute.setTabCompleter(c); }
    }

    public void reloadPlugin() { reloadConfig(); configManager.reload(); chatManager.reload(); networkBridge.reload(); }
    public DatabaseManager databaseManager() { return databaseManager; }
    public MuteManager muteManager() { return muteManager; }
    public ExecutorService asyncExecutor() { return asyncExecutor; }
    public DiscordSRVBridge discordSRVBridge() { return discordSRVBridge; }
    public NetworkBridge networkBridge() { return networkBridge; }

    public Component message(String path) {
        String raw = getConfig().getString(path, "");
        return new AdventureColorParser().parse(raw);
    }

    public Component message(org.bukkit.entity.Player player, String path) {
        String raw = getConfig().getString(path, "");
        return new AdventureColorParser().parse(placeholderService.parse(player, raw));
    }

    public Component replace(Component component, String placeholder, String value) {
        return component.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral(placeholder).replacement(value).build());
    }

    @Override public void onDisable() {
        if (networkBridge != null) networkBridge.shutdown();
        if (muteScheduler != null) muteScheduler.cancelAll();
        if (asyncExecutor != null) { asyncExecutor.shutdown(); }
        if (databaseManager != null) databaseManager.close();
        getLogger().info("EasyChat disabled.");
    }
}
