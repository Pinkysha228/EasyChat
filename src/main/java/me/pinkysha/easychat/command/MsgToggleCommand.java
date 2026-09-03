package me.pinkysha.easychat.command;

import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.chat.PrivateMessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class MsgToggleCommand implements CommandExecutor, TabCompleter {
    private final EasyChat plugin;
    private final PrivateMessageManager pmManager;

    public MsgToggleCommand(EasyChat plugin, PrivateMessageManager pmManager) {
        this.plugin = plugin;
        this.pmManager = pmManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!pmManager.isEnabled()) {
            sender.sendMessage(plugin.message("private-messages.messages.disabled"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be executed by players."));
            return true;
        }

        String requiredPerm = plugin.getConfig().getString("private-messages.toggle-permission", "easychat.pm.toggle");
        if (!player.hasPermission(requiredPerm) && !player.hasPermission("easychat.admin")) {
            player.sendMessage(plugin.message("moderation.messages.no-permission"));
            return true;
        }

        boolean enabled = pmManager.toggleMessages(player.getUniqueId());
        if (enabled) {
            player.sendMessage(plugin.message("private-messages.messages.toggle-on"));
        } else {
            player.sendMessage(plugin.message("private-messages.messages.toggle-off"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
