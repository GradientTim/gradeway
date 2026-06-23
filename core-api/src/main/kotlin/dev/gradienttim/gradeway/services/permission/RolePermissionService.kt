/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.permission

import arrow.core.Either
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.services.PermissionService
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
    ): Either<PermissionService.SetPermissionError, Boolean>

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
    ): Either<PermissionService.SetPermissionError, Boolean>

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
    ): Either<PermissionService.SetPermissionError, Boolean>

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
    ): Either<PermissionService.BulkSetPermissionError, Boolean>

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
    ): Either<PermissionService.BulkSetPermissionError, Boolean>

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
    ): Either<PermissionService.BulkSetPermissionError, Boolean>

    /**
     * Removes a specific permission from a role identified by its UUID.
     * If the operation fails, an error indicating the cause will be returned.
     *
     * @param id The unique identifier of the role from which the permission should be removed.
     * @param permission The name of the permission to be unset.
     * @return Either an instance of `PermissionService.UnsetPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetRolePermission(id: UUID, permission: String): Either<PermissionService.UnsetPermissionError, Boolean>

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
    ): Either<PermissionService.UnsetPermissionError, Boolean>

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
    ): Either<PermissionService.UnsetPermissionError, Boolean>

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
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean>

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
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean>

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
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean>

    /**
     * Clears all permissions associated with a role identified by its unique UUID.
     *
     * @param id The unique identifier of the role whose permissions need to be cleared.
     * @return Either an instance of `PermissionService.ClearPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun clearRolePermissions(id: UUID): Either<PermissionService.ClearPermissionError, Boolean>

    /**
     * Clears all permissions associated with the provided role entity.
     *
     * @param entity The role entity whose permissions need to be cleared.
     * @return Either an instance of `PermissionService.ClearPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun clearRolePermissions(entity: RoleEntity): Either<PermissionService.ClearPermissionError, Boolean>

    /**
     * Clears all permissions associated with a role identified by its unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the role whose permissions need to be cleared.
     * @return Either an instance of `PermissionService.ClearPermissionError` if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun clearRolePermissions(idOrName: String): Either<PermissionService.ClearPermissionError, Boolean>

    /**
     * Checks if a role identified by its unique UUID has a specific permission.
     *
     * @param id The unique identifier of the role.
     * @param permission The name of the permission to check.
     * @return `true` if the role has the specified permission, otherwise `false`.
     */
    fun hasRolePermission(id: UUID, permission: String): Boolean

    /**
     * Checks if the given role entity has the specified permission.
     *
     * @param entity The role entity to check for permissions.
     * @param permission The permission to verify against the role entity.
     * @return `true` if the role entity has the specified permission, `false` otherwise.
     */
    fun hasRolePermission(entity: RoleEntity, permission: String): Boolean

    /**
     * Checks if a role identified by its unique identifier or name has the specified permission.
     *
     * @param idOrName The unique identifier or name of the role.
     * @param permission The name of the permission to check.
     * @return `true` if the role has the specified permission, otherwise `false`.
     */
    fun hasRolePermission(idOrName: String, permission: String): Boolean

    /**
     * Checks if a role identified by its unique UUID has at least one of the specified permissions.
     *
     * @param id The unique identifier of the role.
     * @param permissions A list of permission names to check.
     * @return `true` if the role has any of the specified permissions, otherwise `false`.
     */
    fun hasRoleAnyPermissions(id: UUID, permissions: List<String>): Boolean

    /**
     * Checks if the given role entity has any of the specified permissions.
     *
     * @param entity The role entity to be checked for permissions.
     * @param permissions A list of permission identifiers to check against.
     * @return True if the role entity has at least one of the specified permissions, otherwise false.
     */
    fun hasRoleAnyPermissions(entity: RoleEntity, permissions: List<String>): Boolean

    /**
     * Checks if a role identified by its unique identifier or name has at least one of the specified permissions.
     *
     * @param idOrName The unique identifier or name of the role.
     * @param permissions A list of permission names to check.
     * @return `true` if the role has any of the specified permissions, otherwise `false`.
     */
    fun hasRoleAnyPermissions(idOrName: String, permissions: List<String>): Boolean

    /**
     * Verifies if the given role, identified by its unique ID, possesses all the specified permissions.
     *
     * @param id The unique identifier of the role.
     * @param permissions A list of permissions to check against the role.
     * @return True if the role has all the specified permissions, otherwise false.
     */
    fun hasRoleAllPermissions(id: UUID, permissions: List<String>): Boolean

    /**
     * Checks if the provided role entity has all the specified permissions.
     *
     * @param entity The role entity being evaluated.
     * @param permissions A list of permissions to check against the role entity.
     * @return True if the role entity has all the specified permissions, otherwise false.
     */
    fun hasRoleAllPermissions(entity: RoleEntity, permissions: List<String>): Boolean

    /**
     * Verifies if the given role, identified by its unique identifier or name, possesses all the specified permissions.
     *
     * @param idOrName The unique identifier or name of the role.
     * @param permissions A list of permissions to check against the role.
     * @return True if the role has all the specified permissions, otherwise false.
     */
    fun hasRoleAllPermissions(idOrName: String, permissions: List<String>): Boolean

    /**
     * Retrieves the permissions associated with a specific role.
     *
     * @param id The unique identifier of the role for which permissions are to be retrieved.
     * @return A map where the keys represent permission names as strings,
     *         and the values indicate whether the permission is granted (true) or not (false).
     */
    fun getRolePermissions(id: UUID): Map<String, Boolean>

    /**
     * Retrieves the permissions associated with a given role.
     *
     * @param entity The role entity for which permissions are being fetched.
     * @return A map where the keys represent permission names and the values indicate
     *         whether the permission is granted (true) or denied (false).
     */
    fun getRolePermissions(entity: RoleEntity): Map<String, Boolean>

    /**
     * Retrieves the permissions associated with a role identified by its unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the role for which permissions are to be retrieved.
     * @return A map where the keys represent permission names as strings,
     *         and the values indicate whether the permission is granted (true) or not (false).
     */
    fun getRolePermissions(idOrName: String): Map<String, Boolean>

    /**
     * Retrieves the set of permissions associated with a specific role.
     *
     * @param id The unique identifier of the role.
     * @param status A boolean indicating whether to include permissions based on their active status.
     * @return A set of permission strings associated with the specified role.
     */
    fun getRolePermissions(id: UUID, status: Boolean): Set<String>

    /**
     * Retrieves the set of permissions associated with a given role entity based on its status.
     *
     * @param entity The role entity for which permissions are to be retrieved.
     * @param status A boolean value indicating the status that can influence the permissions.
     * @return A set of permissions corresponding to the role entity and status.
     */
    fun getRolePermissions(entity: RoleEntity, status: Boolean): Set<String>

    /**
     * Retrieves the set of permissions associated with a role identified by its unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the role.
     * @param status A boolean indicating whether to include permissions based on their active status.
     * @return A set of permission strings associated with the specified role.
     */
    fun getRolePermissions(idOrName: String, status: Boolean): Set<String>

    /**
     * Retrieves the set of permissions enabled for a given role.
     *
     * @param id The unique identifier of the role for which enabled permissions are being fetched.
     * @return A set of strings representing the permissions enabled for the specified role.
     */
    fun getRoleEnabledPermissions(id: UUID): Set<String> = getRolePermissions(id, true)

    /**
     * Retrieves the set of permissions enabled for a specific role.
     *
     * @param entity The RoleEntity instance representing the role for which permissions are to be retrieved.
     * @return A set of enabled permissions associated with the given role.
     */
    fun getRoleEnabledPermissions(entity: RoleEntity): Set<String> = getRolePermissions(entity, true)

    /**
     * Retrieves the set of permissions enabled for a role identified by its unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the role for which enabled permissions are being fetched.
     * @return A set of strings representing the permissions enabled for the specified role.
     */
    fun getRoleEnabledPermissions(idOrName: String): Set<String> = getRolePermissions(idOrName, true)

    /**
     * Retrieves a set of disabled permissions associated with a specific role.
     *
     * @param id The unique identifier of the role for which disabled permissions are to be retrieved.
     * @return A set of permission strings that are disabled for the specified role.
     */
    fun getRoleDisabledPermissions(id: UUID): Set<String> = getRolePermissions(id, false)

    /**
     * Retrieves the set of permissions that are disabled for the given role entity.
     *
     * @param entity The role entity whose disabled permissions are to be retrieved.
     * @return A set of strings representing the disabled permissions for the specified role entity.
     */
    fun getRoleDisabledPermissions(entity: RoleEntity): Set<String> = getRolePermissions(entity, false)

    /**
     * Retrieves the set of permissions that are disabled for a role identified by its unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the role whose disabled permissions are to be retrieved.
     * @return A set of strings representing the disabled permissions for the specified role.
     */
    fun getRoleDisabledPermissions(idOrName: String): Set<String> = getRolePermissions(idOrName, false)
}
