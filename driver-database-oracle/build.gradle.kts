plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core-api"))

    implementation("com.oracle.database.jdbc:ojdbc8:23.26.0.0.0")
}
