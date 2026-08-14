plugins {
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlin.jvm")
}

val gradewayModuleName = findProperty("module.name") as? String ?: project.name
val gradewayModuleVersion = findProperty("module.version") as? String ?: rootProject.version.toString()

val githubModulePath = project.path.removePrefix(":").replace(":", "/")

dokka {
    dokkaSourceSets.configureEach {
        moduleName.set(gradewayModuleName)
        moduleVersion.set(gradewayModuleVersion)

        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/GradientTim/gradeway/blob/main/$githubModulePath/src/main/kotlin")
            remoteLineSuffix.set("#L")
        }
    }
}
