/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver

import com.microsoft.sqlserver.jdbc.SQLServerDataSource
import dev.gradienttim.gradeway.driver.adapters.DatabaseAdapter
import dev.gradienttim.gradeway.driver.meta.CreateDriver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.Environment
import javax.sql.DataSource

@CreateDriver(
    id = "sqlserver",
    type = DriverType.DATABASE
)
class SQLServerDriver : Driver(), DatabaseAdapter {
    override fun createDataSource(environment: Environment): DataSource {
        val databaseName = environment.stringDefault(
            names = arrayOf("GRADEWAY_DATABASE_NAME", "GRADEWAY_SQLSERVER_NAME"),
            default = "gradeway"
        )

        val databaseHostName = environment.stringDefault(
            names = arrayOf("GRADEWAY_DATABASE_HOST", "GRADEWAY_SQLSERVER_HOST"),
            default = "localhost"
        )

        val databaseHostPort = environment.intDefault(
            names = arrayOf("GRADEWAY_DATABASE_PORT", "GRADEWAY_SQLSERVER_PORT"),
            default = 1433
        )

        val databaseUserName = environment.string(
            names = arrayOf("GRADEWAY_DATABASE_USER", "GRADEWAY_SQLSERVER_USER"),
        )

        val databaseUserPassword = environment.string(
            names = arrayOf("GRADEWAY_DATABASE_PASSWORD", "GRADEWAY_SQLSERVER_PASSWORD"),
        )

        return SQLServerDataSource().apply {
            this.serverName = databaseHostName
            this.portNumber = databaseHostPort
            this.databaseName = databaseName

            databaseUserName?.let { user = it }
            databaseUserPassword?.let { setPassword(it) }
        }
    }
}
