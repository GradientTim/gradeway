---
name: sync-translation-keys
description: Reconcile core-common/src/main/resources/languages/en.properties against the Component.translatable(...) keys actually referenced in the Kotlin command builders under core-common/src/main/kotlin/dev/gradienttim/gradeway/commands/, including the $entityType-templated keys used by the generic createEntityPermissionHandler/createEntityAttributeHandler helpers in CommandHelpers.kt. Use when asked to check for missing/unused/orphaned translation keys, audit en.properties after touching a command builder, or add new command messages and need the matching properties entries.
---

# Sync translation keys

Run the bundled script to get the exact add/remove diff instead of grepping by hand:

```bash
python3 .claude/skills/sync-translation-keys/scripts/diff_keys.py <repo_root>
```

It extracts every literal string passed to `Component.translatable(...)` across all
`.kt` files in the commands directory, and diffs that set against the keys in
`en.properties`. Output is two lists: `REMOVE` (stale keys, safe to delete outright)
and `ADD` (keys referenced in code but missing from the properties file).

`$entityType`-templated keys (used by generic helpers like
`createEntityPermissionHandler`/`createEntityAttributeHandler` in `CommandHelpers.kt`)
are expanded fully automatically: the script finds every top-level function that takes
an `entityType: String` parameter, reads the `$entityType` keys in its body, finds
every call site of that function anywhere in the commands directory, and expands using
whatever entity strings are actually passed there. Nothing needs to be told to the
script by hand when commands change - new sub-commands, new entity types, and brand
new `$entityType`-templated helper functions are all picked up on the next run. If it
ever can't attribute a `$entityType` key to a function (e.g. a helper stops using a
block body), it prints a warning to stderr rather than silently misreporting.

## Applying the diff

**REMOVE**: just delete those lines, no replacement needed.

**ADD**: for each missing key, don't invent wording from scratch — read the call site
in the `.kt` file to see exactly which `Component.text(...)` args are passed and in
what order (that's your `<arg:N>` count), then find the closest analogous *existing*
key in `en.properties` (same suffix — e.g. `clearPermissions.success` for a new
`clearAttributes.success`, `permission.add.invalidType` for a new
`*.attributeTypeNotRegistered`) and copy its exact tag structure
(`<prefix>`, `<red>`/`<gray>`/`<green>`, `<primary>`/`<secondary>`), only swapping the
noun/arg indices. Insert the new line at the position matching the code's declaration
order within its command group, not at the end of the file.

Watch for two recurring patterns in this codebase:
- A key's args don't always map 1:1 to its name — e.g. some `invalidUuid`/`invalidName`
  keys only reference `<arg:1>` and leave `<arg:0>` (the `idOrName`) unused in the
  message text. Match what the call site actually passes, not what seems intuitive.
- The generic-helper keys (`setPermission`, `clearAttributes`, etc.) are shared across
  `group`/`player`/`role` — when one is missing for one entity type, check whether
  the identical suffix already exists for a sibling entity type and mirror its wording
  exactly, only swapping the entity noun (player/role/group).

## After editing

Re-run the script — both `REMOVE` and `ADD` should be empty, with no warnings on
stderr. Also spot-check for duplicate keys:
`grep -oP '^gradeway\.[a-zA-Z0-9._]+(?==)' en.properties | sort | uniq -d` should print
nothing.
