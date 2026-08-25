package me.pinkysha.easychat.group;

import java.util.UUID;

public interface GroupWeightProvider {
    int getWeight(UUID uuid);
}
