/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver

import dev.gradienttim.gradeway.driver.adapters.DatabaseAdapter
import dev.gradienttim.gradeway.driver.meta.CreateDriver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.Environment
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

@CreateDriver(
    id = "postgres",
    type = DriverType.DATABASE
)
class PostgresDriver : Driver(), DatabaseAdapter {
    override fun createDatabaseSource(environment: Environment): DataSource {
        val databaseName = environment.stringDefault(
            names = arrayOf("GRADEWAY_DATABASE_NAME", "GRADEWAY_POSTGRES_NAME"),
            default = "gradeway"
        )

        val databaseHostName = environment.stringDefault(
            names = arrayOf("GRADEWAY_DATABASE_HOST", "GRADEWAY_POSTGRES_HOST"),
            default = "localhost"
        )

        val databaseHostPort = environment.intDefault(
            names = arrayOf("GRADEWAY_DATABASE_PORT", "GRADEWAY_POSTGRES_PORT"),
            default = 5432
        )

        val databaseUserName = environment.string(
            names = arrayOf("GRADEWAY_DATABASE_USER", "GRADEWAY_POSTGRES_USER"),
        )

        val databaseUserPassword = environment.string(
            names = arrayOf("GRADEWAY_DATABASE_PASSWORD", "GRADEWAY_POSTGRES_PASSWORD"),
        )

        return PGSimpleDataSource().apply {
            setUrl("jdbc:postgresql://$databaseHostName:$databaseHostPort/$databaseName")

            databaseUserName?.let { user = it }
            databaseUserPassword?.let { password = it }
        }
    }
}
