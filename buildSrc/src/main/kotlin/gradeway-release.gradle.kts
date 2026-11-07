import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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

val releaseDebugMode = (findProperty("gradeway.release.debug") as? String)?.toBoolean() ?: false

val hangarApiKey = findProperty("gradeway.hangar.apiKey") as? String
val modrinthToken = findProperty("gradeway.modrinth.token") as? String

val readMeContent = rootProject.file("README.md").readText()

val shadowJarFile = tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile }

val releaseNotes = providers.of(GitHubReleaseNotes::class) {
    parameters.repository.set("GradientTim/gradeway")
    parameters.tag.set("v$moduleVersion")
}

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
        versionName.set("$moduleVersion - ${moduleInfo.prettyName}")
        changelog.set(releaseNotes)

        uploadFile.set(shadowJarFile)
        syncBodyFrom = readMeContent

        debugMode.set(releaseDebugMode)

        loaders.addAll(moduleInfo.loaders)
        gameVersions.addAll(*supportedMinecraftVersions)
    }
}

configurations.consumable("releaseJar") {
    outgoing.artifact(shadowJarFile)
}

// Hangar keeps all platforms of a version in one entry, so plugin-paper uploads the Velocity jar too
val hangarSupported = !releaseDebugMode && hangarApiKey != null && project.name == "plugin-paper"

if (hangarSupported) {
    val velocityRelease = configurations.dependencyScope("velocityRelease")
    val velocityReleaseJar = configurations.resolvable("velocityReleaseJar") {
        extendsFrom(velocityRelease.get())
    }

    dependencies {
        add(velocityRelease.name, project(path = ":plugin-velocity", configuration = "releaseJar"))
    }

    hangarPublish {
        publications.register("plugin") {
            id = "gradeway"
            version = moduleVersion
            channel = "Release"
            changelog = releaseNotes

            apiKey = hangarApiKey

            pages.resourcePage(readMeContent)

            platforms {
                paper {
                    jar = shadowJarFile
                    platformVersions.addAll(*supportedMinecraftVersions)
                }

                velocity {
                    jar = layout.file(velocityReleaseJar.flatMap { it.elements }.map { it.single().asFile })
                    platformVersions.addAll(*supportedVelocityVersions)
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
