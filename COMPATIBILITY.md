# Platforms & Databases

## Platforms

| Platform   | Type   | Module              |
|------------|--------|---------------------|
| Bukkit     | Server | `plugin-bukkit`     |
| Paper      | Server | `plugin-paper`      |
| BungeeCord | Proxy  | `plugin-bungeecord` |
| Velocity   | Proxy  | `plugin-velocity`   |

## Databases

| Backend    | Type      | Module                      |
|------------|-----------|-----------------------------|
| PostgreSQL | Database  | `driver-database-postgres`  |
| MySQL      | Database  | `driver-database-mysql`     |
| MariaDB    | Database  | `driver-database-mariadb`   |
| SQLite     | Database  | `driver-database-sqlite`    |
| SQL Server | Database  | `driver-database-sqlserver` |
| Oracle     | Database  | `driver-database-oracle`    |
| H2         | Database  | `driver-database-h2`        |
| Redis      | Messaging | `driver-messaging-redis`    |

Every backend builds into its own driver jar. Drop the ones you need into the `drivers/` folder next to the plugin -
only the client libraries for the drivers you actually use get loaded.
