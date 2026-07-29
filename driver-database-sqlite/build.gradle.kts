plugins {
    id("gradeway-base")
    id("gradeway-shadow")
    id("com.google.devtools.ksp")
}

@Suppress("DataClassEqualsAndHashCodeShareKey")
dependencies {
    ksp(project(":core-api"))
    compileOnly(project(":core-api"))

    implementation("org.xerial:sqlite-jdbc:3.50.2.0")
}
