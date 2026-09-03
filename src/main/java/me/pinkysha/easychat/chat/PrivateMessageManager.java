package me.pinkysha.easychat.chat;

import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.moderation.Mute;
import me.pinkysha.easychat.moderation.MuteManager;
import me.pinkysha.easychat.permission.PermissionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PrivateMessageManager {
    private final EasyChat plugin;
    private final PermissionManager permissions;
    private final MuteManager muteManager;

    private final Map<UUID, String> lastRepliedName = new ConcurrentHashMap<>();
    private final Set<UUID> pmDisabled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<UUID>> ignoredPlayers = new ConcurrentHashMap<>();

    private final LegacyComponentSerializer sectionSerializer = LegacyComponentSerializer.builder()
            .hexColors()
            .character('§')
            .build();

    public PrivateMessageManager(EasyChat plugin, PermissionManager permissions, MuteManager muteManager) {
        this.plugin = plugin;
        this.permissions = permissions;
        this.muteManager = muteManager;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("private-messages.enabled", true);
    }

    public boolean isToggledOff(UUID playerUuid) {
        return pmDisabled.contains(playerUuid);
    }

    public boolean toggleMessages(UUID playerUuid) {
        if (pmDisabled.contains(playerUuid)) {
            pmDisabled.remove(playerUuid);
            return true; // enabled now
        } else {
            pmDisabled.add(playerUuid);
            return false; // disabled now
        }
    }

    public boolean isIgnoring(UUID targetUuid, UUID senderUuid) {
        Set<UUID> ignored = ignoredPlayers.get(targetUuid);
        return ignored != null && ignored.contains(senderUuid);
    }

    public boolean toggleIgnore(UUID playerUuid, UUID targetUuid) {
        Set<UUID> set = ignoredPlayers.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet());
        if (set.contains(targetUuid)) {
            set.remove(targetUuid);
            return false; // unignored
        } else {
            set.add(targetUuid);
            return true; // ignored
        }
    }

    public void setReplyTarget(UUID playerUuid, String targetName) {
        lastRepliedName.put(playerUuid, targetName);
    }

    public String getReplyTarget(UUID playerUuid) {
        return lastRepliedName.get(playerUuid);
    }

    public void sendLocalPrivateMessage(Player sender, Player receiver, String rawMessage) {
        UUID senderUuid = sender.getUniqueId();
        UUID receiverUuid = receiver.getUniqueId();

        if (senderUuid.equals(receiverUuid)) {
            sender.sendMessage(plugin.message("private-messages.messages.cannot-msg-self"));
            return;
        }

        if (isToggledOff(senderUuid)) {
            sender.sendMessage(plugin.message("private-messages.messages.pm-disabled-sender"));
            return;
        }

        if (isToggledOff(receiverUuid) && !sender.hasPermission("easychat.admin")) {
            Component msg = plugin.message("private-messages.messages.pm-disabled-target");
            sender.sendMessage(plugin.replace(msg, "{player}", receiver.getName()));
            return;
        }

        if (isIgnoring(receiverUuid, senderUuid) && !sender.hasPermission("easychat.admin")) {
            Component msg = plugin.message("private-messages.messages.player-ignored-you");
            sender.sendMessage(plugin.replace(msg, "{player}", receiver.getName()));
            return;
        }

        Mute mute = muteManager.get(senderUuid);
        if (mute != null && !mute.isExpired(Instant.now())
                && !permissions.canBypassMute(sender, plugin.getConfig().getString("moderation.bypass-permission"))) {
            String path = mute.reason().isBlank() ? "moderation.messages.muted-no-reason" : "moderation.messages.muted";
            sender.sendMessage(plugin.replace(plugin.message(path), "{reason}", mute.reason()));
            return;
        }

        // Apply colors
        String filtered = permissions.applyColorPermissions(sender, rawMessage);
        Component msgComponent = sectionSerializer.deserialize(filtered);

        // Sender format
        String senderFormat = plugin.getConfig().getString("private-messages.format.sender", "&7[&6Me &7-> &6{receiver}&7] &f{message}");
        Component senderView = formatPm(senderFormat, sender.getName(), receiver.getName(), msgComponent);
        sender.sendMessage(senderView);

        // Receiver format
        String receiverFormat = plugin.getConfig().getString("private-messages.format.receiver", "&7[&6{sender} &7-> &6Me&7] &f{message}");
        Component receiverView = formatPm(receiverFormat, sender.getName(), receiver.getName(), msgComponent);
        receiver.sendMessage(receiverView);

        // Update reply targets
        setReplyTarget(senderUuid, receiver.getName());
        setReplyTarget(receiverUuid, sender.getName());

        // Spy
        broadcastSpy(sender, receiver.getName(), msgComponent);

        plugin.getLogger().info("[PM] " + sender.getName() + " -> " + receiver.getName() + ": " + rawMessage);
    }

    public void deliverRemotePrivateMessage(String senderName, UUID senderUuid, Player receiver, String filteredMessage) {
        UUID receiverUuid = receiver.getUniqueId();

        if (isToggledOff(receiverUuid)) {
            return;
        }

        if (isIgnoring(receiverUuid, senderUuid)) {
            return;
        }

        Component msgComponent = sectionSerializer.deserialize(filteredMessage);

        String receiverFormat = plugin.getConfig().getString("private-messages.format.receiver", "&7[&6{sender} &7-> &6Me&7] &f{message}");
        Component receiverView = formatPm(receiverFormat, senderName, receiver.getName(), msgComponent);
        receiver.sendMessage(receiverView);

        setReplyTarget(receiverUuid, senderName);

        // Spy on this server
        String spyFormat = plugin.getConfig().getString("private-messages.format.spy", "&8[SPY] &7[{sender} -> {receiver}]: &f{message}");
        Component spyComponent = formatPm(spyFormat, senderName, receiver.getName(), msgComponent);
        String spyPerm = plugin.getConfig().getString("private-messages.spy-permission", "easychat.pm.spy");

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(spyPerm) && !p.getUniqueId().equals(receiverUuid)) {
                p.sendMessage(spyComponent);
            }
        }

        plugin.getLogger().info("[NET-PM] " + senderName + " -> " + receiver.getName() + ": " + filteredMessage);
    }

    private Component formatPm(String template, String senderName, String receiverName, Component messageComponent) {
        String base = template.replace("{sender}", senderName).replace("{receiver}", receiverName);
        if (base.contains("{message}")) {
            int index = base.indexOf("{message}");
            String before = base.substring(0, index);
            String after = base.substring(index + "{message}".length());

            Component beforeComp = plugin.messageRaw(before);
            Component afterComp = plugin.messageRaw(after);

            return beforeComp.append(messageComponent).append(afterComp);
        }
        return plugin.messageRaw(base);
    }

    private void broadcastSpy(Player sender, String receiverName, Component messageComponent) {
        String spyFormat = plugin.getConfig().getString("private-messages.format.spy", "&8[SPY] &7[{sender} -> {receiver}]: &f{message}");
        Component spyComponent = formatPm(spyFormat, sender.getName(), receiverName, messageComponent);
        String spyPerm = plugin.getConfig().getString("private-messages.spy-permission", "easychat.pm.spy");

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(spyPerm) && !p.equals(sender) && !p.getName().equalsIgnoreCase(receiverName)) {
                p.sendMessage(spyComponent);
            }
        }
    }
}
