package me.pinkysha.easychat.command;

import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.chat.PrivateMessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class IgnoreCommand implements CommandExecutor, TabCompleter {
    private final EasyChat plugin;
    private final PrivateMessageManager pmManager;

    public IgnoreCommand(EasyChat plugin, PrivateMessageManager pmManager) {
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

        String requiredPerm = plugin.getConfig().getString("private-messages.ignore-permission", "easychat.pm.ignore");
        if (!player.hasPermission(requiredPerm) && !player.hasPermission("easychat.admin")) {
            player.sendMessage(plugin.message("moderation.messages.no-permission"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(plugin.message("private-messages.messages.usage-ignore"));
            return true;
        }

        String targetName = args[0];
        if (targetName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(Component.text("You cannot ignore yourself."));
            return true;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Component msg = plugin.message("private-messages.messages.player-not-found");
            player.sendMessage(plugin.replace(msg, "{player}", targetName));
            return true;
        }

        boolean nowIgnoring = pmManager.toggleIgnore(player.getUniqueId(), target.getUniqueId());
        if (nowIgnoring) {
            Component msg = plugin.message("private-messages.messages.now-ignoring");
            player.sendMessage(plugin.replace(msg, "{player}", target.getName()));
        } else {
            Component msg = plugin.message("private-messages.messages.no-longer-ignoring");
            player.sendMessage(plugin.replace(msg, "{player}", target.getName()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!pmManager.isEnabled() || args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix) && (!name.equalsIgnoreCase(sender.getName())))
                .toList();
    }
}
