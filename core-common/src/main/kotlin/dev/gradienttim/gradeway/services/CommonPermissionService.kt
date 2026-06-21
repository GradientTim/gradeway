/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.database.entities.PermissionEntity
import dev.gradienttim.gradeway.database.models.player.PlayerEntity
import dev.gradienttim.gradeway.database.models.role.RoleEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class CommonPermissionService(val gradeway: CommonGradeway) : PermissionService, KoinComponent {
    private val rolesService: RoleService by inject()
    private val playersService: PlayerService by inject()

    override fun setRolePermission(
        id: UUID,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Boolean> = either {
        val entity = rolesService.findById(id) ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setRolePermission(entity, permission, enabled)
    }

    override fun setRolePermission(
        entity: RoleEntity,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Boolean> = setEntityPermission(entity, permission, enabled)

    override fun setRolePermissions(
        id: UUID,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Boolean> = either {
        val entity = rolesService.findById(id) ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setRolePermissions(entity, permissions)
    }

    override fun setRolePermissions(
        entity: RoleEntity,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Boolean> = setEntityPermissions(entity, permissions)

    override fun unsetRolePermission(
        id: UUID,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Boolean> = either {
        val entity = rolesService.findById(id) ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetRolePermission(entity, permission)
    }

    override fun unsetRolePermission(
        entity: RoleEntity,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Boolean> = unsetEntityPermission(entity, permission)

    override fun unsetRolePermissions(
        id: UUID,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean> = either {
        val entity = rolesService.findById(id) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetRolePermissions(entity, permissions)
    }

    override fun unsetRolePermissions(
        entity: RoleEntity,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean> = unsetEntityPermissions(entity, permissions)

    override fun clearRolePermissions(id: UUID): Either<PermissionService.ClearPermissionError, Boolean> = either {
        val entity = rolesService.findById(id) ?: raise(PermissionService.ClearPermissionError.EntityNotFound)
        return clearRolePermissions(entity)
    }

    override fun clearRolePermissions(
        entity: RoleEntity
    ): Either<PermissionService.ClearPermissionError, Boolean> = clearEntityPermissions(entity)

    override fun hasRolePermission(id: UUID, permission: String): Boolean {
        val entityPermissions = getRolePermissions(id, true)
        return permission in entityPermissions
    }

    override fun hasRolePermission(entity: RoleEntity, permission: String): Boolean {
        val entityPermissions = getRolePermissions(entity, true)
        return permission in entityPermissions
    }

    override fun hasRoleAnyPermissions(id: UUID, permissions: List<String>): Boolean {
        val entityPermissions = getRolePermissions(id, true)
        return permissions.any { entityPermissions.contains(it) }
    }

    override fun hasRoleAnyPermissions(entity: RoleEntity, permissions: List<String>): Boolean {
        val entityPermissions = getRolePermissions(entity, true)
        return permissions.any { entityPermissions.contains(it) }
    }

    override fun hasRoleAllPermissions(id: UUID, permissions: List<String>): Boolean {
        val entityPermissions = getRolePermissions(id, true)
        return permissions.all { entityPermissions.contains(it) }
    }

    override fun hasRoleAllPermissions(entity: RoleEntity, permissions: List<String>): Boolean {
        val entityPermissions = getRolePermissions(entity, true)
        return permissions.all { entityPermissions.contains(it) }
    }

    override fun getRolePermissions(id: UUID): Map<String, Boolean> {
        val entity = rolesService.findById(id) ?: return emptyMap<String, Boolean>()
        return getRolePermissions(entity)
    }

    override fun getRolePermissions(entity: RoleEntity): Map<String, Boolean> {
        return entity.permissions
    }

    override fun getRolePermissions(id: UUID, status: Boolean): Set<String> {
        val entity = rolesService.findById(id) ?: return emptySet<String>()
        return getRolePermissions(entity, status)
    }

    override fun getRolePermissions(entity: RoleEntity, status: Boolean): Set<String> {
        return entity.permissions.filter { (_, enabled) -> status == enabled }.keys
    }

    override fun setPlayerPermission(
        id: UUID,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Boolean> = either {
        val entity = playersService.findById(id) ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setPlayerPermission(entity, permission, enabled)
    }

    override fun setPlayerPermission(
        entity: PlayerEntity,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Boolean> = setEntityPermission(entity, permission, enabled)

    override fun setPlayerPermissions(
        id: UUID,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Boolean> = either {
        val entity = playersService.findById(id) ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setPlayerPermissions(entity, permissions)
    }

    override fun setPlayerPermissions(
        entity: PlayerEntity,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Boolean> = setEntityPermissions(entity, permissions)

    override fun unsetPlayerPermission(
        id: UUID,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Boolean> = either {
        val entity = playersService.findById(id) ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetPlayerPermission(entity, permission)
    }

    override fun unsetPlayerPermission(
        entity: PlayerEntity,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Boolean> = unsetEntityPermission(entity, permission)

    override fun unsetPlayerPermissions(
        id: UUID,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean> = either {
        val entity = playersService.findById(id) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetPlayerPermissions(entity, permissions)
    }

    override fun unsetPlayerPermissions(
        entity: PlayerEntity,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean> = unsetEntityPermissions(entity, permissions)

    override fun clearPlayerPermissions(id: UUID): Either<PermissionService.ClearPermissionError, Boolean> = either {
        val entity = playersService.findById(id) ?: raise(PermissionService.ClearPermissionError.EntityNotFound)
        return clearPlayerPermissions(entity)
    }

    override fun clearPlayerPermissions(
        entity: PlayerEntity
    ): Either<PermissionService.ClearPermissionError, Boolean> = clearEntityPermissions(entity)

    override fun hasPlayerPermission(id: UUID, permission: String): Boolean {
        val entityPermissions = getPlayerPermissions(id, true)
        return permission in entityPermissions
    }

    override fun hasPlayerPermission(entity: PlayerEntity, permission: String): Boolean {
        val entityPermissions = getPlayerPermissions(entity, true)
        return permission in entityPermissions
    }

    override fun hasPlayerAnyPermissions(id: UUID, permissions: List<String>): Boolean {
        val entityPermissions = getPlayerPermissions(id, true)
        return permissions.any { entityPermissions.contains(it) }
    }

    override fun hasPlayerAnyPermissions(entity: PlayerEntity, permissions: List<String>): Boolean {
        val entityPermissions = getPlayerPermissions(entity, true)
        return permissions.any { entityPermissions.contains(it) }
    }

    override fun hasPlayerAllPermissions(id: UUID, permissions: List<String>): Boolean {
        val entityPermissions = getPlayerPermissions(id, true)
        return permissions.all { entityPermissions.contains(it) }
    }

    override fun hasPlayerAllPermissions(entity: PlayerEntity, permissions: List<String>): Boolean {
        val entityPermissions = getPlayerPermissions(entity, true)
        return permissions.all { entityPermissions.contains(it) }
    }

    override fun getPlayerPermissions(id: UUID): Map<String, Boolean> {
        val entity = playersService.findById(id) ?: return emptyMap<String, Boolean>()
        return getPlayerPermissions(entity)
    }

    override fun getPlayerPermissions(entity: PlayerEntity): Map<String, Boolean> {
        return entity.permissions
    }

    override fun getPlayerPermissions(id: UUID, status: Boolean): Set<String> {
        val entity = playersService.findById(id) ?: return emptySet<String>()
        return getPlayerPermissions(entity, status)
    }

    override fun getPlayerPermissions(entity: PlayerEntity, status: Boolean): Set<String> {
        return entity.permissions.filter { (_, enabled) -> status == enabled }.keys
    }

    private fun setEntityPermission(
        entity: PermissionEntity,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Boolean> = either {
        val entityPermissions = entity.permissions.toMutableMap()
        if (entityPermissions.containsKey(permission)) {
            val isEnabled = entityPermissions[permission]!!
            if (enabled && isEnabled) {
                raise(PermissionService.SetPermissionError.PermissionAlreadyEnabled)
            }
            if (!enabled && !isEnabled) {
                raise(PermissionService.SetPermissionError.PermissionAlreadyDisabled)
            }
        }
        entityPermissions[permission] = enabled
        try {
            transaction(gradeway.database) {
                entity.permissions = entityPermissions
                entity.flush(null)
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.SetPermissionError.Unexpected(throwable))
        }
    }

    private fun setEntityPermissions(
        entity: PermissionEntity,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Boolean> = either {
        val entityPermissions = entity.permissions.toMutableMap()
        for ((permission, status) in permissions) {
            entityPermissions[permission] = status
        }
        try {
            transaction(gradeway.database) {
                entity.permissions = entityPermissions
                entity.flush(null)
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.BulkSetPermissionError.Unexpected(throwable))
        }
    }

    private fun unsetEntityPermission(
        entity: PermissionEntity,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Boolean> = either {
        val entityPermissions = entity.permissions.toMutableMap()
        if (!entity.permissions.containsKey(permission)) {
            raise(PermissionService.UnsetPermissionError.PermissionNotFound)
        }
        entityPermissions.remove(permission)
        try {
            transaction(gradeway.database) {
                entity.permissions = entityPermissions
                entity.flush(null)
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.UnsetPermissionError.Unexpected(throwable))
        }
    }

    private fun unsetEntityPermissions(
        entity: PermissionEntity,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean> = either {
        val entityPermissions = entity.permissions.toMutableMap()
        for (permission in permissions) {
            entityPermissions.remove(permission)
        }
        try {
            transaction(gradeway.database) {
                entity.permissions = entityPermissions
                entity.flush(null)
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.BulkUnsetPermissionError.Unexpected(throwable))
        }
    }

    private fun clearEntityPermissions(
        entity: PermissionEntity
    ): Either<PermissionService.ClearPermissionError, Boolean> = either {
        try {
            transaction(gradeway.database) {
                entity.permissions = emptyMap()
                entity.flush(null)
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.ClearPermissionError.Unexpected(throwable))
        }
    }
}
