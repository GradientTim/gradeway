/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.player

import arrow.core.Either
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.player.PlayerRoleEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.services.PlayerService
import java.time.Instant
import java.util.*

/**
 * Service interface for managing roles assigned to players.
 * Provides methods to add, remove, pause, resume, and update roles with specific conditions or time constraints.
 */
interface RolePlayerService {
    /**
     * Assigns a role to a player with an optional expiration date.
     *
     * @param playerId The unique identifier of the player to whom the role will be assigned.
     * @param roleId The unique identifier of the role being assigned.
     * @param until The expiration date and time of the role, or null if it does not expire.
     * @return Either an error of type [PlayerService.AddRoleError] if the operation fails,
     *         or an instance of [PlayerRoleEntity] representing the assigned role if the operation succeeds.
     */
    fun addRole(
        playerId: UUID,
        roleId: UUID,
        until: Instant? = null
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity>

    /**
     * Assigns a specified role to a player with an optional expiration date.
     *
     * @param playerId The unique identifier of the player to whom the role will be assigned.
     * @param role The role entity that will be assigned to the player.
     * @param until The expiration date and time of the role, or null if the role does not expire.
     * @return Either an error of type [PlayerService.AddRoleError] if the operation fails, or
     *         a [PlayerRoleEntity] instance representing the assigned role if the operation succeeds.
     */
    fun addRole(
        playerId: UUID,
        role: RoleEntity,
        until: Instant? = null
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity>

    /**
     * Assigns a specified role to a player in the system with an optional expiration date.
     *
     * @param player The player entity to whom the role is being assigned.
     * @param roleId The unique identifier of the role being assigned.
     * @param until The expiration date and time of the role, or null if the role does not expire.
     * @return Either an error of type [PlayerService.AddRoleError] if the operation fails, or
     *         a [PlayerRoleEntity] instance representing the assigned role if the operation succeeds.
     */
    fun addRole(
        player: PlayerEntity,
        roleId: UUID,
        until: Instant? = null
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity>

    /**
     * Assigns a role to a player with an optional expiration date.
     *
     * @param player The player entity to whom the role will be assigned.
     * @param role The role entity that will be assigned to the player.
     * @param until The expiration date and time of the role, or null if the role does not expire.
     * @return Either an error of type [PlayerService.AddRoleError] if the operation fails, or
     *         a [PlayerRoleEntity] representing the assigned role if the operation succeeds.
     */
    fun addRole(
        player: PlayerEntity,
        role: RoleEntity,
        until: Instant? = null
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity>

    /**
     * Adds a role to a player identified by their ID or name, with an optional expiration time.
     *
     * @param playerIdOrName The ID or name of the player to which the role will be assigned.
     * @param roleId The unique identifier of the role to be assigned to the player.
     * @param untilAt The expiration timestamp for the assigned role.
     * @return Either an error indicating why the role could not be added, or the created PlayerRoleEntity representing the role assignment.
     */
    fun addRole(
        playerIdOrName: String,
        roleId: UUID,
        untilAt: Instant?
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity>

    /**
     * Adds a role to a player either by their unique identifier or by their name.
     * The role will be assigned until the specified expiration time.
     *
     * @param playerIdOrName The unique identifier or name of the player to whom the role will be assigned.
     * @param role The role entity to be assigned to the player.
     * @param untilAt The instant specifying the expiration time of the assigned role.
     * @return Either an error indicating the failure reason or the assigned player role entity.
     */
    fun addRole(
        playerIdOrName: String,
        role: RoleEntity,
        untilAt: Instant?
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity>

    /**
     * Removes a role from a player in the system.
     *
     * @param playerId The unique identifier of the player from whom the role will be removed.
     * @param roleId The unique identifier of the role to be removed from the player.
     * @return Either an error of type [PlayerService.RemoveRoleError] if the operation fails,
     *         or [Unit] if the operation succeeds.
     */
    fun removeRole(playerId: UUID, roleId: UUID): Either<PlayerService.RemoveRoleError, Unit>

    /**
     * Removes a specific role from a player in the system.
     *
     * @param playerId The unique identifier of the player from whom the role will be removed.
     * @param role The role entity that will be removed from the player.
     * @return Either an error of type [PlayerService.RemoveRoleError] if the operation fails,
     *         or [Unit] if the operation succeeds.
     */
    fun removeRole(playerId: UUID, role: RoleEntity): Either<PlayerService.RemoveRoleError, Unit>

    /**
     * Removes a specified role from a player in the system.
     *
     * @param player The player entity from whom the role will be removed.
     * @param roleId The unique identifier of the role to be removed from the player.
     * @return Either an error of type [PlayerService.RemoveRoleError] if the operation fails,
     *         or [Unit] if the operation succeeds.
     */
    fun removeRole(player: PlayerEntity, roleId: UUID): Either<PlayerService.RemoveRoleError, Unit>

    /**
     * Removes a specific role from a player in the system.
     *
     * @param player The player entity from whom the role will be removed.
     * @param role The role entity that will be removed from the player.
     * @return Either an error of type [PlayerService.RemoveRoleError] if the operation fails,
     *         or [Unit] if the operation succeeds.
     */
    fun removeRole(player: PlayerEntity, role: RoleEntity): Either<PlayerService.RemoveRoleError, Unit>

    /**
     * Removes a specified role from a player identified by their ID or name.
     *
     * @param playerIdOrName The ID or name of the player from whom the role will be removed.
     * @param roleId The UUID of the role to be removed from the player.
     * @return Either an error of type [PlayerService.RemoveRoleError] if the operation fails,
     *         or [Unit] if the role is successfully removed.
     */
    fun removeRole(playerIdOrName: String, roleId: UUID): Either<PlayerService.RemoveRoleError, Unit>

    /**
     * Removes a specific role from a player identified by their ID or name.
     *
     * @param playerIdOrName The unique identifier or name of the player from whom the role is to be removed.
     * @param role The role entity that should be removed from the player.
     * @return Either an error indicating why the role removal failed, or Unit if the operation is successful.
     */
    fun removeRole(playerIdOrName: String, role: RoleEntity): Either<PlayerService.RemoveRoleError, Unit>

    /**
     * Sets the expiration date and time for a specific role assigned to a player.
     *
     * @param playerId The unique identifier of the player whose role is being updated.
     * @param roleId The unique identifier of the role to be updated.
     * @param untilAt The instant specifying when the role will expire.
     * @return An [Either] instance containing [PlayerService.SetRoleUntilAtError] if an error occurs during the operation,
     *         or [Unit] if the operation completes successfully.
     */
    fun setRoleUntilAt(playerId: UUID, roleId: UUID, untilAt: Instant): Either<PlayerService.SetRoleUntilAtError, Unit>

    /**
     * Updates the expiration date for a specific role assigned to a player.
     *
     * @param playerId The unique identifier of the player whose role expiration date is being updated.
     * @param role The role entity whose expiration date is being set.
     * @param untilAt The timestamp specifying the new expiration date and time for the role.
     * @return An [Either] containing [PlayerService.SetRoleUntilAtError] if an error occurs during the operation,
     *         or [Unit] if the operation is successful.
     */
    fun setRoleUntilAt(
        playerId: UUID,
        role: RoleEntity,
        untilAt: Instant
    ): Either<PlayerService.SetRoleUntilAtError, Unit>

    /**
     * Updates the expiration date and time for a specific role assigned to a player.
     *
     * @param player The player entity to whom the role is assigned.
     * @param roleId The unique identifier of the role whose expiration date is being updated.
     * @param untilAt The timestamp specifying the new expiration date and time for the role.
     * @return An [Either] containing [PlayerService.SetRoleUntilAtError] if an error occurs during the operation,
     *         or [Unit] if the operation completes successfully.
     */
    fun setRoleUntilAt(
        player: PlayerEntity,
        roleId: UUID,
        untilAt: Instant
    ): Either<PlayerService.SetRoleUntilAtError, Unit>

    /**
     * Assigns a specific role to a player until a given point in time.
     *
     * @param player The player entity to whom the role will be assigned.
     * @param role The role entity that will be assigned to the player.
     * @param untilAt The time until which the role will remain assigned to the player.
     * @return Either an error indicating why the role could not be assigned, or Unit if the operation was successful.
     */
    fun setRoleUntilAt(
        player: PlayerEntity,
        role: RoleEntity,
        untilAt: Instant
    ): Either<PlayerService.SetRoleUntilAtError, Unit>

    /**
     * Assigns a specific role to a player until a specified expiration time.
     *
     * @param playerIdOrName The unique identifier or name of the player to whom the role will be assigned.
     * @param roleId The unique identifier of the role to be assigned.
     * @param untilAt The timestamp indicating when the role assignment will expire.
     * @return Either an error indicating the failure reason, or Unit if the role assignment was successful.
     */
    fun setRoleUntilAt(
        playerIdOrName: String,
        roleId: UUID,
        untilAt: Instant
    ): Either<PlayerService.SetRoleUntilAtError, Unit>

    /**
     * Assigns a specific role to a player until a defined expiration time.
     *
     * @param playerIdOrName The unique identifier or name of the player to whom the role is to be assigned.
     * @param role The role to be assigned to the player.
     * @param untilAt The instant until which the role will remain active.
     * @return Either an error object of type PlayerService.SetRoleUntilAtError if the operation fails,
     *         or Unit if the operation is successful.
     */
    fun setRoleUntilAt(
        playerIdOrName: String,
        role: RoleEntity,
        untilAt: Instant
    ): Either<PlayerService.SetRoleUntilAtError, Unit>

    /**
     * Sets the paused state for a role associated with a specific player at a given timestamp.
     *
     * @param playerId The unique identifier of the player whose role is being updated.
     * @param roleId The unique identifier of the role to be paused.
     * @param pausedAt The timestamp indicating when the role was paused.
     * @return An Either type containing a [PlayerService.SetRolePausedAtError] on failure or Unit on success.
     */
    fun setRolePausedAt(
        playerId: UUID,
        roleId: UUID,
        pausedAt: Instant
    ): Either<PlayerService.SetRolePausedAtError, Unit>

    /**
     * Sets the paused timestamp of a role for a specific player.
     *
     * @param playerId The unique identifier of the player whose role is being updated.
     * @param role The role entity associated with the player.
     * @param pausedAt The instant at which the role is paused.
     * @return Either an error of type PlayerService.SetRolePausedAtError if the operation fails,
     *         or Unit if the operation succeeds.
     */
    fun setRolePausedAt(
        playerId: UUID,
        role: RoleEntity,
        pausedAt: Instant
    ): Either<PlayerService.SetRolePausedAtError, Unit>

    /**
     * Updates the paused timestamp for a specific role assigned to a player.
     *
     * @param player The player entity whose role's paused timestamp is being updated.
     * @param roleId The unique identifier of the role to update.
     * @param pausedAt The timestamp to set as the role's paused moment.
     * @return Either an error of type `PlayerService.SetRolePausedAtError` if the operation fails, or `Unit` if successful.
     */
    fun setRolePausedAt(
        player: PlayerEntity,
        roleId: UUID,
        pausedAt: Instant
    ): Either<PlayerService.SetRolePausedAtError, Unit>

    /**
     * Sets the paused timestamp for a specific role assigned to a player.
     *
     * @param player The player entity whose role pause time is being updated.
     * @param role The role entity that is being paused.
     * @param pausedAt The timestamp indicating when the role was paused.
     * @return Either an error of type PlayerService.SetRolePausedAtError if the operation fails, or Unit if successful.
     */
    fun setRolePausedAt(
        player: PlayerEntity,
        role: RoleEntity,
        pausedAt: Instant
    ): Either<PlayerService.SetRolePausedAtError, Unit>

    /**
     * Sets the paused timestamp for a specific role of a player.
     *
     * @param playerIdOrName The unique identifier or the name of the player whose role's paused state is to be set.
     * @param roleId The unique identifier of the role to be paused.
     * @param pausedAt The timestamp at which the role is paused.
     * @return Either an error of type [PlayerService.SetRolePausedAtError] if the operation fails, or [Unit] if successful.
     */
    fun setRolePausedAt(
        playerIdOrName: String,
        roleId: UUID,
        pausedAt: Instant
    ): Either<PlayerService.SetRolePausedAtError, Unit>

    /**
     * Updates the paused time for a specific role associated with a player.
     *
     * @param playerIdOrName The ID or name of the player whose role's paused time is being updated.
     * @param role The specific role entity associated with the player.
     * @param pausedAt The timestamp indicating when the role was paused.
     * @return Either an error of type PlayerService.SetRolePausedAtError if the operation fails, or Unit if successful.
     */
    fun setRolePausedAt(
        playerIdOrName: String,
        role: RoleEntity,
        pausedAt: Instant
    ): Either<PlayerService.SetRolePausedAtError, Unit>

    /**
     * Pauses the specified role for the given player.
     *
     * @param playerId The unique identifier of the player whose role is to be paused.
     * @param roleId The unique identifier of the role that needs to be paused.
     * @return Either a [PlayerService.PauseRoleError] if an error occurs during the operation,
     *         or [Unit] if the role is successfully paused.
     */
    fun pauseRole(playerId: UUID, roleId: UUID): Either<PlayerService.PauseRoleError, Unit>

    /**
     * Pauses the specified role for a player.
     *
     * @param playerId The unique identifier of the player whose role is to be paused.
     * @param role The role entity that needs to be paused for the specified player.
     * @return Either a PauseRoleError if pausing the role fails or Unit if the operation is successful.
     */
    fun pauseRole(playerId: UUID, role: RoleEntity): Either<PlayerService.PauseRoleError, Unit>

    /**
     * Pauses the role associated with the given role ID for the specified player.
     *
     * @param player The player entity for whom the role needs to be paused.
     * @param roleId The unique identifier of the role to be paused.
     * @return Either an error of type PlayerService.PauseRoleError if the operation fails, or Unit if the operation succeeds.
     */
    fun pauseRole(player: PlayerEntity, roleId: UUID): Either<PlayerService.PauseRoleError, Unit>

    /**
     * Pauses the specified role for the given player.
     *
     * @param player The player entity for whom the role is to be paused.
     * @param role The role entity to be paused for the player.
     * @return Either a PauseRoleError if the operation fails, or Unit if it succeeds.
     */
    fun pauseRole(player: PlayerEntity, role: RoleEntity): Either<PlayerService.PauseRoleError, Unit>

    /**
     * Pauses the specified role for a given player.
     *
     * @param playerIdOrName The ID or name of the player whose role is to be paused.
     * @param roleId The unique identifier of the role to be paused.
     * @return Either an error of type PlayerService.PauseRoleError if the operation fails, or Unit if the role is successfully paused.
     */
    fun pauseRole(playerIdOrName: String, roleId: UUID): Either<PlayerService.PauseRoleError, Unit>

    /**
     * Pauses the specified role for a player identified by their ID or name.
     *
     * @param playerIdOrName The unique identifier or name of the player whose role is to be paused.
     * @param role The role entity to be paused for the specified player.
     * @return Either a PauseRoleError if the operation fails or Unit if the role is successfully paused.
     */
    fun pauseRole(playerIdOrName: String, role: RoleEntity): Either<PlayerService.PauseRoleError, Unit>

    /**
     * Resumes a previously assigned role for the specified player.
     *
     * @param playerId The unique identifier of the player.
     * @param roleId The unique identifier of the role to resume.
     * @return Either an error of type PlayerService.ResumeRoleError if the operation fails,
     *         or Unit if the role is successfully resumed.
     */
    fun resumeRole(playerId: UUID, roleId: UUID): Either<PlayerService.ResumeRoleError, Unit>

    /**
     * Resumes the specified role for the given player.
     *
     * @param playerId The unique identifier of the player whose role is to be resumed.
     * @param role The role entity that needs to be resumed for the player.
     * @return Either an error of type [PlayerService.ResumeRoleError] if the operation fails,
     *         or [Unit] if the operation succeeds.
     */
    fun resumeRole(playerId: UUID, role: RoleEntity): Either<PlayerService.ResumeRoleError, Unit>

    /**
     * Resumes the specified role for the given player.
     *
     * @param player The player entity for whom the role will be resumed.
     * @param roleId The unique identifier of the role to be resumed.
     * @return Either a resume role error or a Unit indicating success.
     */
    fun resumeRole(player: PlayerEntity, roleId: UUID): Either<PlayerService.ResumeRoleError, Unit>

    /**
     * Resumes the specified role for the given player.
     *
     * @param player The player for whom the role is to be resumed.
     * @param role The role that should be resumed for the player.
     * @return Either an error of type PlayerService.ResumeRoleError, indicating the reason the role
     *         could not be resumed, or a Unit instance if the operation was successful.
     */
    fun resumeRole(player: PlayerEntity, role: RoleEntity): Either<PlayerService.ResumeRoleError, Unit>

    /**
     * Resumes a role for a specified player.
     *
     * @param playerIdOrName The identifier or name of the player whose role is to be resumed.
     * @param roleId The unique identifier of the role to be resumed for the player.
     * @return Either a [PlayerService.ResumeRoleError] if an error occurs during the operation, or [Unit] if the operation succeeds.
     */
    fun resumeRole(playerIdOrName: String, roleId: UUID): Either<PlayerService.ResumeRoleError, Unit>

    /**
     * Resumes the specified role for a player identified by their ID or name.
     *
     * @param playerIdOrName The unique identifier or name of the player.
     * @param role The role entity to be resumed for the player.
     * @return Either an error of type PlayerService.ResumeRoleError if the action fails,
     *         or Unit if the role is successfully resumed.
     */
    fun resumeRole(playerIdOrName: String, role: RoleEntity): Either<PlayerService.ResumeRoleError, Unit>

    /**
     * Sets the primary role of a player identified by the provided player ID.
     *
     * @param playerId the unique identifier of the player whose primary role is being updated
     * @param roleId the unique identifier of the role to set as the primary role for the player
     * @return either containing a SetPrimaryRoleError if the operation fails, or Unit if the operation is successful
     */
    fun setPrimaryRole(playerId: UUID, roleId: UUID): Either<PlayerService.SetPrimaryRoleError, Unit>

    /**
     * Sets the primary role for a player indicated by their unique identifier.
     *
     * @param playerId The unique identifier of the player whose primary role is to be set.
     * @param role The role to be assigned as the primary role for the specified player.
     * @return An instance of Either that contains a SetPrimaryRoleError on failure or Unit on success.
     */
    fun setPrimaryRole(playerId: UUID, role: RoleEntity): Either<PlayerService.SetPrimaryRoleError, Unit>

    /**
     * Sets the primary role for a specified player.
     *
     * @param player The player entity whose primary role is being updated.
     * @param roleId The unique identifier of the role to set as primary.
     * @return Either an error of type PlayerService.SetPrimaryRoleError if the operation fails, or Unit if successful.
     */
    fun setPrimaryRole(player: PlayerEntity, roleId: UUID): Either<PlayerService.SetPrimaryRoleError, Unit>

    /**
     * Sets the primary role for a given player.
     *
     * @param player The player entity whose primary role is to be set.
     * @param role The role entity to be assigned as the primary role for the player.
     * @return Either an error of type PlayerService.SetPrimaryRoleError if the operation fails,
     *         or Unit if the operation is successful.
     */
    fun setPrimaryRole(player: PlayerEntity, role: RoleEntity): Either<PlayerService.SetPrimaryRoleError, Unit>

    /**
     * Assigns a primary role to a player identified by either their ID or name.
     *
     * @param playerIdOrName A string representing the player's unique ID or name.
     * @param roleId The UUID of the role to be set as primary for the player.
     * @return Either an error of type PlayerService.SetPrimaryRoleError if the operation fails or Unit if successful.
     */
    fun setPrimaryRole(playerIdOrName: String, roleId: UUID): Either<PlayerService.SetPrimaryRoleError, Unit>

    /**
     * Assigns a primary role to a player identified by their ID or name.
     *
     * @param playerIdOrName The unique identifier or name of the player for whom the primary role is being set.
     * @param role The role to be assigned as the primary role for the player.
     * @return Either an error of type PlayerService.SetPrimaryRoleError if the operation fails,
     *         or Unit if the primary role is successfully set.
     */
    fun setPrimaryRole(playerIdOrName: String, role: RoleEntity): Either<PlayerService.SetPrimaryRoleError, Unit>

    /**
     * Removes every role assigned to the player identified by the given unique identifier that has
     * expired (its `untilAt` has passed) and is not currently paused.
     *
     * @param playerId The unique identifier of the player whose expired roles should be removed.
     * @return Either an error of type [PlayerService.RemoveExpiredRolesError] if the operation fails,
     *         or the list of [RoleEntity] instances that were removed.
     */
    fun removeExpiredRoles(playerId: UUID): Either<PlayerService.RemoveExpiredRolesError, List<RoleEntity>>

    /**
     * Removes every role assigned to the specified player that has expired (its `untilAt` has passed)
     * and is not currently paused.
     *
     * @param player The player entity whose expired roles should be removed.
     * @return Either an error of type [PlayerService.RemoveExpiredRolesError] if the operation fails,
     *         or the list of [RoleEntity] instances that were removed.
     */
    fun removeExpiredRoles(player: PlayerEntity): Either<PlayerService.RemoveExpiredRolesError, List<RoleEntity>>

    /**
     * Removes every role assigned to the player identified by their ID or name that has expired
     * (its `untilAt` has passed) and is not currently paused.
     *
     * @param playerIdOrName The unique identifier or name of the player whose expired roles should be removed.
     * @return Either an error of type [PlayerService.RemoveExpiredRolesError] if the operation fails,
     *         or the list of [RoleEntity] instances that were removed.
     */
    fun removeExpiredRoles(
        playerIdOrName: String
    ): Either<PlayerService.RemoveExpiredRolesError, List<RoleEntity>>
}
