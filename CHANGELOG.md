# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [0.1.0] - 2026-10-05

### Added

- `/maintenance on [duration]|off|status|reload` and `/maintenance whitelist add|remove|list`
- Permissions `maintenance.manage`, `maintenance.bypass` and `maintenance.reload`
- UUID-based maintenance whitelist stored in `whitelist.yml`
- Login blocking in `PlayerLoginEvent`, so permission and operator checks work
- Kicking of online players who are not exempt when maintenance starts
- Timed maintenance with a strict duration parser, countdown announcements and automatic end
- Optional maintenance MOTD
- MiniMessage templates for every message, configurable in `config.yml`
- `persist-state` option with atomic `state.yml` writes and safe handling of corrupt files
