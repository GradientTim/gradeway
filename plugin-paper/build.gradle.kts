import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("gradeway-base")
    id("gradeway-shadow")
    id("gradeway-release")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("de.eldoria.plugin-yml.paper") version "0.9.0"
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(project(":core-common"))
    implementation(project(":plugin-bukkit-shared"))

    implementation("org.incendo:cloud-paper:2.0.0-beta.17")
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.9-alpha")
}

tasks {
    shadowJar {
        // defining what to include is simpler than excluding every "dependency" from core-common/core-api
        include(
            "dev/gradienttim/gradeway/**",
            "_GRADEWAY/**",
            "languages/**",
            "paper-libraries.json",
            "paper-plugin.yml"
        )
    }
}

paper {
    main = "dev.gradienttim.gradeway.paper.GradewayPlugin"
    loader = "dev.gradienttim.gradeway.paper.GradewayPluginLoader"

    name = "gradeway"
    prefix = "Gradeway"
    description = "A Minecraft permission gateway."

    version = rootProject.version.toString()
    apiVersion = "26.1"

    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    website = "https://github.com/GradientTim/Gradeway"

    foliaSupported = true
    hasOpenClassloader = false
    generateLibrariesJson = false

    authors = listOf("GradientTim")
}
