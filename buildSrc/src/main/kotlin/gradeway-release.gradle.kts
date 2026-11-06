import gradle.kotlin.dsl.accessors._7efdda50f9f959eb80d21b302d04c803.shadowJar

plugins {
    id("com.modrinth.minotaur")
    id("io.papermc.hangar-publish-plugin")
}

val moduleName = findProperty("module.name") as? String ?: project.name
val moduleVersion = rootProject.version.toString()

val supportedMinecraftVersions = arrayOf(
    "26.1",
    "26.1.1",
    "26.1.2",
    "26.2",
    "26.3",
)

val supportedVelocityVersions = arrayOf(
    "4.3.0",
    "4.3.1"
)

val hangarApiKey = findProperty("gradeway.hangar.apiKey") as? String
val modrinthToken = findProperty("gradeway.modrinth.token") as? String

val readMeContent = rootProject.file("README.md").readText()

data class ModuleInfo(val loaders: List<String>, val prettyName: String)

// the mappings for Modrinth loader types
val moduleInfo = when (project.name) {
    "plugin-bukkit" -> ModuleInfo(listOf("bukkit", "spigot"), "Bukkit")
    "plugin-paper" -> ModuleInfo(listOf("paper", "purpur", "folia"), "Paper")
    "plugin-bungeecord" -> ModuleInfo(listOf("bungeecord"), "BungeeCord")
    "plugin-velocity" -> ModuleInfo(listOf("velocity"), "Velocity")
    else -> null
}

if (modrinthToken != null) {
    require(moduleInfo != null) {
        "No Modrinth loader mapped for project '${project.name}' - add one to moduleLoaders in gradeway-release.gradle.kts"
    }

    modrinth {
        token.set(modrinthToken)

        projectId.set("gradeway")
        versionType.set("release")
        versionNumber.set(moduleVersion)
        versionName.set(moduleVersion + " - " + moduleInfo.prettyName)

        uploadFile.set(tasks.shadowJar.flatMap { it.archiveFile })
        syncBodyFrom = readMeContent

        loaders.addAll(moduleInfo.loaders)
        gameVersions.addAll(*supportedMinecraftVersions)
    }
}

val hangarSupported = hangarApiKey != null && project.name in listOf("plugin-paper", "plugin-velocity")

if (hangarSupported) {
    hangarPublish {
        publications.register("plugin") {
            id = "gradeway"
            version = moduleVersion
            channel = "Release"

            apiKey = hangarApiKey

            pages.resourcePage(readMeContent)

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
                        platformVersions.addAll(*supportedVelocityVersions)
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
        if (hangarSupported) tasks.publishAllPublicationsToHangar else null,
    )

    if (releaseDependencies.isNotEmpty()) {
        register("release") {
            group = "publishing"
            description = "Release the plugin to all configured platforms"

            dependsOn(releaseDependencies)
        }
    }
}
