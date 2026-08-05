plugins {
    id("gradeway-base")
    id("gradeway-shadow")
    id("gradeway-release")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

@Suppress("DataClassEqualsAndHashCodeShareKey")
dependencies {
    implementation(project(":core-common"))

    implementation("org.incendo:cloud-velocity:2.0.0-beta.17")

    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    testImplementation(kotlin("test"))
}
