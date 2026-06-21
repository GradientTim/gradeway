plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
}

dependencies {
    api(project(":core-api"))
    implementation("com.mojang:brigadier:1.0.18")

    implementation("org.apache.commons:commons-configuration2:2.11.0")

    api(libs.koin.core)
    api(libs.bundles.exposed)
    api(libs.bundles.ktoml)
    api(libs.bundles.kyori)
    api(libs.bundles.arrow)
}
