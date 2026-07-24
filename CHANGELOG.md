# Changelog

All notable changes to this project are documented in this file.

## [Unreleased]

### Features

- *(commands)* Introduce cache management commands and enhance parser consistency

- *(services)* Add periodic expired role removal and sweep configuration

- *(tests)* Extend test coverage with platform config support and enhance JAR naming

- *(core-common)* Generalize `CommonGradeway` for enhanced type safety and serializer support

- *(core-api)* Introduce platform config extensibility for multi-platform support

- *(core-common)* Add child role management and caching optimizations

- *(services)* Add player disconnect cache invalidation and streamline imports

- *(core-common)* Enhance core services, tests, and messaging integrations

- *(core-api)* Implement authenticated messaging infrastructure with shared secrets

- *(plugins)* Implement modular support for Bukkit and BungeeCord platforms

- *(core-common)* Enhance APIs with sender-based confirmations and granular migration checks

- *(core-common)* Add effective weight caching for players and roles

- *(core-common)* Introduce migration and confirmation management systems

- *(core-common)* Add messaging lifecycle support to Gradeway initialization

- *(messaging)* Implement Velocity messaging driver and caching for permissions

- *(permissions)* Add effective permission resolution for groups and roles

- *(commands)* Add backup command for export and import support

- *(commands)* Improve permission command handling and error clarity

### Refactor

- *(core-common)* Migrated from plain brigadier to cloud command framework

- *(velocity)* Reorganize package structure for consistency

- *(core-api)* Make entity properties immutable and correct function signatures

- *(plugin-paper)* Rename bukkit packages to paper for consistency

- *(plugin)* Replace Bukkit plugin module with Paper plugin module

- *(plugin-bungeecord)* Remove BungeeCord plugin module

- Unify entity structure by transitioning from `database` to `entity` package, standardize `flush` method, and introduce
  new `PlayerEntity` and `PlayerRoleEntity` interfaces.


