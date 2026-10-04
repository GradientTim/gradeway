import dev.detekt.gradle.Detekt

plugins {
    id("dev.detekt")
    id("com.diffplug.spotless")
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(25)
}

detekt {
    config.setFrom(rootProject.file(".data/configs/detekt.yml"))
    source.setFrom(files(projectDir))
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        licenseHeaderFile(rootProject.file(".data/assets/LICENSE_HEADER"))
    }
}

tasks {
    withType<Detekt> {
        exclude("**/Build*.kt")
    }
}
