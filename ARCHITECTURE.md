# Architecture: Player, Role & Group model

Gradeway's permission model has three core entities: **Player**, **Role**, and **Group**. If you've used LuckPerms or a
similar plugin before, the naming here is a common source of confusion — in particular, **Gradeway's "group" is a
different concept as a LuckPerms "group"**. This document explains what each entity actually is, how they relate, and
how permissions and weight are resolved.

For the underlying table layout, see [DATABASE.md](./DATABASE.md).

## TL;DR for LuckPerms users

| LuckPerms concept                                                                                 | Gradeway equivalent                           |
|---------------------------------------------------------------------------------------------------|-----------------------------------------------|
| A "group" (e.g. `admin`, `default`) assigned to a user, with permissions, weight, and inheritance | **Role**                                      |
| Group inheritance (`parent` groups)                                                               | **Role** parents (`RoleParentEntity`)         |
| A user's "primary group"                                                                          | A player's **primary role** (`primaryRoleId`) |
| (no real equivalent)                                                                              | **Group** — see below                         |

In other words: what you assign to a player, give permissions to, weight, and chain via inheritance is called a **Role**
in Gradeway, not a Group. Gradeway's **Group** is a separate, coarser concept: a container that a set of roles can
belong to, used to share permissions/templates across those roles and to provide a fallback weight. Groups are never
assigned to players directly.

## The three entities

### Player

A `PlayerEntity` represents a single Minecraft player (keyed by UUID). A player can have:

- its own directly assigned permissions, permission templates, and attributes
- an explicit `weight` (`-1` means "not set", see [Weight resolution](#weight-resolution))
- any number of assigned **roles**, via `PlayerRoleEntity` — each assignment can be paused (`pausedAt`), time-limited
  (`untilAt`), and one of them can be flagged as the player's **primary role**
  (`primaryRoleId` on the player)

Source: `core-api/.../entity/player/PlayerEntity.kt`, `PlayerRoleEntity.kt`.

### Role

A `RoleEntity` is the thing you actually assign to players to grant them permissions — this is the concept LuckPerms
calls a "group". A role has:

- its own permissions, permission templates, and attributes
- a `weight`, used to decide priority when a player has multiple roles (e.g., for prefix/suffix selection or precedence)
- **parent roles** (`RoleParentEntity`) — a role inherits its parents' permissions and attributes recursively, the same
  way LuckPerms group inheritance works
- membership in any number of **groups** (`RoleGroupEntity`)

Source: `core-api/.../entity/role/RoleEntity.kt`, `RoleParentEntity.kt`, `RoleGroupEntity.kt`.

### Group

A `GroupEntity` is *not* assignable to a player. It's a container for roles: a role can belong to zero or more groups
via `RoleGroupEntity`. Groups exist to let a set of related roles share:

- permissions and permission templates that apply to every role in the group
- a `defaultWeight`, used as a fallback for a role's weight when the role itself has no explicit weight (see below)

Practically, groups are useful for organizing many roles that should share a baseline (e.g., all
"staff" roles sharing a moderation permission set) without duplicating permissions across each role or forcing them into
a single inheritance chain.

Source: `core-api/.../entity/group/GroupEntity.kt`, `RoleService.kt`.

## Relationships

Key points:

- A player can hold many roles at once; one may be flagged **primary**.
- A role can have multiple parent roles, and parents can themselves have parents — inheritance is resolved recursively.
- A role can belong to multiple groups, and a group can contain multiple roles. This is a plain many-to-many
  association, not a hierarchy — groups don't nest and don't inherit from each other.
- Groups sit "beside" roles, not above players. A player is never a member of a group directly.

## Weight resolution

Weight determines priority (e.g., which role should "win" for display purposes) and is resolved with a fallback chain at
each level:

- **Role effective weight** (`RoleService.getEffectiveWeight`): the role's own `weight` if set, otherwise the highest
  `defaultWeight` among the groups it belongs to, otherwise `0`.
- **Player effective weight** (`PlayerService.getEffectiveWeight`): the player's own `weight` if set, otherwise the
  highest effective weight among their active (non-expired, non-paused) roles, otherwise
  `0`.

## Effective permission resolution

`PermissionService.getEffective{Role,Player}Permissions` compute the full, flattened permission set:

- **Effective role permissions**: the role's own enabled permissions, plus permissions from its assigned permission
  templates, plus permissions from every group it belongs to, plus permissions inherited from its parent roles —
  recursively.
- **Effective player permissions**: the player's own enabled permissions, plus permissions from their assigned
  templates, plus the effective permissions of every *active* role assigned to them (active meaning not paused and not
  expired). Each role's effective permissions already fold in its groups and parent chain as described above.

Results are cached per entity (`gradeway.caches.*EffectivePermissions`) and invalidated on any change to a player, role,
group, permission, or template — see `CommonPermissionService.invalidateFor`.
