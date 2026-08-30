package me.pinkysha.easychat.chat;

import me.pinkysha.easychat.color.ColorParser;
import me.pinkysha.easychat.util.PlaceholderService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class ChatFormatter {
    private final ColorParser parser;
    private final PlaceholderService placeholders;

    public ChatFormatter(ColorParser parser, PlaceholderService placeholders) {
        this.parser = parser;
        this.placeholders = placeholders;
    }

    public Component format(Player player, ChatMessage message) {
        String text = message.channel().format()
                .replace("{player}", player.getName());

        text = placeholders.parse(player, text);

        text = text.replace("{message}", message.rawMessage());

        return parser.parse(text);
    }

    public Component formatRemote(ChatChannel channel, String tagFormat, String serverName, String senderName, String rawMessage) {
        String tag = (tagFormat == null || tagFormat.isEmpty()) ? "" : tagFormat.replace("{server}", serverName);
        String text = tag + channel.format()
                .replace("{player}", senderName)
                .replace("{message}", rawMessage);

        return parser.parse(text);
    }
}
