plugins {
    id("org.jetbrains.kotlin.jvm")
}

open class GradewayArtifactMetadataExtension {
    var exclude: Set<String> = emptySet()
    var repositories: Map<String, String> = mapOf(
        "mavencentral" to "https://maven-central.storage-download.googleapis.com/maven2",
        "minecraft" to "https://libraries.minecraft.net/",
        "papermc" to "https://repo.papermc.io/repository/maven-public/",
        "spigot" to "https://hub.spigotmc.org/nexus/content/groups/public/"
    )
}

val extension = extensions.create<GradewayArtifactMetadataExtension>("gradewayArtifactMetadata")

val generatedResourceDir = layout.buildDirectory.dir("generated/resources/main")

tasks.register("generateArtifactMetadata") {
    description = "Generate artifact metadata for this module"

    outputs.dir(generatedResourceDir)

    doLast {
        val runtimeConfig = project.configurations.runtimeClasspath.get()

        val allDependencies = runtimeConfig.resolvedConfiguration.resolvedArtifacts
            .filter { it.moduleVersion.id.group != "gradeway" }
            .filter { artifact ->
                val group = artifact.moduleVersion.id.group
                val coordinates = "${group}:${artifact.moduleVersion.id.name}"

                !extension.exclude.any { pattern -> pattern == group || pattern == coordinates }
            }
            .map { artifact ->
                val moduleId = artifact.moduleVersion.id
                "${moduleId.group}:${moduleId.name}:${moduleId.version}"
            }
            .sorted()

        val gradewayDir = generatedResourceDir.get().asFile.resolve("_GRADEWAY")
        gradewayDir.mkdirs()

        gradewayDir.resolve("dependencies.txt").writeText(
            allDependencies.joinToString("\n")
        )

        gradewayDir.resolve("repositories.txt").writeText(
            extension.repositories.entries.joinToString("\n") { (id, url) ->
                "$id=$url"
            }
        )
    }
}

tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    dependsOn(tasks.named("generateArtifactMetadata"))
}

tasks.named("processResources") {
    dependsOn(tasks.named("generateArtifactMetadata"))
}

sourceSets {
    main {
        resources.srcDir(generatedResourceDir)
    }
}
