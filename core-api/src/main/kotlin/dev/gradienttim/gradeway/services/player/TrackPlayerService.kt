/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.player

import arrow.core.Either
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.entity.track.TrackStageEntity
import dev.gradienttim.gradeway.services.track.PlayerTrackService.DemotePlayerError
import dev.gradienttim.gradeway.services.track.PlayerTrackService.PromotePlayerError
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.*

interface TrackPlayerService {
    fun promote(playerId: UUID, trackId: UUID): Either<PromotePlayerError, Unit>
    fun promote(playerId: UUID, trackId: TrackEntity): Either<PromotePlayerError, Unit>
    fun promote(playerId: PlayerEntity, trackId: UUID): Either<PromotePlayerError, Unit>
    fun promote(player: PlayerEntity, track: TrackEntity): Either<PromotePlayerError, Unit>

    fun demote(playerId: UUID, trackId: UUID): Either<DemotePlayerError, Unit>
    fun demote(playerId: UUID, trackId: TrackEntity): Either<DemotePlayerError, Unit>
    fun demote(playerId: PlayerEntity, trackId: UUID): Either<DemotePlayerError, Unit>
    fun demote(player: PlayerEntity, track: TrackEntity): Either<DemotePlayerError, Unit>

    fun findCurrentStage(playerId: UUID, trackId: UUID): TrackStageEntity?
    fun findCurrentStage(playerId: UUID, track: TrackEntity): TrackStageEntity?
    fun findCurrentStage(player: PlayerEntity, trackId: UUID): TrackStageEntity?
    fun findCurrentStage(player: PlayerEntity, track: TrackEntity): TrackStageEntity?

    fun findNextStage(playerId: UUID, trackId: UUID): TrackStageEntity?
    fun findNextStage(playerId: UUID, track: TrackEntity): TrackStageEntity?
    fun findNextStage(player: PlayerEntity, trackId: UUID): TrackStageEntity?
    fun findNextStage(player: PlayerEntity, track: TrackEntity): TrackStageEntity?

    fun findPreviousStage(playerId: UUID, trackId: UUID): TrackStageEntity?
    fun findPreviousStage(playerId: UUID, track: TrackEntity): TrackStageEntity?
    fun findPreviousStage(player: PlayerEntity, trackId: UUID): TrackStageEntity?
    fun findPreviousStage(player: PlayerEntity, track: TrackEntity): TrackStageEntity?

    fun isOnTrack(playerId: UUID, trackId: UUID): Boolean = findCurrentStage(playerId, trackId) != null
    fun isOnTrack(playerId: UUID, track: TrackEntity): Boolean = findCurrentStage(playerId, track) != null
    fun isOnTrack(player: PlayerEntity, trackId: UUID): Boolean = findCurrentStage(player, trackId) != null
    fun isOnTrack(player: PlayerEntity, track: TrackEntity): Boolean = findCurrentStage(player, track) != null

    fun listTracks(playerId: UUID): SizedIterable<TrackEntity>
    fun listTracks(player: PlayerEntity): SizedIterable<TrackEntity>
}
