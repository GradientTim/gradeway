plugins {
    id("gradeway-base")
    id("gradeway-publish")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation(libs.google.devtools.ksp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.apache.commons.compress)

    api(libs.caffeine)
    api(libs.koin.core)
    api(libs.kyori.adventure)
    api(libs.bundles.exposed)
    api(libs.bundles.arrow)
    compileOnly(libs.bundles.ktoml)
}
