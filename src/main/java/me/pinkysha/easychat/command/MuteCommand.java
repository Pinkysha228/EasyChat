package me.pinkysha.easychat.command;

import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.moderation.MuteScheduler;
import me.pinkysha.easychat.util.DurationParser;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MuteCommand implements CommandExecutor, TabCompleter {
    private final EasyChat plugin;
    private final MuteScheduler scheduler;

    public MuteCommand(EasyChat plugin, MuteScheduler scheduler) { this.plugin = plugin; this.scheduler = scheduler; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.hasPermission("easychat.moderation.mute")) {
            sender.sendMessage(plugin.message("moderation.messages.no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.message("messages.usage-mute"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        Duration duration;
        try { duration = DurationParser.parse(args[1]); }
        catch (IllegalArgumentException ex) {
            sender.sendMessage(plugin.message("messages.invalid-duration"));
            return true;
        }

        String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim()
                : plugin.getConfig().getString("moderation.default-reason", "нарушение правил");
        if (reason == null || reason.isBlank()) reason = "нарушение правил";
        if (plugin.muteManager().isMuted(target.getUniqueId())) {
            sender.sendMessage(plugin.message("moderation.messages.already-muted"));
            return true;
        }

        final String finalReason = reason;
        plugin.muteManager().mute(sender, target.getUniqueId(), duration, finalReason).thenAccept(mute ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    scheduler.schedule(target.getUniqueId(), mute.expiresAt());
                    String playerName = target.getName() == null ? args[0] : target.getName();
                    String durationText = duration == null ? "навсегда" : format(duration);
                    var msg = plugin.message("moderation.messages.mute-created");
                    msg = plugin.replace(msg, "{player}", playerName);
                    msg = plugin.replace(msg, "{duration}", durationText);
                    msg = plugin.replace(msg, "{reason}", finalReason);
                    sender.sendMessage(msg);
                    plugin.getLogger().info("[MUTE] " + senderName(sender) + " muted " + playerName + " for " + durationText
                            + ". Reason: " + finalReason + " (weight=" + mute.moderatorWeight() + ")");
                }));
        return true;
    }

    private String senderName(CommandSender sender) { return sender instanceof Player p ? p.getName() : "Console"; }
    private String format(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds % 604800 == 0) return seconds / 604800 + "w";
        if (seconds % 86400 == 0) return seconds / 86400 + "d";
        if (seconds % 3600 == 0) return seconds / 3600 + "h";
        if (seconds % 60 == 0) return seconds / 60 + "m";
        return seconds + "s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase();
        List<String> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) if (player.getName().toLowerCase().startsWith(prefix)) result.add(player.getName());
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }
}
