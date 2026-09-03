package me.pinkysha.easychat.command;

import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.List;

public final class EasyChatCommand implements CommandExecutor, TabCompleter {
    private final EasyChat plugin;

    public EasyChatCommand(EasyChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("easychat.admin")) {
            sender.sendMessage(plugin.message("moderation.messages.no-permission"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(plugin.message("messages.reload"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("migrate")) {
            handleMigration(sender);
            return true;
        }

        sender.sendMessage(plugin.message("messages.usage-easychat"));
        return true;
    }

    private void handleMigration(CommandSender sender) {
        DatabaseManager dbManager = plugin.databaseManager();
        if (dbManager.isOffline()) {
            sender.sendMessage(plugin.message("messages.migrate-offline-error"));
            return;
        }

        String sqliteFileName = plugin.getConfig().getString("database.file", "easychat.db");
        File sqliteFile = new File(plugin.getDataFolder(), sqliteFileName);
        if (!sqliteFile.exists()) {
            Component msg = plugin.message("messages.migrate-no-source");
            msg = plugin.replace(msg, "{file}", sqliteFileName);
            sender.sendMessage(msg);
            return;
        }

        Component startMsg = plugin.message("messages.migrate-start");
        startMsg = plugin.replace(startMsg, "{target}", dbManager.type().name());
        sender.sendMessage(startMsg);

        plugin.asyncExecutor().execute(() -> {
            try {
                DatabaseManager.MigrationResult result = dbManager.migrateFromSqlite(sqliteFile);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Component successMsg = plugin.message("messages.migrate-success");
                    successMsg = plugin.replace(successMsg, "{mutes}", String.valueOf(result.mutesCount()));
                    successMsg = plugin.replace(successMsg, "{moderators}", String.valueOf(result.moderatorsCount()));
                    successMsg = plugin.replace(successMsg, "{time}", String.valueOf(result.durationMs()));
                    sender.sendMessage(successMsg);

                    plugin.getLogger().info("[MIGRATION] Successfully migrated " + result.mutesCount()
                            + " mutes and " + result.moderatorsCount() + " moderators from SQLite to "
                            + dbManager.type() + " in " + result.durationMs() + "ms.");

                    plugin.muteManager().load();
                });
            } catch (Exception e) {
                plugin.getLogger().severe("[MIGRATION] Migration failed: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Component failMsg = plugin.message("messages.migrate-fail");
                    failMsg = plugin.replace(failMsg, "{error}", e.getMessage() == null ? "Unknown error" : e.getMessage());
                    sender.sendMessage(failMsg);
                });
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return List.of("reload", "migrate").stream()
                    .filter(sub -> sub.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
