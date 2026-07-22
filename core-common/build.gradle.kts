import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Instant

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net/")
}

dependencies {
    api(project(":core-api"))

    implementation(libs.apache.commons.compress)
    implementation(libs.apache.commons.configuration2)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)

    implementation("io.github.cdimascio:dotenv-java:3.2.0")

    api(libs.koin.core)
    api(libs.bundles.exposed)
    implementation(libs.bundles.exposed.migration)
    api(libs.bundles.cloud)
    api("com.mojang:brigadier:1.3.10")
    api(libs.bundles.ktoml)
    api(libs.bundles.kyori)
    api(libs.bundles.arrow)

    testImplementation(kotlin("test"))
    testImplementation("com.h2database:h2:2.4.240")
}

val generatedSourceDir = layout.buildDirectory.dir("generated/sources/build-info/kotlin")

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(generatedSourceDir)
        }
    }
}

tasks {
    val generateBuildInfo by registering {
        description = "Generate a Kotlin file with project information in it."

        outputs.dir(generatedSourceDir)

        val packageName = "dev.gradienttim.gradeway"

        doLast {
            val projectVersion = rootProject.version.toString()

            val gitCommitHash = providers.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
            }.standardOutput.asText.map { it.trim() }.orElse("unknown")

            val gitDirty = providers.exec {
                commandLine("git", "status", "--porcelain")
            }.standardOutput.asText.map { it.isNotBlank() }.orElse(false)

            val buildTimestamp = Instant.now().toString()

            val packageDir = generatedSourceDir.get().asFile.resolve(packageName)
            packageDir.mkdirs()

            packageDir.resolve("BuildInfo.kt").writeText(
                """
            package $packageName

            object BuildInfo {
                const val VERSION: String = "$projectVersion"
                const val GIT_IS_DIRTY: Boolean = ${gitDirty.get()}
                const val GIT_COMMIT_HASH: String = "${gitCommitHash.get()}"
                const val BUILD_TIMESTAMP: String = "$buildTimestamp"
            }
        """.trimIndent()
            )
        }
    }

    withType<KotlinCompile>().configureEach {
        dependsOn(generateBuildInfo)
    }

    withType<org.gradle.jvm.tasks.Jar>().configureEach {
        dependsOn(generateBuildInfo)
    }

    test {
        useJUnitPlatform()
    }
}
