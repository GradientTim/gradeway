/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.permission

import arrow.core.Either
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.role.RolePermissionEntity
import dev.gradienttim.gradeway.services.PermissionService
import net.kyori.adventure.util.TriState
import java.util.*

/**
 * Provides services for managing permissions associated with roles.
 * Allows setting, unsetting, clearing, and querying permissions for roles by their identifiers
 * or corresponding entities.
 */
interface RolePermissionService {
    /**
     * Sets a specific permission for a role identified by its UUID.
     * The permission can be enabled or disabled based on the `enabled` parameter.
     *
     * @param id The unique identifier of the role.
     * @param permission The name of the permission to be set.
     * @param enabled A flag indicating whether the permission should be enabled or disabled. Defaults to true.
     * @return Either a `PermissionService.SetPermissionError` if an error occurs
     *         or `true` if the update succeeds.
     */
    fun setRolePermission(
        id: UUID,
        permission: String,
        enabled: Boolean = true
    ): Either<PermissionService.SetPermissionError, Unit>

    /**
     * Sets a specific permission for a role entity. The permission can be enabled or disabled
     * depending on the value of the `enabled` parameter.
     *
     * @param entity The role entity for which the permission is to be set.
     * @param permission The name of the permission to be set.
     * @param enabled A flag indicating whether the permission should be enabled or disabled. Defaults to true.
     * @return Either an instance of `PermissionService.SetPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setRolePermission(
        entity: RoleEntity,
        permission: String,
        enabled: Boolean = true
    ): Either<PermissionService.SetPermissionError, Unit>

    /**
     * Sets a specific permission for a role identified by its unique identifier or name.
     * The permission can be enabled or disabled based on the `enabled` parameter.
     *
     * @param idOrName The unique identifier or name of the role.
     * @param permission The name of the permission to be set.
     * @param enabled A flag indicating whether the permission should be enabled or disabled. Defaults to true.
     * @return Either a `PermissionService.SetPermissionError` if an error occurs
     *         or `true` if the update succeeds.
     */
    fun setRolePermission(
        idOrName: String,
        permission: String,
        enabled: Boolean = true
    ): Either<PermissionService.SetPermissionError, Unit>

    /**
     * Assigns multiple permissions to a role identified by its UUID.
     * Each permission in the map can be enabled or disabled based on its associated value.
     *
     * @param id The unique identifier of the role.
     * @param permissions A map where the key is the name of the permission and the value is a boolean
     * indicating whether the permission should be enabled (true) or disabled (false).
     * @return Either an instance of `PermissionService.BulkSetPermissionError` if an error occurs during the operation,
     *         or `true` if the update succeeds.
     */
    fun setRolePermissions(
        id: UUID,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit>

    /**
     * Assigns multiple permissions to a given role entity.
     * Each permission in the map can either be enabled or disabled based on its associated value.
     *
     * @param entity The role entity to which the permissions will be assigned.
     * @param permissions A map where each key represents a permission name, and each value is a boolean
     *                    indicating whether the permission should be enabled (true) or disabled (false).
     * @return Either an instance of `PermissionService.BulkSetPermissionError` if an error occurs during the operation,
     *         or `true` if the update succeeds.
     */
    fun setRolePermissions(
        entity: RoleEntity,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit>

    /**
     * Assigns multiple permissions to a role identified by its unique identifier or name.
     * Each permission in the map can be enabled or disabled based on its associated value.
     *
     * @param idOrName The unique identifier or name of the role.
     * @param permissions A map where the key is the name of the permission and the value is a boolean
     * indicating whether the permission should be enabled (true) or disabled (false).
     * @return Either an instance of `PermissionService.BulkSetPermissionError` if an error occurs during the operation,
     *         or `true` if the update succeeds.
     */
    fun setRolePermissions(
        idOrName: String,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit>

    /**
     * Removes a specific permission from a role identified by its UUID.
     * If the operation fails, an error indicating the cause will be returned.
     *
     * @param id The unique identifier of the role from which the permission should be removed.
     * @param permission The name of the permission to be unset.
     * @return Either an instance of `PermissionService.UnsetPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetRolePermission(id: UUID, permission: String): Either<PermissionService.UnsetPermissionError, Unit>

    /**
     * Removes a specific permission from the given role entity.
     * If the operation fails, an error indicating the cause will be returned.
     *
     * @param entity The role entity from which the permission should be removed.
     * @param permission The name of the permission to be unset.
     * @return Either an instance of `PermissionService.UnsetPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetRolePermission(
        entity: RoleEntity,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit>

    /**
     * Removes a specific permission from a role identified by its unique identifier or name.
     * If the operation fails, an error indicating the cause will be returned.
     *
     * @param idOrName The unique identifier or name of the role from which the permission should be removed.
     * @param permission The name of the permission to be unset.
     * @return Either an instance of `PermissionService.UnsetPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetRolePermission(
        idOrName: String,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit>

    /**
     * Removes multiple permissions from a role identified by its UUID.
     * If the operation fails, an error indicating the cause will be returned.
     *
     * @param id The unique identifier of the role from which the permissions should be removed.
     * @param permissions A list of permission names to be unset.
     * @return Either an instance of `PermissionService.BulkUnsetPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetRolePermissions(
        id: UUID,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit>

    /**
     * Removes multiple permissions from the given role entity.
     * If the operation fails, an error indicating the cause will be returned.
     *
     * @param entity The role entity from which the permissions should be removed.
     * @param permissions A list of permission names to be unset.
     * @return Either an instance of `PermissionService.BulkUnsetPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetRolePermissions(
        entity: RoleEntity,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit>

    /**
     * Removes multiple permissions from a role identified by its unique identifier or name.
     * If the operation fails, an error indicating the cause will be returned.
     *
     * @param idOrName The unique identifier or name of the role from which the permissions should be removed.
     * @param permissions A list of permission names to be unset.
     * @return Either an instance of `PermissionService.BulkUnsetPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetRolePermissions(
        idOrName: String,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit>

    /**
     * Clears all permissions associated with a role identified by its unique UUID.
     *
     * @param id The unique identifier of the role whose permissions need to be cleared.
     * @return Either an instance of `PermissionService.ClearPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun clearRolePermissions(id: UUID): Either<PermissionService.ClearPermissionError, Unit>

    /**
     * Clears all permissions associated with the provided role entity.
     *
     * @param entity The role entity whose permissions need to be cleared.
     * @return Either an instance of `PermissionService.ClearPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun clearRolePermissions(entity: RoleEntity): Either<PermissionService.ClearPermissionError, Unit>

    /**
     * Clears all permissions associated with a role identified by its unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the role whose permissions need to be cleared.
     * @return Either an instance of `PermissionService.ClearPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun clearRolePermissions(idOrName: String): Either<PermissionService.ClearPermissionError, Unit>

    /**
     * Checks whether an entity with the given ID has the specified role permission.
     *
     * @param id The unique identifier of the entity whose permissions are being checked.
     * @param permission The name of the permission to be verified.
     * @return A TriState value indicating whether the permission is granted (true), denied (false),
     *         or if the state is indeterminate (unknown).
     */
    fun hasRolePermission(id: UUID, permission: String): TriState

    /**
     * Checks whether an entity with the given ID has the specified role permission.
     *
     * @param entity The role entity to check for permissions.
     * @param permission The name of the permission to be verified.
     * @return A TriState value indicating whether the permission is granted (true), denied (false),
     *         or if the state is indeterminate (unknown).
     */
    fun hasRolePermission(entity: RoleEntity, permission: String): TriState

    /**
     * Checks whether an entity with the given ID or Name has the specified role permission.
     *
     * @param idOrName The unique identifier or name of the role.
     * @param permission The name of the permission to be verified.
     * @return A TriState value indicating whether the permission is granted (true), denied (false),
     *         or if the state is indeterminate (unknown).
     */
    fun hasRolePermission(idOrName: String, permission: String): TriState

    /**
     * Checks if a role identified by its unique UUID has at least one of the specified permissions.
     *
     * @param id The unique identifier of the role.
     * @param permissions A collection of permission names to check.
     * @return `true` if the role has any of the specified permissions, otherwise `false`.
     */
    fun hasRoleAnyPermissions(id: UUID, permissions: Collection<String>): TriState

    /**
     * Checks if the given role entity has any of the specified permissions.
     *
     * @param entity The role entity to be checked for permissions.
     * @param permissions A collection of permission identifiers to check against.
     * @return True if the role entity has at least one of the specified permissions, otherwise false.
     */
    fun hasRoleAnyPermissions(entity: RoleEntity, permissions: Collection<String>): TriState

    /**
     * Checks if a role identified by its unique identifier or name has at least one of the specified permissions.
     *
     * @param idOrName The unique identifier or name of the role.
     * @param permissions A collection of permission names to check.
     * @return `true` if the role has any of the specified permissions, otherwise `false`.
     */
    fun hasRoleAnyPermissions(idOrName: String, permissions: Collection<String>): TriState

    /**
     * Verifies if the given role, identified by its unique ID, possesses all the specified permissions.
     *
     * @param id The unique identifier of the role.
     * @param permissions A collection of permissions to check against the role.
     * @return True if the role has all the specified permissions, otherwise false.
     */
    fun hasRoleAllPermissions(id: UUID, permissions: Collection<String>): TriState

    /**
     * Checks if the provided role entity has all the specified permissions.
     *
     * @param entity The role entity being evaluated.
     * @param permissions A collection of permissions to check against the role entity.
     * @return True if the role entity has all the specified permissions, otherwise false.
     */
    fun hasRoleAllPermissions(entity: RoleEntity, permissions: Collection<String>): TriState

    /**
     * Verifies if the given role, identified by its unique identifier or name, possesses all the specified permissions.
     *
     * @param idOrName The unique identifier or name of the role.
     * @param permissions A collection of permissions to check against the role.
     * @return True if the role has all the specified permissions, otherwise false.
     */
    fun hasRoleAllPermissions(idOrName: String, permissions: Collection<String>): TriState

    /**
     * Retrieves the set of permissions associated with a specific role.
     *
     * @param id The unique identifier of the role for which permissions are to be retrieved.
     * @return A set of RolePermissionEntity objects representing the permissions linked to the specified role.
     */
    fun getRolePermissions(id: UUID): Set<RolePermissionEntity>

    /**
     * Retrieves the set of permissions associated with a given role.
     *
     * @param entity The role entity for which permissions are being retrieved.
     * @return A set of RolePermissionEntity objects representing the permissions linked to the specified role.
     */
    fun getRolePermissions(entity: RoleEntity): Set<RolePermissionEntity>

    /**
     * Retrieves the set of permissions associated with a role identified by its unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the role for which permissions are to be retrieved.
     * @return A set of RolePermissionEntity objects representing the permissions linked to the specified role.
     */
    fun getRolePermissions(idOrName: String): Set<RolePermissionEntity>
}
