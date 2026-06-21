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

    shadow("com.h2database:h2:2.4.240")
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
