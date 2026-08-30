package me.pinkysha.easychat.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.chat.ChatChannel;
import me.pinkysha.easychat.chat.ChatFormatter;
import me.pinkysha.easychat.chat.ChatMessage;
import me.pinkysha.easychat.permission.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Forwards global and administrative chat messages to other servers within the network
 * using a custom plugin-messaging channel ("easychat:notify"). The channel is handled
 * by the proxy-side NotifyRelay plugin, which distributes messages to the appropriate
 * backend servers.
 *
 * Previously, this class used the standard Velocity "bungeecord:main" channel with
 * the "Forward"/"ALL" mechanism. Network transport is now handled by NotifyRelay,
 * while this class is responsible solely for sending and receiving raw binary packets.
 *
 * The packet uses a compact binary format without JSON or reflection:
 * protocol version (1 byte) + shared secret + server ID + server name + channel +
 * sender name + message content.
 *
 * Each forwarded chat message requires exactly one outgoing plugin message. The message
 * is sent through the sender's own connection, which guarantees that the sender is
 * online at the time of transmission and eliminates the need for a separate carrier player.
 */
public final class NetworkBridge implements PluginMessageListener {
    private static final String NETWORK_CHANNEL = "easychat:notify"; // Must match the "channel=" value configured in NotifyRelay's config.properties
    private static final byte PROTOCOL_VERSION = 1;

    private final EasyChat plugin;
    private final PermissionManager permissions;
    private final ChatFormatter formatter;

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

    public void reload() {
        var c = plugin.getConfig();

        if (serverId == null) {
            String configured = c.getString("network.server-id", "");

            // Do not write an automatically generated ID back to config.yml in order
            // to preserve existing comments when save() is called. A randomly generated
            // ID is sufficient to prevent message loops within the current process.
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
        if (registered) return;

        var messenger = plugin.getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(plugin, NETWORK_CHANNEL);
        messenger.registerIncomingPluginChannel(plugin, NETWORK_CHANNEL, this);
        registered = true;
    }

    private void unregister() {
        if (!registered) return;

        var messenger = plugin.getServer().getMessenger();
        messenger.unregisterOutgoingPluginChannel(plugin, NETWORK_CHANNEL);
        messenger.unregisterIncomingPluginChannel(plugin, NETWORK_CHANNEL, this);
        registered = false;
    }

    public void shutdown() {
        unregister();
    }

    /** Called by ChatManager immediately after the message has been broadcast locally. */
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

        plugin.getLogger().info(
                "[NET] Forwarded to network: ["
                        + message.channel().id().toUpperCase()
                        + "] "
                        + sender.getName()
        );
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!NETWORK_CHANNEL.equals(channel)) {
            return; // Not our channel; silently ignore messages from other plugins.
        }

        if (!enabled) {
            plugin.getLogger().warning(
                    "A network chat packet was received, but network.enabled=false on this server. "
                            + "The packet has been discarded."
            );
            return;
        }

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);

            if (in.readByte() != PROTOCOL_VERSION) {
                // Ignore packets using an unsupported protocol version rather than
                // allowing them to cause an exception or affect server stability.
                return;
            }

            if (!in.readUTF().equals(secret)) {
                plugin.getLogger().warning(
                        "Network chat packet rejected: the provided network secret does not match. "
                                + "Ensure that plugins/EasyChat/network.secret is copied from one server "
                                + "to all other servers in the network. The secret must be identical "
                                + "across all servers and must not be generated independently on each server."
                );
                return;
            }

            String remoteServerId = in.readUTF();

            if (remoteServerId.equals(serverId)) {
                // Ignore packets originating from this server. NotifyRelay is not expected
                // to return packets to their source, but this additional check prevents
                // accidental message loops.
                return;
            }

            String remoteServerName = in.readUTF();
            ChatChannel target = resolveChannel(in.readUTF());
            String senderName = in.readUTF();
            String rawMessage = in.readUTF();

            if (target == null) {
                plugin.getLogger().warning(
                        "Network chat packet rejected: unknown chat channel."
                );
                return;
            }

            if (!target.enabled()) {
                plugin.getLogger().warning(
                        "Network chat packet (" + target.id()
                                + ") rejected: chat." + target.id()
                                + ".enabled=false on this server."
                );
                return;
            }

            if (!Boolean.TRUE.equals(forward.get(target))) {
                plugin.getLogger().warning(
                        "Network chat packet (" + target.id()
                                + ") rejected: network.forward." + target.id()
                                + "=false on this server."
                );
                return;
            }

            broadcast(target, remoteServerName, senderName, rawMessage);

        } catch (Exception e) {
            plugin.getLogger().warning(
                    "Malformed network chat packet received and discarded: " + e.getMessage()
            );
        }
    }

    private ChatChannel resolveChannel(String id) {
        for (ChatChannel c : ChatChannel.values()) {
            if (c.id().equals(id)) return c;
        }
        return null;
    }

    private void broadcast(
            ChatChannel channel,
            String remoteServerName,
            String senderName,
            String rawMessage
    ) {
        var formatted = formatter.formatRemote(
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

        plugin.getLogger().info(
                "[NET] [" + channel.id().toUpperCase()
                        + "@" + remoteServerName + "] "
                        + senderName + ": " + rawMessage
        );
    }
}