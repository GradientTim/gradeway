## Description

<!-- What does this PR change, and why? Link any related issue/discussion. -->

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] New driver (`driver-database-*` / `driver-messaging-*`)
- [ ] Refactor / internal change
- [ ] Documentation

## Checklist

- [ ] `./gradlew detekt` passes
- [ ] `./gradlew spotlessCheck` passes (or `spotlessApply` was run)
- [ ] `./gradlew :core-common:test` passes
- [ ] `./gradlew :core-common:build` passes
- [ ] New/changed `.kt` files have the MIT license header
- [ ] If a command builder changed: translation keys reconciled against `en.properties`
  (`sync-translation-keys`)
- [ ] Manager/service methods that can fail return `Either<Throwable, T>`, not exceptions or null
- [ ] Added/updated tests where applicable (`core-common/src/test`)
