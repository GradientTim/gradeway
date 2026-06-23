/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.services.shared.SharedAttributeService
import dev.gradienttim.gradeway.services.shared.SharedPermissionService
import java.util.*

/**
 * Service interface for managing player entities. It defines operations for creating, updating,
 * retrieving, and deleting players, along with checks for their existence.
 */
interface PlayerService : SharedAttributeService<PlayerEntity>, SharedPermissionService<PlayerEntity> {
    /**
     * Creates a new player with the specified unique identifier and name.
     *
     * @param id The unique identifier of the player to be created.
     * @param name The name of the player to be created.
     * @return An instance of [Either] containing [CreatePlayerError] if the creation fails,
     *         or [Unit] if the creation succeeds.
     */
    fun create(id: UUID, name: String): Either<CreatePlayerError, PlayerEntity>

    /**
     * Deletes a player identified by the given unique identifier.
     *
     * @param id The unique identifier of the player to be deleted.
     * @return An instance of [Either] containing [DeletePlayerError] if the deletion fails,
     *         or [Unit] if the deletion succeeds.
     */
    fun delete(id: UUID): Either<DeletePlayerError, Unit>

    /**
     * Updates the name of a player identified by the specified unique identifier.
     *
     * @param id The unique identifier of the player whose name is to be updated.
     * @param name The new name to assign to the player.
     * @return An instance of [Either] containing [SetNameError] if the update fails,
     *         or `true` if the update succeeds.
     */
    fun setName(id: UUID, name: String): Either<SetNameError, Boolean>

    /**
     * Updates the name of the specified player entity.
     *
     * @param entity The player entity whose name is to be updated.
     * @param name The new name to assign to the player entity.
     * @return An instance of [Either] containing [SetNameError] if the update fails,
     *         or `true` if the update succeeds.
     */
    fun setName(entity: PlayerEntity, name: String): Either<SetNameError, Boolean>

    /**
     * Retrieves a player entity by its unique identifier.
     *
     * @param id The unique identifier of the player to be retrieved.
     * @return The [PlayerEntity] associated with the specified identifier, or null if no such entity exists.
     */
    fun findById(id: UUID): PlayerEntity?

    /**
     * Retrieves a player entity by its name.
     *
     * @param name The name of the player to be retrieved.
     * @return The [PlayerEntity] associated with the specified name, or null if no such entity exists.
     */
    fun findByName(name: String): PlayerEntity?

    /**
     * Retrieves a player entity by its unique identifier or name.
     *
     * @param value The unique identifier or name of the player to be retrieved.
     * @return The [PlayerEntity] associated with the specified identifier or name, or null if no such entity exists.
     */
    fun findByIdOrName(value: String): PlayerEntity?

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
     * Checks whether an entity with the specified unique identifier or name exists.
     *
     * @param value The unique identifier or name of the entity to check for existence.
     * @return `true` if an entity with the specified identifier or name exists, `false` otherwise.
     */
    fun existsByIdOrName(value: String): Boolean

    sealed interface CreatePlayerError {
        object EntityAlreadyExists : CreatePlayerError
        object InvalidName : CreatePlayerError
        data class Unexpected(val throwable: Throwable) : CreatePlayerError
    }

    sealed interface DeletePlayerError {
        object EntityNotFound : DeletePlayerError
        data class Unexpected(val throwable: Throwable) : DeletePlayerError
    }

    sealed interface SetNameError {
        object EntityNotFound : SetNameError
        object InvalidName : SetNameError
        data class Unexpected(val throwable: Throwable) : SetNameError
    }
}
