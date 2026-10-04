/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.track

import arrow.core.Either
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.entity.track.TrackStageEntity
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.*

interface PlayerTrackService {
    /**
     * Promotes a player to the next stage within a specified track.
     *
     * This method attempts to move a player to the next stage of their progression on a
     * given track. If the progression is successful, it returns a success result. Otherwise,
     * it returns an error encapsulated in the result, indicating the reason for the failure.
     *
     * @param playerId The unique identifier of the player to be promoted.
     * @param trackId The unique identifier of the track on which the player's progression occurs.
     * @return An `Either` containing a `PromotePlayerError` if the promotion fails, or `Unit` if the promotion is successful.
     */
    fun promotePlayer(playerId: UUID, trackId: UUID): Either<PromotePlayerError, Unit>

    /**
     * Promotes a player to the next stage within a specified track.
     *
     * This method attempts to progress a player to the next stage on the given track.
     * If the promotion is successful, the result contains `Unit`. If the promotion fails,
     * an error of type `PromotePlayerError` is returned, indicating the reason for the failure.
     *
     * @param playerId The unique identifier of the player to be promoted.
     * @param track The track entity on which the player's progression is managed.
     * @return An `Either` containing a `PromotePlayerError` if the promotion fails, or `Unit` if successful.
     */
    fun promotePlayer(playerId: UUID, track: TrackEntity): Either<PromotePlayerError, Unit>

    /**
     * Promotes a player to the next stage within a specified track.
     *
     * This method attempts to progress a player to the next stage on the given track.
     * If the promotion is successful, it returns a success result encapsulated as `Unit`.
     * If the promotion fails, it returns a `PromotePlayerError` indicating the reason.
     *
     * @param player The player entity to be promoted.
     * @param trackId The unique identifier of the track on which the player's progression occurs.
     * @return An `Either` containing a `PromotePlayerError` if the promotion fails, or `Unit` if the promotion is successful.
     */
    fun promotePlayer(player: PlayerEntity, trackId: UUID): Either<PromotePlayerError, Unit>

    /**
     * Promotes a player to the next stage within a specified track.
     *
     * This method attempts to progress a player to the next stage on the given track.
     * If the promotion is successful, it returns a success result encapsulated as `Unit`.
     * If the promotion fails, it returns a `PromotePlayerError` indicating the reason.
     *
     * @param player The player entity to be promoted.
     * @param track The track entity on which the player's progression is managed.
     * @return An `Either` containing a `PromotePlayerError` if the promotion fails, or `Unit` if the promotion is successful.
     */
    fun promotePlayer(player: PlayerEntity, track: TrackEntity): Either<PromotePlayerError, Unit>

    /**
     * Demotes a player to the previous stage within a specified track.
     *
     * This method attempts to move a player to the previous stage of their progression on a
     * given track. If the demotion is successful, it returns a success result. Otherwise,
     * it returns an error encapsulated in the result, indicating the reason for the failure.
     *
     * @param playerId The unique identifier of the player to be demoted.
     * @param trackId The unique identifier of the track on which the player's regression occurs.
     * @return An `Either` containing a `DemotePlayerError` if the demotion fails, or `Unit` if the demotion is successful.
     */
    fun demotePlayer(playerId: UUID, trackId: UUID): Either<DemotePlayerError, Unit>

    /**
     * Demotes a player to the previous stage within a specified track.
     *
     * This method attempts to move a player to the previous stage of their progression
     * on a given track. If the demotion is successful, it returns a success result.
     * Otherwise, it returns an error encapsulated in the result, indicating the reason
     * for the failure.
     *
     * @param playerId The unique identifier of the player to be demoted.
     * @param track The track entity on which the player's regression occurs.
     * @return An `Either` containing a `DemotePlayerError` if the demotion fails, or `Unit` if the demotion is successful.
     */
    fun demotePlayer(playerId: UUID, track: TrackEntity): Either<DemotePlayerError, Unit>

    /**
     * Demotes a player to the previous stage within a specified track.
     *
     * This method attempts to move a player to the previous stage of their progression
     * on a given track. If the demotion is successful, it returns `Unit` inside a success result.
     * Otherwise, it returns a `DemotePlayerError` inside a failure result, indicating the reason for failure.
     *
     * @param player The player entity to be demoted.
     * @param trackId The unique identifier of the track on which the player's regression occurs.
     * @return An `Either` containing a `DemotePlayerError` if the demotion fails, or `Unit` if the demotion is successful.
     */
    fun demotePlayer(player: PlayerEntity, trackId: UUID): Either<DemotePlayerError, Unit>

    /**
     * Demotes a player to the previous stage within a specified track.
     *
     * This method attempts to move a player to the previous stage of their progression
     * on the given track. If the demotion is successful, it returns a success result
     * encapsulated as `Unit`. If the demotion fails, an error of type `DemotePlayerError`
     * is returned, indicating the reason for failure.
     *
     * @param player The player entity to be demoted.
     * @param track The track entity on which the player's regression occurs.
     * @return An `Either` containing a `DemotePlayerError` if the demotion fails,
     * or `Unit` if the demotion is successful.
     */
    fun demotePlayer(player: PlayerEntity, track: TrackEntity): Either<DemotePlayerError, Unit>

    /**
     * Retrieves the current stage of a player within a specified track.
     *
     * This method fetches the player's current progression stage for the given track. If the player
     * does not have a current stage in the specified track, the method returns null.
     *
     * @param playerId The unique identifier of the player whose current stage is being retrieved.
     * @param trackId The unique identifier of the track for which the player's current stage is being determined.
     * @return The current stage of the player within the specified track as a `TrackStageEntity`, or `null`
     *         if the player has no stage in the track.
     */
    fun findPlayersCurrentStage(playerId: UUID, trackId: UUID): TrackStageEntity?

    /**
     * Finds the current stage of a given player on a specific track.
     *
     * @param playerId the unique identifier of the player whose stage is being determined
     * @param track the track entity representing the specific track being queried
     * @return the current stage of the player on the specified track, or null if no stage is found
     */
    fun findPlayersCurrentStage(playerId: UUID, track: TrackEntity): TrackStageEntity?

    /**
     * Determines the current stage of a specific track that a player is in.
     *
     * @param player The player entity whose current stage is being determined.
     * @param trackId The unique identifier for the track to look up the stage for.
     * @return The stage entity of the track the player is currently in, or null if the player is not in any stage for the given track.
     */
    fun findPlayersCurrentStage(player: PlayerEntity, trackId: UUID): TrackStageEntity?

    /**
     * Determines the current stage of a given player within a specified track.
     *
     * @param player The player whose current stage is to be identified.
     * @param track The track in which the player's current stage is to be located.
     * @return The current stage of the player within the track, or null if the stage cannot be determined.
     */
    fun findPlayersCurrentStage(player: PlayerEntity, track: TrackEntity): TrackStageEntity?

    /**
     * Finds the next stage for a player on a specific track.
     *
     * @param playerId The unique identifier of the player whose next stage is being retrieved.
     * @param trackId The unique identifier of the track for which the player's next stage is being determined.
     * @return The next stage of the player on the specified track as a TrackStageEntity if available,
     *         or null if no next stage exists.
     */
    fun findPlayersNextStage(playerId: UUID, trackId: UUID): TrackStageEntity?

    /**
     * Determines the next stage for a given player on a specific track.
     *
     * @param playerId The unique identifier of the player whose next stage is being determined.
     * @param track The track entity that the player is currently on.
     * @return The next stage entity on the track for the given player, or null if no next stage exists.
     */
    fun findPlayersNextStage(playerId: UUID, track: TrackEntity): TrackStageEntity?

    /**
     * Determines the next stage of a track for the specified player.
     *
     * @param player The player entity for whom the next stage is to be determined.
     * @param trackId The unique identifier of the track being queried.
     * @return The next stage of the track as a TrackStageEntity, or null if no next stage exists.
     */
    fun findPlayersNextStage(player: PlayerEntity, trackId: UUID): TrackStageEntity?

    /**
     * Determines the next stage for a given player on a particular track.
     *
     * @param player the player entity for which the next stage is being found.
     * @param track the track entity associated with the player.
     * @return the next stage entity for the player on the specified track, or null if no further stage exists.
     */
    fun findPlayersNextStage(player: PlayerEntity, track: TrackEntity): TrackStageEntity?

    /**
     * Retrieves the previous stage of a given player within a specific track.
     *
     * @param playerId The unique identifier of the player whose previous stage is being queried.
     * @param trackId The unique identifier of the track in which the previous stage is being searched for.
     * @return The previous stage entity of the player within the track if found, or null if no such stage exists.
     */
    fun findPlayersPreviousStage(playerId: UUID, trackId: UUID): TrackStageEntity?

    /**
     * Finds the previous stage of a specified player within the given track.
     *
     * @param playerId The unique identifier of the player whose previous stage is being queried.
     * @param track The track entity where the player's stages are being inspected.
     * @return The previous stage of the player as a TrackStageEntity, or null if no previous stage exists.
     */
    fun findPlayersPreviousStage(playerId: UUID, track: TrackEntity): TrackStageEntity?

    /**
     * Finds the previous stage of the track that the given player has completed.
     *
     * @param player The player entity whose previous stage is to be found.
     * @param trackId The unique identifier of the track for which the previous stage is being queried.
     * @return The previous stage of the track completed by the player, or null if no such stage exists.
     */
    fun findPlayersPreviousStage(player: PlayerEntity, trackId: UUID): TrackStageEntity?

    /**
     * Finds the previous stage of a given track for a specified player.
     *
     * @param player The player entity whose previous track stage is to be retrieved.
     * @param track The track entity for which the previous stage will be determined.
     * @return The previous track stage entity for the given player and track, or null if no previous stage exists.
     */
    fun findPlayersPreviousStage(player: PlayerEntity, track: TrackEntity): TrackStageEntity?

    /**
     * Checks if the player is currently on the specified track.
     *
     * @param playerId The unique identifier of the player.
     * @param trackId The unique identifier of the track.
     * @return `true` if the player is on the track, `false` otherwise.
     */
    fun isPlayerOnTrack(playerId: UUID, trackId: UUID): Boolean = findPlayersCurrentStage(playerId, trackId) != null

    /**
     * Checks if a player is currently on a specific track.
     *
     * @param playerId The unique identifier of the player.
     * @param track The track entity to check against.
     * @return True if the player is on the track, otherwise false.
     */
    fun isPlayerOnTrack(playerId: UUID, track: TrackEntity): Boolean = findPlayersCurrentStage(playerId, track) != null

    /**
     * Checks if the given player is currently on the specified track.
     *
     * @param player The player entity to check.
     * @param trackId The unique identifier of the track to verify.
     * @return True if the player is on the specified track, false otherwise.
     */
    fun isPlayerOnTrack(player: PlayerEntity, trackId: UUID): Boolean = findPlayersCurrentStage(player, trackId) != null

    /**
     * Checks whether the given player is currently on the specified track.
     *
     * @param player the player entity to check.
     * @param track the track entity to check against.
     * @return true if the player is on the track, false otherwise.
     */
    fun isPlayerOnTrack(player: PlayerEntity, track: TrackEntity): Boolean =
        findPlayersCurrentStage(player, track) != null

    /**
     * Retrieves a list of tracks associated with a specific player.
     *
     * @param playerId The unique identifier of the player whose tracks are to be retrieved.
     * @return A SizedIterable containing the tracks linked to the specified player.
     */
    fun listPlayerTracks(playerId: UUID): SizedIterable<TrackEntity>

    /**
     * Retrieves a list of tracks associated with the specified player.
     *
     * @param player the player whose tracks are to be listed
     * @return a SizedIterable containing the tracks linked to the given player
     */
    fun listPlayerTracks(player: PlayerEntity): SizedIterable<TrackEntity>

    sealed interface PromotePlayerError {
        object EntityNotFound : PromotePlayerError
        object TargetNotFound : PromotePlayerError
        object MaximumStageReached : PromotePlayerError
        object NoStages : PromotePlayerError
        data class Unexpected(val throwable: Throwable) : PromotePlayerError
    }

    sealed interface DemotePlayerError {
        object EntityNotFound : DemotePlayerError
        object TargetNotFound : DemotePlayerError
        object MinimumStageReached : DemotePlayerError
        data class Unexpected(val throwable: Throwable) : DemotePlayerError
    }
}
