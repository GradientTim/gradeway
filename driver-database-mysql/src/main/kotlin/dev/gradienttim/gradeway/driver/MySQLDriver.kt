/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver

import com.mysql.cj.jdbc.MysqlDataSource
import dev.gradienttim.gradeway.driver.adapters.DatabaseAdapter
import dev.gradienttim.gradeway.driver.meta.CreateDriver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.Environment
import javax.sql.DataSource

@CreateDriver(
    id = "mysql",
    type = DriverType.DATABASE
)
class MySQLDriver : Driver(), DatabaseAdapter {
    override fun createDataSource(environment: Environment): DataSource {
        val databaseName = environment.stringDefault(
            names = arrayOf("GRADEWAY_DATABASE_NAME", "GRADEWAY_MYSQL_NAME"),
            default = "gradeway"
        )

        val databaseHostName = environment.stringDefault(
            names = arrayOf("GRADEWAY_DATABASE_HOST", "GRADEWAY_MYSQL_HOST"),
            default = "localhost"
        )

        val databaseHostPort = environment.intDefault(
            names = arrayOf("GRADEWAY_DATABASE_PORT", "GRADEWAY_MYSQL_PORT"),
            default = 3306
        )

        val databaseUserName = environment.string(
            names = arrayOf("GRADEWAY_DATABASE_USER", "GRADEWAY_MYSQL_USER"),
        )

        val databaseUserPassword = environment.string(
            names = arrayOf("GRADEWAY_DATABASE_PASSWORD", "GRADEWAY_MYSQL_PASSWORD"),
        )

        return MysqlDataSource().apply {
            setUrl("jdbc:mysql://$databaseHostName:$databaseHostPort/$databaseName")

            databaseUserName?.let { user = it }
            databaseUserPassword?.let { password = it }
        }
    }
}
