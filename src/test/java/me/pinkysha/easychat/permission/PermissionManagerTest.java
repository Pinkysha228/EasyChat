package me.pinkysha.easychat.permission;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;

import static me.pinkysha.easychat.TestAssertions.assertEquals;

public final class PermissionManagerTest {

    public static void runAll() {
        new PermissionManagerTest().testColorFilteringWithNoPermissions();
        new PermissionManagerTest().testColorFilteringWithSpecificColor();
        new PermissionManagerTest().testColorFilteringWithAllColors();
        new PermissionManagerTest().testStyleFiltering();
        new PermissionManagerTest().testHexColorFiltering();
    }

    private static Player createMockPlayer(Set<String> permissions) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("hasPermission") && args != null && args.length == 1) {
                        return permissions.contains(args[0]);
                    }
                    if (method.getName().equals("getName")) {
                        return "TestPlayer";
                    }
                    return null;
                }
        );
    }

    @Test
    void testColorFilteringWithNoPermissions() {
        PermissionManager manager = new PermissionManager();
        Player player = createMockPlayer(Set.of());

        String input = "&aHello &cWorld &lBold &#ff0000Hex";
        String filtered = manager.applyColorPermissions(player, input);

        assertEquals("&aHello &cWorld &lBold &#ff0000Hex", filtered);
    }

    @Test
    void testColorFilteringWithSpecificColor() {
        PermissionManager manager = new PermissionManager();
        Player player = createMockPlayer(Set.of("easychat.chat.color.a"));

        String input = "&aHello &cWorld";
        String filtered = manager.applyColorPermissions(player, input);

        assertEquals("§aHello &cWorld", filtered);
    }

    @Test
    void testColorFilteringWithAllColors() {
        PermissionManager manager = new PermissionManager();
        Player player = createMockPlayer(Set.of("easychat.chat.color.*"));

        String input = "&aGreen &cRed &lBold";
        String filtered = manager.applyColorPermissions(player, input);

        assertEquals("§aGreen §cRed &lBold", filtered);
    }

    @Test
    void testStyleFiltering() {
        PermissionManager manager = new PermissionManager();
        Player player = createMockPlayer(Set.of("easychat.chat.style.bold", "easychat.chat.style.italic"));

        String input = "&lBold &oItalic &mStrike";
        String filtered = manager.applyColorPermissions(player, input);

        assertEquals("§lBold §oItalic &mStrike", filtered);
    }

    @Test
    void testHexColorFiltering() {
        PermissionManager manager = new PermissionManager();

        // Specific hex
        Player player1 = createMockPlayer(Set.of("easychat.chat.color.hex.ff0000"));
        String input1 = "&#ff0000Red &#00ff00Green";
        String filtered1 = manager.applyColorPermissions(player1, input1);
        assertEquals("§x§f§f§0§0§0§0Red &#00ff00Green", filtered1);

        // All hex
        Player player2 = createMockPlayer(Set.of("easychat.chat.color.hex"));
        String input2 = "&#ff0000Red &#00ff00Green";
        String filtered2 = manager.applyColorPermissions(player2, input2);
        assertEquals("§x§f§f§0§0§0§0Red §x§0§0§f§f§0§0Green", filtered2);
    }
}
