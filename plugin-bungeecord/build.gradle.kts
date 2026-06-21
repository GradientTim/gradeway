plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
    maven("https://hub.spigotmc.org/nexus/repository/central-portal-snapshots")
}

dependencies {
    implementation(project(":core-api"))
    implementation(project(":core-common"))

    compileOnly("net.md-5:bungeecord-api:26.1-R0.1-SNAPSHOT")
}
