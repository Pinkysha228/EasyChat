package me.pinkysha.easychat.command;

import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.moderation.Mute;
import me.pinkysha.easychat.moderation.MuteScheduler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UnmuteCommand implements CommandExecutor, TabCompleter {
    private final EasyChat plugin;
    private final MuteScheduler scheduler;
    public UnmuteCommand(EasyChat plugin, MuteScheduler scheduler) { this.plugin = plugin; this.scheduler = scheduler; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("easychat.moderation.unmute")) {
            sender.sendMessage(plugin.message("moderation.messages.no-permission"));
            return true;
        }
        if (args.length != 1) { sender.sendMessage(plugin.message("messages.usage-unmute")); return true; }

        Mute mute = plugin.muteManager().getByName(args[0]);
        if (mute == null) { sender.sendMessage(plugin.message("moderation.messages.not-found")); return true; }

        if (sender instanceof Player moderator) {
            if (!plugin.getConfig().getBoolean("moderation.self-unmute", false) && mute.moderatorUuid() != null && mute.moderatorUuid().equals(moderator.getUniqueId())) {
                sender.sendMessage(plugin.message("moderation.messages.cannot-unmute")); return true;
            }
            if (!plugin.muteManager().canUnmute(moderator.getUniqueId(), mute)) {
                sender.sendMessage(plugin.message("moderation.messages.cannot-unmute")); return true;
            }
        }

        plugin.muteManager().unmute(mute.playerUuid()).thenAccept(ok -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            scheduler.cancel(mute.playerUuid());
            if (!ok) { sender.sendMessage(plugin.message("moderation.messages.not-found")); return; }
            String playerName = plugin.muteManager().getPlayerName(mute.playerUuid());
            if (playerName == null) playerName = args[0];
            sender.sendMessage(plugin.replace(plugin.message("moderation.messages.unmuted"), "{player}", playerName));
            plugin.getLogger().info("[UNMUTE] " + (sender instanceof Player p ? p.getName() : "Console") + " unmuted " + playerName
                    + " (original moderator=" + mute.moderatorUuid() + ", original weight=" + mute.moderatorWeight() + ")");
        }));
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        plugin.muteManager().getActiveMuteNames().stream().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted(String.CASE_INSENSITIVE_ORDER).forEach(result::add);
        return result;
    }
}
