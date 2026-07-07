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

    implementation(libs.mojang.brigadier)
    implementation(libs.apache.commons.compress)
    implementation(libs.apache.commons.configuration2)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)

    api(libs.koin.core)
    api(libs.bundles.exposed)
    api(libs.bundles.ktoml)
    api(libs.bundles.kyori)
    api(libs.bundles.arrow)
}
