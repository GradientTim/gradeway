plugins {
    id("com.gradleup.shadow")
    id("org.jetbrains.kotlin.jvm")
}

val moduleName = findProperty("module.name") as? String ?: project.name
val moduleVersion = findProperty("module.version") as? String ?: rootProject.version

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        configurations = listOf(project.configurations.runtimeClasspath.get(), project.configurations.shadow.get())
        archiveFileName.set("gradeway-$moduleName-$moduleVersion.jar")

        filesMatching("META-INF/*.kotlin_module") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}
