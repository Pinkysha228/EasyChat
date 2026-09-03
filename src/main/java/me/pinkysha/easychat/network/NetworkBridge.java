package me.pinkysha.easychat.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.chat.ChatChannel;
import me.pinkysha.easychat.chat.ChatFormatter;
import me.pinkysha.easychat.chat.ChatMessage;
import me.pinkysha.easychat.chat.PrivateMessageManager;
import me.pinkysha.easychat.permission.PermissionManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Forwards global, admin, and private chat messages across servers in the network
 * using a custom plugin-messaging channel ("easychat:notify").
 * The channel is handled by the proxy-side EasyChatBridge plugin, which distributes
 * messages to the appropriate backend servers.
 */
public final class NetworkBridge implements PluginMessageListener {
    private static final String NETWORK_CHANNEL = "easychat:notify";
    private static final byte PROTOCOL_VERSION = 1;
    private static final String PM_CHANNEL_ID = "__PM__";
    private static final String PM_ACK_CHANNEL_ID = "__PM_ACK__";

    private final EasyChat plugin;
    private final PermissionManager permissions;
    private final ChatFormatter formatter;
    private PrivateMessageManager pmManager;

    private final Map<ChatChannel, Boolean> forward = new EnumMap<>(ChatChannel.class);
    private boolean enabled;
    private boolean registered;
    private String secret;
    private String serverId;
    private String serverName;
    private String tagFormat;

    public NetworkBridge(EasyChat plugin, PermissionManager permissions, ChatFormatter formatter) {
        this.plugin = plugin;
        this.permissions = permissions;
        this.formatter = formatter;
    }

    public void setPrivateMessageManager(PrivateMessageManager pmManager) {
        this.pmManager = pmManager;
    }

    public void reload() {
        var c = plugin.getConfig();

        if (serverId == null) {
            String configured = c.getString("network.server-id", "");
            serverId = configured.isBlank() ? UUID.randomUUID().toString() : configured;
        }

        serverName = c.getString("network.server-name", "server");
        tagFormat = c.getString("network.tag-format", "&8[{server}]&r ");

        // Local chat is intentionally never forwarded outside the current server.
        forward.put(ChatChannel.LOCAL, false);
        forward.put(ChatChannel.GLOBAL, c.getBoolean("network.forward.global", true));
        forward.put(ChatChannel.ADMIN, c.getBoolean("network.forward.admin", true));

        boolean wantEnabled = c.getBoolean("network.enabled", false);
        if (!wantEnabled) {
            enabled = false;
            unregister();
            return;
        }

        if (secret == null) {
            secret = NetworkSecret.loadOrCreate(plugin);
        }

        if (secret == null) {
            enabled = false;
            unregister();
            return;
        }

        enabled = true;
        register();
    }

    private void register() {
        if (registered) {
            return;
        }

        var messenger = plugin.getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(plugin, NETWORK_CHANNEL);
        messenger.registerIncomingPluginChannel(plugin, NETWORK_CHANNEL, this);
        registered = true;
    }

    private void unregister() {
        if (!registered) {
            return;
        }

        var messenger = plugin.getServer().getMessenger();
        messenger.unregisterOutgoingPluginChannel(plugin, NETWORK_CHANNEL);
        messenger.unregisterIncomingPluginChannel(plugin, NETWORK_CHANNEL, this);
        registered = false;
    }

    public void shutdown() {
        unregister();
    }

    public void forward(Player sender, ChatMessage message) {
        if (!enabled || !Boolean.TRUE.equals(forward.get(message.channel()))) {
            return;
        }

        ByteArrayDataOutput payload = ByteStreams.newDataOutput();
        payload.writeByte(PROTOCOL_VERSION);
        payload.writeUTF(secret);
        payload.writeUTF(serverId);
        payload.writeUTF(serverName);
        payload.writeUTF(message.channel().id());
        payload.writeUTF(sender.getName());
        payload.writeUTF(message.rawMessage());

        sender.sendPluginMessage(plugin, NETWORK_CHANNEL, payload.toByteArray());

        plugin.getLogger().info("[NET] Forwarded to network: ["
                + message.channel().id().toUpperCase() + "] "
                + sender.getName());
    }

    public void forwardPrivateMessage(Player sender, String targetPlayerName, String rawMessage) {
        if (!enabled || !plugin.getConfig().getBoolean("private-messages.network-forward", true)) {
            Component msg = plugin.message("private-messages.messages.player-not-found");
            sender.sendMessage(plugin.replace(msg, "{player}", targetPlayerName));
            return;
        }

        String filtered = permissions.applyColorPermissions(sender, rawMessage);

        ByteArrayDataOutput payload = ByteStreams.newDataOutput();
        payload.writeByte(PROTOCOL_VERSION);
        payload.writeUTF(secret);
        payload.writeUTF(serverId);
        payload.writeUTF(serverName);
        payload.writeUTF(PM_CHANNEL_ID);
        payload.writeUTF(sender.getName());
        payload.writeUTF(sender.getUniqueId().toString());
        payload.writeUTF(targetPlayerName);
        payload.writeUTF(filtered);

        sender.sendPluginMessage(plugin, NETWORK_CHANNEL, payload.toByteArray());
    }

    private void sendPmAck(String originalServerId, UUID senderUuid, String senderName, String targetName, String filteredMessage) {
        Player anyPlayer = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (anyPlayer == null) {
            return;
        }

        ByteArrayDataOutput payload = ByteStreams.newDataOutput();
        payload.writeByte(PROTOCOL_VERSION);
        payload.writeUTF(secret);
        payload.writeUTF(serverId);
        payload.writeUTF(serverName);
        payload.writeUTF(PM_ACK_CHANNEL_ID);
        payload.writeUTF(originalServerId);
        payload.writeUTF(senderUuid.toString());
        payload.writeUTF(senderName);
        payload.writeUTF(targetName);
        payload.writeUTF(filteredMessage);

        anyPlayer.sendPluginMessage(plugin, NETWORK_CHANNEL, payload.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!NETWORK_CHANNEL.equals(channel)) {
            return;
        }

        if (!enabled) {
            return;
        }

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);

            if (in.readByte() != PROTOCOL_VERSION) {
                return;
            }

            if (!in.readUTF().equals(secret)) {
                plugin.getLogger().warning(
                        "Network chat packet rejected: the provided network secret does not match. "
                                + "Ensure that plugins/EasyChat/network.secret is copied from one server "
                                + "to all other servers in the network."
                );
                return;
            }

            String remoteServerId = in.readUTF();
            if (remoteServerId.equals(serverId)) {
                return;
            }

            String remoteServerName = in.readUTF();
            String channelId = in.readUTF();

            if (PM_CHANNEL_ID.equals(channelId)) {
                String senderName = in.readUTF();
                UUID senderUuid = UUID.fromString(in.readUTF());
                String targetPlayerName = in.readUTF();
                String filteredMessage = in.readUTF();

                Player target = Bukkit.getPlayerExact(targetPlayerName);
                if (target != null && pmManager != null) {
                    pmManager.deliverRemotePrivateMessage(senderName, senderUuid, target, filteredMessage);
                    sendPmAck(remoteServerId, senderUuid, senderName, target.getName(), filteredMessage);
                }
                return;
            }

            if (PM_ACK_CHANNEL_ID.equals(channelId)) {
                String targetServerId = in.readUTF();
                if (!targetServerId.equals(serverId)) {
                    return;
                }
                UUID senderUuid = UUID.fromString(in.readUTF());
                String senderName = in.readUTF();
                String targetName = in.readUTF();
                String filteredMessage = in.readUTF();

                Player sender = Bukkit.getPlayer(senderUuid);
                if (sender != null && pmManager != null) {
                    String senderFormat = plugin.getConfig().getString("private-messages.format.sender", "&7[&6Me &7-> &6{receiver}&7] &f{message}");
                    Component msgComp = plugin.colorParser().parseFiltered(filteredMessage);
                    String base = senderFormat.replace("{sender}", senderName).replace("{receiver}", targetName);
                    Component senderView;
                    if (base.contains("{message}")) {
                        int index = base.indexOf("{message}");
                        Component before = plugin.messageRaw(base.substring(0, index));
                        Component after = plugin.messageRaw(base.substring(index + "{message}".length()));
                        senderView = before.append(msgComp).append(after);
                    } else {
                        senderView = plugin.messageRaw(base);
                    }
                    sender.sendMessage(senderView);
                    pmManager.setReplyTarget(senderUuid, targetName);
                }
                return;
            }

            ChatChannel target = resolveChannel(channelId);
            String senderName = in.readUTF();
            String rawMessage = in.readUTF();

            if (target == null) {
                plugin.getLogger().warning("Network chat packet rejected: unknown chat channel.");
                return;
            }

            if (!target.enabled()) {
                plugin.getLogger().warning("Network chat packet (" + target.id()
                        + ") rejected: chat." + target.id() + ".enabled=false on this server.");
                return;
            }

            if (!Boolean.TRUE.equals(forward.get(target))) {
                plugin.getLogger().warning("Network chat packet (" + target.id()
                        + ") rejected: network.forward." + target.id() + "=false on this server.");
                return;
            }

            broadcast(target, remoteServerName, senderName, rawMessage);

        } catch (Exception e) {
            plugin.getLogger().warning("Malformed network chat packet received and discarded: " + e.getMessage());
        }
    }

    private ChatChannel resolveChannel(String id) {
        for (ChatChannel c : ChatChannel.values()) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return null;
    }

    private void broadcast(ChatChannel channel, String remoteServerName, String senderName, String rawMessage) {
        Component formatted = formatter.formatRemote(
                channel,
                tagFormat,
                remoteServerName,
                senderName,
                rawMessage
        );

        for (Player receiver : Bukkit.getOnlinePlayers()) {
            if (permissions.canUse(receiver, channel)) {
                receiver.sendMessage(formatted);
            }
        }

        plugin.getLogger().info("[NET] [" + channel.id().toUpperCase()
                + "@" + remoteServerName + "] "
                + senderName + ": " + rawMessage);
    }
}
