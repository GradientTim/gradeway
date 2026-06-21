/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver

import dev.gradienttim.gradeway.driver.adapters.DatabaseAdapter
import dev.gradienttim.gradeway.driver.meta.CreateDriver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.Environment
import org.mariadb.jdbc.MariaDbDataSource
import javax.sql.DataSource

@CreateDriver(
    id = "mariadb",
    type = DriverType.DATABASE
)
class MariaDBDriver : Driver(), DatabaseAdapter {
    override fun createDatabaseSource(environment: Environment): DataSource {
        val databaseName = environment.stringDefault(
            names = arrayOf("GRADEWAY_DATABASE_NAME", "GRADEWAY_MARIADB_NAME"),
            default = "gradeway"
        )

        val databaseHostName = environment.stringDefault(
            names = arrayOf("GRADEWAY_DATABASE_HOST", "GRADEWAY_MARIADB_HOST"),
            default = "localhost"
        )

        val databaseHostPort = environment.intDefault(
            names = arrayOf("GRADEWAY_DATABASE_PORT", "GRADEWAY_MARIADB_PORT"),
            default = 3306
        )

        val databaseUserName = environment.string(
            names = arrayOf("GRADEWAY_DATABASE_USER", "GRADEWAY_MARIADB_USER"),
        )

        val databaseUserPassword = environment.string(
            names = arrayOf("GRADEWAY_DATABASE_PASSWORD", "GRADEWAY_MARIADB_PASSWORD"),
        )

        return MariaDbDataSource().apply {
            url = "jdbc:mariadb://$databaseHostName:$databaseHostPort/$databaseName"

            databaseUserName?.let { user = it }
            databaseUserPassword?.let { setPassword(it) }
        }
    }
}
