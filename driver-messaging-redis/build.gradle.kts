plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core-api"))

    implementation("redis.clients:jedis:7.5.2")
}
