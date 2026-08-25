# EasyChat 1.0.5

Paper 1.21.4 / Java 21.

## Features
- Local, global and admin chat with configurable permissions, symbols and radius.
- Adventure hex and legacy colors.
- Optional PlaceholderAPI integration.
- Message cooldown with bypass permission.
- Temporary and permanent mute.
- Weight-based unmute protection.
- SQLite persistence and expiration restoration after restart.
- `/mute` tab completion for online players.
- `/unmute` tab completion from active mutes loaded from the database.
- Real-time console logs for chat, mute and unmute events.

## PlaceholderAPI
PlaceholderAPI is optional. If installed and `placeholders.enabled: true`, PAPI placeholders can be used in EasyChat formats and messages, for example:

```yaml
chat:
  local:
    format: "&7[%luckperms_prefix%&7] &f%player_name%&7: &r{message}"
```

The project uses the PlaceholderAPI API as a provided dependency, so PlaceholderAPI itself is not bundled into EasyChat. PlaceholderAPI expansions are managed by PlaceholderAPI and can be installed through its eCloud.

## Build

```bash
mvn clean package
```

The shaded jar is written to `target/EasyChat-1.0.5.jar`.


## DiscordSRV

DiscordSRV is an optional soft dependency. EasyChat sends selected Minecraft chat messages through DiscordSRV's webhook API, so the Discord message uses the player's name and Minecraft avatar instead of the bot's normal identity. Because EasyChat cancels Paper's original chat event, EasyChat forwards the message explicitly.

Configure under `discordsrv:` in `config.yml`. By default only global chat is forwarded. DiscordSRV itself must be connected to your Discord bot and target channel.
