package me.pinkysha.easychat;

import me.pinkysha.easychat.chat.ChatColorManagerTest;
import me.pinkysha.easychat.chat.MessageCooldownManagerTest;
import me.pinkysha.easychat.chat.PrivateMessageManagerTest;
import me.pinkysha.easychat.color.AdventureColorParserTest;
import me.pinkysha.easychat.database.DatabaseTypeTest;
import me.pinkysha.easychat.moderation.MuteTest;
import me.pinkysha.easychat.permission.PermissionManagerTest;
import me.pinkysha.easychat.util.DurationParserTest;

public final class AllTestsRunner {

    public static void main(String[] args) {
        System.out.println("Running EasyChat tests...");

        DurationParserTest.runAll();
        System.out.println("✓ DurationParserTest passed");

        DatabaseTypeTest.runAll();
        System.out.println("✓ DatabaseTypeTest passed");

        MessageCooldownManagerTest.runAll();
        System.out.println("✓ MessageCooldownManagerTest passed");

        MuteTest.runAll();
        System.out.println("✓ MuteTest passed");

        AdventureColorParserTest.runAll();
        System.out.println("✓ AdventureColorParserTest passed");

        PermissionManagerTest.runAll();
        System.out.println("✓ PermissionManagerTest passed");

        ChatColorManagerTest.runAll();
        System.out.println("✓ ChatColorManagerTest passed");

        PrivateMessageManagerTest.runAll();
        System.out.println("✓ PrivateMessageManagerTest passed");

        System.out.println("All EasyChat tests completed successfully!");
    }
}
