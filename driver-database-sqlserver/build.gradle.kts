plugins {
    id("gradeway-base")
    id("gradeway-shadow")
    id("com.google.devtools.ksp")
}

@Suppress("DataClassEqualsAndHashCodeShareKey")
dependencies {
    ksp(project(":core-api"))
    compileOnly(project(":core-api"))

    implementation("com.microsoft.sqlserver:mssql-jdbc:13.2.1.jre11")
}
