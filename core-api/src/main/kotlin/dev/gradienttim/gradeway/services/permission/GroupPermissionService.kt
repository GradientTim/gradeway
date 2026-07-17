/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.permission

import arrow.core.Either
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.group.GroupPermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.services.PermissionService
import net.kyori.adventure.util.TriState
import java.util.*

/**
 * Provides operations for managing permissions associated with groups.
 * Offers functionality to set, unset, clear, and query permissions for individual groups,
 * identified either by UUID or GroupEntity.
 */
interface GroupPermissionService {
    /**
     * Sets a specific permission for a group, identified by their unique identifier, to either enable or disabled.
     *
     * @param id The unique identifier of the group.
     * @param permission The name of the permission to modify.
     * @param enabled Whether the permission should be enabled (true) or disabled (false). Defaults to true.
     * @return An instance of [Either], containing either a [PermissionService.SetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setGroupPermission(
        id: UUID,
        permission: String,
        enabled: Boolean = true
    ): Either<PermissionService.SetPermissionError, Unit>

    /**
     * Sets a specific permission for a group, identified by their associated [GroupEntity], to either enable or disabled.
     *
     * @param entity The [GroupEntity] representing the group for whom the permission is being modified.
     * @param permission The name of the permission to modify.
     * @param enabled A flag indicating whether the permission should be enabled (true) or disabled (false). Defaults to true.
     * @return An [Either] containing a [PermissionService.SetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setGroupPermission(
        entity: GroupEntity,
        permission: String,
        enabled: Boolean = true
    ): Either<PermissionService.SetPermissionError, Unit>

    /**
     * Sets a specific permission for a group, identified by their unique identifier or name, to either enable or disabled.
     *
     * @param idOrName The unique identifier or name of the group.
     * @param permission The name of the permission to modify.
     * @param enabled Whether the permission should be enabled (true) or disabled (false). Defaults to true.
     * @return An instance of [Either], containing either a [PermissionService.SetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setGroupPermission(
        idOrName: String,
        permission: String,
        enabled: Boolean = true
    ): Either<PermissionService.SetPermissionError, Unit>

    /**
     * Sets multiple permissions for a group, identified by their unique identifier.
     *
     * @param id The unique identifier of the group whose permissions are being modified.
     * @param permissions A map where each key is the name of a permission and the corresponding value is a boolean
     *                    indicating whether the permission should be enabled (true) or disabled (false).
     * @return An [Either] instance containing either a [PermissionService.BulkSetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setGroupPermissions(
        id: UUID,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit>

    /**
     * Sets multiple permissions for a group, identified by their associated [GroupEntity].
     *
     * @param entity The [GroupEntity] representing the group whose permissions are being modified.
     * @param permissions A map where each key is the name of a permission and the corresponding value is a boolean
     *                    indicating whether the permission should be enabled (true) or disabled (false).
     * @return An instance of [Either], containing either a [PermissionService.BulkSetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setGroupPermissions(
        entity: GroupEntity,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit>

    /**
     * Sets multiple permissions for a group, identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the group whose permissions are being modified.
     * @param permissions A map where each key is the name of a permission and the corresponding value is a boolean
     *                    indicating whether the permission should be enabled (true) or disabled (false).
     * @return An [Either] instance containing either a [PermissionService.BulkSetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun setGroupPermissions(
        idOrName: String,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit>

    /**
     * Revokes a specific permission for a group, identified by their unique identifier.
     *
     * @param id The unique identifier of the group from whom the permission will be revoked.
     * @param permission The name of the permission to be revoked.
     * @return An [Either] containing a [PermissionService.UnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetGroupPermission(id: UUID, permission: String): Either<PermissionService.UnsetPermissionError, Unit>

    /**
     * Revokes a specific permission for a group, identified by their associated [GroupEntity].
     *
     * @param entity The [GroupEntity] representing the group whose permission is being revoked.
     * @param permission The name of the permission to be revoked.
     * @return An [Either] containing a [PermissionService.UnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetGroupPermission(
        entity: GroupEntity,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit>

    /**
     * Revokes a specific permission for a group, identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the group from whom the permission will be revoked.
     * @param permission The name of the permission to be revoked.
     * @return An [Either] containing a [PermissionService.UnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetGroupPermission(
        idOrName: String,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit>

    /**
     * Revokes multiple permissions for a group, identified by their unique identifier.
     *
     * @param id The unique identifier of the group whose permissions will be revoked.
     * @param permissions A collection of permission names to be revoked for the specified group.
     * @return An [Either] containing a [PermissionService.BulkUnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetGroupPermissions(
        id: UUID,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit>

    /**
     * Revokes multiple permissions for a group, identified by their associated [GroupEntity].
     *
     * @param entity The [GroupEntity] representing the group whose permissions will be revoked.
     * @param permissions A collection of permission names to be revoked for the specified group.
     * @return An [Either] containing a [PermissionService.BulkUnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetGroupPermissions(
        entity: GroupEntity,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit>

    /**
     * Revokes multiple permissions for a group, identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the group whose permissions will be revoked.
     * @param permissions A collection of permission names to be revoked for the specified group.
     * @return An [Either] containing a [PermissionService.BulkUnsetPermissionError] if an error occurs,
     *         or `true` if the update succeeds.
     */
    fun unsetGroupPermissions(
        idOrName: String,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit>

    /**
     * Clears all permissions associated with a specific group, identified by their unique identifier.
     *
     * @param id The unique identifier of the group whose permissions should be removed.
     * @return An [Either] containing a [PermissionService.ClearPermissionsError] if an error occurs,
     *         or `Unit` if the update succeeds.
     */
    fun clearGroupPermissions(id: UUID): Either<PermissionService.ClearPermissionsError, Unit>

    /**
     * Removes all permissions associated with the specified group.
     *
     * @param entity The [GroupEntity] representing the group whose permissions should be cleared.
     * @return An [Either] containing a [PermissionService.ClearPermissionsError] if an error occurs,
     *         or `Unit` if the update succeeds.
     */
    fun clearGroupPermissions(entity: GroupEntity): Either<PermissionService.ClearPermissionsError, Unit>

    /**
     * Clears all permissions associated with a specific group, identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the group whose permissions should be removed.
     * @return An [Either] containing a [PermissionService.ClearPermissionsError] if an error occurs,
     *         or `Unit` if the update succeeds.
     */
    fun clearGroupPermissions(idOrName: String): Either<PermissionService.ClearPermissionsError, Unit>

    /**
     * Checks if a group, identified by their unique identifier, has a specific permission.
     *
     * @param id The unique identifier of the group.
     * @param permission The name of the permission to check.
     * @return [TriState.TRUE] if the permission is enabled, [TriState.NOT_SET] if it is not configured or the group
     *         is not found, [TriState.FALSE] if the permission is explicitly disabled.
     */
    fun hasGroupPermission(id: UUID, permission: String): Boolean

    /**
     * Checks if a group, represented by their [GroupEntity], has a specific permission.
     *
     * @param entity The [GroupEntity] representing the group whose permission is being checked.
     * @param permission The name of the permission to verify.
     * @return [TriState.TRUE] if the permission is enabled, [TriState.NOT_SET] if it is not configured or disabled,
     *         [TriState.FALSE] if the group entity is not found.
     */
    fun hasGroupPermission(entity: GroupEntity, permission: String): Boolean

    /**
     * Checks if a group, identified by their unique identifier or name, has a specific permission.
     *
     * @param idOrName The unique identifier or name of the group.
     * @param permission The name of the permission to check.
     * @return [TriState.TRUE] if the permission is enabled, [TriState.NOT_SET] if it is not configured or the group
     *         is not found, [TriState.FALSE] if the permission is explicitly disabled.
     */
    fun hasGroupPermission(idOrName: String, permission: String): Boolean

    /**
     * Checks if a group, identified by their UUID, has at least one of the specified permissions.
     *
     * @param id The unique identifier (UUID) of the group whose permissions are being checked.
     * @param permissions A collection of permission strings to check against the group's permissions.
     * @return [TriState.TRUE] if at least one permission is enabled, [TriState.FALSE] if permissions are found but
     *         none enabled, [TriState.NOT_SET] if none of the permissions are configured or the group is not found.
     */
    fun hasGroupAnyPermissions(id: UUID, permissions: Collection<String>): Boolean

    /**
     * Checks if the given group entity has any of the specified permissions.
     *
     * @param entity The group entity to check for permissions.
     * @param permissions A collection of permission strings to verify against the group entity.
     * @return [TriState.TRUE] if at least one permission is enabled, [TriState.FALSE] if permissions are found but
     *         none enabled, [TriState.NOT_SET] if none of the permissions are configured.
     */
    fun hasGroupAnyPermissions(entity: GroupEntity, permissions: Collection<String>): Boolean

    /**
     * Checks if a group, identified by their unique identifier or name, has at least one of the specified permissions.
     *
     * @param idOrName The unique identifier or name of the group whose permissions are being checked.
     * @param permissions A collection of permission strings to check against the group's permissions.
     * @return [TriState.TRUE] if at least one permission is enabled, [TriState.FALSE] if permissions are found but
     *         none enabled, [TriState.NOT_SET] if none of the permissions are configured or the group is not found.
     */
    fun hasGroupAnyPermissions(idOrName: String, permissions: Collection<String>): Boolean

    /**
     * Checks if a group identified by their UUID has all the specified permissions.
     *
     * @param id The unique identifier (UUID) of the group whose permissions are being checked.
     * @param permissions The collection of permissions to verify against the group's assigned permissions.
     * @return [TriState.TRUE] if all permissions are enabled, [TriState.FALSE] if a permission is found but disabled,
     *         [TriState.NOT_SET] if any permission is not configured or the group is not found.
     */
    fun hasGroupAllPermissions(id: UUID, permissions: Collection<String>): Boolean

    /**
     * Checks if a group entity has all the specified permissions.
     *
     * @param entity The group entity to check.
     * @param permissions The collection of permissions to validate against the group's permissions.
     * @return [TriState.TRUE] if all permissions are enabled, [TriState.FALSE] if a permission is found but disabled,
     *         [TriState.NOT_SET] if any permission is not configured.
     */
    fun hasGroupAllPermissions(entity: GroupEntity, permissions: Collection<String>): Boolean

    /**
     * Checks if a group, identified by their unique identifier or name, has all the specified permissions.
     *
     * @param idOrName The unique identifier or name of the group whose permissions are being checked.
     * @param permissions The collection of permissions to verify against the group's assigned permissions.
     * @return [TriState.TRUE] if all permissions are enabled, [TriState.FALSE] if a permission is found but disabled,
     *         [TriState.NOT_SET] if any permission is not configured or the group is not found.
     */
    fun hasGroupAllPermissions(idOrName: String, permissions: Collection<String>): Boolean

    /**
     * Retrieves the permissions assigned to a group identified by their unique identifier.
     *
     * @param id The unique identifier of the group whose permissions are being fetched.
     * @return A map where the keys represent permission names and the values are booleans
     * indicating whether the permission is granted (true) or denied (false).
     */
    fun getGroupPermissions(id: UUID): Set<GroupPermissionEntity>

    /**
     * Retrieves the permission mapping for a given group.
     *
     * @param entity the group entity for which the permissions are being fetched.
     * @return a map where the keys represent permission names and the values indicate
     * whether the group has the corresponding permission (true) or not (false).
     */
    fun getGroupPermissions(entity: GroupEntity): Set<GroupPermissionEntity>

    /**
     * Retrieves the permissions assigned to a group identified by their unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the group whose permissions are being fetched.
     * @return A map where the keys represent permission names and the values are booleans
     * indicating whether the permission is granted (true) or denied (false).
     */
    fun getGroupPermissions(idOrName: String): Set<GroupPermissionEntity>

    /**
     * Resolves the full set of permissions effectively granted to a group, i.e., its own enabled
     * permissions plus permissions inherited from its assigned templates.
     *
     * @param id The unique identifier of the group.
     * @return The set of [PermissionEntity] instances effectively granted to the group.
     */
    fun getEffectiveGroupPermissions(id: UUID): Set<PermissionEntity>

    /**
     * Resolves the full set of permissions effectively granted to a group, i.e., its own enabled
     * permissions plus permissions inherited from its assigned templates.
     *
     * @param entity The group entity.
     * @return The set of [PermissionEntity] instances effectively granted to the group.
     */
    fun getEffectiveGroupPermissions(entity: GroupEntity): Set<PermissionEntity>

    /**
     * Resolves the full set of permissions effectively granted to a group, i.e., its own enabled
     * permissions plus permissions inherited from its assigned templates.
     *
     * @param idOrName The unique identifier or name of the group.
     * @return The set of [PermissionEntity] instances effectively granted to the group.
     */
    fun getEffectiveGroupPermissions(idOrName: String): Set<PermissionEntity>

    /**
     * Checks whether a group effectively has the specified permission, considering its own
     * permissions plus everything inherited from its assigned templates.
     *
     * @param id The unique identifier of the group.
     * @param permission The name of the permission to check.
     * @return `true` if the group effectively has the permission, otherwise `false`.
     */
    fun hasEffectiveGroupPermission(id: UUID, permission: String): Boolean

    /**
     * Checks whether a group effectively has the specified permission, considering its own
     * permissions plus everything inherited from its assigned templates.
     *
     * @param entity The group entity to check.
     * @param permission The name of the permission to check.
     * @return `true` if the group effectively has the permission, otherwise `false`.
     */
    fun hasEffectiveGroupPermission(entity: GroupEntity, permission: String): Boolean

    /**
     * Checks whether a group effectively has the specified permission, considering its own
     * permissions plus everything inherited from its assigned templates.
     *
     * @param idOrName The unique identifier or name of the group.
     * @param permission The name of the permission to check.
     * @return `true` if the group effectively has the permission, otherwise `false`.
     */
    fun hasEffectiveGroupPermission(idOrName: String, permission: String): Boolean
}
