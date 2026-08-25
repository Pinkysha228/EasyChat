package me.pinkysha.easychat.command;

import me.pinkysha.easychat.EasyChat;
import org.bukkit.command.*;

import java.util.List;

public final class EasyChatCommand implements CommandExecutor, TabCompleter {
    private final EasyChat plugin;
    public EasyChatCommand(EasyChat plugin) { this.plugin=plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("easychat.admin")) { sender.sendMessage(plugin.message("moderation.messages.no-permission")); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) { plugin.reloadPlugin(); sender.sendMessage(plugin.message("messages.reload")); return true; }
        sender.sendMessage(plugin.message("messages.usage-easychat")); return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) { return args.length == 1 ? List.of("reload") : List.of(); }
}
