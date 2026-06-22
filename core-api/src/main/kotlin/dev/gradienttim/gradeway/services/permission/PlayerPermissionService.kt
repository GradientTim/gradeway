/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.permission

import arrow.core.Either
import dev.gradienttim.gradeway.database.models.player.PlayerEntity
import dev.gradienttim.gradeway.services.PermissionService
import java.util.*

/**
 * Provides operations for managing permissions associated with players.
 * Offers functionality to set, unset, clear, and query permissions for individual players,
 * identified either by UUID or PlayerEntity.
 */
interface PlayerPermissionService {
    /**
     * Sets a specific permission for a player, identified by their unique identifier, to either enable or disabled.
     *
     * @param id The unique identifier of the player.
     * @param permission The name of the permission to modify.
     * @param enabled Whether the permission should be enabled (true) or disabled (false). Defaults to true.
     * @return An instance of [Either], containing either a [PermissionService.SetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setPlayerPermission(
        id: UUID,
        permission: String,
        enabled: Boolean = true
    ): Either<PermissionService.SetPermissionError, Boolean>

    /**
     * Sets a specific permission for a player, identified by their associated [PlayerEntity], to either enable or disabled.
     *
     * @param entity The [PlayerEntity] representing the player for whom the permission is being modified.
     * @param permission The name of the permission to modify.
     * @param enabled A flag indicating whether the permission should be enabled (true) or disabled (false). Defaults to true.
     * @return An [Either] containing a [PermissionService.SetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setPlayerPermission(
        entity: PlayerEntity,
        permission: String,
        enabled: Boolean = true
    ): Either<PermissionService.SetPermissionError, Boolean>

    /**
     * Sets a specific permission for a player, identified by their unique identifier or name, to either enable or disabled.
     *
     * @param idOrName The unique identifier or name of the player.
     * @param permission The name of the permission to modify.
     * @param enabled Whether the permission should be enabled (true) or disabled (false). Defaults to true.
     * @return An instance of [Either], containing either a [PermissionService.SetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setPlayerPermission(
        idOrName: String,
        permission: String,
        enabled: Boolean = true
    ): Either<PermissionService.SetPermissionError, Boolean>

    /**
     * Sets multiple permissions for a player, identified by their unique identifier.
     *
     * @param id The unique identifier of the player whose permissions are being modified.
     * @param permissions A map where each key is the name of a permission and the corresponding value is a boolean
     *                    indicating whether the permission should be enabled (true) or disabled (false).
     * @return An [Either] instance containing either a [PermissionService.BulkSetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setPlayerPermissions(
        id: UUID,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Boolean>

    /**
     * Sets multiple permissions for a player, identified by their associated [PlayerEntity].
     *
     * @param entity The [PlayerEntity] representing the player whose permissions are being modified.
     * @param permissions A map where each key is the name of a permission and the corresponding value is a boolean
     *                    indicating whether the permission should be enabled (true) or disabled (false).
     * @return An instance of [Either], containing either a [PermissionService.BulkSetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setPlayerPermissions(
        entity: PlayerEntity,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Boolean>

    /**
     * Sets multiple permissions for a player, identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the player whose permissions are being modified.
     * @param permissions A map where each key is the name of a permission and the corresponding value is a boolean
     *                    indicating whether the permission should be enabled (true) or disabled (false).
     * @return An [Either] instance containing either a [PermissionService.BulkSetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setPlayerPermissions(
        idOrName: String,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Boolean>

    /**
     * Revokes a specific permission for a player, identified by their unique identifier.
     *
     * @param id The unique identifier of the player from whom the permission will be revoked.
     * @param permission The name of the permission to be revoked.
     * @return An [Either] containing a [PermissionService.UnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetPlayerPermission(id: UUID, permission: String): Either<PermissionService.UnsetPermissionError, Boolean>

    /**
     * Revokes a specific permission for a player, identified by their associated [PlayerEntity].
     *
     * @param entity The [PlayerEntity] representing the player whose permission is being revoked.
     * @param permission The name of the permission to be revoked.
     * @return An [Either] containing a [PermissionService.UnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetPlayerPermission(
        entity: PlayerEntity,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Boolean>

    /**
     * Revokes a specific permission for a player, identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the player from whom the permission will be revoked.
     * @param permission The name of the permission to be revoked.
     * @return An [Either] containing a [PermissionService.UnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetPlayerPermission(
        idOrName: String,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Boolean>

    /**
     * Revokes multiple permissions for a player, identified by their unique identifier.
     *
     * @param id The unique identifier of the player whose permissions will be revoked.
     * @param permissions A list of permission names to be revoked for the specified player.
     * @return An [Either] containing a [PermissionService.BulkUnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetPlayerPermissions(
        id: UUID,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean>

    /**
     * Revokes multiple permissions for a player, identified by their associated [PlayerEntity].
     *
     * @param entity The [PlayerEntity] representing the player whose permissions will be revoked.
     * @param permissions A list of permission names to be revoked for the specified player.
     * @return An [Either] containing a [PermissionService.BulkUnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetPlayerPermissions(
        entity: PlayerEntity,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean>

    /**
     * Revokes multiple permissions for a player, identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the player whose permissions will be revoked.
     * @param permissions A list of permission names to be revoked for the specified player.
     * @return An [Either] containing a [PermissionService.BulkUnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetPlayerPermissions(
        idOrName: String,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Boolean>

    /**
     * Clears all permissions associated with a specific player, identified by their unique identifier.
     *
     * @param id The unique identifier of the player whose permissions should be removed.
     * @return An [Either] containing a [PermissionService.ClearPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun clearPlayerPermissions(id: UUID): Either<PermissionService.ClearPermissionError, Boolean>

    /**
     * Removes all permissions associated with the specified player.
     *
     * @param entity The [PlayerEntity] representing the player whose permissions should be cleared.
     * @return An [Either] containing a [PermissionService.ClearPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun clearPlayerPermissions(entity: PlayerEntity): Either<PermissionService.ClearPermissionError, Boolean>

    /**
     * Clears all permissions associated with a specific player, identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the player whose permissions should be removed.
     * @return An [Either] containing a [PermissionService.ClearPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun clearPlayerPermissions(idOrName: String): Either<PermissionService.ClearPermissionError, Boolean>

    /**
     * Checks if a player, identified by their unique identifier, has a specific permission.
     *
     * @param id The unique identifier of the player.
     * @param permission The name of the permission to check.
     * @return `true` if the player has the specified permission, otherwise `false`.
     */
    fun hasPlayerPermission(id: UUID, permission: String): Boolean

    /**
     * Checks if a player, represented by their [PlayerEntity], has a specific permission.
     *
     * @param entity The [PlayerEntity] representing the player whose permission is being checked.
     * @param permission The name of the permission to verify.
     * @return `true` if the player has the specified permission; otherwise, `false`.
     */
    fun hasPlayerPermission(entity: PlayerEntity, permission: String): Boolean

    /**
     * Checks if a player, identified by their unique identifier or name, has a specific permission.
     *
     * @param idOrName The unique identifier or name of the player.
     * @param permission The name of the permission to check.
     * @return `true` if the player has the specified permission, otherwise `false`.
     */
    fun hasPlayerPermission(idOrName: String, permission: String): Boolean

    /**
     * Checks if a player, identified by their UUID, has at least one of the specified permissions.
     *
     * @param id The unique identifier (UUID) of the player whose permissions are being checked.
     * @param permissions A list of permission strings to check against the player's permissions.
     * @return `true` if the player has at least one of the permissions in the list, `false` otherwise.
     */
    fun hasPlayerAnyPermissions(id: UUID, permissions: List<String>): Boolean

    /**
     * Checks if the given player entity has any of the specified permissions.
     *
     * @param entity The player entity to check for permissions.
     * @param permissions A list of permission strings to verify against the player entity.
     * @return True if the player entity has at least one of the specified permissions, false otherwise.
     */
    fun hasPlayerAnyPermissions(entity: PlayerEntity, permissions: List<String>): Boolean

    /**
     * Checks if a player, identified by their unique identifier or name, has at least one of the specified permissions.
     *
     * @param idOrName The unique identifier or name of the player whose permissions are being checked.
     * @param permissions A list of permission strings to check against the player's permissions.
     * @return `true` if the player has at least one of the permissions in the list, `false` otherwise.
     */
    fun hasPlayerAnyPermissions(idOrName: String, permissions: List<String>): Boolean

    /**
     * Checks if a player identified by their UUID has all the specified permissions.
     *
     * @param id The unique identifier (UUID) of the player whose permissions are being checked.
     * @param permissions The list of permissions to verify against the player's assigned permissions.
     * @return `true` if the player has all the specified permissions, `false` otherwise.
     */
    fun hasPlayerAllPermissions(id: UUID, permissions: List<String>): Boolean

    /**
     * Checks if a player entity has all the specified permissions.
     *
     * @param entity The player entity to check.
     * @param permissions The list of permissions to validate against the player's permissions.
     * @return `true` if the player has all the specified permissions; `false` otherwise.
     */
    fun hasPlayerAllPermissions(entity: PlayerEntity, permissions: List<String>): Boolean

    /**
     * Checks if a player, identified by their unique identifier or name, has all the specified permissions.
     *
     * @param idOrName The unique identifier or name of the player whose permissions are being checked.
     * @param permissions The list of permissions to verify against the player's assigned permissions.
     * @return `true` if the player has all the specified permissions, `false` otherwise.
     */
    fun hasPlayerAllPermissions(idOrName: String, permissions: List<String>): Boolean

    /**
     * Retrieves the permissions assigned to a player identified by their unique identifier.
     *
     * @param id The unique identifier of the player whose permissions are being fetched.
     * @return A map where the keys represent permission names and the values are booleans
     * indicating whether the permission is granted (true) or denied (false).
     */
    fun getPlayerPermissions(id: UUID): Map<String, Boolean>

    /**
     * Retrieves the permission mapping for a given player.
     *
     * @param entity the player entity for which the permissions are being fetched.
     * @return a map where the keys represent permission names and the values indicate
     * whether the player has the corresponding permission (true) or not (false).
     */
    fun getPlayerPermissions(entity: PlayerEntity): Map<String, Boolean>

    /**
     * Retrieves the permissions assigned to a player identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the player whose permissions are being fetched.
     * @return A map where the keys represent permission names and the values are booleans
     * indicating whether the permission is granted (true) or denied (false).
     */
    fun getPlayerPermissions(idOrName: String): Map<String, Boolean>

    /**
     * Retrieves a set of permissions associated with a player based on their unique identifier and status.
     *
     * @param id The unique identifier of the player.
     * @param status The status of the player, which may influence the permissions returned.
     * @return A set of permissions associated with the player.
     */
    fun getPlayerPermissions(id: UUID, status: Boolean): Set<String>

    /**
     * Retrieves the set of permissions assigned to a player entity based on the provided status.
     *
     * @param entity The player entity whose permissions are being queried.
     * @param status A boolean flag indicating the specific condition or context for which permissions should be retrieved.
     * @return A set of strings representing the permissions associated with the given player entity and status.
     */
    fun getPlayerPermissions(entity: PlayerEntity, status: Boolean): Set<String>

    /**
     * Retrieves a set of permissions associated with a player based on their unique identifier or name and status.
     *
     * @param idOrName The unique identifier or name of the player.
     * @param status The status of the player, which may influence the permissions returned.
     * @return A set of permissions associated with the player.
     */
    fun getPlayerPermissions(idOrName: String, status: Boolean): Set<String>

    /**
     * Retrieves the set of permissions enabled for a specific player.
     *
     * @param id The unique identifier of the player whose enabled permissions are to be retrieved.
     * @return A set of strings representing the enabled permissions for the specified player.
     */
    fun getPlayerEnabledPermissions(id: UUID): Set<String> = getPlayerPermissions(id, true)

    /**
     * Retrieves the set of enabled permissions for a given player entity.
     *
     * @param entity The player entity for which to retrieve the enabled permissions.
     * @return A set of strings representing the enabled permissions for the specified player entity.
     */
    fun getPlayerEnabledPermissions(entity: PlayerEntity): Set<String> = getPlayerPermissions(entity, true)

    /**
     * Retrieves the set of permissions enabled for a player identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the player whose enabled permissions are to be retrieved.
     * @return A set of strings representing the enabled permissions for the specified player.
     */
    fun getPlayerEnabledPermissions(idOrName: String): Set<String> = getPlayerPermissions(idOrName, true)

    /**
     * Retrieves a set of permissions that are disabled for a specific player.
     *
     * @param id The unique identifier of the player whose disabled permissions are to be retrieved.
     * @return A set of strings representing the disabled permissions for the specified player.
     */
    fun getPlayerDisabledPermissions(id: UUID): Set<String> = getPlayerPermissions(id, false)

    /**
     * Retrieves the set of permissions that are disabled for a specific player.
     *
     * @param entity The player entity whose disabled permissions are to be retrieved.
     * @return A set of strings representing the permissions that are disabled for the provided player.
     */
    fun getPlayerDisabledPermissions(entity: PlayerEntity): Set<String> = getPlayerPermissions(entity, false)

    /**
     * Retrieves the set of permissions that are disabled for a player identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the player whose disabled permissions are to be retrieved.
     * @return A set of strings representing the disabled permissions for the specified player.
     */
    fun getPlayerDisabledPermissions(idOrName: String): Set<String> = getPlayerPermissions(idOrName, false)
}
