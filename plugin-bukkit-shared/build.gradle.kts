plugins {
    id("gradeway-base")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    compileOnly(project(":core-common"))
    compileOnly("org.spigotmc:spigot-api:1.20.2-experimental-SNAPSHOT")
}
