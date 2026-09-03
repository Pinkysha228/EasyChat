package me.pinkysha.easychat.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.util.WebhookUtil;
import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.chat.ChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Optional DiscordSRV integration.
 */
public final class DiscordSRVBridge {
    private final EasyChat plugin;

    public DiscordSRVBridge(EasyChat plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        Plugin discord = plugin.getServer()
                .getPluginManager()
                .getPlugin("DiscordSRV");

        return discord != null
                && discord.isEnabled()
                && plugin.getConfig().getBoolean("discordsrv.enabled", true);
    }

    public void forward(Player sender, ChatMessage message) {
        if (!isAvailable()
                || !plugin.getConfig().getBoolean("discordsrv.webhook", true)
                || !plugin.getConfig().getBoolean("discordsrv.channels." + message.channel().id(), false)) {
            return;
        }

        try {
            DiscordSRV discord = DiscordSRV.getPlugin();

            var channel = discord.getDestinationTextChannelForGameChannelName(
                    message.channel().id()
            );

            if (channel == null) {
                channel = discord.getMainTextChannel();
            }

            if (channel == null) {
                plugin.getLogger().warning("DiscordSRV is enabled, but no destination text channel was found.");
                return;
            }

            String text = message.rawMessage();
            String avatarUrl = DiscordSRV.getAvatarUrl(sender);

            WebhookUtil.deliverMessage(
                    channel,
                    sender.getName(),
                    avatarUrl,
                    text,
                    (MessageEmbed) null
            );

        } catch (Throwable throwable) {
            plugin.getLogger().warning("Failed to forward chat to DiscordSRV: " + throwable.getMessage());
        }
    }
}
