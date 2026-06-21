/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver

import dev.gradienttim.gradeway.driver.adapters.DatabaseAdapter
import dev.gradienttim.gradeway.driver.meta.CreateDriver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.Environment
import org.sqlite.SQLiteDataSource
import javax.sql.DataSource

@CreateDriver(
    id = "sqlite",
    type = DriverType.DATABASE
)
class SQLiteDriver : Driver(), DatabaseAdapter {
    override fun createDatabaseSource(environment: Environment): DataSource {
        val databaseFile = environment.stringRequired(
            names = arrayOf("GRADEWAY_DATABASE_FILE", "GRADEWAY_SQLITE_FILE")
        )

        return SQLiteDataSource().apply {
            url = "jdbc:sqlite:$databaseFile"
        }
    }
}
