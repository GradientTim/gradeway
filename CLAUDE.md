# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Gradeway is a work-in-progress Minecraft permission gateway ("gateway" — a LuckPerms-style permission/role/group system)
written in Kotlin, designed to run across multiple Minecraft server platforms (Bukkit, Paper, BungeeCord, Velocity)
sharing one database-backed permission model.

## Common commands

```bash
./gradlew build                      # build all modules
./gradlew :core-common:build         # build a single module
./gradlew detekt                     # static analysis (config: .config/detekt.yml)
./gradlew spotlessCheck              # check formatting/license headers
./gradlew spotlessApply              # fix formatting/license headers
./gradlew :core-common:test          # run all core-common tests (the only module with tests)
./gradlew :core-common:test --tests "dev.gradienttim.gradeway.extensions.JavaExtensionsTest"
./gradlew :core-common:test --tests "*.JavaExtensionsTest.isUuid accepts valid uuids*"
```

CI (`.github/workflows/`) runs `detekt`, `spotlessCheck`, `:core-common:test`, then `:core-common:build`, in that order,
against JDK 25 —
mirror that sequence locally before considering work done.

Tests use `kotlin.test` on JUnit Platform with backtick-quoted test names (`` fun `isUuid accepts valid uuids`() ``).
New tests belong under `core-common/src/test`; other modules have no test sources yet.

Spotless enforces the license header from `.assets/LICENSE_HEADER` on every `.kt` file — new files need it,
`spotlessApply` will add it automatically.

## Module layout

- `core-api` — pure interfaces/data model, no implementation logic. Defines `Driver`,
  `*Manager`/`*Service` interfaces, entities (`PlayerEntity`, `RoleEntity`, `GroupEntity`,
  `PermissionEntity`, attribute types, etc.), the KSP processor, and config data classes. Almost everything here is
  meant to be implemented elsewhere.
- `core-common` — the actual engine: `Common`-prefixed implementations of every `core-api`
  interface (e.g. `CommonPermissionService` implements `PermissionService`,
  `CommonDriverManager` implements `DriverManager`), Exposed table definitions (`database/models/**`), Cloud command
  builders (`commands/**`), and the migration strategy for importing from LuckPerms. This is the module platform plugins
  depend on and the only one with meaningful test coverage.
- `driver-database-*` / `driver-messaging-redis` — standalone driver plugins for a single backend (Postgres, MySQL,
  MariaDB, SQLite, SQL Server, Oracle, H2, Redis). Each driver module compiles against `core-api` only (`compileOnly`),
  not `core-common`, and is shaded into its own fat jar via the Shadow plugin, dropped into a `drivers/` folder the
  running plugin scans.
- `plugin-bukkit-shared` — code shared between `plugin-bukkit` and `plugin-paper` (plugin messaging driver/broker,
  connection listener, custom `PermissibleBase`).
- `plugin-bukkit`, `plugin-paper`, `plugin-bungeecord`, `plugin-velocity` — the actual platform entry points
  (`GradewayPlugin`/`GradewayVelocity`), each wiring a `CommonGradeway<TheirPlatformConfig>`
  to platform APIs (command manager, audiences, permission providers, connection listeners).

## Architecture

### Driver loading (the KSP-generated plugin system)

Drivers are the mechanism by which database/messaging backends are added without `core-common`
depending on their client libraries. The full loop:

1. A driver class extends `Driver` (`core-api/.../driver/Driver.kt`) and implements an adapter interface such as
   `DatabaseAdapter` or `MessagingAdapter`, annotated `@CreateDriver(id, type)`
   (see `driver-database-postgres/.../PostgresDriver.kt`).
2. `GradewayProcessor` (KSP, in `core-api/.../ksp/`) scans for `@CreateDriver` at compile time and writes a
   `driver.json` (`{id, type, entry}`) into the driver module's jar.
3. At runtime, `CommonDriverManager` (`core-common/.../managers/CommonDriverManager.kt`) scans a
   `drivers/` directory next to the plugin data folder, opens each jar as a `ZipFile`, reads
   `driver.json`, loads the class via a dedicated `URLClassLoader`, and instantiates it.
4. Platform-specific drivers that need direct access to a platform object (e.g. a Bukkit
   `JavaPlugin` for plugin-messaging) skip the jar-scanning path and are registered directly via
   `DriverManager.registerDriver(...)` (see `GradewayPlugin.onEnable()`).

When adding a new database/messaging backend, follow the pattern of an existing
`driver-database-*` module: `ksp(project(":core-api"))` + `compileOnly(project(":core-api"))`, implement the adapter,
shade third-party client deps via the Shadow plugin, and disable the plain `jar` task in favor of `shadowJar`.

### Dependency injection & lifecycle

`CommonGradeway<TPlatformConfig>` (`core-common/.../CommonGradeway.kt`) is the root object each platform plugin
constructs and owns. It uses Koin (`startKoin`/`stopKoin`) to wire every
`Common*` implementation to its `core-api` interface, generic over the platform's own config type
(`BukkitPlatformConfig`, `VelocityPlatformConfig`, etc. — see `ConfigManager<TPlatformConfig>`). Lifecycle is explicit
and ordered: `load()` starts Koin and loads configs → drivers → languages → messaging → confirmations; `enable()`/
`disable()` toggle databases and messaging without tearing down Koin; `unload()` reverses `load()` and calls
`stopKoin()` (not `koin.close()` — `stopKoin()`
also deregisters from Koin's global context, which a plain `close()` doesn't, so a second
`load()` in the same JVM — e.g. a plugin disable/enable cycle — would otherwise fail with
`KoinApplicationAlreadyStartedException`). `GradewayState` gates re-entrant calls (`allowLoad`/
`allowUnload`).

### Error handling

Manager/service methods that can fail return Arrow's `Either<Throwable, T>`, composed with
`either { ... }` / `raise(...)` blocks rather than thrown exceptions — follow this convention for new manager/service
code instead of introducing exceptions or nullable-return error signaling.

### Persistence

Exposed (`org.jetbrains.exposed`) DAO/DSL over JDBC. Tables live in
`core-common/.../database/models/**`, one file per entity, generally paired with a "reference"
type in `core-api` (e.g. `RoleEntity`/`RoleReference`) used to pass around an identity without pulling in the full row.
Migrations are handled by `MigrationManager`/`MigrationStrategy` — the existing `LuckPermsMigrationStrategy` is the
reference implementation for importing another plugin's data.

### Commands

Built with the Cloud command framework (`org.incendo.cloud`), platform-agnostic via
`CommandManager<C>` and `AudienceProvider<C>`. `createGradewayCommand` in
`core-common/.../commands/GradewayCommand.kt` is the single shared entry point every platform plugin calls, registering
per-entity subcommands from `commands/gradeway/*Command.kt`
(`RoleCommand`, `GroupCommand`, `PlayerCommand`, `PermissionCommand`, `BackupCommand`,
`MigrationCommand`, `ConfirmationCommand`). Generic, reusable subcommand builders (list/set/clear for permissions and
attributes, shared across role/group/player) live in
`commands/extensions/CloudCommandHelpers.kt`; dynamic argument suggestions live in
`CloudCommandSuggestions.kt`. Cloud's default suggestion processor is replaced with
`SuggestionProcessor.passThrough()` because id suggestions are UUIDs, not the name text the user typed, which the
default filtering processor would incorrectly exclude.

User-facing text is never hardcoded — commands send `Component.translatable("gradeway...")` keys resolved against
`core-common/src/main/resources/languages/en.properties`. After touching any command builder, use the
**sync-translation-keys** skill to reconcile added/removed
`Component.translatable` keys against `en.properties`, including the `$entityType`-templated keys used by the generic
entity-permission/attribute helpers.

### Multi-platform config

Each platform module defines its own `@Serializable` platform config class (e.g.
`BukkitPlatformConfig`, `VelocityPlatformConfig`) and passes it plus its `KSerializer` into
`CommonGradeway`'s constructor; `GradewayConfig<TPlatformConfig>` embeds it into the shared TOML config alongside
database/messaging/appearance settings. New platform-specific settings go in that platform's own config class, not into
shared config types in `core-api`/`core-common`.

## Code style

- Kotlin 2.4.10, JDK 25, `-Xexplicit-backing-fields` enabled in `core-api` (used for the encapsulated-mutable-field
  pattern, e.g. `KeyedRegistry.items`).
- Every `.kt` file requires the MIT license header (enforced by Spotless, see above).
- `warningsAsErrors` is enabled for Kotlin compiler warnings at the root build; keep new code warning-free rather than
  suppressing.
