import gradle.kotlin.dsl.accessors._7efdda50f9f959eb80d21b302d04c803.shadowJar

plugins {
    id("com.modrinth.minotaur")
    id("io.papermc.hangar-publish-plugin")
}

val moduleName = findProperty("module.name") as? String ?: project.name
val moduleVersion = findProperty("module.version") as? String ?: rootProject.version.toString()

val supportedMinecraftVersions = arrayOf(
    "26.1",
    "26.1.1",
    "26.1.2",
    "26.2"
)

val hangarApiKey = findProperty("gradeway.hangar.apiKey") as? String
val modrinthToken = findProperty("gradeway.modrinth.token") as? String

// the mappings for Modrinth loader types
val moduleLoaders = when (project.name) {
    "plugin-bukkit" -> listOf("bukkit", "spigot")
    "plugin-paper" -> listOf("paper", "purpur")
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

if (hangarApiKey != null) {
    hangarPublish {
        publications.register("plugin") {
            id = "gradeway"
            version = moduleVersion
            channel = "Release"

            apiKey = hangarApiKey

            platforms {
                if (project.name == "plugin-paper") {
                    paper {
                        jar = tasks.shadowJar.flatMap { it.archiveFile }
                        platformVersions.addAll(*supportedMinecraftVersions)
                    }
                }

                if (project.name == "plugin-velocity") {
                    velocity {
                        jar = tasks.shadowJar.flatMap { it.archiveFile }
                        platformVersions.addAll(*supportedMinecraftVersions)
                    }
                }
            }
        }
    }
}

tasks {
    if (modrinthToken != null) {
        modrinth.configure {
            dependsOn(tasks.modrinthSyncBody)
        }
    }

    val releaseDependencies = listOfNotNull(
        if (modrinthToken != null) tasks.modrinth else null,
        if (hangarApiKey != null) tasks.publishAllPublicationsToHangar else null,
    )

    if (releaseDependencies.isNotEmpty()) {
        register("release") {
            group = "publishing"
            description = "Release the plugin to all configured platforms"

            dependsOn(releaseDependencies)
        }
    }
}
