dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("gradlePlugins") {
            from(files("../gradle/gradle-plugins.versions.toml"))
        }
    }
}

rootProject.name = "buildSrc"
