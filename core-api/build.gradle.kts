plugins {
    id("gradeway-base")
    id("gradeway-dokka")
    id("gradeway-publish")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    compileOnly(libs.google.devtools.ksp)
    compileOnly(libs.akuleshov7.ktoml.core)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.apache.commons.compress)

    api(libs.caffeine)
    api(libs.koin.core)
    api(libs.arrow.core)
    api(libs.kyori.adventure)
    api(libs.bundles.exposed)
    api(libs.mojang.brigadier)
}
