# minecraft-maintenance

Maintenance mode for Paper servers.

## Description

`minecraft-maintenance` closes a Paper server to everyone except a chosen group. Players are exempt by a
UUID-based maintenance whitelist, by the `maintenance.bypass` permission, or (optionally) by being an operator.
Maintenance can be open-ended or timed, with countdown announcements and an automatic end. Every message is a
MiniMessage template in `config.yml`.

Status: the plugin compiles and its core logic is covered by unit tests. It has not been run on a real Paper
server. Manual testing on a server is still required (see [Testing](#testing)).

## Features

- `/maintenance on [duration]`, `off`, `status`, `reload`, `whitelist add|remove|list`
- Maintenance whitelist keyed by UUID, stored in `whitelist.yml`
- Online players who are not exempt are kicked with a configurable message when maintenance starts
- Logins are refused during maintenance with a configurable message
- Timed maintenance (`30m`, `1h30m`, `2d`) with countdown announcements and automatic end
- Optional maintenance MOTD in the server list
- All messages are MiniMessage templates with placeholders
- Explicit restart behavior through `persist-state`
- Atomic state writes; corrupt state or whitelist files never stop the server from starting

## Installation

Requires Paper 1.20.4 or newer in the 1.20 line (`api-version: 1.20`) and Java 17 or newer.

1. Build the jar (see [Development](#development)) or take it from a release.
2. Copy `target/minecraft-maintenance-0.1.0.jar` to the server `plugins/` directory.
3. Start the server. `plugins/Maintenance/config.yml` is created on first start.

## Usage

| Command | Permission | Effect |
| --- | --- | --- |
| `/maintenance on` | `maintenance.manage` | Start maintenance with no end time |
| `/maintenance on <duration>` | `maintenance.manage` | Start maintenance that ends automatically |
| `/maintenance off` | `maintenance.manage` | End maintenance |
| `/maintenance status` | `maintenance.manage` | Show whether maintenance is on and the time left |
| `/maintenance whitelist add <player or uuid>` | `maintenance.manage` | Add a player to the maintenance whitelist |
| `/maintenance whitelist remove <player or uuid>` | `maintenance.manage` | Remove a player |
| `/maintenance whitelist list` | `maintenance.manage` | List whitelisted players |
| `/maintenance reload` | `maintenance.reload` | Reload `config.yml` |

`/maint` is an alias. All three permissions default to operators.

Durations are one or more `<number><unit>` segments with units `s`, `m`, `h`, `d`, each unit at most once
(`90m`, `1h30m`, `2d`). Zero, negative values, unknown units, repeated units, spaces, decimals and anything
above 30 days are rejected.

### Who may join during maintenance

A player may join when any of these is true:

- their UUID is on the maintenance whitelist
- they have the `maintenance.bypass` permission
- they are an operator and `bypass-operators` is `true`

### Why the check runs in `PlayerLoginEvent`

`AsyncPlayerPreLoginEvent` fires before a `Player` object exists, so permissions and operator status cannot be
queried there. Only a UUID check is possible at that stage. This plugin therefore does the whole decision in
`PlayerLoginEvent` at high priority, where the player has their permissions attached, and disallows the login
with `KICK_OTHER`. The whitelist check, the permission check and the operator check all run in that one place and
give the same answer. The cost is that a refused player is rejected slightly later in the login sequence than
with a pre-login check. The decision itself is a handful of in-memory lookups.

### Whitelisting offline players

`Bukkit.getOfflinePlayer(String)` can block on a Mojang lookup, and that must not happen on the main thread.
The plugin never calls it. `whitelist add <name>` accepts:

- a player who is online
- a player the server already has cached (`getOfflinePlayerIfCached`), meaning someone who joined before
- a UUID, dashed or compact. The UUID is trusted as given; a name is shown if the server knows one

An unknown name is refused with a message instead of being looked up. To whitelist a player who never joined,
use their UUID.

## Configuration

`plugins/Maintenance/config.yml`:

```yaml
persist-state: true
bypass-operators: true
motd:
  enabled: true
  text: "<red><bold>Server under maintenance</bold></red>\n<gray>We will be back soon.</gray>"
countdown-announcements: [10m, 5m, 1m, 30s, 10s, 5s, 3s, 2s, 1s]
messages:
  kick: "<red><bold>Server maintenance</bold></red>\n<gray>Please come back later.</gray>"
```

The shipped file lists every message key. Missing keys fall back to built-in defaults, and unknown or invalid
values are logged as warnings.

| Option | Default | Meaning |
| --- | --- | --- |
| `persist-state` | `true` | Whether maintenance mode survives a restart |
| `bypass-operators` | `true` | Whether operators are exempt without the bypass permission |
| `motd.enabled` | `true` | Replace the server list MOTD during maintenance |
| `motd.text` | see above | MOTD template |
| `countdown-announcements` | see above | Remaining times at which a timed maintenance is announced |
| `messages.*` | see `config.yml` | MiniMessage templates |

### Restart behavior

`persist-state` decides what a restart does, and the default is `true`.

- `true`: the state is written to `state.yml` on every change and restored on start. A server restarted for the
  maintenance work stays closed, which is the usual intent. A timed maintenance whose end passed while the
  server was offline is treated as ended. One still running resumes its countdown.
- `false`: the server always starts with maintenance off. `state.yml` is neither read nor written.

`state.yml` is written to a temporary file in the same directory, synced, and moved over the old file
(atomic move where the filesystem supports it). A missing file means maintenance is off. A corrupt, oversized,
non-UTF-8 or wrongly typed file is ignored: maintenance starts disabled and a warning is logged. The same applies
to `whitelist.yml`, where individually invalid entries are skipped with a warning.

Changing `persist-state` through `/maintenance reload` takes effect immediately for later changes.

### Messages and placeholders

Messages use [MiniMessage](https://docs.advntr.dev/minimessage/format.html). `<prefix>` expands to
`messages.prefix` in any template. Other placeholders:

| Placeholder | Available in |
| --- | --- |
| `<duration>` | `enabled-timed` |
| `<remaining>` | `status-on-timed`, `countdown` |
| `<player>` | `whitelist-*`, `player-unknown` |
| `<count>` | `whitelist-header`, `reload-warnings` |
| `<input>`, `<reason>` | `invalid-duration` |
| `<error>` | `reload-failed` |

Placeholder values are inserted as plain text, so a player name cannot inject MiniMessage tags or click events.

## Examples

Close the server for 30 minutes and let a friend in:

```text
/maintenance whitelist add Steve
/maintenance on 30m
```

Open-ended maintenance, then check and end it:

```text
/maintenance on
/maintenance status
/maintenance off
```

Whitelist by UUID:

```text
/maintenance whitelist add 069a79f4-44e9-4726-a5be-fca90e38aaf5
```

## Architecture

```text
net.sadness.maintenance
  core      state machine, access policy, whitelist, duration parsing/formatting, countdown (no Bukkit)
  io        atomic file writer, bounded reader, YAML codec, state and whitelist files, async writer
  config    config model, parser, message keys (no Bukkit)
  message   MiniMessage rendering
  plugin    Bukkit layer: plugin class, commands, listeners, scheduler, service
```

`core`, `io` and `config` do not import Bukkit and are what the unit tests cover. `message` depends only on
Adventure. `plugin` is a thin layer that connects those parts to Paper.

## Security

- Permission checks guard every subcommand; tab completion only offers what the sender may run.
- YAML is parsed with SnakeYAML `SafeConstructor`, so no arbitrary types are instantiated, and alias count is capped.
- Config, state and whitelist files are read with a 256 KiB limit and strict UTF-8 decoding.
- Durations are capped at 30 days and parsed with overflow-checked arithmetic.
- User input is inserted into messages as unparsed text.
- A broken config on reload keeps the previous configuration active.

## Performance

- The join decision uses a concurrent map lookup, a permission check and an operator check. No I/O.
- All file writes go through a single background thread. Reading `config.yml` on reload also happens off the main thread.
- The startup read of `state.yml` and `whitelist.yml` is on the main thread during plugin enable; both are bounded to 256 KiB.
- The countdown task runs once per second only while a timed maintenance is active.
- No benchmarks have been run.

## Development

Requires Java 17 and Maven 3.9.

```sh
mvn -B verify
```

This compiles against Paper API 1.20.4, runs the tests and builds `target/minecraft-maintenance-0.1.0.jar`.
The Paper API comes from `https://repo.papermc.io/repository/maven-public/`.

## Testing

The unit tests (JUnit 5) cover the state machine transitions, the join decision for whitelisted, bypass, operator
and normal players, duration parsing (invalid, overflow, zero, negative), countdown announcements, atomic writes,
`state.yml` and `whitelist.yml` reading, writing and corruption, config validation, and message templating.

What is verified: the project compiles against Paper API 1.20.4 and the core logic passes its unit tests.

What is not verified: the Bukkit layer (commands, listeners, scheduler, kicking, MOTD) has not been run on a real
Paper server, and there are no tests for it. Test it manually on a server before relying on it.

## License

MIT. See [LICENSE](LICENSE).
