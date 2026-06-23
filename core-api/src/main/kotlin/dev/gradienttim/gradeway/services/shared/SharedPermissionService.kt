/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.shared

import arrow.core.Either
import dev.gradienttim.gradeway.services.PermissionService
import java.util.*

/**
 * A service interface for managing and querying permissions for entities.
 *
 * @param TEntity The type of entity for which permissions are managed.
 */
interface SharedPermissionService<TEntity> {
    /**
     * Sets the specified permission for the entity identified by the given ID.
     *
     * @param id The unique identifier of the entity for which the permission is being set.
     * @param permission The name of the permission to set.
     * @param enabled A flag indicating whether the permission should be enabled or disabled. Defaults to true.
     * @return An instance of [Either] containing either a [PermissionService.SetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setPermission(
        id: UUID,
        permission: String,
        enabled: Boolean = true
    ): Either<PermissionService.SetPermissionError, Boolean>

    /**
     * Sets the specified permission for the given entity.
     *
     * @param entity The entity for which the permission is being set.
     * @param permission The name of the permission to set.
     * @param enabled A flag indicating whether the permission should be enabled or disabled. Defaults to true.
     * @return An instance of [Either] containing either a [PermissionService.SetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setPermission(
        entity: TEntity,
        permission: String,
        enabled: Boolean = true
    ): Either<PermissionService.SetPermissionError, Boolean>

    /**
     * Sets the specified permission for the entity identified by the given unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity for which the permission is being set.
     * @param permission The name of the permission to set.
     * @param enabled A flag indicating whether the permission should be enabled or disabled. Defaults to true.
     * @return An instance of [Either] containing either a [PermissionService.SetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setPermission(
        idOrName: String,
        permission: String,
        enabled: Boolean = true
    ): Either<PermissionService.SetPermissionError, Boolean>

    /**
     * Sets multiple permissions for the entity identified by the given ID.
     *
     * @param id The unique identifier of the entity for which permissions are being set.
     * @param permissions A map of permission names to their enabled states, where the map key represents the
     *                    permission name and the value represents whether the permission should be enabled (true)
     *                    or disabled (false).
     * @return An [Either] containing either a [PermissionService.BulkSetPermissionError] if an error occurs
     *         or `true` if the update succeeds.
     */
    fun setPermissions(
        id: UUID,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Boolean>

    /**
     * Sets multiple permissions for the specified entity.
     *
     * @param entity The entity for which permissions are being set.
     * @param permissions A map of permission names to their enabled states, where the map key represents the
     *                    permission name and the value represents whether the permission should be enabled (true)
     *                    or disabled (false).
     * @return An [Either] containing either a [PermissionService.BulkSetPermissionError] if an error occurs
     *         or `true` if the update succeeds.
     */
    fun setPermissions(
        entity: TEntity,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Boolean>

    /**
     * Sets multiple permissions for the entity identified by the given unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity for which permissions are being set.
     * @param permissions A map of permission names to their enabled states, where the map key represents the
     *                    permission name and the value represents whether the permission should be enabled (true)
     *                    or disabled (false).
     * @return An [Either] containing either a [PermissionService.BulkSetPermissionError] if an error occurs
     *         or `true` if the update succeeds.
     */
    fun setPermissions(
        idOrName: String,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Boolean>

    /**
     * Removes the specified permission for the entity identified by the given ID.
     *
     * @param id The unique identifier of the entity whose permission is to be removed.
     * @param permission The name of the permission to unset.
     * @return An [Either] containing either a [PermissionService.UnsetPermissionError] if an error occurs
     *         or `true` if the update succeeds.
     */
    fun unsetPermission(id: UUID, permission: String): Either<PermissionService.UnsetPermissionError, Boolean>

    /**
     * Removes the specified permission for the given entity.
     *
     * @param entity The entity for which the permission is to be removed.
     * @param permission The name of the permission to unset.
     * @return An [Either] containing either a [PermissionService.UnsetPermissionError] if an error occurs
     *         or `true` if the update succeeds.
     */
    fun unsetPermission(entity: TEntity, permission: String): Either<PermissionService.UnsetPermissionError, Boolean>

    /**
     * Removes the specified permission for the entity identified by the given unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity whose permission is to be removed.
     * @param permission The name of the permission to unset.
     * @return An [Either] containing either a [PermissionService.UnsetPermissionError] if an error occurs
     *         or `true` if the update succeeds.
     */
    fun unsetPermission(idOrName: String, permission: String): Either<PermissionService.UnsetPermissionError, Boolean>

    /**
     * Removes multiple permissions for the entity identified by the given ID.
     *
     * @param id The unique identifier of the entity whose permissions are to be removed.
     * @param permissions A list of permission names to unset.
     * @return An [Either] containing either a [PermissionService.BulkUnsetPermissionError] if an error occurs
     *         or `true` if the update succeeds.
     */
    fun unsetPermissions(
        id: UUID,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean>

    /**
     * Removes multiple permissions for the specified entity.
     *
     * @param entity The entity for which permissions are to be removed.
     * @param permissions A list of permission names to unset from the specified entity.
     * @return An [Either] containing either a [PermissionService.BulkUnsetPermissionError] if an error occurs
     *         or `true` if the update succeeds.
     */
    fun unsetPermissions(
        entity: TEntity,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean>

    /**
     * Removes multiple permissions for the entity identified by the given unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity whose permissions are to be removed.
     * @param permissions A list of permission names to unset from the specified entity.
     * @return An [Either] containing either a [PermissionService.BulkUnsetPermissionError] if an error occurs
     *         or `true` if the update succeeds.
     */
    fun unsetPermissions(
        idOrName: String,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean>

    /**
     * Clears all permissions associated with the entity identified by the given unique identifier.
     *
     * @param id The unique identifier of the entity whose permissions are to be cleared.
     * @return An [Either] containing either a [PermissionService.ClearPermissionError] if an error occurs
     *         or `true` if the update succeeds.
     */
    fun clearPermissions(id: UUID): Either<PermissionService.ClearPermissionError, Boolean>

    /**
     * Clears all permissions associated with the specified entity.
     *
     * @param entity The entity whose permissions are to be cleared.
     * @return An [Either] containing either a [PermissionService.ClearPermissionError] if an error occurs during
     *         or `true` if the update succeeds.
     */
    fun clearPermissions(entity: TEntity): Either<PermissionService.ClearPermissionError, Boolean>

    /**
     * Clears all permissions associated with the entity identified by the given unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity whose permissions are to be cleared.
     * @return An [Either] containing either a [PermissionService.ClearPermissionError] if an error occurs during
     *         or `true` if the update succeeds.
     */
    fun clearPermissions(idOrName: String): Either<PermissionService.ClearPermissionError, Boolean>

    /**
     * Checks whether the entity identified by the given ID has the specified permission.
     *
     * @param id The unique identifier of the entity whose permission is being checked.
     * @param permission The name of the permission to verify for the specified entity.
     * @return True if the entity has the specified permission; false otherwise.
     */
    fun hasPermission(id: UUID, permission: String): Boolean

    /**
     * Checks if the specified entity has the given permission.
     *
     * @param entity The entity for which the permission is being checked.
     * @param permission The name of the permission to verify for the specified entity.
     * @return True if the entity has the specified permission; false otherwise.
     */
    fun hasPermission(entity: TEntity, permission: String): Boolean

    /**
     * Checks whether the entity identified by the given unique identifier or name has the specified permission.
     *
     * @param idOrName The unique identifier or name of the entity whose permission is being checked.
     * @param permission The name of the permission to verify for the specified entity.
     * @return True if the entity has the specified permission; false otherwise.
     */
    fun hasPermission(idOrName: String, permission: String): Boolean

    /**
     * Checks whether the entity identified by the given ID has at least one of the specified permissions.
     *
     * @param id The unique identifier of the entity whose permissions are being checked.
     * @param permissions A list of permission names to check for the specified entity.
     * @return True if the entity has at least one of the specified permissions; false otherwise.
     */
    fun hasAnyPermissions(id: UUID, permissions: List<String>): Boolean

    /**
     * Checks if the given entity has any of the specified permissions.
     *
     * @param entity The entity to check for permissions.
     * @param permissions A list of permissions to verify against the entity.
     * @return True if the entity has at least one of the specified permissions, false otherwise.
     */
    fun hasAnyPermissions(entity: TEntity, permissions: List<String>): Boolean

    /**
     * Checks if the entity identified by the given unique identifier or name has at least one of the
     * specified permissions.
     *
     * @param idOrName The unique identifier or name of the entity whose permissions are being checked.
     * @param permissions A list of permission names to check for the specified entity.
     * @return True if the entity has at least one of the specified permissions; false otherwise.
     */
    fun hasAnyPermissions(idOrName: String, permissions: List<String>): Boolean

    /**
     * Checks if the given entity identified by its UUID has all the specified permissions.
     *
     * @param id The unique identifier of the entity whose permissions are being checked.
     * @param permissions A list of permissions to verify for the given entity.
     * @return `true` if the entity possesses all the specified permissions, otherwise `false`.
     */
    fun hasAllPermissions(id: UUID, permissions: List<String>): Boolean

    /**
     * Checks if the specified entity has all the permissions provided in the list.
     *
     * @param entity The entity for which the permissions are being checked.
     * @param permissions A list of permissions to verify against the entity.
     * @return `true` if the entity possesses all the specified permissions, otherwise `false`.
     */
    fun hasAllPermissions(entity: TEntity, permissions: List<String>): Boolean

    /**
     * Checks if the entity identified by the given unique identifier or name has all the permissions
     * provided in the list.
     *
     * @param idOrName The unique identifier or name of the entity for which the permissions are being checked.
     * @param permissions A list of permissions to verify against the entity.
     * @return `true` if the entity possesses all the specified permissions, otherwise `false`.
     */
    fun hasAllPermissions(idOrName: String, permissions: List<String>): Boolean

    /**
     * Retrieves the permissions associated with a specific ID.
     *
     * @param id The unique identifier for which permissions are being retrieved.
     * @return A map where the keys represent permission names and the values indicate
     * whether the permission is granted (true) or denied (false).
     */
    fun getPermissions(id: UUID): Map<String, Boolean>

    /**
     * Retrieves a map of permissions for the given entity, where the keys represent permission names
     * and the values indicate whether each permission is granted or not.
     *
     * @param entity The entity for which permissions are being retrieved.
     * @return A map where the keys are permission names and the values are booleans indicating whether
     *         each permission is granted (true) or denied (false).
     */
    fun getPermissions(entity: TEntity): Map<String, Boolean>

    /**
     * Retrieves a map of permissions for the entity identified by the given unique identifier or name,
     * where the keys represent permission names and the values indicate whether each permission is
     * granted or not.
     *
     * @param idOrName The unique identifier or name of the entity for which permissions are being retrieved.
     * @return A map where the keys are permission names and the values are booleans indicating whether
     *         each permission is granted (true) or denied (false).
     */
    fun getPermissions(idOrName: String): Map<String, Boolean>

    /**
     * Retrieves a set of permissions based on the provided ID and enabled flag.
     *
     * @param id The unique identifier for which permissions are fetched.
     * @param enabled A Boolean flag indicating whether to include only enabled permissions.
     * @return A set of strings representing the permissions associated with the given ID and criteria.
     */
    fun getPermissions(id: UUID, enabled: Boolean): Set<String>

    /**
     * Retrieves the set of permissions associated with the given entity.
     *
     * @param entity The entity for which permissions are being fetched.
     * @param enabled A flag indicating whether only enabled permissions should be included.
     * @return A set of permissions associated with the entity.
     */
    fun getPermissions(entity: TEntity, enabled: Boolean): Set<String>

    /**
     * Retrieves the set of permissions associated with the entity identified by the given unique
     * identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity for which permissions are being fetched.
     * @param enabled A flag indicating whether only enabled permissions should be included.
     * @return A set of permissions associated with the entity.
     */
    fun getPermissions(idOrName: String, enabled: Boolean): Set<String>

    /**
     * Retrieves the enabled permissions for a specific entity identified by the given UUID.
     *
     * @param id The unique identifier of the entity whose enabled permissions are to be fetched.
     * @return A set of strings representing the enabled permissions associated with the specified entity.
     */
    fun getEnabledPermissions(id: UUID): Set<String> = getPermissions(id, true)

    /**
     * Retrieves the set of enabled permissions for the specified entity.
     *
     * @param entity The entity for which enabled permissions are to be fetched.
     * @return A set containing the names of the enabled permissions associated with the entity.
     */
    fun getEnabledPermissions(entity: TEntity): Set<String> = getPermissions(entity, true)

    /**
     * Retrieves the enabled permissions for the entity identified by the given unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity whose enabled permissions are to be fetched.
     * @return A set of strings representing the enabled permissions associated with the specified entity.
     */
    fun getEnabledPermissions(idOrName: String): Set<String> = getPermissions(idOrName, true)

    /**
     * Retrieves a set of permissions that are currently disabled for a given user or entity.
     *
     * @param id The unique identifier of the user or entity whose disabled permissions are to be retrieved.
     * @return A set of strings representing the disabled permissions.
     */
    fun getDisabledPermissions(id: UUID): Set<String> = getPermissions(id, false)

    /**
     * Retrieves the set of permissions that are disabled for the given entity.
     *
     * @param entity The entity for which to retrieve the disabled permissions.
     * @return A set of disabled permissions associated with the specified entity.
     */
    fun getDisabledPermissions(entity: TEntity): Set<String> = getPermissions(entity, false)

    /**
     * Retrieves the set of permissions that are disabled for the entity identified by the given unique
     * identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity for which to retrieve the disabled permissions.
     * @return A set of disabled permissions associated with the specified entity.
     */
    fun getDisabledPermissions(idOrName: String): Set<String> = getPermissions(idOrName, false)
}
