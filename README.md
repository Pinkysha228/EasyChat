# EasyChat 1.0.5

Paper 1.21.4 / Java 21.

## Features
- Local, global and admin chat with configurable permissions, symbols and radius.
- Adventure hex and legacy colors with fine-grained per-color and per-style permissions.
- Persistent ChatColor (`/chatcolor`, `/color`) with database storage and DeluxeMenus/PlaceholderAPI integration.
- Private messages (`/msg`, `/tell`, `/w`, `/r`, `/reply`, `/msgtoggle`, `/ignore`) with social spy and Velocity cross-server forwarding.
- Optional PlaceholderAPI integration.
- Message cooldown with bypass permission.
- Temporary and permanent mute.
- Configurable weight-based mute and unmute protection (supporting LuckPerms groups and fallback permissions).
- SQLite, MySQL, and MariaDB database support.
- Built-in migration tool (`/easychat migrate`) from SQLite to remote MySQL/MariaDB.
- Expiration restoration after server restart.
- `/mute` tab completion for online players.
- `/unmute` tab completion from active mutes loaded from the database.
- Real-time console logs for chat, PM, mute and unmute events.

## Commands

- `/easychat <reload|migrate>` — reload configuration or migrate SQLite database to MySQL/MariaDB (`easychat.admin`).
- `/mute <player> <duration|perm> [reason]` — mute a player (`easychat.moderation.mute`).
- `/unmute <player>` — unmute a player (`easychat.moderation.unmute`).
- `/chatcolor <color|hex|reset>` (alias: `/color`) — set or reset your default chat color (`easychat.chatcolor`).
- `/msg <player> <message>` (aliases: `/tell`, `/w`, `/whisper`) — send a private message (`easychat.pm`).
- `/reply <message>` (alias: `/r`) — reply to the last private message sender (`easychat.pm`).
- `/msgtoggle` — toggle receiving private messages on or off (`easychat.pm.toggle`).
- `/ignore <player>` — toggle ignoring private messages from a player (`easychat.pm.ignore`).

## Permissions

### Chat Colors & Styles
- `easychat.chat.color.*` — allows using all standard colors (`&0`–`&f`).
- `easychat.chat.color.<code|name>` — allows specific color (e.g. `easychat.chat.color.a` or `easychat.chat.color.green`, `easychat.chat.color.red`, etc.).
- `easychat.chat.color.hex` / `easychat.chat.color.hex.*` — allows all HEX colors (`&#RRGGBB` / `<#RRGGBB>`).
- `easychat.chat.color.hex.<hex>` — allows specific HEX color (e.g. `easychat.chat.color.hex.ff0000`).
- `easychat.chat.style.*` — allows using all styles (`&l`, `&o`, `&n`, `&m`, `&k`, `&r`).
- `easychat.chat.style.<code|name>` — allows specific style (e.g. `easychat.chat.style.bold` / `easychat.chat.style.l`, `easychat.chat.style.italic` / `easychat.chat.style.o`, `easychat.chat.style.underlined` / `easychat.chat.style.n`, `easychat.chat.style.strikethrough` / `easychat.chat.style.m`, `easychat.chat.style.obfuscated` / `easychat.chat.style.magic` / `easychat.chat.style.k`, `easychat.chat.style.reset` / `easychat.chat.style.r`).

### Private Messages & Staff
- `easychat.pm` — access to `/msg` and `/r`.
- `easychat.pm.spy` — view all private messages in real-time.
- `easychat.pm.toggle` — access to `/msgtoggle`.
- `easychat.pm.ignore` — access to `/ignore`.

## PlaceholderAPI & DeluxeMenus Integration

EasyChat registers the `%easychat_...%` placeholder expansion:

- `%easychat_chatcolor%` — current player chat color code (e.g. `&a` or `&#ff5555`).
- `%easychat_chatcolor_name%` — current player chat color display name (e.g. `Green`, `Red`, `Default`).
- `%easychat_has_color_<code_or_name>%` — returns `true` or `false` (useful for DeluxeMenus `view_requirement` / permissions).
- `%easychat_is_selected_<code_or_name>%` — returns `true` or `false` (useful for showing active/selected state in DeluxeMenus).

### DeluxeMenus Example Slot
```yaml
'green_color':
  material: LIME_DYE
  slot: 10
  display_name: '&aGreen Color'
  lore:
    - '&7Click to select green chat color.'
    - ''
    - '%easychat_is_selected_&a?&a✔ Selected:&7Click to select%'
  left_click_requirement:
    requirements:
      has_perm:
        type: string equals
        input: '%easychat_has_color_&a%'
        output: 'true'
  left_click_commands:
    - '[player] chatcolor &a'
    - '[refresh]'
```

## Build

```bash
mvn clean package
```

The shaded jar is written to `target/EasyChat-1.0.5.jar`.

## DiscordSRV

DiscordSRV is an optional soft dependency. EasyChat sends selected Minecraft chat messages through DiscordSRV's webhook API, so the Discord message uses the player's name and Minecraft avatar instead of the bot's normal identity. Because EasyChat cancels Paper's original chat event, EasyChat forwards the message explicitly.

Configure under `discordsrv:` in `config.yml`. By default only global chat is forwarded. DiscordSRV itself must be connected to your Discord bot and target channel.
