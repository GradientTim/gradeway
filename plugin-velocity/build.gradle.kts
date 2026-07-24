plugins {
    alias(libs.plugins.kapt)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    implementation(project(":core-common"))

    implementation("org.incendo:cloud-velocity:2.0.0-beta.17")

    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    testImplementation(kotlin("test"))
}

tasks {
    shadowJar {
        filesMatching("META-INF/*.kotlin_module") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}
