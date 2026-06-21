plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
    alias(libs.plugins.ksp) apply false
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
