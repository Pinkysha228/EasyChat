package me.pinkysha.easychat.util;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.chat.ChatColorManager;
import me.pinkysha.easychat.permission.PermissionManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class EasyChatPlaceholderExpansion extends PlaceholderExpansion {
    private final EasyChat plugin;
    private final ChatColorManager chatColorManager;
    private final PermissionManager permissions;

    public EasyChatPlaceholderExpansion(EasyChat plugin, ChatColorManager chatColorManager, PermissionManager permissions) {
        this.plugin = plugin;
        this.chatColorManager = chatColorManager;
        this.permissions = permissions;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "easychat";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Pinkysha";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) {
            return "";
        }

        Player player = offlinePlayer.getPlayer();
        String lower = params.toLowerCase(Locale.ROOT);

        if (lower.equals("chatcolor")) {
            return chatColorManager.getColor(offlinePlayer.getUniqueId());
        }

        if (lower.equals("chatcolor_name")) {
            String color = chatColorManager.getColor(offlinePlayer.getUniqueId());
            return chatColorManager.getColorDisplayName(color);
        }

        if (lower.startsWith("has_color_")) {
            String codeOrName = lower.substring("has_color_".length());
            if (player == null) {
                return "false";
            }
            if (codeOrName.startsWith("#") || codeOrName.length() == 6) {
                return String.valueOf(permissions.hasHexPermission(player, codeOrName));
            }
            if (codeOrName.length() == 1 || (codeOrName.startsWith("&") && codeOrName.length() == 2)) {
                char c = codeOrName.charAt(codeOrName.length() - 1);
                return String.valueOf(permissions.hasColorPermission(player, c));
            }
            return String.valueOf(player.hasPermission("easychat.chat.color." + codeOrName)
                    || player.hasPermission("easychat.chat.color.*")
                    || player.hasPermission("easychat.admin"));
        }

        if (lower.startsWith("is_selected_")) {
            String target = lower.substring("is_selected_".length());
            String current = chatColorManager.getColor(offlinePlayer.getUniqueId());
            if (current == null) {
                current = "";
            }
            String normalizedTarget = chatColorManager.normalizeColorInput(target);
            if (normalizedTarget == null) {
                normalizedTarget = target;
            }
            return String.valueOf(current.equalsIgnoreCase(normalizedTarget));
        }

        return null;
    }
}
