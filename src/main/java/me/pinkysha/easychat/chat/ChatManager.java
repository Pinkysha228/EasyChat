package me.pinkysha.easychat.chat;

import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.moderation.MuteManager;
import me.pinkysha.easychat.permission.PermissionManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;

public final class ChatManager {
    private final EasyChat plugin;
    private final PermissionManager permissions;
    private final MuteManager muteManager;
    private final ChatFormatter formatter;
    private final MessageCooldownManager cooldowns;
    private final Map<ChatChannel, ChatChannel> channels = new EnumMap<>(ChatChannel.class);

    public ChatManager(EasyChat plugin,
                       PermissionManager permissions,
                       MuteManager muteManager,
                       ChatFormatter formatter,
                       MessageCooldownManager cooldowns) {
        this.plugin = plugin;
        this.permissions = permissions;
        this.muteManager = muteManager;
        this.formatter = formatter;
        this.cooldowns = cooldowns;
        for (ChatChannel c : ChatChannel.values()) {
            channels.put(c, c);
        }
    }

    public void reload() {
        var c = plugin.getConfig();
        configure(ChatChannel.LOCAL,
                c.getString("chat.local.symbol", ""),
                c.getString("chat.local.permission", "easychat.chat.local"),
                c.getString("chat.local.format", "{player}: {message}"),
                c.getDouble("chat.local.radius", 100.0),
                c.getBoolean("chat.local.enabled", true));
        configure(ChatChannel.GLOBAL,
                c.getString("chat.global.symbol", "!"),
                c.getString("chat.global.permission", "easychat.chat.global"),
                c.getString("chat.global.format", "&6[G] &f{player}&7: &r{message}"),
                c.getDouble("chat.global.radius", -1.0),
                c.getBoolean("chat.global.enabled", true));
        configure(ChatChannel.ADMIN,
                c.getString("chat.admin.symbol", "%"),
                c.getString("chat.admin.permission", "easychat.chat.admin"),
                c.getString("chat.admin.format", "&c[ADMIN] &f{player}&7: &r{message}"),
                c.getDouble("chat.admin.radius", -1.0),
                c.getBoolean("chat.admin.enabled", true));
        cooldowns.setCooldownSeconds(c.getLong("chat.cooldown.seconds", 3));
    }

    private void configure(ChatChannel channel, String symbol, String permission, String format, double radius, boolean enabled) {
        channel.configure(symbol, permission, format, radius, enabled);
    }

    public ChatChannel resolveChannel(String text) {
        for (ChatChannel channel : ChatChannel.values()) {
            if (channel.enabled() && !channel.symbol().isEmpty() && text.startsWith(channel.symbol())) {
                return channel;
            }
        }
        return ChatChannel.LOCAL;
    }

    public String stripSymbol(String text, ChatChannel channel) {
        String symbol = channel.symbol();
        return symbol.isEmpty() ? text : text.substring(symbol.length()).stripLeading();
    }

    public boolean canUse(Player player, ChatChannel channel) {
        return channel.enabled() && permissions.canUse(player, channel);
    }

    public void send(Player sender, ChatMessage message) {
        Component formatted = formatter.format(sender, message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canReceive(player, sender, message.channel())) {
                continue;
            }
            player.sendMessage(formatted);
        }

        // Console is intentionally logged in a readable plain-text form.
        plugin.getLogger().info("[CHAT] [" + message.channel().id().toUpperCase() + "] "
                + sender.getName() + ": " + message.rawMessage());

        // Paper chat is cancelled by EasyChat, so DiscordSRV needs the message explicitly.
        plugin.discordSRVBridge().forward(sender, message);
        plugin.networkBridge().forward(sender, message);
    }

    private boolean canReceive(Player receiver, Player sender, ChatChannel channel) {
        if (!permissions.canUse(receiver, channel)) {
            return false;
        }

        double radius = channel.radius();
        if (radius < 0) {
            return true;
        }

        return receiver.getWorld().equals(sender.getWorld())
                && receiver.getLocation().distanceSquared(sender.getLocation()) <= radius * radius;
    }
}
