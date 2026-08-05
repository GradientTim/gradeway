import dev.detekt.gradle.Detekt

plugins {
    id("dev.detekt")
    id("com.diffplug.spotless")
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
}

detekt {
    config.setFrom(rootProject.file(".config/detekt.yml"))
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
    withType<Detekt> {
        exclude("**/Build*.kt")
    }
}
