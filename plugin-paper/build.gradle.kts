plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":core-common"))

    implementation("org.incendo:cloud-paper:2.0.0-beta.17")
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.9-alpha")
}
