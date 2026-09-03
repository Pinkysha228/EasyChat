package me.pinkysha.easychat.group;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public final class LuckPermsGroupWeightProvider implements GroupWeightProvider {
    private final Map<String, Integer> fallbackWeights;
    private final int defaultWeight;
    private final LuckPerms luckPerms;

    public LuckPermsGroupWeightProvider(Map<String, Integer> fallbackWeights, int defaultWeight) {
        this.fallbackWeights = fallbackWeights;
        this.defaultWeight = defaultWeight;

        LuckPerms found;
        try {
            found = LuckPermsProvider.get();
        } catch (IllegalStateException ex) {
            found = null;
        }
        this.luckPerms = found;
    }

    @Override
    public int getWeight(UUID uuid) {
        if (luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(uuid);
            if (user != null && user.getPrimaryGroup() != null) {
                Group group = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());
                if (group != null) {
                    return group.getWeight().orElse(defaultWeight);
                }
            }
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return fallbackWeights.entrySet().stream()
                    .filter(entry -> player.hasPermission(entry.getKey()))
                    .mapToInt(Map.Entry::getValue)
                    .max()
                    .orElse(defaultWeight);
        }

        return defaultWeight;
    }
}
