/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatePermissionsTable
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.database.models.player.*
import dev.gradienttim.gradeway.database.models.role.RoleAttributesTable
import dev.gradienttim.gradeway.database.models.role.RoleParentsTable
import dev.gradienttim.gradeway.database.models.role.RolePermissionsTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.driver.adapters.DatabaseAdapter
import dev.gradienttim.gradeway.driver.meta.DriverType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class CommonDatabaseManager(val gradeway: CommonGradeway) : DatabaseManager {
    override fun load(): Either<Throwable, Unit> = either {
        val driverId = gradeway.configs.config.database.driver
        if (driverId.isBlank()) {
            raise(Throwable("Driver identifier cannot be blank."))
        }

        val databaseDriver = gradeway.drivers.findDriver(driverId, DriverType.DATABASE)
            ?: raise(Throwable("No database driver found with id '$driverId'"))

        if (databaseDriver !is DatabaseAdapter) {
            raise(Throwable("Driver '$driverId' has no DatabaseAdapter."))
        }

        try {
            val dataSource = databaseDriver.createDataSource(gradeway.environment)
            gradeway.database = Database.connect(dataSource)

            transaction(gradeway.database) {
                SchemaUtils.create(
                    PermissionsTable,
                    PermissionTemplatePermissionsTable,
                    PermissionTemplatesTable
                )

                SchemaUtils.create(
                    PlayersTable,
                    PlayerRolesTable,
                    PlayerAttributesTable,
                    PlayerPermissionsTable,
                    PlayerPermissionTemplatesTable,
                )

                SchemaUtils.create(
                    RolesTable,
                    RoleParentsTable,
                    RoleAttributesTable,
                    RolePermissionsTable,
                    PlayerPermissionTemplatesTable
                )
            }
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun unload(): Either<Throwable, Unit> = either {
        try {
            val connector = gradeway.database.connector()
            if (!connector.isClosed) {
                connector.close()
            }
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }
}
