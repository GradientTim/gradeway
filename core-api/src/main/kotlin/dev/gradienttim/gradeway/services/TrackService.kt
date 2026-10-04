/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.entity.track.TrackStageEntity
import dev.gradienttim.gradeway.services.track.PlayerTrackService
import java.util.*

interface TrackService : PlayerTrackService {
    /**
     * Creates a new track with the specified unique slug.
     *
     * This method attempts to create a `TrackEntity` identified by the provided slug.
     * If the creation fails due to business rules or unexpected errors,
     * an appropriate `CreateTrackError` is returned.
     *
     * @param slug A unique, human-readable identifier for the track to be created.
     *             It should be concise, URL-friendly, and not already in use.
     * @return Either a `TrackEntity` representing the newly created track or
     *         a `CreateTrackError` if the operation fails.
     */
    fun createTrack(slug: String): Either<CreateTrackError, TrackEntity>

    /**
     * Deletes a track identified by the given UUID.
     *
     * This method attempts to remove a track from the system using its unique identifier.
     * If the track does not exist or an unexpected error occurs during the deletion process,
     * an appropriate `DeleteTrackError` is returned.
     *
     * @param id The unique identifier of the track to be deleted.
     * @return Either a `Unit` if the deletion is successful, or a `DeleteTrackError` detailing the failure.
     */
    fun deleteTrack(id: UUID): Either<DeleteTrackError, Unit>

    /**
     * Deletes the specified track from the system.
     *
     * This method attempts to remove a `TrackEntity` instance. If the operation fails due to
     * the track not being found or any unexpected error, an appropriate `DeleteTrackError` is returned.
     *
     * @param track The `TrackEntity` to be deleted.
     * @return Either a `Unit` if the deletion is successful, or a `DeleteTrackError` if the operation fails.
     */
    fun deleteTrack(track: TrackEntity): Either<DeleteTrackError, Unit>

    /**
     * Deletes a track identified by the given ID or slug.
     *
     * This method attempts to remove a track from the system using its unique identifier
     * or a human-readable slug. If the track does not exist or an unexpected error occurs
     * during the deletion process, an appropriate `DeleteTrackError` is returned.
     *
     * @param idOrSlug A unique identifier (UUID as a string) or a human-readable slug representing the track to be deleted.
     * @return Either a `Unit` if the deletion is successful, or a `DeleteTrackError` indicating the reason for failure.
     */
    fun deleteTrack(idOrSlug: String): Either<DeleteTrackError, Unit>

    /**
     * Changes the slug of the track identified by the given UUID.
     *
     * @param id The unique identifier of the track to be updated.
     * @param slug The new unique, human-readable identifier of the track.
     * @return Either a `Unit` if the slug has been changed, or a `SetSlugError` detailing the failure.
     */
    fun setSlug(id: UUID, slug: String): Either<SetSlugError, Unit>

    /**
     * Changes the slug of the specified track.
     *
     * @param track The `TrackEntity` to be updated.
     * @param slug The new unique, human-readable identifier of the track.
     * @return Either a `Unit` if the slug has been changed, or a `SetSlugError` detailing the failure.
     */
    fun setSlug(track: TrackEntity, slug: String): Either<SetSlugError, Unit>

    /**
     * Changes the slug of the track identified by the given ID or current slug.
     *
     * @param idOrSlug A unique identifier (UUID as a string) or the current slug of the track.
     * @param slug The new unique, human-readable identifier of the track.
     * @return Either a `Unit` if the slug has been changed, or a `SetSlugError` detailing the failure.
     */
    fun setSlug(idOrSlug: String, slug: String): Either<SetSlugError, Unit>

    /**
     * Adds a stage to the specified track using the provided track and role identifiers.
     *
     * This method associates a role with a track as a new stage. If the operation fails due to
     * specified business rules, such as the stage already existing, or any unexpected errors,
     * a corresponding `AddStageError` will be returned.
     *
     * @param trackId The unique identifier of the track to which the stage should be added.
     * @param roleId The unique identifier of the role to be associated with the newly added stage.
     * @return Either a `TrackStageEntity` representing the newly created stage, or an `AddStageError` if the operation fails.
     */
    fun addStage(trackId: UUID, roleId: UUID): Either<AddStageError, TrackStageEntity>

    /**
     * Adds a stage to the specified track using the given `TrackEntity` and `RoleEntity` references.
     *
     * This method associates a role with a track, creating a new stage in the process. If the association
     * fails due to business rules, such as the stage already existing within the track, or due to
     * unexpected errors, an `AddStageError` is returned.
     *
     * @param track The `TrackEntity` to which the stage should be added.
     * @param role The `RoleEntity` to be associated with the newly added stage.
     * @return Either a `TrackStageEntity` representing the newly created stage, or an `AddStageError`
     *         detailing the reason for the failure.
     */
    fun addStage(track: TrackEntity, role: RoleEntity): Either<AddStageError, TrackStageEntity>

    /**
     * Adds a stage to a track using the specified track identifier (or slug) and role identifier.
     *
     * This method creates a new stage within a track by associating the specified role with it.
     * If the operation is not successful due to business rules, such as the stage already existing
     * in the track, or any other errors, an `AddStageError` will be returned.
     *
     * @param trackIdOrSlug The unique identifier or slug of the track to which the stage will be added.
     *                      This can either be a UUID-like string or a human-readable slug of the track.
     * @param roleId The unique identifier of the role to be associated with the stage being added.
     * @return Either a `TrackStageEntity` representing the newly created stage, or an `AddStageError`
     *         indicating the reason for the failure.
     */
    fun addStage(trackIdOrSlug: String, roleId: UUID): Either<AddStageError, TrackStageEntity>

    /**
     * Removes a stage identified by the specified unique identifier.
     *
     * This method attempts to delete a stage from the system based on its UUID.
     * If the stage does not exist or an unexpected error occurs during the removal,
     * an appropriate `RemoveStageError` is returned.
     *
     * @param stageId The unique identifier of the stage to be removed.
     * @return Either a `Unit` if the removal is successful, or a `RemoveStageError` detailing the failure.
     */
    fun removeStage(stageId: UUID): Either<RemoveStageError, Unit>

    /**
     * Removes the specified stage from the system.
     *
     * This method attempts to delete a given `TrackStageEntity` instance. If the operation fails
     * due to the stage not being found or an unexpected error, a corresponding `RemoveStageError` is returned.
     *
     * @param stage The `TrackStageEntity` to be removed.
     * @return Either a `Unit` if the removal is successful, or a `RemoveStageError` detailing the reason for failure.
     */
    fun removeStage(stage: TrackStageEntity): Either<RemoveStageError, Unit>

    /**
     * Moves the stage identified by the given UUID to a new position within its track.
     *
     * The remaining stages of the track keep their relative order and are shifted to make room,
     * after which all positions of the track are renumbered to a contiguous, zero-based sequence.
     *
     * @param stageId The unique identifier of the stage to be moved.
     * @param index The zero-based target position of the stage within its track.
     * @return Either the moved `TrackStageEntity`, or a `MoveStageError` detailing the failure.
     */
    fun moveStage(stageId: UUID, index: Int): Either<MoveStageError, TrackStageEntity>

    /**
     * Moves the specified stage to a new position within its track.
     *
     * The remaining stages of the track keep their relative order and are shifted to make room,
     * after which all positions of the track are renumbered to a contiguous, zero-based sequence.
     *
     * @param stage The `TrackStageEntity` to be moved.
     * @param index The zero-based target position of the stage within its track.
     * @return Either the moved `TrackStageEntity`, or a `MoveStageError` detailing the failure.
     */
    fun moveStage(stage: TrackStageEntity, index: Int): Either<MoveStageError, TrackStageEntity>

    /**
     * Retrieves a track entity by its unique identifier.
     *
     * This method searches for a `TrackEntity` in the system using the provided UUID.
     * If a matching track is found, it is returned. If no match is found, null is returned.
     *
     * @param id The unique identifier of the track to be retrieved.
     * @return The `TrackEntity` corresponding to the provided ID, or null if no track is found.
     */
    fun findTrackById(id: UUID): TrackEntity?

    /**
     * Retrieves a track entity by its unique slug.
     *
     * This method searches for a `TrackEntity` in the system using the provided slug.
     * If a matching track is found, it is returned. If no match is found, null is returned.
     *
     * @param slug A unique, human-readable identifier for the track being retrieved.
     * @return The `TrackEntity` corresponding to the provided slug, or null if no track is found.
     */
    fun findTrackBySlug(slug: String): TrackEntity?

    /**
     * Retrieves a track entity using either its unique identifier or a human-readable slug.
     *
     * This method searches for a `TrackEntity` in the system using the provided value, which can
     * represent either a UUID (as a string) or a slug. If a matching track is found, it is returned.
     * If no match is found, null is returned.
     *
     * @param value A unique identifier (UUID as a string) or a human-readable slug
     *              representing the track being searched.
     * @return The `TrackEntity` corresponding to the given identifier or slug,
     *         or null if no matching track is found.
     */
    fun findTrackByIdOrSlug(value: String): TrackEntity?

    /**
     * Finds and returns the stage entity associated with the given ID.
     *
     * @param id the unique identifier of the stage to be retrieved
     * @return the stage entity if found, or null if no such stage exists
     */
    fun findStageById(id: UUID): TrackStageEntity?

    /**
     * Finds a stage associated with a specific role within a given track.
     *
     * @param trackId The unique identifier of the track.
     * @param roleId The unique identifier of the role.
     * @return The corresponding TrackStageEntity if found, or null if no match exists.
     */
    fun findStageByRole(trackId: UUID, roleId: UUID): TrackStageEntity?

    sealed interface CreateTrackError {
        object InvalidSlug : CreateTrackError
        object EntityAlreadyExists : CreateTrackError
        data class Unexpected(val throwable: Throwable) : CreateTrackError
    }

    sealed interface DeleteTrackError {
        object EntityNotFound : DeleteTrackError
        data class Unexpected(val throwable: Throwable) : DeleteTrackError
    }

    sealed interface SetSlugError {
        object EntityNotFound : SetSlugError
        object InvalidSlug : SetSlugError
        object SlugAlreadySet : SetSlugError
        object SlugAlreadyExists : SetSlugError
        data class Unexpected(val throwable: Throwable) : SetSlugError
    }

    sealed interface AddStageError {
        object EntityNotFound : AddStageError
        object TargetNotFound : AddStageError
        object AlreadyExists : AddStageError
        data class Unexpected(val throwable: Throwable) : AddStageError
    }

    sealed interface RemoveStageError {
        object EntityNotFound : RemoveStageError
        data class Unexpected(val throwable: Throwable) : RemoveStageError
    }

    sealed interface MoveStageError {
        object EntityNotFound : MoveStageError
        data class InvalidPosition(val stageCount: Int) : MoveStageError
        object AlreadyAtPosition : MoveStageError
        data class Unexpected(val throwable: Throwable) : MoveStageError
    }
}
