/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.database.models.player.PlayerRolesTable
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.database.models.role.RoleParentsTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.driver.adapters.DatabaseAdapter
import dev.gradienttim.gradeway.driver.meta.DriverType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class CommonDatabaseManager(val gradeway: CommonGradeway) : DatabaseManager {
    override fun load() {
        val driverId = gradeway.configs.config.database.driver
        if (driverId.isBlank()) {
            error("Driver identifier cannot be blank.")
        }

        val databaseDriver = gradeway.drivers.findDriver(driverId, DriverType.DATABASE)
            ?: error("No database driver found with id '$driverId'")

        if (databaseDriver !is DatabaseAdapter) {
            error("Driver '$driverId' has no DatabaseAdapter.")
        }

        val dataSource = databaseDriver.createDatabaseSource(gradeway.environment)
        gradeway.database = Database.connect(dataSource)

        transaction(gradeway.database) {
            SchemaUtils.create(
                PlayersTable,
                PlayerRolesTable,
                RolesTable,
                RoleParentsTable
            )
        }
    }

    override fun unload() {
        val connector = gradeway.database.connector()
        if (!connector.isClosed) {
            connector.close()
        }
    }
}
