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

import java.util.Arrays;
import java.util.List;

public final class MsgCommand implements CommandExecutor, TabCompleter {
    private final EasyChat plugin;
    private final PrivateMessageManager pmManager;

    public MsgCommand(EasyChat plugin, PrivateMessageManager pmManager) {
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

        String requiredPerm = plugin.getConfig().getString("private-messages.permission", "easychat.pm");
        if (!player.hasPermission(requiredPerm) && !player.hasPermission("easychat.admin")) {
            player.sendMessage(plugin.message("moderation.messages.no-permission"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.message("private-messages.messages.usage-msg"));
            return true;
        }

        String targetName = args[0];
        String rawMessage = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            pmManager.sendLocalPrivateMessage(player, target, rawMessage);
            return true;
        }

        // Try network forward
        if (plugin.getConfig().getBoolean("network.enabled", false)
                && plugin.getConfig().getBoolean("private-messages.network-forward", true)
                && plugin.networkBridge() != null) {
            plugin.networkBridge().forwardPrivateMessage(player, targetName, rawMessage);
            return true;
        }

        String rawNotFound = plugin.getConfig().getString("private-messages.messages.player-not-found");
        if (rawNotFound == null || rawNotFound.isBlank()) {
            rawNotFound = plugin.getConfig().getString("private-messages.format.offline");
        }
        if (rawNotFound == null || rawNotFound.isBlank()) {
            rawNotFound = plugin.getConfig().getString("messages.player-not-found", "&cPlayer &f{player} &cwas not found online.");
        }

        Component msg = plugin.colorParser().parse(rawNotFound);
        player.sendMessage(plugin.replace(msg, "{player}", targetName));
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
