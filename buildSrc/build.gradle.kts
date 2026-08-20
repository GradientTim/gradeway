plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(gradlePlugins.shadow)
    implementation(gradlePlugins.detekt)
    implementation(gradlePlugins.spotless)

    implementation(gradlePlugins.google.ksp)
    implementation(gradlePlugins.vanniktech.publish)

    implementation(gradlePlugins.modrinth.minotaur)
    implementation(gradlePlugins.papermc.hangar.publish)

    implementation(gradlePlugins.jetbrains.dokka)
    implementation(gradlePlugins.jetbrains.kotlin)
    implementation(gradlePlugins.jetbrains.kotlin.kapt)
    implementation(gradlePlugins.jetbrains.kotlin.plugin.serialization)

    // Fix Jackson compatibility for plugin-yml
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.1")
}
