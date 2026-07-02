plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.google.devtools.ksp)
    implementation(libs.kotlinx.serialization.json)

    api(libs.koin.core)
    api(libs.kyori.adventure)
    api(libs.bundles.exposed)
    api(libs.bundles.arrow)
    compileOnly(libs.bundles.ktoml)
}

tasks {
    compileKotlin {
        compilerOptions {
            freeCompilerArgs.set(listOf("-Xexplicit-backing-fields"))
        }
    }
}
