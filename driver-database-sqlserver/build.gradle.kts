plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core-api"))

    implementation("com.microsoft.sqlserver:mssql-jdbc:13.2.1.jre11")
}
