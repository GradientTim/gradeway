/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver

import dev.gradienttim.gradeway.driver.adapters.DatabaseAdapter
import dev.gradienttim.gradeway.driver.meta.CreateDriver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.Environment
import oracle.jdbc.pool.OracleDataSource
import javax.sql.DataSource

@CreateDriver(
    id = "oracle",
    type = DriverType.DATABASE
)
class OracleDriver : Driver(), DatabaseAdapter {
    override fun createDataSource(environment: Environment): DataSource {
        val databaseName = environment.stringDefault(
            names = arrayOf("GRADEWAY_DATABASE_NAME", "GRADEWAY_ORACLE_NAME"),
            default = "gradeway"
        )

        val databaseHostName = environment.stringDefault(
            names = arrayOf("GRADEWAY_DATABASE_HOST", "GRADEWAY_ORACLE_HOST"),
            default = "localhost"
        )

        val databaseHostPort = environment.intDefault(
            names = arrayOf("GRADEWAY_DATABASE_PORT", "GRADEWAY_ORACLE_PORT"),
            default = 1521
        )

        val databaseUserName = environment.string(
            names = arrayOf("GRADEWAY_DATABASE_USER", "GRADEWAY_ORACLE_USER"),
        )

        val databaseUserPassword = environment.string(
            names = arrayOf("GRADEWAY_DATABASE_PASSWORD", "GRADEWAY_ORACLE_PASSWORD"),
        )

        return OracleDataSource().apply {
            url = "jdbc:oracle:thin://$databaseHostName:$databaseHostPort/$databaseName"

            databaseUserName?.let { user = it }
            databaseUserPassword?.let { setPassword(it) }
        }
    }
}
