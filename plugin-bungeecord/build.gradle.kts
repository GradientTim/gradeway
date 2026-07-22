plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net/")
}

dependencies {
    implementation(project(":core-common"))

    implementation("org.incendo:cloud-bungee:2.0.0-beta.17")
    implementation("net.kyori:adventure-platform-bungeecord:4.4.1")

    implementation("net.md-5:bungeecord-api:1.21-R0.4")
}

tasks {
    shadowJar {
        filesMatching("META-INF/*.kotlin_module") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}
