/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver.adapters

import dev.gradienttim.gradeway.platform.Environment
import javax.sql.DataSource

/**
 * Represents an adapter responsible for creating a database connection source
 * based on the provided environment configuration.
 */
interface DatabaseAdapter {
    /**
     * Creates a database connection source based on the provided environment configuration.
     *
     * @param environment The environment configuration containing required settings for the database connection.
     * @return A DataSource instance representing the database connection source configured with the specified environment.
     */
    fun createDatabaseSource(environment: Environment): DataSource
}
