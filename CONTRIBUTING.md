# Contributing to Gradeway

Thanks for your interest in contributing! Gradeway is still a work-in-progress project, so expect some rough edges –
both in the code and in this guide.

By participating in this project, you agree to abide by the [Code of Conduct](./CODE_OF_CONDUCT.md).

## Getting help / discussing changes

Before starting significant work (a new driver, a new command, a change to the config model), it's worth discussing it
first:

- Open a [GitHub issue](https://github.com/GradientTim/gradeway/issues) describing the bug or feature.
- Or drop by the [Discord server](https://discord.gg/f35EemU4jS) – it's the fastest way to get feedback before you
  invest time in an implementation.

## Project layout

See the "Module layout" and "Architecture" sections of [CLAUDE.md](./CLAUDE.md) for a detailed breakdown of `core-api`,
`core-common`, the `driver-*` modules, and the platform plugins (`plugin-bukkit`, `plugin-paper`, `plugin-bungeecord`,
`plugin-velocity`). Read that before making structural changes - it documents where new code is expected to live (e.g.,
new database/messaging backends follow the `driver-database-*` pattern, platform-specific config goes in that platform's
own config class, not into `core-api`/`core-common`).

## Development setup

Requires JDK 25.

```bash
./gradlew build                      # build all modules
./gradlew :core-common:build         # build a single module
./gradlew detekt                     # static analysis (config: .config/detekt.yml)
./gradlew spotlessCheck              # check formatting/license headers
./gradlew spotlessApply              # fix formatting/license headers
./gradlew :core-common:test          # run all core-common tests (the only module with tests)
```

Run a single test:

```bash
./gradlew :core-common:test --tests "dev.gradienttim.gradeway.extensions.JavaExtensionsTest"
./gradlew :core-common:test --tests "*.JavaExtensionsTest.isUuid accepts valid uuids*"
```

Tests use `kotlin.test` on JUnit Platform with backtick-quoted test names, e.g.
`` fun `isUuid accepts valid uuids`() ``. New tests belong under `core-common/src/test` - it is currently the only
module with test sources.

## Before opening a pull request

CI (`.github/workflows/core-common-ci.yml`) runs `detekt`, then `spotlessCheck`, then
`:core-common:test`, then `:core-common:build`, in that order, against JDK 25. Run the same sequence locally before
opening a PR:

```bash
./gradlew detekt
./gradlew spotlessCheck
./gradlew :core-common:test
./gradlew :core-common:build
```

Additional checks depending on what you touched:

- **Any new `.kt` file** needs the MIT license header from `.assets/LICENSE_HEADER`
  (`spotlessApply` adds it automatically; `spotlessCheck` verifies it).
- **Any command builder change** (`core-common/.../commands/**`) - run the
  `sync-translation-keys` check/skill to reconcile `Component.translatable(...)` keys against
  `core-common/src/main/resources/languages/en.properties`, including the `$entityType`-templated keys used by the
  generic entity-permission/attribute helpers. User-facing text must never be hardcoded.
- **Manager/service methods that can fail** should return Arrow's `Either<Throwable, T>`
  (`either { ... }` / `raise(...)`), not throw exceptions or return null - follow the existing convention.

## Code style

- Kotlin 2.4.10, JDK 25, `-Xexplicit-backing-fields` is enabled in `core-api`.
- `warningsAsErrors` is enabled for the Kotlin compiler at the root build - new code must be warning-free rather than
  suppressing warnings.
- Formatting/license headers are enforced by Spotless; run `./gradlew spotlessApply` rather than hand-formatting.
- Static analysis is enforced by detekt (`.config/detekt.yml`).

## Commit / PR expectations

- Keep PRs focused – one logical change per PR is easier to review than a bundle of unrelated fixes.
- Write commit messages and PR descriptions that explain *why* a change was made, not just what changed.
- Link the related issue/discussion if one exists.
- Make sure `detekt`, `spotlessCheck`, and `:core-common:test` all pass locally - CI will run the same checks and PRs
  that fail them won't be merged.

## Reporting bugs / requesting features

Use [GitHub issues](https://github.com/GradientTim/gradeway/issues). Since the project is pre-release, please check
existing issues first – you may be hitting a known limitation. For quick questions or discussion,
the [Discord server](https://discord.gg/f35EemU4jS) is usually faster than an issue.
