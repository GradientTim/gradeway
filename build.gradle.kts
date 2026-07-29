group = property("project.group") as String
version = property("project.version") as String

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

            rootProject.extra["bumpedVersion"] = newVersion
            logger.lifecycle("Bumped project.version: ${rootProject.version} -> $newVersion")
        }
    }

    register<Exec>("generateChangelog") {
        group = "release"
        description =
            "Regenerates CHANGELOG.md via git-cliff, tagged with the current (or just-bumped) project.version."

        doFirst {
            val version = rootProject.extra.takeIf { it.has("bumpedVersion") }?.get("bumpedVersion") as String?
                ?: rootProject.version.toString()
            commandLine("git-cliff", "--tag", "v$version", "-o", "CHANGELOG.md")
        }
    }

    named("bumpVersion") {
        finalizedBy("generateChangelog")
    }
}
