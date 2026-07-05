/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.database.models.group.GroupPermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.group.GroupPermissionsTable
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatePermissionsTable
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.database.models.player.*
import dev.gradienttim.gradeway.database.models.role.*
import dev.gradienttim.gradeway.driver.adapters.DatabaseAdapter
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.throwables.driver.DriverBlankIdentifierThrowable
import dev.gradienttim.gradeway.throwables.driver.DriverNotFoundThrowable
import dev.gradienttim.gradeway.throwables.driver.DriverUnsupportedAdapterThrowable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class CommonDatabaseManager(val gradeway: CommonGradeway) : DatabaseManager {
    override fun load(): Either<Throwable, Unit> = either {
        val driverId = gradeway.configs.config.database.driver
        if (driverId.isBlank()) {
            raise(DriverBlankIdentifierThrowable())
        }

        val databaseDriver = gradeway.drivers.findDriver(driverId, DriverType.DATABASE)
            ?: raise(DriverNotFoundThrowable(id = driverId))

        if (databaseDriver !is DatabaseAdapter) {
            raise(DriverUnsupportedAdapterThrowable(id = driverId, adapter = DatabaseAdapter::class))
        }

        try {
            val dataSource = databaseDriver.createDataSource(gradeway.environment)
            gradeway.database = Database.connect(dataSource)

            transaction(gradeway.database) {
                SchemaUtils.create(
                    GroupsTable,
                    GroupPermissionsTable,
                    GroupPermissionTemplatesTable
                )

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
                    RoleGroupsTable,
                    RoleParentsTable,
                    RoleAttributesTable,
                    RolePermissionsTable,
                    RolePermissionTemplatesTable
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
