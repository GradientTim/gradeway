plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    api(project(":core-common"))
    implementation(project(":plugin-bukkit-shared"))

    implementation("org.incendo:cloud-paper:2.0.0-beta.17")
    implementation("net.kyori:adventure-platform-bukkit:4.4.1")

    compileOnly("org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveFileName.set("gradeway-plugin-bukkit-${rootProject.version}.jar")
        filesMatching("META-INF/*.kotlin_module") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}
