plugins {
    id("gradeway-base")
    id("gradeway-shadow")
    id("gradeway-release")
    id("gradeway-artifact-metadata")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

@Suppress("DataClassEqualsAndHashCodeShareKey")
dependencies {
    implementation(project(":core-common"))

    compileOnly("com.velocitypowered:velocity-api:4.2.0")
    kapt("com.velocitypowered:velocity-api:4.2.0")
}

gradewayArtifactMetadata {
    exclude = setOf(
        "org.slf4j",
        "net.kyori",
        "com.mojang:brigadier",
        "org.jspecify:jspecify",
        "org.jetbrains:annotations",
        "com.velocitypowered"
    )
}

tasks {
    shadowJar {
        include(
            "dev/gradienttim/gradeway/**",
            "_GRADEWAY/**.txt",
            "languages/**",
            "velocity-plugin.json"
        )
    }
}
