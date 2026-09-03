package me.pinkysha.easychat.color;

import net.kyori.adventure.text.Component;

public interface ColorParser {
    Component parse(String input);
    Component parseFiltered(String input);
}
