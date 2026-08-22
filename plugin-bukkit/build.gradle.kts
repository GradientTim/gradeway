import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("gradeway-base")
    id("gradeway-shadow")
    id("gradeway-release")
    id("gradeway-artifact-metadata")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("de.eldoria.plugin-yml.bukkit") version "0.9.0"
}

dependencies {
    api(project(":core-common"))
    implementation(project(":plugin-bukkit-shared"))

    implementation("org.incendo:cloud-paper:2.0.0-beta.17")
    implementation("net.kyori:adventure-platform-bukkit:4.4.1")

    compileOnly("org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT")
}

gradewayArtifactMetadata {
    exclude = setOf(
        "org.slf4j",
        "com.mojang:brigadier",
        "org.jspecify:jspecify",
        "org.jetbrains:annotations"
    )
}

tasks {
    shadowJar {
        // defining what to include is simpler than excluding every "dependency" from core-common/core-api
        include(
            "dev/gradienttim/gradeway/**",
            "_GRADEWAY/**.txt",
            "languages/**",
            "plugin.yml"
        )
    }
}

bukkit {
    main = "dev.gradienttim.gradeway.bukkit.GradewayPlugin"

    name = "gradeway"
    prefix = "Gradeway"
    description = "A Minecraft permission gateway."

    version = rootProject.version.toString()
    apiVersion = "26.1"

    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    website = "https://github.com/GradientTim/Gradeway"

    authors = listOf("GradientTim")
}
