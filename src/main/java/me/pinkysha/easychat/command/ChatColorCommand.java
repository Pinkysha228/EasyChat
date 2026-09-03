package me.pinkysha.easychat.command;

import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.chat.ChatColorManager;
import me.pinkysha.easychat.permission.PermissionManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ChatColorCommand implements CommandExecutor, TabCompleter {
    private final EasyChat plugin;
    private final ChatColorManager colorManager;
    private final PermissionManager permissions;

    public ChatColorCommand(EasyChat plugin, ChatColorManager colorManager, PermissionManager permissions) {
        this.plugin = plugin;
        this.colorManager = colorManager;
        this.permissions = permissions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getConfig().getBoolean("chatcolor.enabled", true)) {
            sender.sendMessage(plugin.message("chatcolor.messages.disabled"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be executed by players."));
            return true;
        }

        String requiredPerm = plugin.getConfig().getString("chatcolor.permission", "easychat.chatcolor");
        if (!player.hasPermission(requiredPerm) && !player.hasPermission("easychat.admin")) {
            player.sendMessage(plugin.message("moderation.messages.no-permission"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(plugin.message("chatcolor.messages.usage"));
            return true;
        }

        String input = args[0];
        if (input.equalsIgnoreCase("reset") || input.equalsIgnoreCase("clear") || input.equalsIgnoreCase("off")) {
            colorManager.resetColor(player.getUniqueId());
            player.sendMessage(plugin.message("chatcolor.messages.reset"));
            return true;
        }

        String normalized = colorManager.normalizeColorInput(input);
        if (normalized == null) {
            player.sendMessage(plugin.message("chatcolor.messages.usage"));
            return true;
        }

        boolean allowed;
        if (normalized.startsWith("&#")) {
            String hex = normalized.substring(2);
            allowed = permissions.hasHexPermission(player, hex);
        } else {
            char code = normalized.charAt(1);
            allowed = permissions.hasColorPermission(player, code);
        }

        if (!allowed) {
            player.sendMessage(plugin.message("chatcolor.messages.no-permission"));
            return true;
        }

        colorManager.setColor(player.getUniqueId(), normalized);
        String displayName = colorManager.getColorDisplayName(normalized);
        Component msg = plugin.message("chatcolor.messages.set");
        msg = plugin.replace(msg, "{color}", normalized + displayName);
        msg = plugin.replace(msg, "{raw_color}", normalized);
        player.sendMessage(msg);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            return List.of();
        }

        List<String> suggestions = new ArrayList<>();
        suggestions.add("reset");

        char[] legacyCodes = "0123456789abcdef".toCharArray();
        for (char c : legacyCodes) {
            if (permissions.hasColorPermission(player, c)) {
                suggestions.add("&" + c);
            }
        }

        if (permissions.hasHexPermission(player, "ffffff")) {
            suggestions.add("#ff5555");
            suggestions.add("#55ff55");
            suggestions.add("#55ffff");
            suggestions.add("#ffaa00");
        }

        String prefix = args[0].toLowerCase();
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix))
                .toList();
    }
}
