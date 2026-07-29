plugins {
    id("gradeway-base")
    id("gradeway-shadow")
    id("com.google.devtools.ksp")
}

@Suppress("DataClassEqualsAndHashCodeShareKey")
dependencies {
    ksp(project(":core-api"))
    compileOnly(project(":core-api"))

    shadow("mysql:mysql-connector-java:8.0.33")
}
