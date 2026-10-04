/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.reference

import arrow.core.Either
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.entity.track.TrackStageEntity
import dev.gradienttim.gradeway.services.track.PlayerTrackService.DemotePlayerError
import dev.gradienttim.gradeway.services.track.PlayerTrackService.PromotePlayerError
import java.util.*

/**
 * Represents a reference to a collection of tracks and provides operations for navigating,
 * promoting, and demoting within the stages of the tracks.
 *
 * A `TrackReference` serves as an interface to interact with tracks and their respective stages,
 * enabling queries to determine current, next, or previous stages within a track. It also provides
 * methods to evaluate whether a specific track or track stage is currently active.
 */
interface TrackReference {
    /**
     * Promotes the track identified by the specified track ID to its next stage.
     *
     * This operation attempts to advance the track associated with the given [trackId]
     * to the subsequent stage. The promotion may fail due to various reasons, such as
     * the track not being found, the target stage not being available, reaching the
     * maximum stage, or encountering an unexpected error.
     *
     * @param trackId The unique identifier of the track to be promoted.
     * @return An [Either] containing either a [PromotePlayerError] if the promotion fails,
     *         or [Unit] if the promotion is successful.
     */
    fun promote(trackId: UUID): Either<PromotePlayerError, Unit>

    /**
     * Promotes the given track to its next stage.
     *
     * This method attempts to advance the specified [track] to the subsequent stage within
     * its progression. The operation may fail for several reasons, including when the entity
     * is not found, the target stage is unavailable, the maximum stage has been reached, or
     * an unexpected error occurs.
     *
     * @param track The [TrackEntity] representing the track to be promoted.
     * @return An [Either] containing a [PromotePlayerError] if the promotion fails, or [Unit]
     *         if the promotion is successful.
     */
    fun promote(track: TrackEntity): Either<PromotePlayerError, Unit>

    /**
     * Demotes the track identified by the given track ID to its previous stage.
     *
     * This method attempts to regress the specified track to its preceding stage
     * within its progression. The operation may fail due to various reasons, such as
     * the track not being found, the target stage not being available, reaching the
     * minimum stage, or encountering an unexpected error.
     *
     * @param trackId The unique identifier of the track to be demoted.
     * @return An [Either] containing a [DemotePlayerError] if the demotion fails, or [Unit] if the demotion is successful.
     */
    fun demote(trackId: UUID): Either<DemotePlayerError, Unit>

    /**
     * Demotes the given track to its previous stage.
     *
     * This method attempts to regress the specified [track] to its preceding stage within
     * its progression. The operation may fail due to various reasons, such as the track not
     * being found, the target stage not being available, reaching the minimum stage, or
     * encountering an unexpected error.
     *
     * @param track The [TrackEntity] representing the track to be demoted.
     * @return An [Either] containing a [DemotePlayerError] if the demotion fails, or [Unit] if the demotion is successful.
     */
    fun demote(track: TrackEntity): Either<DemotePlayerError, Unit>

    /**
     * Retrieves the current stage of the track specified by the given track ID.
     *
     * This method returns the [TrackStageEntity] that represents the current position
     * of the track identified by the provided [trackId]. If the track does not exist or
     * cannot be found, the method will return `null`.
     *
     * @param trackId The unique identifier of the track whose current stage is to be retrieved.
     * @return A [TrackStageEntity] representing the current stage of the track, or `null` if the track
     *         is not found or has no associated stages.
     */
    fun findCurrentStage(trackId: UUID): TrackStageEntity?

    /**
     * Retrieves the current stage of the given track.
     *
     * This method returns the [TrackStageEntity] that represents the current position
     * of the provided [track]. If the track has no associated stages or if it cannot be
     * found, the method will return `null`.
     *
     * @param track The [TrackEntity] whose current stage is to be retrieved.
     * @return A [TrackStageEntity] representing the current stage of the track, or `null` if the track
     *         has no associated stages or cannot be found.
     */
    fun findCurrentStage(track: TrackEntity): TrackStageEntity?

    /**
     * Retrieves the next stage of the track specified by the given track ID.
     *
     * This method returns the [TrackStageEntity] that represents the subsequent stage
     * in the progression of the track identified by the provided [trackId]. If there
     * are no further stages or the track cannot be found, the method will return `null`.
     *
     * @param trackId The unique identifier of the track whose next stage is to be retrieved.
     * @return A [TrackStageEntity] representing the next stage of the track, or `null` if no next stage exists or the track is not found.
     */
    fun findNextStage(trackId: UUID): TrackStageEntity?

    /**
     * Retrieves the next stage of the given track.
     *
     * This method returns the [TrackStageEntity] that represents the subsequent stage
     * in the progression of the provided [track]. If there are no further stages or
     * the track is not found, the method will return `null`.
     *
     * @param track The [TrackEntity] whose next stage is to be retrieved.
     * @return A [TrackStageEntity] representing the next stage of the track, or `null` if no next stage exists or the track is not found.
     */
    fun findNextStage(track: TrackEntity): TrackStageEntity?

    /**
     * Retrieves the previous stage of the track specified by the given track ID.
     *
     * This method returns the [TrackStageEntity] that represents the preceding stage
     * in the progression of the track identified by the provided [trackId]. If there
     * are no prior stages or the track cannot be found, the method will return `null`.
     *
     * @param trackId The unique identifier of the track whose previous stage is to be retrieved.
     * @return A [TrackStageEntity] representing the previous stage of the track, or `null` if no previous stage exists or the track is not found.
     */
    fun findPreviousStage(trackId: UUID): TrackStageEntity?

    /**
     * Retrieves the previous stage of the given track.
     *
     * This method returns the [TrackStageEntity] that represents the preceding stage
     * in the progression of the provided [track]. If there are no prior stages or
     * the track cannot be found, the method will return `null`.
     *
     * @param track The [TrackEntity] whose previous stage is to be retrieved.
     * @return A [TrackStageEntity] representing the previous stage of the track,
     *         or `null` if no previous stage exists or the track is not found.
     */
    fun findPreviousStage(track: TrackEntity): TrackStageEntity?

    /**
     * Checks if the track identified by the specified track ID is currently active.
     *
     * This method determines whether there is an active stage for the track identified
     * by the provided [trackId]. It returns `true` if a current stage is found, and `false` otherwise.
     *
     * @param trackId The unique identifier of the track to be checked.
     * @return `true` if the track has an active stage, or `false` if the track is not found or has no stages.
     */
    fun isOnTrack(trackId: UUID): Boolean = findCurrentStage(trackId) != null

    /**
     * Checks if the given track is currently on track by verifying the presence of a current stage.
     *
     * This method determines whether there is an active stage for the provided [track].
     * It returns `true` if a current stage is found, and `false` otherwise.
     *
     * @param track The [TrackEntity] whose status is to be checked.
     * @return `true` if the track has an active stage, or `false` if the track has no active stages or cannot be found.
     */
    fun isOnTrack(track: TrackEntity): Boolean = findCurrentStage(track) != null
}
