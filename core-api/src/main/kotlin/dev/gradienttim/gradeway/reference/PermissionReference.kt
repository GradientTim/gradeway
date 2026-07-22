/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.reference

import arrow.core.Either
import dev.gradienttim.gradeway.services.PermissionService
import org.jetbrains.exposed.v1.jdbc.SizedIterable

/**
 * Represents an entity or object that maintains a reference to a collection of permissions.
 *
 * This interface provides mechanisms to query and manage permissions associated with the
 * entity. Each permission represents a specific property or characteristic, enabling
 * efficient handling of permission data while preserving flexibility for further extension
 * or customization.
 *
 * @param TReference The type of the permission references managed by this interface.
 */
interface PermissionReference<TReference> {
    /**
     * Represents a collection of permissions associated with the entity. Each permission
     * defines a specific action or access level that can be granted or denied. This collection
     * can be queried or manipulated to manage the entity's permission states effectively.
     */
    val permissions: SizedIterable<TReference>

    /**
     * Sets the specified permission for the current entity.
     *
     * This method attempts to enable or disable the given permission for the entity. If the
     * operation is successful, it returns a success result. If it fails, an error describing
     * the issue is returned.
     *
     * @param permission The name of the permission to set.
     * @param enabled A flag indicating whether the permission should be enabled or disabled. Defaults to true.
     * @return An [Either] instance which:
     *         - Contains a [PermissionService.SetPermissionError] if the operation fails, detailing reasons
     *           such as the entity not being found, or the permission already being in the requested state.
     *         - Contains [Unit] upon successful update of the permission.
     */
    fun setPermission(permission: String, enabled: Boolean = true): Either<PermissionService.SetPermissionError, Unit>

    /**
     * Sets multiple permissions for the current entity.
     *
     * This method attempts to enable or disable each of the given permissions for the entity in bulk.
     * If the operation is successful, it returns a success result. If it fails, an error describing
     * the issue is returned.
     *
     * @param permissions A map where each key represents a permission name, and each value is a boolean
     *                     indicating whether the permission should be enabled (true) or disabled (false).
     * @return An [Either] instance which:
     *         - Contains a [PermissionService.BulkSetPermissionError] if the operation fails, detailing
     *           reasons such as the entity not being found, or an unexpected error.
     *         - Contains [Unit] upon successful update of the permissions.
     */
    fun setPermissions(
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit>

    /**
     * Removes the specified permission from the current entity.
     *
     * This method attempts to unset a permission identified by the given name from the entity.
     * If the operation succeeds, it returns a success result. If it fails, it provides an error
     * detailing the reason for the failure, such as the entity not being found, the permission
     * not existing, or an unexpected error.
     *
     * @param permission The name of the permission to be unset.
     * @return An [Either] instance which:
     *         - Contains a [PermissionService.UnsetPermissionError] if the operation fails, providing
     *           details about the failure.
     *         - Contains [Unit] upon successful removal of the permission.
     */
    fun unsetPermission(permission: String): Either<PermissionService.UnsetPermissionError, Unit>

    /**
     * Removes multiple permissions from the current entity.
     *
     * This method attempts to unset each of the given permissions from the entity in bulk.
     * If the operation succeeds, it returns a success result. If it fails, it provides an error
     * detailing the reason for the failure.
     *
     * @param permissions A collection of permission names to unset from the entity.
     * @return An [Either] instance which:
     *         - Contains a [PermissionService.BulkUnsetPermissionError] if the operation fails,
     *           providing details about the failure.
     *         - Contains [Unit] upon successful removal of the permissions.
     */
    fun unsetPermissions(
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit>

    /**
     * Clears all permissions associated with the current entity.
     *
     * This method removes all permissions currently set on the entity. It returns a result
     * indicating the success or failure of the operation. The operation may fail if the entity
     * associated with the permissions is not found, or due to an unexpected error.
     *
     * @return An [Either] instance which:
     *         - Contains a [PermissionService.ClearPermissionsError] if the operation fails, detailing
     *           reasons such as the entity not being found, or an unexpected error.
     *         - Contains [Unit] on successful removal of all permissions.
     */
    fun clearPermissions(): Either<PermissionService.ClearPermissionsError, Unit>

    /**
     * Checks if the current entity has the specified permission.
     *
     * This method queries the entity's permissions to determine whether the given permission
     * is currently enabled.
     *
     * @param permission The name of the permission to check for.
     * @return `true` if the entity has the specified permission enabled, otherwise `false`.
     */
    fun hasPermission(permission: String): Boolean

    /**
     * Checks if the current entity has at least one of the specified permissions.
     *
     * @param permissions A collection of permission names to check.
     * @return `true` if the entity has at least one of the specified permissions, otherwise `false`.
     */
    fun hasAnyPermissions(permissions: Collection<String>): Boolean

    /**
     * Checks if the current entity has all the specified permissions.
     *
     * @param permissions A collection of permission names to check.
     * @return `true` if the entity has all the specified permissions, otherwise `false`.
     */
    fun hasAllPermissions(permissions: Collection<String>): Boolean
}
