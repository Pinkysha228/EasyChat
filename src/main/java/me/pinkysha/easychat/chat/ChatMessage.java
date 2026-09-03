package me.pinkysha.easychat.chat;

import java.time.Instant;
import java.util.UUID;

public record ChatMessage(
        UUID sender,
        ChatChannel channel,
        String rawMessage,
        Instant timestamp
) {}
