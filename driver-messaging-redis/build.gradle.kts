plugins {
    id("gradeway-base")
    id("gradeway-shadow")
    id("com.google.devtools.ksp")
}

@Suppress("DataClassEqualsAndHashCodeShareKey")
dependencies {
    ksp(project(":core-api"))
    compileOnly(project(":core-api"))

    implementation("redis.clients:jedis:7.5.2")
}
