plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":core-api"))

    implementation("redis.clients:jedis:7.5.2")
}

tasks {
    jar {
        archiveFileName.set("gradeway-driver-redis-${rootProject.version}.jar")
    }
}
