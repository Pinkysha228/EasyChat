package me.pinkysha.easychat.chat;

import me.pinkysha.easychat.color.ColorParser;
import me.pinkysha.easychat.permission.PermissionManager;
import me.pinkysha.easychat.util.PlaceholderService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class ChatFormatter {
    private final ColorParser parser;
    private final PlaceholderService placeholders;
    private final PermissionManager permissions;
    private final ChatColorManager chatColorManager;

    public ChatFormatter(ColorParser parser,
                         PlaceholderService placeholders,
                         PermissionManager permissions,
                         ChatColorManager chatColorManager) {
        this.parser = parser;
        this.placeholders = placeholders;
        this.permissions = permissions;
        this.chatColorManager = chatColorManager;
    }

    public Component format(Player player, ChatMessage message) {
        String template = message.channel().format()
                .replace("{player}", player.getName());

        template = placeholders.parse(player, template);

        String raw = message.rawMessage();
        if (chatColorManager != null) {
            String defaultColor = chatColorManager.getColor(player.getUniqueId());
            if (defaultColor != null && !defaultColor.isBlank()) {
                raw = defaultColor + raw;
            }
        }

        String filteredMessage = permissions.applyColorPermissions(player, raw);

        if (template.contains("{message}")) {
            int index = template.indexOf("{message}");
            String before = template.substring(0, index);
            String after = template.substring(index + "{message}".length());

            Component beforeComp = parser.parse(before);
            Component msgComp = parser.parseFiltered(filteredMessage);
            Component afterComp = parser.parse(after);

            return beforeComp.append(msgComp).append(afterComp);
        }

        return parser.parse(template);
    }

    public Component formatRemote(ChatChannel channel, String tagFormat, String serverName, String senderName, String rawMessage) {
        String tag = (tagFormat == null || tagFormat.isEmpty()) ? "" : tagFormat.replace("{server}", serverName);
        String template = tag + channel.format().replace("{player}", senderName);

        if (template.contains("{message}")) {
            int index = template.indexOf("{message}");
            String before = template.substring(0, index);
            String after = template.substring(index + "{message}".length());

            Component beforeComp = parser.parse(before);
            Component msgComp = parser.parseFiltered(rawMessage);
            Component afterComp = parser.parse(after);

            return beforeComp.append(msgComp).append(afterComp);
        }

        return parser.parse(template);
    }
}
