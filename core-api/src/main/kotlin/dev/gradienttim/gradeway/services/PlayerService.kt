/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import dev.gradienttim.gradeway.entity.player.PlayerAttributeEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.player.PlayerPermissionEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.services.player.RolePlayerService
import dev.gradienttim.gradeway.services.shared.SharedAttributeService
import dev.gradienttim.gradeway.services.shared.SharedPermissionService
import java.util.*

/**
 * Service interface for managing player entities. It defines operations for creating, updating,
 * retrieving, and deleting players, along with checks for their existence.
 */
interface PlayerService : RolePlayerService, SharedAttributeService<PlayerEntity, PlayerAttributeEntity>,
    SharedPermissionService<PlayerEntity, PlayerPermissionEntity> {
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

    /**
     * Retrieves the primary role associated with the player identified by the given unique identifier.
     *
     * @param id The unique identifier of the player whose primary role is to be retrieved.
     * @return The [RoleEntity] representing the player's primary role, or `null` if no primary role is found.
     */
    fun getPrimaryRole(id: UUID): RoleEntity?

    /**
     * Retrieves the primary role associated with the specified player entity.
     *
     * @param entity The player entity whose primary role is to be retrieved.
     * @return The [RoleEntity] representing the player's primary role, or `null` if no primary role is found.
     */
    fun getPrimaryRole(entity: PlayerEntity): RoleEntity?

    /**
     * Removes every expired, non-paused role from each of the given players in a single batched
     * operation. Intended for periodic maintenance sweeps over a bounded set of players (e.g. all
     * currently online players) rather than a per-player loop.
     *
     * @param playerIds The unique identifiers of the players whose expired roles should be removed.
     * @return Either an error of type [RemoveExpiredRolesError] if the operation fails, or the list of
     *         `(playerId, role)` pairs describing every role that was removed.
     */
    fun removeExpiredRoles(
        playerIds: Collection<UUID>
    ): Either<RemoveExpiredRolesError, List<Pair<UUID, RoleEntity>>>

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

    sealed interface AddRoleError {
        object EntityNotFound : AddRoleError
        object TargetNotFound : AddRoleError
        object AlreadyExists : AddRoleError
        object UntilInPast : AddRoleError
        data class Unexpected(val throwable: Throwable) : AddRoleError
    }

    sealed interface RemoveRoleError {
        object EntityNotFound : RemoveRoleError
        object TargetNotFound : RemoveRoleError
        object NotExists : RemoveRoleError
        data class Unexpected(val throwable: Throwable) : RemoveRoleError
    }

    sealed interface SetRoleUntilAtError {
        object EntityNotFound : SetRoleUntilAtError
        object TargetNotFound : SetRoleUntilAtError
        object RelationNotFound : SetRoleUntilAtError
        object UntilInPast : SetRoleUntilAtError
        data class Unexpected(val throwable: Throwable) : SetRoleUntilAtError
    }

    sealed interface SetRolePausedAtError {
        object EntityNotFound : SetRolePausedAtError
        object TargetNotFound : SetRolePausedAtError
        object RelationNotFound : SetRolePausedAtError
        object PauseInPast : SetRolePausedAtError
        data class Unexpected(val throwable: Throwable) : SetRolePausedAtError
    }

    sealed interface PauseRoleError {
        object EntityNotFound : PauseRoleError
        object TargetNotFound : PauseRoleError
        object RelationNotFound : PauseRoleError
        object AlreadyPaused : PauseRoleError
        data class Unexpected(val throwable: Throwable) : PauseRoleError
    }

    sealed interface ResumeRoleError {
        object EntityNotFound : ResumeRoleError
        object TargetNotFound : ResumeRoleError
        object RelationNotFound : ResumeRoleError
        object NotPaused : ResumeRoleError
        data class Unexpected(val throwable: Throwable) : ResumeRoleError
    }

    sealed interface SetPrimaryRoleError {
        object EntityNotFound : SetPrimaryRoleError
        object TargetNotFound : SetPrimaryRoleError
        object AlreadyPrimary : SetPrimaryRoleError
        data class Unexpected(val throwable: Throwable) : SetPrimaryRoleError
    }

    sealed interface RemoveExpiredRolesError {
        object EntityNotFound : RemoveExpiredRolesError
        data class Unexpected(val throwable: Throwable) : RemoveExpiredRolesError
    }
}
