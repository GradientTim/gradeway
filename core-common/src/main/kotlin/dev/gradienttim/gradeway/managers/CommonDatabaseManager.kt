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
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

class CommonDatabaseManager(val gradeway: CommonGradeway) : DatabaseManager {
    @Suppress("LongMethod")
    override fun enable(): Either<Throwable, Unit> = either {
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
                val tables = arrayOf(
                    GroupsTable,
                    GroupPermissionsTable,
                    GroupPermissionTemplatesTable,
                    PermissionsTable,
                    PermissionTemplatePermissionsTable,
                    PermissionTemplatesTable,
                    PlayersTable,
                    PlayerRolesTable,
                    PlayerAttributesTable,
                    PlayerPermissionsTable,
                    PlayerPermissionTemplatesTable,
                    RolesTable,
                    RoleGroupsTable,
                    RoleParentsTable,
                    RoleAttributesTable,
                    RolePermissionsTable,
                    RolePermissionTemplatesTable
                )

                val tableStates = tables.map { it to it.exists() }

                // Tables that already exist may have been created by an older version of Gradeway and can be
                // missing columns/constraints introduced since; freshly created tables below already match the
                // current definitions exactly and must be excluded here, or Exposed re-detects their own
                // brand-new constraints as missing and tries to add them a second time.
                val existingTables = tableStates.filter { (_, exists) -> exists }.map { (table, _) -> table }
                val nonExistingTables = tableStates.filter { (_, exists) -> !exists }.map { (table, _) -> table }

                SchemaUtils.create(*nonExistingTables.toTypedArray())

                if (existingTables.isNotEmpty()) {
                    val migrationStatements = MigrationUtils.statementsRequiredForDatabaseMigration(
                        *existingTables.toTypedArray(),
                        withLogs = false
                    )

                    if (migrationStatements.isNotEmpty()) {
                        migrationStatements.forEach { statement -> exec(statement) }
                    }
                }
            }
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun disable(): Either<Throwable, Unit> = either {
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
