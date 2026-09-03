package me.pinkysha.easychat.group;

import java.util.UUID;

@FunctionalInterface
public interface GroupWeightProvider {
    int getWeight(UUID uuid);
}
