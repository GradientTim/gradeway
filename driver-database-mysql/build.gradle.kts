plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core-api"))

    implementation("mysql:mysql-connector-java:8.0.33")
}
