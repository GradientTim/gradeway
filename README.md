# Gradeway [WIP]

A Minecraft permission gateway.

> This project has not yet been fully released, which means it may contain some bugs and may undergo some changes in future versions.  
> However, you can already use it and provide feedback to help improve the project further.  
> Feel free to request changes/features or provide your feedback on my Discord server: https://discord.gg/f35EemU4jS

[Changelog](./CHANGELOG.md) – [Security Policy](./SECURITY.md) – [Code of Conduct](./CODE_OF_CONDUCT.md) – [Discord](https://discord.gg/f35EemU4jS)

## Features

- Player, role, and group management with inheritance, weighted priority, and effective permission resolution
- Permission management with presets via templates
- Integrated messaging service to refresh data on every connected server
- Backup export/import, plus a migration path for bringing over existing permission data
- Confirmation prompts before destructive commands
- Pluggable database and messaging backends, loaded as separate driver jars at runtime
- Pausing and setting an expired date for individual player roles
- Relational database layout to support efficient querying and in-database cleanups
- Built-in attribute types and extendable attributes with custom serializers

## Installation

TODO

## Supported platforms and databases

Platforms: `Bukkit`, `Paper`, `BungeeCord`, and `Velocity`.  
Drivers: `Postgres`, `MySQL`, `MariaDB`, `SQLite`, `SQL Server`, `Oracle`, `H2`, and `Redis` as pluggable backends.  
See [COMPATIBILITY.md](./COMPATIBILITY.md) for the full overview.

## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for how the Player, Role, and Group model works – especially relevant if you're
coming from LuckPerms, where "group" means something different.

## Database Schema

See [DATABASE.md](./DATABASE.md) for an interactive diagram of the schema.

## Building from Source

Requires JDK 25. (`sdk install java 25.0.3-tem`)

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
