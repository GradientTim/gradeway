plugins {
    id("gradeway-base")
    id("gradeway-shadow")
    id("gradeway-release")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    api(project(":core-common"))
    implementation(project(":plugin-bukkit-shared"))

    implementation("org.incendo:cloud-paper:2.0.0-beta.17")
    implementation("net.kyori:adventure-platform-bukkit:4.4.1")

    compileOnly("org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT")
}
