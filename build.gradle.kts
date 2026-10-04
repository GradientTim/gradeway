plugins {
    id("org.jetbrains.dokka")
}

group = property("project.group") as String
version = property("project.version") as String

dependencies {
    dokka(project(":core-api"))
    dokka(project(":core-common"))
}

tasks {
    register("bumpVersion") {
        group = "release"
        description =
            "Bumps project.version in gradle.properties. Usage: ./gradlew bumpVersion -Ptype=[major|minor|patch]"

        doLast {
            val type = (findProperty("type") as String?)?.lowercase() ?: "patch"
            require(type in setOf("major", "minor", "patch")) {
                "Unknown bump type '$type', expected 'major', 'minor' or 'patch'"
            }

            val (major, minor, patch) = rootProject.version.toString().split(".").map { it.toInt() }
            val newVersion = when (type) {
                "major" -> "${major + 1}.0.0"
                "minor" -> "$major.${minor + 1}.0"
                else -> "$major.$minor.${patch + 1}"
            }

            val propsFile = rootProject.file("gradle.properties")
            propsFile.writeText(
                propsFile.readText().replace(
                    "project.version=${rootProject.version}",
                    "project.version=$newVersion",
                ),
            )

            logger.lifecycle("Bumped project.version: ${rootProject.version} -> $newVersion")
        }
    }
}
