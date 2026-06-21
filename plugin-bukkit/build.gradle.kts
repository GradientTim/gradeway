plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":core-common"))

    compileOnly("io.papermc.paper:paper-api:26.1.2.build.9-alpha")
}
