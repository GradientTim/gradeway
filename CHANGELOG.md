# Changelog

All notable changes to this project are documented in this file.

## [0.1.2] - 2026-08-20

### **chore

- *(scheduler)* Refactor platform-specific schedulers and remove unused Bukkit/Paper classes**

## [0.1.1] - 2026-08-16

### **feat

- *(messaging)* Add signing support and PostgreSQL messaging backend**


### **fix

- *(build, docs)* Disable Hangar publishing due to upload size limits** [skip ci]

- *(build, docs)* Restrict Hangar publishing to supported modules and update links** [skip ci]

## [0.1.0] - 2026-08-14

### **docs

- *(readme)* Update sections with installation guides and API reference**

### **feat

- *(build, docs)* Add Dokka integration and Dockerfile for HTML site generation**

- *(commands)* Add more confirmations for destructive actions**

- *(core, build)* Improve permission handling and driver lifecycle management**

### **fix

- *(docs)* Update punctuation for clarity and consistency across documentation**

### Features

- *(core-api, commands)* Enhance DI support and payload-specific command refresh handling

- *(build)* Add gradeway-release plugin and automate multi-platform publishing

- *(core, build)* Streamline imports and enhance environment configuration

- *(core-api)* Remove `MessagingAuthenticator`, refactor brokers for modular lifecycle contracts

- *(build)* Add renovate configuration and improve Gradle publishing metadata

- *(skills)* Add `calc-ai-code-proportion` to analyze code composition

- *(build)* Centralize repository declarations for simplified dependency management

- *(docs)* Enhance visual identity and expand architecture documentation

- *(docs)* Migrate database schema to new markdown format

- *(docs)* Add compatibility matrix and expand project documentation

- *(github)* Add contributing docs, issue templates, and CI updates

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


