plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    implementation(project(":core-common"))

    implementation("org.incendo:cloud-bungee:2.0.0-beta.17")
    implementation("net.kyori:adventure-platform-bungeecord:4.4.1")

    implementation("net.md-5:bungeecord-api:1.21-R0.4")
}

tasks {
    shadowJar {
        archiveFileName.set("gradeway-plugin-bungeecord-${rootProject.version}.jar")
        filesMatching("META-INF/*.kotlin_module") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}
