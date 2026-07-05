/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.permission

import arrow.core.Either
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.player.PlayerPermissionEntity
import dev.gradienttim.gradeway.services.PermissionService
import net.kyori.adventure.util.TriState
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
    ): Either<PermissionService.SetPermissionError, Unit>

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
    ): Either<PermissionService.SetPermissionError, Unit>

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
    ): Either<PermissionService.SetPermissionError, Unit>

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
    ): Either<PermissionService.BulkSetPermissionError, Unit>

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
    ): Either<PermissionService.BulkSetPermissionError, Unit>

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
    ): Either<PermissionService.BulkSetPermissionError, Unit>

    /**
     * Revokes a specific permission for a player, identified by their unique identifier.
     *
     * @param id The unique identifier of the player from whom the permission will be revoked.
     * @param permission The name of the permission to be revoked.
     * @return An [Either] containing a [PermissionService.UnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetPlayerPermission(id: UUID, permission: String): Either<PermissionService.UnsetPermissionError, Unit>

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
    ): Either<PermissionService.UnsetPermissionError, Unit>

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
    ): Either<PermissionService.UnsetPermissionError, Unit>

    /**
     * Revokes multiple permissions for a player, identified by their unique identifier.
     *
     * @param id The unique identifier of the player whose permissions will be revoked.
     * @param permissions A collection of permission names to be revoked for the specified player.
     * @return An [Either] containing a [PermissionService.BulkUnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetPlayerPermissions(
        id: UUID,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit>

    /**
     * Revokes multiple permissions for a player, identified by their associated [PlayerEntity].
     *
     * @param entity The [PlayerEntity] representing the player whose permissions will be revoked.
     * @param permissions A collection of permission names to be revoked for the specified player.
     * @return An [Either] containing a [PermissionService.BulkUnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetPlayerPermissions(
        entity: PlayerEntity,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit>

    /**
     * Revokes multiple permissions for a player, identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the player whose permissions will be revoked.
     * @param permissions A collection of permission names to be revoked for the specified player.
     * @return An [Either] containing a [PermissionService.BulkUnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetPlayerPermissions(
        idOrName: String,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit>

    /**
     * Clears all permissions associated with a specific player, identified by their unique identifier.
     *
     * @param id The unique identifier of the player whose permissions should be removed.
     * @return An [Either] containing a [PermissionService.ClearPermissionError] if an error occurs,
     *         or `Unit` if the update succeeds.
     */
    fun clearPlayerPermissions(id: UUID): Either<PermissionService.ClearPermissionError, Unit>

    /**
     * Removes all permissions associated with the specified player.
     *
     * @param entity The [PlayerEntity] representing the player whose permissions should be cleared.
     * @return An [Either] containing a [PermissionService.ClearPermissionError] if an error occurs,
     *         or `Unit` if the update succeeds.
     */
    fun clearPlayerPermissions(entity: PlayerEntity): Either<PermissionService.ClearPermissionError, Unit>

    /**
     * Clears all permissions associated with a specific player, identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the player whose permissions should be removed.
     * @return An [Either] containing a [PermissionService.ClearPermissionError] if an error occurs,
     *         or `Unit` if the update succeeds.
     */
    fun clearPlayerPermissions(idOrName: String): Either<PermissionService.ClearPermissionError, Unit>

    /**
     * Checks if a player, identified by their unique identifier, has a specific permission.
     *
     * @param id The unique identifier of the player.
     * @param permission The name of the permission to check.
     * @return [TriState.TRUE] if the permission is enabled, [TriState.NOT_SET] if it is not configured or the player
     *         is not found, [TriState.FALSE] if the permission is explicitly disabled.
     */
    fun hasPlayerPermission(id: UUID, permission: String): Boolean

    /**
     * Checks if a player, represented by their [PlayerEntity], has a specific permission.
     *
     * @param entity The [PlayerEntity] representing the player whose permission is being checked.
     * @param permission The name of the permission to verify.
     * @return [TriState.TRUE] if the permission is enabled, [TriState.NOT_SET] if it is not configured or disabled,
     *         [TriState.FALSE] if the player entity is not found.
     */
    fun hasPlayerPermission(entity: PlayerEntity, permission: String): Boolean

    /**
     * Checks if a player, identified by their unique identifier or name, has a specific permission.
     *
     * @param idOrName The unique identifier or name of the player.
     * @param permission The name of the permission to check.
     * @return [TriState.TRUE] if the permission is enabled, [TriState.NOT_SET] if it is not configured or the player
     *         is not found, [TriState.FALSE] if the permission is explicitly disabled.
     */
    fun hasPlayerPermission(idOrName: String, permission: String): Boolean

    /**
     * Checks if a player, identified by their UUID, has at least one of the specified permissions.
     *
     * @param id The unique identifier (UUID) of the player whose permissions are being checked.
     * @param permissions A collection of permission strings to check against the player's permissions.
     * @return [TriState.TRUE] if at least one permission is enabled, [TriState.FALSE] if permissions are found but
     *         none enabled, [TriState.NOT_SET] if none of the permissions are configured or the player is not found.
     */
    fun hasPlayerAnyPermissions(id: UUID, permissions: Collection<String>): Boolean

    /**
     * Checks if the given player entity has any of the specified permissions.
     *
     * @param entity The player entity to check for permissions.
     * @param permissions A collection of permission strings to verify against the player entity.
     * @return [TriState.TRUE] if at least one permission is enabled, [TriState.FALSE] if permissions are found but
     *         none enabled, [TriState.NOT_SET] if none of the permissions are configured.
     */
    fun hasPlayerAnyPermissions(entity: PlayerEntity, permissions: Collection<String>): Boolean

    /**
     * Checks if a player, identified by their unique identifier or name, has at least one of the specified permissions.
     *
     * @param idOrName The unique identifier or name of the player whose permissions are being checked.
     * @param permissions A collection of permission strings to check against the player's permissions.
     * @return [TriState.TRUE] if at least one permission is enabled, [TriState.FALSE] if permissions are found but
     *         none enabled, [TriState.NOT_SET] if none of the permissions are configured or the player is not found.
     */
    fun hasPlayerAnyPermissions(idOrName: String, permissions: Collection<String>): Boolean

    /**
     * Checks if a player identified by their UUID has all the specified permissions.
     *
     * @param id The unique identifier (UUID) of the player whose permissions are being checked.
     * @param permissions The collection of permissions to verify against the player's assigned permissions.
     * @return [TriState.TRUE] if all permissions are enabled, [TriState.FALSE] if a permission is found but disabled,
     *         [TriState.NOT_SET] if any permission is not configured or the player is not found.
     */
    fun hasPlayerAllPermissions(id: UUID, permissions: Collection<String>): Boolean

    /**
     * Checks if a player entity has all the specified permissions.
     *
     * @param entity The player entity to check.
     * @param permissions The collection of permissions to validate against the player's permissions.
     * @return [TriState.TRUE] if all permissions are enabled, [TriState.FALSE] if a permission is found but disabled,
     *         [TriState.NOT_SET] if any permission is not configured.
     */
    fun hasPlayerAllPermissions(entity: PlayerEntity, permissions: Collection<String>): Boolean

    /**
     * Checks if a player, identified by their unique identifier or name, has all the specified permissions.
     *
     * @param idOrName The unique identifier or name of the player whose permissions are being checked.
     * @param permissions The collection of permissions to verify against the player's assigned permissions.
     * @return [TriState.TRUE] if all permissions are enabled, [TriState.FALSE] if a permission is found but disabled,
     *         [TriState.NOT_SET] if any permission is not configured or the player is not found.
     */
    fun hasPlayerAllPermissions(idOrName: String, permissions: Collection<String>): Boolean

    /**
     * Retrieves the permissions assigned to a player identified by their unique identifier.
     *
     * @param id The unique identifier of the player whose permissions are being fetched.
     * @return A map where the keys represent permission names and the values are booleans
     * indicating whether the permission is granted (true) or denied (false).
     */
    fun getPlayerPermissions(id: UUID): Set<PlayerPermissionEntity>

    /**
     * Retrieves the permission mapping for a given player.
     *
     * @param entity the player entity for which the permissions are being fetched.
     * @return a map where the keys represent permission names and the values indicate
     * whether the player has the corresponding permission (true) or not (false).
     */
    fun getPlayerPermissions(entity: PlayerEntity): Set<PlayerPermissionEntity>

    /**
     * Retrieves the permissions assigned to a player identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the player whose permissions are being fetched.
     * @return A map where the keys represent permission names and the values are booleans
     * indicating whether the permission is granted (true) or denied (false).
     */
    fun getPlayerPermissions(idOrName: String): Set<PlayerPermissionEntity>
}
