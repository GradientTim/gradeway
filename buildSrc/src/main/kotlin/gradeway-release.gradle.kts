import gradle.kotlin.dsl.accessors._7efdda50f9f959eb80d21b302d04c803.shadowJar

plugins {
    id("com.modrinth.minotaur")

    // Hangar publishing is disabled for now - plugin-paper's shaded jar (~16.4MB even after
    // shadowJar { minimize() }) exceeds Hangar's 15MB per-version upload limit, and the classes
    // pushing it over (kotlin-reflect, Exposed's DAO layer) are genuinely load-bearing, not dead
    // code that can be safely stripped. Re-enable once there's a real fix (e.g., loading the
    // persistence layer through the same dynamic driver-loading mechanism used for
    // driver-database-*, instead of shading it directly into the platform jar).
    // id("io.papermc.hangar-publish-plugin")
}

val moduleName = findProperty("module.name") as? String ?: project.name
val moduleVersion = rootProject.version.toString()

val supportedMinecraftVersions = arrayOf(
    "26.1",
    "26.1.1",
    "26.1.2",
    "26.2"
)

// val hangarApiKey = findProperty("gradeway.hangar.apiKey") as? String
val modrinthToken = findProperty("gradeway.modrinth.token") as? String

// the mappings for Modrinth loader types
val moduleLoaders = when (project.name) {
    "plugin-bukkit" -> listOf("bukkit", "spigot")
    "plugin-paper" -> listOf("paper", "purpur", "folia")
    "plugin-bungeecord" -> listOf("bungeecord")
    "plugin-velocity" -> listOf("velocity")
    else -> emptyList()
}

if (modrinthToken != null) {
    require(moduleLoaders.isNotEmpty()) {
        "No Modrinth loader mapped for project '${project.name}' - add one to moduleLoaders in gradeway-release.gradle.kts"
    }

    modrinth {
        token.set(modrinthToken)

        projectId.set("gradeway")
        versionType.set("release")
        versionNumber.set(moduleVersion)

        uploadFile.set(tasks.shadowJar.flatMap { it.archiveFile })
        syncBodyFrom = rootProject.file("README.md").readText()

        loaders.addAll(moduleLoaders)
        gameVersions.addAll(*supportedMinecraftVersions)
    }
}

// Hangar only recognizes PAPER, WATERFALL and VELOCITY as platforms - there is no plain "Bukkit"
// or "BungeeCord" platform, so those modules have nothing to publish there.
// val hangarSupported = hangarApiKey != null && project.name in listOf("plugin-paper", "plugin-velocity")
//
// if (hangarSupported) {
//     hangarPublish {
//         publications.register("plugin") {
//             id = "gradeway"
//             version = moduleVersion
//             channel = "Release"
//
//             apiKey = hangarApiKey
//
//             platforms {
//                 if (project.name == "plugin-paper") {
//                     paper {
//                         jar = tasks.shadowJar.flatMap { it.archiveFile }
//                         platformVersions.addAll(*supportedMinecraftVersions)
//                     }
//                 }
//
//                 if (project.name == "plugin-velocity") {
//                     velocity {
//                         jar = tasks.shadowJar.flatMap { it.archiveFile }
//                         platformVersions.addAll(*supportedMinecraftVersions)
//                     }
//                 }
//             }
//         }
//     }
// }

tasks {
    if (modrinthToken != null) {
        modrinth.configure {
            dependsOn(tasks.modrinthSyncBody)
        }
    }

    val releaseDependencies = listOfNotNull(
        if (modrinthToken != null) tasks.modrinth else null,
        // if (hangarSupported) tasks.publishAllPublicationsToHangar else null,
    )

    if (releaseDependencies.isNotEmpty()) {
        register("release") {
            group = "publishing"
            description = "Release the plugin to all configured platforms"

            dependsOn(releaseDependencies)
        }
    }
}
