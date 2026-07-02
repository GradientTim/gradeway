/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.permission

import arrow.core.Either
import dev.gradienttim.gradeway.services.PermissionService
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.time.Instant
import java.util.UUID

/**
 * Represents a permission template entity that can be used to manage sets of permissions
 * and their assignments to specific roles, players, or all entities. This entity is extensible
 * to support advanced permission management scenarios.
 */
interface PermissionTemplateEntity {
    /**
     * Unique identifier for an entity implementing the `PermissionTemplateEntity` interface.
     *
     * This identifier is represented as an `EntityID` with a UUID as its underlying value. It is
     * used to uniquely distinguish individual permission template entities within the system.
     */
    val id: EntityID<UUID>

    /**
     * Represents the name of the permission template entity.
     *
     * This property is a human-readable identifier for the template,
     * used to distinguish and describe permission sets. It is not required
     * to be unique and can be updated as needed to reflect the purpose or
     * assigned context of the template.
     */
    val name: String

    /**
     * Determines the target entity type to which this permission template is assigned.
     *
     * This property specifies the scope of applicability for the current permission template.
     * The assignment can be one of the following options based on the `AssignedTo` enumeration:
     *
     * - `ALL`: The permission template applies to all entities.
     * - `ROLE`: The permission template applies specifically to roles.
     * - `PLAYER`: The permission template applies specifically to players.
     */
    val assignedTo: AssignedTo

    /**
     * Represents the instant when this permission template entity was created.
     *
     * This value is immutable and typically reflects the timestamp when the entity
     * was persisted to the database for the first time. It is used to track the
     * creation time of the permission template for auditing and logging purposes.
     */
    val createdAt: Instant

    /**
     * Represents the timestamp of the last update made to the permission template entity.
     *
     * This property stores the point in time when the associated `PermissionTemplateEntity`
     * was last modified. It is useful for tracking changes or maintaining data consistency
     * in the context of the entity's lifecycle.
     */
    val updatedAt: Instant

    val permissions: SizedIterable<PermissionTemplatePermissionEntity>

    /**
     * Updates the name of this permission template entity.
     *
     * @param name The new name to assign to this template.
     * @return Either an error of type SetNameTemplateError if the update fails,
     *         or a Boolean indicating whether the update was successful.
     */
    fun setName(name: String): Either<PermissionService.SetNameTemplateError, Boolean>

    /**
     * Updates the assigned-to value of this permission template entity.
     *
     * @param assignedTo The new assignment target to set for this template.
     * @return Either an error of type SetAssignedToTemplateError if the update fails,
     *         or a Boolean indicating whether the update was successful.
     */
    fun setAssignedTo(assignedTo: AssignedTo): Either<PermissionService.SetAssignedToTemplateError, Boolean>

    /**
     * Represents the assignment target for a specific permission template entity.
     *
     * The `AssignedTo` enum defines the scope or target to which a permission
     * can be applied. It includes options for assignments to all targets,
     * specific roles, or individual players.
     *
     * Enum Constants:
     * - `ALL`: Indicates the permission applies to all targets.
     * - `ROLE`: Indicates the permission is assigned to specific roles.
     * - `PLAYER`: Indicates the permission is assigned to individual players.
     *
     * Properties:
     * - `allowForRole`: Returns true if the permission is applicable to roles, otherwise false.
     * - `allowForPlayer`: Returns true if the permission is applicable to individual players, otherwise false.
     */
    enum class AssignedTo {
        ALL,
        ROLE,
        PLAYER,
        ;

        val allowForRole: Boolean
            get() = this == ALL || this == ROLE

        val allowForPlayer: Boolean
            get() = this == ALL || this == PLAYER
    }
}
