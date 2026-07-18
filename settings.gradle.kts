rootProject.name = extra["project.name"] as String

include(
    "core-api",
    "core-common",
)

include(
    "driver-database-h2",
    "driver-database-mariadb",
    "driver-database-mysql",
    "driver-database-oracle",
    "driver-database-postgres",
    "driver-database-sqlite",
    "driver-database-sqlserver",
)

include(
    "driver-messaging-redis",
)

include(
    "plugin-bukkit",
    "plugin-bukkit-shared",
    "plugin-bungeecord",
    "plugin-paper",
    "plugin-velocity",
)
