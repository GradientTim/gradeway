plugins {
    id("gradeway-base")
    id("gradeway-shadow")
    id("com.google.devtools.ksp")
}

@Suppress("DataClassEqualsAndHashCodeShareKey")
dependencies {
    ksp(project(":core-api"))
    compileOnly(project(":core-api"))

    shadow("com.oracle.database.jdbc:ojdbc8:23.26.0.0.0")
}
