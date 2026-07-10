/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.adapters.DatabaseAdapter
import dev.gradienttim.gradeway.platform.Environment
import org.h2.jdbcx.JdbcDataSource
import java.util.*
import javax.sql.DataSource

class TestDatabaseDriver : Driver(), DatabaseAdapter {
    override fun createDataSource(environment: Environment): DataSource = JdbcDataSource().apply {
        setUrl("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
    }
}
