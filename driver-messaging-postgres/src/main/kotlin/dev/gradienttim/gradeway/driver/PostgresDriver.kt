/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver

import dev.gradienttim.gradeway.driver.adapters.MessagingAdapter
import dev.gradienttim.gradeway.driver.messaging.PostgresMessagingBroker
import dev.gradienttim.gradeway.driver.meta.CreateDriver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.messaging.MessagingBroker
import dev.gradienttim.gradeway.platform.Environment
import org.postgresql.ds.PGSimpleDataSource

@CreateDriver(
    id = "postgres",
    type = DriverType.MESSAGING
)
class PostgresDriver : Driver(), MessagingAdapter {
    override fun createMessagingBroker(environment: Environment): MessagingBroker {
        val databaseName = environment.stringDefault(
            names = arrayOf("GRADEWAY_MESSAGING_NAME", "GRADEWAY_POSTGRES_NAME"),
            default = "gradeway"
        )

        val databaseHostName = environment.stringDefault(
            names = arrayOf("GRADEWAY_MESSAGING_HOST", "GRADEWAY_POSTGRES_HOST"),
            default = "localhost"
        )

        val databaseHostPort = environment.intDefault(
            names = arrayOf("GRADEWAY_MESSAGING_PORT", "GRADEWAY_POSTGRES_PORT"),
            default = 5432
        )

        val databaseUserName = environment.string(
            names = arrayOf("GRADEWAY_MESSAGING_USER", "GRADEWAY_POSTGRES_USER"),
        )

        val databaseUserPassword = environment.string(
            names = arrayOf("GRADEWAY_MESSAGING_PASSWORD", "GRADEWAY_POSTGRES_PASSWORD"),
        )

        val dataSource = PGSimpleDataSource().apply {
            this.serverNames = arrayOf(databaseHostName)
            this.portNumbers = intArrayOf(databaseHostPort)
            this.databaseName = databaseName

            databaseUserName?.let { user = it }
            databaseUserPassword?.let { password = it }
        }

        return PostgresMessagingBroker(dataSource)
    }
}
