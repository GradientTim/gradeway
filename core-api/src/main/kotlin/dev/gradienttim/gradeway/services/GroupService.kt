/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.group.GroupPermissionEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.role.RoleGroupEntity
import dev.gradienttim.gradeway.services.shared.SharedPermissionService
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.*

/**
 * Service class for managing group entities and their associated operations.
 *
 * This class provides functionality for creating, updating, deleting, and retrieving group entities.
 * It also includes methods to manage associations between groups and roles and to set specific
 * properties for groups, such as their names and default weights.
 */
interface GroupService : SharedPermissionService<GroupEntity, GroupPermissionEntity> {
    /**
     * Creates a new group entity with the specified name and optional custom configuration.
     *
     * @param name The name of the group to be created. It must be unique and non-empty.
     * @param builder A lambda function to configure the group entity using the provided `GroupEntity` context. The default is an empty configuration.
     * @return An `Either` instance containing a `CreateGroupError` if there was an error during creation,
     *         or the successfully created `GroupEntity` object.
     */
    fun create(name: String, builder: GroupEntity.() -> Unit = {}): Either<CreateGroupError, GroupEntity>

    /**
     * Deletes a group entity identified by the provided UUID.
     *
     * @param id The unique identifier of the group to be deleted.
     * @return An `Either` instance containing a `DeleteGroupError` if the deletion fails,
     *         or `Unit` if the operation is successful.
     */
    fun delete(id: UUID): Either<DeleteGroupError, Unit>

    /**
     * Deletes the specified group entity.
     *
     * @param entity The `GroupEntity` instance to be deleted.
     * @return An `Either` containing a `DeleteGroupError` if the deletion fails,
     *         or `Unit` if the operation is successful.
     */
    fun delete(entity: GroupEntity): Either<DeleteGroupError, Unit>

    /**
     * Deletes a group entity identified by its name or unique identifier.
     *
     * @param idOrName The name or unique identifier of the group to delete. It must be a non-empty string.
     * @return An `Either` instance containing a `DeleteGroupError` if the deletion fails,
     *         or `Unit` if the operation is successful.
     */
    fun delete(idOrName: String): Either<DeleteGroupError, Unit>

    /**
     * Updates the name of a group entity identified by the given UUID.
     *
     * @param id The unique identifier of the group whose name is to be updated.
     * @param name The new name to be assigned to the group. It must be unique and non-empty.
     * @return An `Either` instance containing a `SetNameError` if there is a failure during the operation,
     *         or `Unit` if the name is successfully updated.
     */
    fun setName(id: UUID, name: String): Either<SetNameError, Unit>

    /**
     * Updates the name of the specified group entity.
     *
     * @param entity The `GroupEntity` instance whose name is to be updated.
     * @param name The new name to be assigned to the group. It must be unique and non-empty.
     * @return An `Either` instance containing a `SetNameError` if the operation fails,
     *         or `Unit` if the name is successfully updated.
     */
    fun setName(entity: GroupEntity, name: String): Either<SetNameError, Unit>

    /**
     * Updates the name of a group entity identified by its name or unique identifier.
     *
     * @param idOrName A string representing the name or unique identifier of the group. It must be non-empty.
     * @param name The new name to be assigned to the group. It must be unique and non-empty.
     * @return An `Either` instance containing a `SetNameError` if the operation fails, or `Unit` if the name is successfully updated.
     */
    fun setName(idOrName: String, name: String): Either<SetNameError, Unit>

    /**
     * Sets the default weight for a group entity identified by the given UUID.
     *
     * @param id The unique identifier of the group for which the default weight is to be set.
     * @param defaultWeight The default weight value to be assigned to the group.
     * @return An `Either` instance containing a `SetDefaultWeightError` if an error occurs during the operation,
     *         or `Unit` if the default weight is successfully set.
     */
    fun setDefaultWeight(id: UUID, defaultWeight: Int): Either<SetDefaultWeightError, Unit>

    /**
     * Sets the default weight for a given group entity.
     *
     * @param entity The `GroupEntity` instance whose default weight is to be set.
     * @param defaultWeight The default weight value to assign to the group.
     * @return An `Either` containing a `SetDefaultWeightError` if an error occurs during the operation,
     *         or `Unit` if the default weight is successfully set.
     */
    fun setDefaultWeight(entity: GroupEntity, defaultWeight: Int): Either<SetDefaultWeightError, Unit>

    /**
     * Sets the default weight for a group entity identified by its name or unique identifier.
     *
     * @param idOrName A string representing the name or unique identifier of the group. It must be non-empty.
     * @param defaultWeight The default weight value to assign to the group entity.
     * @return An `Either` containing a `SetDefaultWeightError` if an error occurs during the operation,
     *         or `Unit` if the default weight is successfully set.
     */
    fun setDefaultWeight(idOrName: String, defaultWeight: Int): Either<SetDefaultWeightError, Unit>

    /**
     * Retrieves a group entity based on its unique identifier.
     *
     * @param id The unique identifier of the group to retrieve.
     * @return The `GroupEntity` instance associated with the specified UUID, or `null` if no group is found.
     */
    fun findById(id: UUID): GroupEntity?

    /**
     * Retrieves a group entity based on its unique identifier or name.
     *
     * @param value A string representing the unique identifier (UUID) or name of the group.
     *              It must be a non-empty value.
     * @return The `GroupEntity` associated with the given identifier or name, or `null` if no match is found.
     */
    fun findByIdOrName(value: String): GroupEntity?

    /**
     * Retrieves a list of `GroupEntity` instances based on the specified criteria.
     *
     * @param where An optional lambda function that returns a filtering condition represented as an `Op<Boolean>`.
     *              If null, no filtering will be applied.
     * @param orderBy A set of pairs consisting of an `Expression` and a `SortOrder` to define the sorting order
     *                of the resulting list. Defaults to an empty set, meaning no specific sorting is applied.
     * @param limit The maximum number of `GroupEntity` instances to retrieve. Defaults to 20.
     * @return A `SizedIterable` containing the list of retrieved `GroupEntity` instances.
     */
    fun list(
        where: (() -> Op<Boolean>)? = null,
        orderBy: Set<Pair<Expression<*>, SortOrder>> = emptySet(),
        limit: Int = 20
    ): SizedIterable<GroupEntity>

    /**
     * Associates a role with a group by their respective unique identifiers.
     *
     * @param groupId The unique identifier of the group to which the role will be added.
     * @param roleId The unique identifier of the role to be added to the group.
     * @return An `Either` instance containing an `AddTargetError` if an error occurs during the operation,
     *         or the resulting `RoleGroupEntity` if the association is successful.
     */
    fun addRoleToGroup(groupId: UUID, roleId: UUID): Either<AddTargetError, RoleGroupEntity>

    /**
     * Associates a role with a group, linking them together in the system.
     *
     * @param groupId The unique identifier of the group to which the role will be added.
     * @param role The role entity to be added to the specified group.
     * @return An `Either` containing `RoleGroupEntity` if the operation is successful,
     *         or `AddTargetError` if there is an error during the process.
     */
    fun addRoleToGroup(groupId: UUID, role: RoleEntity): Either<AddTargetError, RoleGroupEntity>

    /**
     * Associates a role with a group identified by its ID or name.
     *
     * This method assigns the specified role to a group, identified by either its unique ID or name.
     * If the operation is successful, the updated RoleGroupEntity is returned.
     * In case of an error, an instance of AddTargetError is returned.
     *
     * @param groupIdOrName The unique identifier or name of the group to which the role should be added.
     * @param roleId The unique identifier of the role to be assigned to the group.
     * @return Either an AddTargetError indicating a failure, or a RoleGroupEntity representing the updated group.
     */
    fun addRoleToGroup(groupIdOrName: String, roleId: UUID): Either<AddTargetError, RoleGroupEntity>

    /**
     * Adds a role to a specified group by its ID or name.
     *
     * @param groupIdOrName The unique identifier or name of the group to which the role will be added.
     * @param role The role entity to be added to the group.
     * @return Either an error of type AddTargetError if the operation fails, or the updated RoleGroupEntity if successful.
     */
    fun addRoleToGroup(groupIdOrName: String, role: RoleEntity): Either<AddTargetError, RoleGroupEntity>

    /**
     * Associates a role with a specified group.
     *
     * @param group The group entity to which the role will be added.
     * @param roleId The unique identifier of the role to be added to the group.
     * @return Either an error of type AddTargetError if the operation fails, or a RoleGroupEntity representing the association if successful.
     */
    fun addRoleToGroup(group: GroupEntity, roleId: UUID): Either<AddTargetError, RoleGroupEntity>

    /**
     * Associates a specified role with a specified group.
     *
     * @param group the group to which the role is being added
     * @param role the role that is being added to the group
     * @return either an error of type AddTargetError in case of failure or the resulting RoleGroupEntity if the operation succeeds
     */
    fun addRoleToGroup(group: GroupEntity, role: RoleEntity): Either<AddTargetError, RoleGroupEntity>

    /**
     * Removes a role from a specified group.
     *
     * @param groupId The unique identifier of the group from which the role will be removed.
     * @param roleId The unique identifier of the role to be removed.
     * @return Either a RemoveTargetError if the removal operation fails, or Unit if the operation is successful.
     */
    fun removeRoleFromGroup(groupId: UUID, roleId: UUID): Either<RemoveTargetError, Unit>

    /**
     * Removes a specified role from a group identified by its unique ID.
     *
     * @param groupId The unique identifier of the group from which the role will be removed.
     * @param role The role entity to be removed from the group.
     * @return An `Either` type that contains a RemoveTargetError on failure or Unit on success.
     */
    fun removeRoleFromGroup(groupId: UUID, role: RoleEntity): Either<RemoveTargetError, Unit>

    /**
     * Removes a role from a specified group.
     *
     * @param groupIdOrName The identifier or name of the group from which the role should be removed.
     * @param roleId The unique identifier of the role to be removed.
     * @return An `Either` containing a `RemoveTargetError` if the operation fails, or `Unit` if the role is successfully removed.
     */
    fun removeRoleFromGroup(groupIdOrName: String, roleId: UUID): Either<RemoveTargetError, Unit>

    /**
     * Removes a specific role from a group identified by its ID or name.
     *
     * @param groupIdOrName The unique identifier or name of the group from which the role will be removed.
     * @param role The role entity to be removed from the group.
     * @return An `Either` containing a `RemoveTargetError` if the operation fails, or `Unit` if the role is successfully removed.
     */
    fun removeRoleFromGroup(groupIdOrName: String, role: RoleEntity): Either<RemoveTargetError, Unit>

    /**
     * Removes a role with the specified roleId from the given group.
     *
     * @param group the group entity from which the role is to be removed
     * @param roleId the unique identifier of the role to be removed
     * @return either a RemoveTargetError in case of a failure or Unit on successful removal
     */
    fun removeRoleFromGroup(group: GroupEntity, roleId: UUID): Either<RemoveTargetError, Unit>

    /**
     * Removes the specified role from the given group.
     *
     * This method performs the operation of detaching a role from a group.
     * It ensures the role is no longer associated with the group and
     * returns the result of the operation.
     *
     * @param group The group entity from which the role should be removed.
     * @param role The role entity that needs to be removed from the group.
     * @return Either a RemoveTargetError if the operation fails, or Unit if it succeeds.
     */
    fun removeRoleFromGroup(group: GroupEntity, role: RoleEntity): Either<RemoveTargetError, Unit>

    sealed interface CreateGroupError {
        object InvalidName : CreateGroupError
        data class Unexpected(val throwable: Throwable) : CreateGroupError
    }

    sealed interface DeleteGroupError {
        object EntityNotFound : DeleteGroupError
        data class Unexpected(val throwable: Throwable) : DeleteGroupError
    }

    sealed interface SetNameError {
        object EntityNotFound : SetNameError
        object NameAlreadySet : SetNameError
        object InvalidName : SetNameError
        data class Unexpected(val throwable: Throwable) : SetNameError
    }

    sealed interface SetDefaultWeightError {
        object EntityNotFound : SetDefaultWeightError
        object WeightAlreadySet : SetDefaultWeightError
        data class Unexpected(val throwable: Throwable) : SetDefaultWeightError
    }

    sealed interface AddTargetError {
        object EntityNotFound : AddTargetError
        object TargetNotFound : AddTargetError
        object AlreadyInGroup : AddTargetError
        data class Unexpected(val throwable: Throwable) : AddTargetError
    }

    sealed interface RemoveTargetError {
        object EntityNotFound : RemoveTargetError
        object TargetNotFound : RemoveTargetError
        object NotInGroup : RemoveTargetError
        data class Unexpected(val throwable: Throwable) : RemoveTargetError
    }
}
