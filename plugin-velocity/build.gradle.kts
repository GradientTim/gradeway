plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":core-api"))
    implementation(project(":core-common"))

    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
}
