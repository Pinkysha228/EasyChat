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
                .replace("{player}", player.getName())
                .replace("{message}", message.rawMessage());
        text = placeholders.parse(player, text);
        return parser.parse(text);
    }
}
