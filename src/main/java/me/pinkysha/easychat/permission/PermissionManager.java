package me.pinkysha.easychat.permission;

import me.pinkysha.easychat.chat.ChatChannel;
import org.bukkit.entity.Player;

public final class PermissionManager {
    public boolean canUse(Player player, ChatChannel channel) { return player.hasPermission(channel.permission()); }
    public boolean canBypassCooldown(Player player, String permission) { return player.hasPermission(permission); }
    public boolean canBypassMute(Player player, String permission) { return player.hasPermission(permission); }
}
