plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core-api"))

    implementation("org.xerial:sqlite-jdbc:3.50.2.0")
}
