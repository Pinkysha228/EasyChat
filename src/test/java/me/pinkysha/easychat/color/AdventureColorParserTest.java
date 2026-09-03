package me.pinkysha.easychat.color;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static me.pinkysha.easychat.TestAssertions.assertEquals;

public final class AdventureColorParserTest {

    public static void runAll() {
        new AdventureColorParserTest().testParseNullOrEmpty();
        new AdventureColorParserTest().testParseLegacyColors();
        new AdventureColorParserTest().testParseHexColors();
    }

    @Test
    void testParseNullOrEmpty() {
        AdventureColorParser parser = new AdventureColorParser();
        assertEquals(Component.empty(), parser.parse(null));
        assertEquals(Component.empty(), parser.parse(""));
    }

    @Test
    void testParseLegacyColors() {
        AdventureColorParser parser = new AdventureColorParser();
        Component component = parser.parse("&aHello &bWorld");
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        assertEquals("Hello World", plainText);
    }

    @Test
    void testParseHexColors() {
        AdventureColorParser parser = new AdventureColorParser();
        Component component = parser.parse("&#ff0000Red text");
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        assertEquals("Red text", plainText);
    }
}
