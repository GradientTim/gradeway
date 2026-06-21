plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

@Suppress("DataClassEqualsAndHashCodeShareKey")
dependencies {
    ksp(project(":core-api"))
    implementation(project(":core-api"))

    shadow("org.postgresql:postgresql:42.7.8")
}

tasks {
    jar {
        dependsOn(shadowJar)
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())
        archiveClassifier.set("")
    }
}
