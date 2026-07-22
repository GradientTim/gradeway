/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.reference

import arrow.core.Either
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.services.GroupService
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.*

/**
 * Represents an entity or object that maintains a reference to a collection of groups.
 *
 * This interface provides mechanisms to query and manage groups associated with the
 * entity. Each group represents a specific collection or logical grouping, enabling
 * efficient handling of group data while supporting operations to add or remove groups.
 *
 * @param TReference The type of the group references managed by this interface.
 */
interface GroupReference<TReference> {
    /**
     * Represents a collection of groups associated with an entity.
     *
     * Each group serves as a logical grouping or categorization of entities and
     * can be queried or manipulated to manage the entity's organizational structure
     * or relationships effectively. This property supports lazy iteration and
     * efficient querying of the contained groups.
     */
    val groups: SizedIterable<TReference>

    /**
     * Adds a group to the current entity using the specified unique identifier.
     *
     * @param id The unique identifier of the group to be added.
     * @return An instance of [Either], containing either a [GroupService.AddTargetError] indicating
     *         the error that occurred during the operation, or a [TReference] representing the
     *         reference to the added group upon success.
     */
    fun addGroup(id: UUID): Either<GroupService.AddTargetError, TReference>

    /**
     * Adds a group to the current entity using the specified unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the group to be added.
     * @return An instance of [Either], containing either a [GroupService.AddTargetError] indicating
     *         the error that occurred during the operation, or a [TReference] representing the
     *         reference to the added group upon success.
     */
    fun addGroup(idOrName: String): Either<GroupService.AddTargetError, TReference>

    /**
     * Adds a group entity to the current collection of groups associated with the entity.
     *
     * This method is responsible for integrating the given [GroupEntity] into the collection
     * of groups. The operation may fail due to various reasons, such as the entity already
     * being part of the group or the group not being found.
     *
     * @param entity The [GroupEntity] instance representing the group to be added.
     * @return An [Either] containing either a [GroupService.AddTargetError] in case of a failure or a
     *         [TReference] representing the reference to the added group upon success.
     */
    fun addGroup(entity: GroupEntity): Either<GroupService.AddTargetError, TReference>

    /**
     * Removes a group associated with the specified unique identifier.
     *
     * This method attempts to remove the group identified by the given [id] from the current
     * collection of groups. The operation may fail if the group is not found, the entity
     * is not part of the group, or due to unexpected errors.
     *
     * @param id The unique identifier of the group to be removed.
     * @return An instance of [Either], containing either a [GroupService.RemoveTargetError]
     *         indicating the reason for failure, or [Unit] upon successful removal.
     */
    fun removeGroup(id: UUID): Either<GroupService.RemoveTargetError, Unit>

    /**
     * Removes a group associated with the specified unique identifier or name.
     *
     * This method attempts to remove the group identified by the given [idOrName] from the current
     * collection of groups. The operation may fail if the group is not found, the entity
     * is not part of the group, or due to unexpected errors.
     *
     * @param idOrName The unique identifier or name of the group to be removed.
     * @return An instance of [Either], containing either a [GroupService.RemoveTargetError]
     *         indicating the reason for failure, or [Unit] upon successful removal.
     */
    fun removeGroup(idOrName: String): Either<GroupService.RemoveTargetError, Unit>

    /**
     * Removes the specified group entity from the current collection of associated groups.
     *
     * This method attempts to remove the given [GroupEntity] from the internally managed collection
     * of groups. The operation may fail for several reasons, such as the group not being found
     * or the entity not being associated with the group. The result of the operation is encapsulated
     * in an [Either] type.
     *
     * @param entity The [GroupEntity] instance representing the group to be removed.
     * @return An [Either] containing a [GroupService.RemoveTargetError] if the operation fails, or
     *         [Unit] if the removal was successful.
     */
    fun removeGroup(entity: GroupEntity): Either<GroupService.RemoveTargetError, Unit>
}
