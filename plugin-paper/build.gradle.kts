plugins {
    id("gradeway-base")
    id("gradeway-shadow")
    id("gradeway-release")
    id("org.jetbrains.kotlin.plugin.serialization")
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
