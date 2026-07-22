/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.reference

import arrow.core.Either
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.services.PermissionService
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.*

/**
 * Represents an interface for managing references to permission templates associated with an entity.
 *
 * This interface defines a structure to handle collections of permission templates, where each
 * template specifies a distinct configuration that governs access control or permissions for
 * entities. It provides efficient mechanisms for querying and iterating over these templates,
 * ensuring flexibility in managing them.
 *
 * @param TReference The type of the permission template references managed by this interface.
 */
interface PermissionTemplateReference<TReference> {
    /**
     * Represents a collection of permission templates associated with an entity.
     *
     * Each permission template defines a specific configuration or model that can
     * be used to manage permissions effectively. This property provides support
     * for lazy iteration and efficient querying of the contained templates.
     */
    val permissionTemplates: SizedIterable<TReference>

    /**
     * Links a permission template, identified by the specified unique identifier, to the current entity.
     *
     * @param id The unique identifier of the permission template to be linked.
     * @return An instance of [Either], containing either a [PermissionService.LinkTemplateError] indicating
     *         the error that occurred during the operation, or [Unit] upon successful linking.
     */
    fun linkTemplate(id: UUID): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links the specified permission template entity to the current entity.
     *
     * @param entity The [PermissionTemplateEntity] instance representing the template to be linked.
     * @return An [Either] containing either a [PermissionService.LinkTemplateError] in case of a failure,
     *         or [Unit] upon successful linking.
     */
    fun linkTemplate(entity: PermissionTemplateEntity): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Removes the link between a permission template, identified by the specified unique identifier,
     * and the current entity.
     *
     * @param id The unique identifier of the permission template to be unlinked.
     * @return An instance of [Either], containing either a [PermissionService.UnlinkTemplateError]
     *         indicating the reason for failure, or [Unit] upon successful removal.
     */
    fun unlinkTemplate(id: UUID): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between the specified permission template entity and the current entity.
     *
     * @param entity The [PermissionTemplateEntity] instance representing the template to be unlinked.
     * @return An [Either] containing a [PermissionService.UnlinkTemplateError] if the operation fails, or
     *         [Unit] if the removal was successful.
     */
    fun unlinkTemplate(entity: PermissionTemplateEntity): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Applies a permission template, identified by the specified unique identifier, to the current entity,
     * copying its permissions onto the entity immediately.
     *
     * @param id The unique identifier of the permission template to be applied.
     * @return An instance of [Either], containing either a [PermissionService.ApplyTemplateError] indicating
     *         the error that occurred during the operation, or a [Boolean] indicating whether the template
     *         was applied successfully.
     */
    fun applyTemplate(id: UUID): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies the specified permission template entity to the current entity, copying its permissions
     * onto the entity immediately.
     *
     * @param entity The [PermissionTemplateEntity] instance representing the template to be applied.
     * @return An [Either] containing either a [PermissionService.ApplyTemplateError] in case of a failure,
     *         or a [Boolean] indicating whether the template was applied successfully.
     */
    fun applyTemplate(entity: PermissionTemplateEntity): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Revokes a permission template, identified by the specified unique identifier, from the current entity,
     * undoing a previous [applyTemplate] operation.
     *
     * @param id The unique identifier of the permission template to be revoked.
     * @return An instance of [Either], containing either a [PermissionService.RevokeTemplateError] indicating
     *         the error that occurred during the operation, or a [Boolean] indicating whether the template
     *         was revoked successfully.
     */
    fun revokeTemplate(id: UUID): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes the specified permission template entity from the current entity, undoing a previous
     * [applyTemplate] operation.
     *
     * @param entity The [PermissionTemplateEntity] instance representing the template to be revoked.
     * @return An [Either] containing either a [PermissionService.RevokeTemplateError] in case of a failure,
     *         or a [Boolean] indicating whether the template was revoked successfully.
     */
    fun revokeTemplate(entity: PermissionTemplateEntity): Either<PermissionService.RevokeTemplateError, Boolean>
}
