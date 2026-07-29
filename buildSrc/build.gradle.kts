plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(gradlePlugins.shadow)
    implementation(gradlePlugins.detekt)
    implementation(gradlePlugins.spotless)

    implementation(gradlePlugins.google.ksp)
    implementation(gradlePlugins.vanniktech.publish)

    implementation(gradlePlugins.jetbrains.kotlin)
    implementation(gradlePlugins.jetbrains.kotlin.kapt)
    implementation(gradlePlugins.jetbrains.kotlin.plugin.serialization)
}
