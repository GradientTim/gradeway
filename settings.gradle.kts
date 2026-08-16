plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
        maven("https://libraries.minecraft.net/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/groups/public/") {
            content {
                includeGroup("org.bukkit")
                includeGroup("org.spigotmc")
            }
        }
    }
}

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
    "driver-messaging-postgres",
    "driver-messaging-redis",
)

include(
    "plugin-bukkit",
    "plugin-bukkit-shared",
    "plugin-bungeecord",
    "plugin-paper",
    "plugin-velocity",
)
