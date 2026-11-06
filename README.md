# Gradeway

**A modern permission plugin for Paper, Bukkit, Velocity, and BungeeCord – one shared database, every server in sync.**

Manage ranks, permissions, and temporary roles across your entire network from a single place.
Coming from LuckPerms? Import your existing data with one command.

<!-- modrinth_exclude.start -->
[Changelog](https://github.com/GradientTim/gradeway/releases) · [Documentation](https://docs.gradienttim.dev/gradeway/) · [Discord](https://discord.gg/f35EemU4jS) · [API Reference](https://dokka.gradienttim.dev/gradeway/)

<a href="https://modrinth.com/project/gradeway" target="_blank">
<img alt="modrinth" height="40" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg">
</a>

<a href="https://hangar.papermc.io/GradientTim/gradeway" target="_blank">
<img alt="hangar" height="40" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/hangar_vector.svg">
</a>
<!-- modrinth_exclude.end -->

> [!NOTE]
> Gradeway is in active development (0.x). It's ready to try, but expect changes between releases – back up your data
> before upgrading.

***

## Why Gradeway?

- **Your whole network, always in sync** – change a rank on one server and every connected server picks it up
  instantly, no restarts or manual reloads.
- **Temporary and pausable ranks** – give someone `vip` for 30 days, or pause a staff role while they're on break
  without losing it.
- **Know exactly why someone has a permission** – `/gradeway why` shows the full chain (player → role → group →
  template), and `/gradeway whocan` lists everyone who has it.
- **Promotion tracks** – define a ladder like `trial → mod → admin` and promote or demote players along it.
- **Permission templates** – build a reusable set of permissions once and apply it to any role or player.
- **Safe by default** – destructive commands ask for confirmation, and full backups export and import with one command.
- **Use the database you already run** – Postgres, MySQL, MariaDB, SQLite, SQL Server, Oracle, or H2, plus Redis for
  cross-server messaging.

## See it in action

```text
/gradeway role create vip
/gradeway role modify vip permissions set essentials.fly
/gradeway player modify Steve roles add vip 30D
/gradeway why Steve essentials.fly
```

<!-- TODO: add a screenshot or GIF of the commands above -->

## Coming from LuckPerms?

Run `/lp export` in LuckPerms, then import the file into Gradeway:

```text
/gradeway migrate luckperms <file>
```

One thing to know: **what LuckPerms calls a "group" is a _role_ in Gradeway.**

| LuckPerms                                    | Gradeway                                                     |
|----------------------------------------------|--------------------------------------------------------------|
| Group (`admin`, `default`, …)                | **Role**                                                     |
| Parent groups                                | Role parents                                                 |
| Primary group                                | Primary role                                                 |
| Track                                        | Track                                                        |
| _no equivalent_                              | **Group** – a container that lets several roles share permissions and a default weight |

The [architecture guide](https://github.com/GradientTim/gradeway/blob/main/ARCHITECTURE.md) explains the model in
detail.

## Getting started

1. Download Gradeway for your platform from [Modrinth](https://modrinth.com/project/gradeway) or
   [Hangar](https://hangar.papermc.io/GradientTim/gradeway).
2. Download the driver for your database and place it in the `drivers/` folder.
3. Start your server and fill in your database credentials in the generated config.

The full [installation guide](https://docs.gradienttim.dev/gradeway/admin/install) covers every platform and option.

## Compatibility

|                | Supported                                                |
|----------------|----------------------------------------------------------|
| **Servers**    | Paper, Bukkit (26.1+)                                    |
| **Proxies**    | Velocity, BungeeCord                                     |
| **Databases**  | Postgres, MySQL, MariaDB, SQLite, SQL Server, Oracle, H2 |
| **Messaging**  | Redis, Plugin Messaging                                  |

See the [compatibility documentation](https://docs.gradienttim.dev/gradeway/compatibility) for the full overview.

## For developers

Gradeway has a public API with events, actor tracking, and custom attribute types.
See the [developer guide](https://docs.gradienttim.dev/gradeway/dev/install) and the
[API reference](https://dokka.gradienttim.dev/gradeway/).

<!-- modrinth_exclude.start -->
## Building from source

Requires JDK 17+ to run Gradle; the JDK 21/25 toolchains are provisioned automatically on first build.

```bash
git clone https://github.com/GradientTim/gradeway.git
cd gradeway
./gradlew build
```

The [database schema](https://docs.gradienttim.dev/gradeway/database) is documented as an interactive diagram.

## Contributing

See [CONTRIBUTING.md](https://github.com/GradientTim/gradeway/blob/main/CONTRIBUTING.md) for the development setup and
PR expectations. The [Discord server](https://discord.gg/f35EemU4jS) is the fastest way to discuss a change before you
start. Please also read the [Code of Conduct](https://github.com/GradientTim/gradeway/blob/main/CODE_OF_CONDUCT.md)
and [Security Policy](https://github.com/GradientTim/gradeway/blob/main/.github/SECURITY.md).
<!-- modrinth_exclude.end -->

## License

Gradeway is licensed under the [MIT License](https://github.com/GradientTim/gradeway/blob/main/LICENSE).
