plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    ksp(project(":core-api"))
    compileOnly(project(":core-api"))

    shadow("com.oracle.database.jdbc:ojdbc8:23.26.0.0.0")
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())
        archiveFileName.set("gradeway-driver-oracle-${rootProject.version}.jar")
    }
}
