/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.services.shared.SharedAttributeService
import dev.gradienttim.gradeway.services.shared.SharedPermissionService
import java.util.*

/**
 * Service interface for managing role entities. It defines operations for creating, updating,
 * retrieving, and deleting roles, along with checks for their existence.
 */
interface RoleService : SharedAttributeService<RoleEntity>, SharedPermissionService<RoleEntity> {
    /**
     * Creates a new role with the specified name.
     *
     * @param name The name of the role to be created.
     * @return An instance of [Either] containing [CreateRoleError] if the creation fails,
     *         or [Unit] if the creation succeeds.
     */
    fun create(name: String): Either<CreateRoleError, RoleEntity>

    /**
     * Deletes a role identified by the given unique identifier.
     *
     * @param id The unique identifier of the role to be deleted.
     * @return An instance of [Either] containing [DeleteRoleError] if the deletion fails,
     *         or [Unit] if the deletion is successful.
     */
    fun delete(id: UUID): Either<DeleteRoleError, Unit>

    /**
     * Updates the weight of a role identified by the specified unique identifier.
     *
     * @param id The unique identifier of the role whose weight is to be updated.
     * @param weight The new weight to assign to the role.
     * @return An instance of [Either] containing [SetWeightError] if the update fails,
     *         or `true` if the update succeeds.
     */
    fun setWeight(id: UUID, weight: Int): Either<SetWeightError, Boolean>

    /**
     * Updates the weight of the specified role entity.
     *
     * @param entity The role entity whose weight is to be updated.
     * @param weight The new weight to assign to the role entity.
     * @return An instance of [Either] containing [SetWeightError] if the update fails,
     *         or `true` if the update succeeds.
     */
    fun setWeight(entity: RoleEntity, weight: Int): Either<SetWeightError, Boolean>

    /**
     * Updates the weight of a role identified by its unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the role whose weight needs to be updated.
     * @param weight The new weight to assign to the role entity.
     * @return An instance of [Either] containing [SetWeightError] if the update fails,
     *         or `true` if the update succeeds.
     */
    fun setWeight(idOrName: String, weight: Int): Either<SetWeightError, Boolean>

    /**
     * Updates the name of a role identified by the specified unique identifier.
     *
     * @param id The unique identifier of the role whose name is to be updated.
     * @param name The new name to assign to the role.
     * @return An instance of [Either] containing [SetNameError] if the update fails,
     *         or `true` if the update succeeds.
     */
    fun setName(id: UUID, name: String): Either<SetNameError, Boolean>

    /**
     * Updates the name of the specified role entity.
     *
     * @param entity The role entity whose name is to be updated.
     * @param name The new name to assign to the role entity.
     * @return An instance of [Either] containing [SetNameError] if the update fails,
     *         or `true` if the update succeeds.
     */
    fun setName(entity: RoleEntity, name: String): Either<SetNameError, Boolean>

    /**
     * Retrieves a role entity by its unique identifier.
     *
     * @param id The unique identifier of the role to be retrieved.
     * @return The [RoleEntity] associated with the specified identifier, or null if no such entity exists.
     */
    fun findById(id: UUID): RoleEntity?

    /**
     * Retrieves a role entity by its name.
     *
     * @param name The name of the role to be retrieved.
     * @return The [RoleEntity] associated with the specified name, or null if no such entity exists.
     */
    fun findByName(name: String): RoleEntity?

    /**
     * Retrieves a role entity by its identifier or name.
     *
     * @param value The identifier or name of the role to be retrieved.
     * @return The [RoleEntity] associated with the specified identifier or name, or null if no such entity exists.
     */
    fun findByIdOrName(value: String): RoleEntity?

    /**
     * Checks whether an entity with the specified unique identifier exists.
     *
     * @param id The unique identifier of the entity to check for existence.
     * @return `true` if an entity with the specified identifier exists, `false` otherwise.
     */
    fun existsById(id: UUID): Boolean

    /**
     * Checks whether an entity with the specified name exists.
     *
     * @param name The name of the entity to check for existence.
     * @return `true` if an entity with the specified name exists, `false` otherwise.
     */
    fun existsByName(name: String): Boolean

    /**
     * Checks whether a role entity with the specified identifier or name exists.
     *
     * @param value The identifier or name of the role entity to check for existence.
     * @return `true` if a role entity with the specified identifier or name exists, `false` otherwise.
     */
    fun existsByIdOrName(value: String): Boolean

    sealed interface CreateRoleError {
        object EntityAlreadyExists : CreateRoleError
        object InvalidName : CreateRoleError
        data class Unexpected(val throwable: Throwable) : CreateRoleError
    }

    sealed interface DeleteRoleError {
        object EntityNotFound : DeleteRoleError
        data class Unexpected(val throwable: Throwable) : DeleteRoleError
    }

    sealed interface SetNameError {
        object EntityNotFound : SetNameError
        object InvalidName : SetNameError
        data class Unexpected(val throwable: Throwable) : SetNameError
    }

    sealed interface SetWeightError {
        object EntityNotFound : SetWeightError
        data class Unexpected(val throwable: Throwable) : SetWeightError
    }
}
