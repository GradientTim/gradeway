import dev.detekt.gradle.Detekt

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
}

group = property("project.group") as String
version = property("project.version") as String

repositories {
    mavenCentral()
}

detekt {
    config.setFrom(".config/detekt.yml")
    source.setFrom(files(projectDir))
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        licenseHeaderFile(rootProject.file(".assets/LICENSE_HEADER"))
    }
}

tasks {
    withType<Detekt>().configureEach {
        exclude("**/Build*.kt")
    }

    register("bumpVersion") {
        group = "release"
        description =
            "Bumps project.version in gradle.properties. Usage: ./gradlew bumpVersion -Ptype=[major|minor|patch]"

        doLast {
            val type = (findProperty("type") as String?)?.lowercase() ?: "patch"
            require(type in setOf("major", "minor", "patch")) {
                "Unknown bump type '$type', expected 'major', 'minor' or 'patch'"
            }

            val (major, minor, patch) = rootProject.version.toString().split(".").map { it.toInt() }
            val newVersion = when (type) {
                "major" -> "${major + 1}.0.0"
                "minor" -> "$major.${minor + 1}.0"
                else -> "$major.$minor.${patch + 1}"
            }

            val propsFile = rootProject.file("gradle.properties")
            propsFile.writeText(
                propsFile.readText().replace(
                    "project.version=${rootProject.version}",
                    "project.version=$newVersion",
                ),
            )

            rootProject.extra["bumpedVersion"] = newVersion
            logger.lifecycle("Bumped project.version: ${rootProject.version} -> $newVersion")
        }
    }

    register<Exec>("generateChangelog") {
        group = "release"
        description =
            "Regenerates CHANGELOG.md via git-cliff, tagged with the current (or just-bumped) project.version."

        doFirst {
            val version = rootProject.extra.takeIf { it.has("bumpedVersion") }?.get("bumpedVersion") as String?
                ?: rootProject.version.toString()
            commandLine("git-cliff", "--tag", "v$version", "-o", "CHANGELOG.md")
        }
    }

    named("bumpVersion") {
        finalizedBy("generateChangelog")
    }
}
