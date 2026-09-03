package me.pinkysha.easychat;

import me.pinkysha.easychat.chat.ChatColorManager;
import me.pinkysha.easychat.chat.ChatFormatter;
import me.pinkysha.easychat.chat.ChatListener;
import me.pinkysha.easychat.chat.ChatManager;
import me.pinkysha.easychat.chat.MessageCooldownManager;
import me.pinkysha.easychat.chat.PrivateMessageManager;
import me.pinkysha.easychat.color.AdventureColorParser;
import me.pinkysha.easychat.color.ColorParser;
import me.pinkysha.easychat.command.ChatColorCommand;
import me.pinkysha.easychat.command.EasyChatCommand;
import me.pinkysha.easychat.command.IgnoreCommand;
import me.pinkysha.easychat.command.MsgCommand;
import me.pinkysha.easychat.command.MsgToggleCommand;
import me.pinkysha.easychat.command.MuteCommand;
import me.pinkysha.easychat.command.ReplyCommand;
import me.pinkysha.easychat.command.UnmuteCommand;
import me.pinkysha.easychat.config.ConfigManager;
import me.pinkysha.easychat.database.DatabaseManager;
import me.pinkysha.easychat.database.MuteRepository;
import me.pinkysha.easychat.database.UserSettingsRepository;
import me.pinkysha.easychat.discord.DiscordSRVBridge;
import me.pinkysha.easychat.group.GroupWeightProvider;
import me.pinkysha.easychat.group.LuckPermsGroupWeightProvider;
import me.pinkysha.easychat.moderation.Mute;
import me.pinkysha.easychat.moderation.MuteManager;
import me.pinkysha.easychat.moderation.MuteScheduler;
import me.pinkysha.easychat.network.NetworkBridge;
import me.pinkysha.easychat.permission.PermissionManager;
import me.pinkysha.easychat.util.EasyChatPlaceholderExpansion;
import me.pinkysha.easychat.util.PlaceholderService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EasyChat extends JavaPlugin {
    private ExecutorService asyncExecutor;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private MuteRepository muteRepository;
    private UserSettingsRepository userSettingsRepository;
    private GroupWeightProvider weightProvider;
    private PermissionManager permissionManager;
    private MessageCooldownManager cooldownManager;
    private ChatColorManager chatColorManager;
    private PrivateMessageManager privateMessageManager;
    private MuteManager muteManager;
    private MuteScheduler muteScheduler;
    private ChatManager chatManager;
    private PlaceholderService placeholderService;
    private DiscordSRVBridge discordSRVBridge;
    private NetworkBridge networkBridge;
    private final ColorParser colorParser = new AdventureColorParser();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getDataFolder().mkdirs();

        asyncExecutor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "EasyChat-DB");
            thread.setDaemon(true);
            return thread;
        });

        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this);

        try {
            databaseManager.connect();
        } catch (Exception e) {
            getLogger().severe("Database startup failed: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        reloadWeights();
        permissionManager = new PermissionManager();
        cooldownManager = new MessageCooldownManager();
        muteRepository = new MuteRepository(databaseManager);
        userSettingsRepository = new UserSettingsRepository(databaseManager);
        chatColorManager = new ChatColorManager(this, userSettingsRepository);
        muteManager = new MuteManager(this, muteRepository, weightProvider);
        muteScheduler = new MuteScheduler(this, muteManager);
        privateMessageManager = new PrivateMessageManager(this, permissionManager, muteManager);
        placeholderService = new PlaceholderService(this);
        discordSRVBridge = new DiscordSRVBridge(this);

        getLogger().info("DiscordSRV: " + (discordSRVBridge.isAvailable() ? "enabled" : "disabled"));

        ChatFormatter chatFormatter = new ChatFormatter(colorParser, placeholderService, permissionManager, chatColorManager);
        chatManager = new ChatManager(this, permissionManager, muteManager, chatFormatter, cooldownManager);

        getLogger().info("PlaceholderAPI: " + (placeholderService.isEnabled() ? "enabled" : "disabled"));
        if (placeholderService.isEnabled()) {
            new EasyChatPlaceholderExpansion(this, chatColorManager, permissionManager).register();
        }

        chatManager.reload();

        networkBridge = new NetworkBridge(this, permissionManager, chatFormatter);
        networkBridge.setPrivateMessageManager(privateMessageManager);
        networkBridge.reload();
        getLogger().info("Cross-server messaging (Velocity): " + (getConfig().getBoolean("network.enabled", false) ? "enabled" : "disabled"));

        getServer().getPluginManager().registerEvents(
                new ChatListener(this, chatManager, permissionManager, muteManager, cooldownManager),
                this
        );

        registerCommands();

        chatColorManager.load();

        muteManager.load().thenAccept(mutes -> getServer().getScheduler().runTask(this, () -> {
            for (Mute mute : mutes) {
                if (mute.isExpired(Instant.now())) {
                    muteManager.unmute(mute.playerUuid());
                } else if (mute.expiresAt() != null) {
                    muteScheduler.schedule(mute.playerUuid(), mute.expiresAt());
                }
            }
        }));

        getLogger().info("EasyChat enabled.");
    }

    public void reloadWeights() {
        Map<String, Integer> fallback = new HashMap<>();
        var section = getConfig().getConfigurationSection("weights.permissions");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                fallback.put(key, section.getInt(key));
            }
        }

        weightProvider = new LuckPermsGroupWeightProvider(fallback, getConfig().getInt("weights.default", 0));
        if (muteManager != null) {
            muteManager.setWeightProvider(weightProvider);
        }
    }

    private void registerCommands() {
        PluginCommand easy = getCommand("easychat");
        if (easy != null) {
            EasyChatCommand cmd = new EasyChatCommand(this);
            easy.setExecutor(cmd);
            easy.setTabCompleter(cmd);
        }

        PluginCommand mute = getCommand("mute");
        if (mute != null) {
            MuteCommand cmd = new MuteCommand(this, muteScheduler);
            mute.setExecutor(cmd);
            mute.setTabCompleter(cmd);
        }

        PluginCommand unmute = getCommand("unmute");
        if (unmute != null) {
            UnmuteCommand cmd = new UnmuteCommand(this, muteScheduler);
            unmute.setExecutor(cmd);
            unmute.setTabCompleter(cmd);
        }

        PluginCommand chatColor = getCommand("chatcolor");
        if (chatColor != null) {
            ChatColorCommand cmd = new ChatColorCommand(this, chatColorManager, permissionManager);
            chatColor.setExecutor(cmd);
            chatColor.setTabCompleter(cmd);
        }

        PluginCommand msg = getCommand("msg");
        if (msg != null) {
            MsgCommand cmd = new MsgCommand(this, privateMessageManager);
            msg.setExecutor(cmd);
            msg.setTabCompleter(cmd);
        }

        PluginCommand reply = getCommand("reply");
        if (reply != null) {
            ReplyCommand cmd = new ReplyCommand(this, privateMessageManager);
            reply.setExecutor(cmd);
            reply.setTabCompleter(cmd);
        }

        PluginCommand msgToggle = getCommand("msgtoggle");
        if (msgToggle != null) {
            MsgToggleCommand cmd = new MsgToggleCommand(this, privateMessageManager);
            msgToggle.setExecutor(cmd);
            msgToggle.setTabCompleter(cmd);
        }

        PluginCommand ignore = getCommand("ignore");
        if (ignore != null) {
            IgnoreCommand cmd = new IgnoreCommand(this, privateMessageManager);
            ignore.setExecutor(cmd);
            ignore.setTabCompleter(cmd);
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        configManager.reload();
        reloadWeights();
        chatManager.reload();
        networkBridge.reload();
    }

    public DatabaseManager databaseManager() {
        return databaseManager;
    }

    public MuteManager muteManager() {
        return muteManager;
    }

    public ChatColorManager chatColorManager() {
        return chatColorManager;
    }

    public PrivateMessageManager privateMessageManager() {
        return privateMessageManager;
    }

    public ExecutorService asyncExecutor() {
        return asyncExecutor;
    }

    public DiscordSRVBridge discordSRVBridge() {
        return discordSRVBridge;
    }

    public NetworkBridge networkBridge() {
        return networkBridge;
    }

    public ColorParser colorParser() {
        return colorParser;
    }

    public Component message(String path) {
        String raw = getConfig().getString(path, "");
        return colorParser.parse(raw);
    }

    public Component messageRaw(String raw) {
        return colorParser.parse(raw);
    }

    public Component message(Player player, String path) {
        String raw = getConfig().getString(path, "");
        return colorParser.parse(placeholderService.parse(player, raw));
    }

    public Component replace(Component component, String placeholder, String value) {
        return component.replaceText(TextReplacementConfig.builder()
                .matchLiteral(placeholder)
                .replacement(value)
                .build());
    }

    @Override
    public void onDisable() {
        if (networkBridge != null) {
            networkBridge.shutdown();
        }
        if (muteScheduler != null) {
            muteScheduler.cancelAll();
        }
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("EasyChat disabled.");
    }
}
