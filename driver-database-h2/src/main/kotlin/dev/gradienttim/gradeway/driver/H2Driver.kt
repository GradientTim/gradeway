/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver

import dev.gradienttim.gradeway.driver.adapters.DatabaseAdapter
import dev.gradienttim.gradeway.driver.meta.CreateDriver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.Environment
import org.h2.jdbcx.JdbcDataSource
import javax.sql.DataSource

@CreateDriver(
    id = "h2",
    type = DriverType.DATABASE
)
class H2Driver : Driver(), DatabaseAdapter {
    override fun createDataSource(environment: Environment): DataSource {
        val databaseFile = environment.stringRequired(
            names = arrayOf("GRADEWAY_DATABASE_FILE", "GRADEWAY_H2_FILE")
        )

        val databaseUserName = environment.string(
            names = arrayOf("GRADEWAY_DATABASE_USER", "GRADEWAY_H2_USER"),
        )

        val databaseUserPassword = environment.string(
            names = arrayOf("GRADEWAY_DATABASE_PASSWORD", "GRADEWAY_H2_PASSWORD"),
        )

        return JdbcDataSource().apply {
            setUrl("jdbc:h2:$databaseFile")

            databaseUserName?.let { user = it }
            databaseUserPassword?.let { password = it }
        }
    }
}
