import dev.gradienttim.buildmeta.helpers.registerEnvironmentMeta
import dev.gradienttim.buildmeta.helpers.registerGitMeta
import dev.gradienttim.buildmeta.helpers.registerProjectMeta

plugins {
    id("gradeway-base")
    id("gradeway-dokka")
    id("gradeway-publish")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("dev.gradienttim.buildmeta") version "0.1.1"
}

dependencies {
    api(project(":core-api"))

    api(libs.koin.core)
    api(libs.arrow.core)
    api(libs.mojang.brigadier)
    api(libs.akuleshov7.ktoml.core)

    api(libs.bundles.exposed)
    api(libs.bundles.adventure)

    implementation(libs.cdimascio.dotenv)
    implementation(libs.apache.commons.compress)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.bundles.exposed.migration)

    testImplementation(kotlin("test"))
    testImplementation("com.h2database:h2:2.5.252")
}

buildMeta {
    fallbackPackageName = "dev.gradienttim.gradeway"

    registerGitMeta()
    registerProjectMeta(
        useRootProjectFallback = true,
    )
    registerEnvironmentMeta(
        includeTimestamp = true,
    )
}

tasks {
    test {
        useJUnitPlatform()

        testLogging {
            showStandardStreams = true
        }
    }
}
