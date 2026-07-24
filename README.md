# Gradeway [WIP]

A Minecraft permission gateway.

> This project has not yet been fully released, which means it may contain some bugs and may undergo some changes in future versions.  
> However, you can already use it and provide feedback to help improve the project further.  
> Feel free to request changes/features or provide your feedback on my Discord server: https://discord.gg/f35EemU4jS

Gradeway is a permission, role, and group system, backed by a single shared database, so every server in a network sees
the same permission state.  
Roles and groups support inheritance and weighting, permissions can be scoped per player, role, or group, and everything
is driven from in-game commands rather than editing files by hand.

[Changelog](./CHANGELOG.md) – [Security Policy](./SECURITY.md) – [Code of Conduct](./CODE_OF_CONDUCT.md) – [Discord](https://discord.gg/f35EemU4jS)

## Features

- Player, role, and group management with inheritance, weighted priority, and effective permission resolution
- Permission management with presets via templates
- One shared database across every connected server, so the permission state stays in sync
- Backup export/import, plus a migration path for bringing over existing permission data
- Confirmation prompts before destructive commands
- Fully translatable command output — no hardcoded messages
- Pluggable database and messaging backends, loaded as separate driver jars at runtime

## Installation

TODO

## Supported platforms & databases

Platforms: `Bukkit`, `Paper`, `BungeeCord`, and `Velocity`  
Drivers: `Postgres`, `MySQL`, `MariaDB`, `SQLite`, `SQL Server`, `Oracle`, `H2`, and `Redis` as pluggable backends.  
See [COMPATIBILITY.md](./COMPATIBILITY.md) for the full overview.

## Database Schema

See [DATABASE.md](./DATABASE.md) for an interactive diagram of the schema.

## Building from Source

Requires JDK 25.

```bash
git clone https://github.com/GradientTim/gradeway.git
cd gradeway
./gradlew build
```

Build a single module instead, e.g. `./gradlew :core-common:build`.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for the development setup, project layout, and PR expectations.  
The [Discord server](https://discord.gg/f35EemU4jS) is the fastest way to discuss a change before you start.

## License

This project is licensed under the [MIT License](./LICENSE).
