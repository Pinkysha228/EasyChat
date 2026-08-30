 package me.pinkysha.easychat.network;

import me.pinkysha.easychat.EasyChat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Shared secret used to authenticate messages exchanged between servers in the network.
 *
 * The secret is stored in a separate file (network.secret) rather than in config.yml.
 * This prevents the secret from being unintentionally included in configuration backups
 * or shared configuration files and requires it to be explicitly copied to the other
 * servers in the network.
 */
public final class NetworkSecret {
    private static final String FILE_NAME = "network.secret";

    private NetworkSecret() {}

    /**
     * Loads the shared secret from the plugin data directory.
     *
     * If the secret file does not exist or contains an empty value, a new cryptographically
     * secure random secret is generated and stored in the file.
     *
     * @param plugin the EasyChat plugin instance
     * @return the loaded or newly generated secret, or {@code null} if the file could not
     *         be read or created
     */
    public static String loadOrCreate(EasyChat plugin) {
        Path path = plugin.getDataFolder().toPath().resolve(FILE_NAME);

        try {
            if (Files.exists(path)) {
                String value = Files.readString(path, StandardCharsets.UTF_8).trim();

                if (!value.isEmpty()) {
                    return value;
                }
            }

            Files.createDirectories(path.getParent());

            String generated = generate();
            Files.writeString(path, generated, StandardCharsets.UTF_8);

            plugin.getLogger().info(
                    "Generated a new " + FILE_NAME
                            + ". Copy this file to all other servers in the network. "
                            + "The secret must be identical across all servers; otherwise, "
                            + "inter-server messages will be rejected."
            );

            return generated;

        } catch (IOException e) {
            plugin.getLogger().warning(
                    "Failed to read or create " + FILE_NAME
                            + ". Inter-server communication will remain disabled: "
                            + e.getMessage()
            );

            return null;
        }
    }

    /**
     * Generates a cryptographically secure random secret.
     *
     * @return a URL-safe Base64-encoded secret without padding
     */
    private static String generate() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}