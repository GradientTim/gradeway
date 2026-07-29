import java.time.LocalDateTime

plugins {
    id("com.vanniktech.maven.publish")
}

val moduleName = findProperty("module.name") as? String ?: project.name
val moduleVersion = findProperty("module.version") as? String ?: rootProject.version.toString()
val moduleDescription = findProperty("module.description") as? String ?: "A Minecraft permission gateway."

mavenPublishing {
    signAllPublications()

    publishToMavenCentral(
        automaticRelease = false
    )

    coordinates(
        groupId = rootProject.group.toString(),
        artifactId = moduleName,
        version = moduleVersion
    )

    pom {
        url.set("https://github.com/GradientTim/gradeway")
        name.set(rootProject.name)
        description.set(moduleDescription)
        inceptionYear.set(LocalDateTime.now().year.toString())

        licenses {
            license {
                name.set("MIT")
                url.set("https://spdx.org/licenses/MIT.html")
                distribution.set("https://spdx.org/licenses/MIT.html")
            }
        }

        developers {
            developer {
                id.set("gradienttim")
                name.set("Tim Kiesel")
                url.set("https://github.com/GradientTim")
            }
        }

        scm {
            url.set("https://github.com/GradientTim/gradeway")
            connection.set("scm:git:git://github.com/GradientTim/gradeway.git")
            developerConnection.set("scm:git:ssh://git@github.com/GradientTim/gradeway.git")
        }
    }
}
