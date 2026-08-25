package me.pinkysha.easychat.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.permission.PermissionManager;
import me.pinkysha.easychat.moderation.Mute;
import me.pinkysha.easychat.moderation.MuteManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.Instant;

public final class ChatListener implements Listener {
    private final EasyChat plugin;
    private final ChatManager chatManager;
    private final PermissionManager permissions;
    private final MuteManager muteManager;
    private final MessageCooldownManager cooldowns;

    public ChatListener(EasyChat plugin, ChatManager chatManager, PermissionManager permissions, MuteManager muteManager, MessageCooldownManager cooldowns) {
        this.plugin=plugin;this.chatManager=chatManager;this.permissions=permissions;this.muteManager=muteManager;this.cooldowns=cooldowns;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String raw = PlainTextComponentSerializer.plainText().serialize(event.message());
        ChatChannel channel = chatManager.resolveChannel(raw);
        if (!chatManager.canUse(player, channel)) {
            event.setCancelled(true);
            player.sendMessage(plugin.message("moderation.messages.no-permission"));
            return;
        }
        Mute mute = muteManager.get(player.getUniqueId());
        if (mute != null && !mute.isExpired(Instant.now()) && !permissions.canBypassMute(player, plugin.getConfig().getString("moderation.bypass-permission"))) {
            event.setCancelled(true);
            String path = mute.reason().isBlank() ? "moderation.messages.muted-no-reason" : "moderation.messages.muted";
            player.sendMessage(plugin.message(path).replaceText(net.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral("{reason}").replacement(mute.reason()).build()));
            return;
        }
        if (plugin.getConfig().getBoolean("chat.cooldown.enabled", true) && !permissions.canBypassCooldown(player, plugin.getConfig().getString("chat.cooldown.bypass-permission"))) {
            if (!cooldowns.tryConsume(player.getUniqueId())) {
                event.setCancelled(true);
                long seconds = cooldowns.remainingSeconds(player.getUniqueId());
                player.sendMessage(plugin.message("moderation.messages.cooldown").replaceText(net.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral("{seconds}").replacement(String.valueOf(seconds)).build()));
                return;
            }
        }
        String content = chatManager.stripSymbol(raw, channel);
        if (content.isBlank()) { event.setCancelled(true); return; }
        ChatMessage message = new ChatMessage(player.getUniqueId(), channel, content, Instant.now());
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> chatManager.send(player, message));
    }
}
