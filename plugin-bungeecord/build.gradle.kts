plugins {
    id("gradeway-base")
    id("gradeway-shadow")
    id("gradeway-release")
    id("gradeway-artifact-metadata")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("de.eldoria.plugin-yml.bungee") version "0.9.0"
}

dependencies {
    implementation(project(":core-common"))

    implementation("org.incendo:cloud-bungee:2.0.0-beta.17")
    implementation("net.kyori:adventure-platform-bungeecord:4.4.1")

    implementation("net.md-5:bungeecord-api:1.21-R0.4")
}

gradewayArtifactMetadata {
    exclude = setOf(
        "org.slf4j",
        "com.mojang:brigadier",
        "org.jspecify:jspecify",
        "org.jetbrains:annotations",
        "net.md-5"
    )
}

tasks {
    shadowJar {
        include(
            "dev/gradienttim/gradeway/**",
            "_GRADEWAY/**.txt",
            "languages/**",
            "bungee.yml"
        )
    }
}

bungee {
    main = "dev.gradienttim.gradeway.bungee.GradewayPlugin"

    name = "gradeway"
    description = "A Minecraft permission gateway."

    version = rootProject.version.toString()

    author = "GradientTim"
}
