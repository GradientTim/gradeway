# TODO

## Features

- [X] **Security** – Block command blocks from executing gradeway commands.
- [ ] **Audit log** - record who changed what and when, viewable per player, role, and group, with undo for a single
  change.
- [ ] **Event system** – a native, platform-independent event API in `core-api` (e.g., a role added/removed, permission
  set/unset, promotion/demotion, a role expired) so other plugins and drivers can listen to and react to changes.
- [ ] **Discord log** - post every permission and role change to a Discord channel via webhook.
- [ ] **Permission lookup** – collect known permissions from installed plugins and observed checks, use them for
  tab-completion and searching ("what's the permission for /fly?"), and warn when setting a permission no plugin uses.
- [ ] **Tree** - `/gw tree` shows roles and their parents as a simple tree in chat.
- [ ] **Who can** - `/gw whocan <permission>` lists every player and role that effectively has a permission, including
  inherited ones (LuckPerms `/lp search` only finds directly set nodes).
- [ ] **Why** - `/gw why <player> <permission>` shows which role, parent, group, or template granted the permission,
  e.g. `Steve -> VIP -> Member -> template basic-cmds -> essentials.home`.
