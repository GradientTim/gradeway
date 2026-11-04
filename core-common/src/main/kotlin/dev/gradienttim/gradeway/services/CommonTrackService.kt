/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.track.DatabaseTrackEntity
import dev.gradienttim.gradeway.database.models.track.DatabaseTrackStageEntity
import dev.gradienttim.gradeway.database.models.track.TrackStagesTable
import dev.gradienttim.gradeway.database.models.track.TracksTable
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.entity.track.TrackStageEntity
import dev.gradienttim.gradeway.extensions.eqAsStr
import dev.gradienttim.gradeway.extensions.eqId
import dev.gradienttim.gradeway.extensions.isIntegrityConstraintViolation
import dev.gradienttim.gradeway.extensions.isNameValid
import dev.gradienttim.gradeway.messaging.payloads.MessagingAction
import dev.gradienttim.gradeway.messaging.payloads.TrackChangedPayload
import dev.gradienttim.gradeway.messaging.payloads.TrackStageChangedPayload
import dev.gradienttim.gradeway.services.track.PlayerTrackService
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.audited
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.emptySized
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.*

class CommonTrackService(
    val gradeway: CommonGradeway<*>
) : TrackService {
    override fun createTrack(
        actor: Actor,
        slug: String,
    ): Either<TrackService.CreateTrackError, DatabaseTrackEntity> = gradeway.audited(actor) { audit ->
        if (!slug.isNameValid(TableConstants.TRACKS_TABLE_MAX_SLUG_LENGTH)) {
            raise(TrackService.CreateTrackError.InvalidSlug)
        }
        try {
            transaction(gradeway.database) {
                DatabaseTrackEntity.new {
                    this.slug = slug
                }.also { audit.created(it) }
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(TrackService.CreateTrackError.EntityAlreadyExists)
            }
            raise(TrackService.CreateTrackError.Unexpected(throwable))
        }
    }.onRight { track ->
        gradeway.messaging.publish(TrackChangedPayload(track.id.value.toString(), MessagingAction.CREATED))
    }

    override fun deleteTrack(
        actor: Actor,
        id: UUID,
    ): Either<TrackService.DeleteTrackError, Unit> = either {
        val entity = findTrackById(id) ?: raise(TrackService.DeleteTrackError.EntityNotFound)
        return deleteTrack(actor, entity)
    }

    override fun deleteTrack(
        actor: Actor,
        track: TrackEntity,
    ): Either<TrackService.DeleteTrackError, Unit> = gradeway.audited(actor) { audit ->
        if (track !is DatabaseTrackEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseTrackEntity")
            raise(TrackService.DeleteTrackError.Unexpected(throwable))
        }
        try {
            transaction(gradeway.database) {
                audit.deleted(track)
                track.delete()
            }
        } catch (throwable: Throwable) {
            raise(TrackService.DeleteTrackError.Unexpected(throwable))
        }
    }.onRight {
        gradeway.messaging.publish(TrackChangedPayload(track.id.value.toString(), MessagingAction.DELETED))
    }

    override fun deleteTrack(
        actor: Actor,
        idOrSlug: String,
    ): Either<TrackService.DeleteTrackError, Unit> = either {
        val entity = findTrackByIdOrSlug(idOrSlug) ?: raise(TrackService.DeleteTrackError.EntityNotFound)
        return deleteTrack(actor, entity)
    }

    override fun setSlug(
        actor: Actor,
        id: UUID,
        slug: String,
    ): Either<TrackService.SetSlugError, Unit> = either {
        val entity = findTrackById(id) ?: raise(TrackService.SetSlugError.EntityNotFound)
        return setSlug(actor, entity, slug)
    }

    override fun setSlug(
        actor: Actor,
        track: TrackEntity,
        slug: String,
    ): Either<TrackService.SetSlugError, Unit> = gradeway.audited(actor) { audit ->
        if (!slug.isNameValid(TableConstants.TRACKS_TABLE_MAX_SLUG_LENGTH)) {
            raise(TrackService.SetSlugError.InvalidSlug)
        }
        if (track.slug == slug) {
            raise(TrackService.SetSlugError.SlugAlreadySet)
        }
        if (track !is DatabaseTrackEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseTrackEntity")
            raise(TrackService.SetSlugError.Unexpected(throwable))
        }
        try {
            transaction(gradeway.database) {
                val before = track.snapshot()
                track.slug = slug
                track.flush()
                audit.updated(track, before)
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(TrackService.SetSlugError.SlugAlreadyExists)
            }
            raise(TrackService.SetSlugError.Unexpected(throwable))
        }
    }.onRight {
        gradeway.messaging.publish(TrackChangedPayload(track.id.value.toString(), MessagingAction.UPDATED))
    }

    override fun setSlug(
        actor: Actor,
        idOrSlug: String,
        slug: String,
    ): Either<TrackService.SetSlugError, Unit> = either {
        val entity = findTrackByIdOrSlug(idOrSlug) ?: raise(TrackService.SetSlugError.EntityNotFound)
        return setSlug(actor, entity, slug)
    }

    override fun addStage(
        actor: Actor,
        trackId: UUID,
        roleId: UUID,
    ): Either<TrackService.AddStageError, TrackStageEntity> =
        either {
            val track = findTrackById(trackId) ?: raise(TrackService.AddStageError.EntityNotFound)
            val role = gradeway.roles.findById(roleId) ?: raise(TrackService.AddStageError.TargetNotFound)
            return addStage(actor, track, role)
        }

    override fun addStage(
        actor: Actor,
        track: TrackEntity,
        role: RoleEntity,
    ): Either<TrackService.AddStageError, DatabaseTrackStageEntity> = gradeway.audited(actor) { audit ->
        try {
            transaction(gradeway.database) {
                val maxPosition = TrackStagesTable.position.max()
                val nextPosition = TrackStagesTable
                    .select(maxPosition)
                    .where { TrackStagesTable.trackId eqId track.id.value }
                    .single()[maxPosition]
                    ?.plus(1) ?: 0
                DatabaseTrackStageEntity.new {
                    this.trackId = track.id
                    this.roleId = role.id
                    this.position = nextPosition
                }.also { audit.created(it) }
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(TrackService.AddStageError.AlreadyExists)
            }
            raise(TrackService.AddStageError.Unexpected(throwable))
        }
    }.onRight { stage ->
        gradeway.messaging.publish(
            TrackStageChangedPayload(track.id.value.toString(), stage.id.value.toString(), MessagingAction.CREATED)
        )
    }

    override fun addStage(
        actor: Actor,
        trackIdOrSlug: String,
        roleId: UUID,
    ): Either<TrackService.AddStageError, TrackStageEntity> = either {
        val track = findTrackByIdOrSlug(trackIdOrSlug) ?: raise(TrackService.AddStageError.EntityNotFound)
        val role = gradeway.roles.findById(roleId) ?: raise(TrackService.AddStageError.TargetNotFound)
        return addStage(actor, track, role)
    }

    override fun removeStage(
        actor: Actor,
        stageId: UUID,
    ): Either<TrackService.RemoveStageError, Unit> = either {
        val entity = findStageById(stageId) ?: raise(TrackService.RemoveStageError.EntityNotFound)
        return removeStage(actor, entity)
    }

    override fun removeStage(
        actor: Actor,
        stage: TrackStageEntity,
    ): Either<TrackService.RemoveStageError, Unit> = gradeway.audited(actor) { audit ->
        if (stage !is DatabaseTrackStageEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseTrackStageEntity")
            raise(TrackService.RemoveStageError.Unexpected(throwable))
        }
        try {
            transaction(gradeway.database) {
                audit.deleted(stage)
                stage.delete()
            }
        } catch (throwable: Throwable) {
            raise(TrackService.RemoveStageError.Unexpected(throwable))
        }
    }.onRight {
        gradeway.messaging.publish(
            TrackStageChangedPayload(stage.trackId.value.toString(), stage.id.value.toString(), MessagingAction.DELETED)
        )
    }

    override fun moveStage(
        actor: Actor,
        stageId: UUID,
        index: Int,
    ): Either<TrackService.MoveStageError, TrackStageEntity> = either {
        val entity = findStageById(stageId) ?: raise(TrackService.MoveStageError.EntityNotFound)
        return moveStage(actor, entity, index)
    }

    override fun moveStage(
        actor: Actor,
        stage: TrackStageEntity,
        index: Int,
    ): Either<TrackService.MoveStageError, TrackStageEntity> = gradeway.audited(actor) { audit ->
        catch({
            transaction(gradeway.database) {
                val stageIds = TrackStagesTable
                    .select(TrackStagesTable.id)
                    .where { TrackStagesTable.trackId eqId stage.trackId.value }
                    .orderBy(TrackStagesTable.position to SortOrder.ASC)
                    .map { it[TrackStagesTable.id].value }
                    .toMutableList()

                val currentIndex = stageIds.indexOf(stage.id.value)
                if (currentIndex == -1) {
                    raise(TrackService.MoveStageError.EntityNotFound)
                }
                if (index !in stageIds.indices) {
                    raise(TrackService.MoveStageError.InvalidPosition(stageIds.size))
                }
                if (index == currentIndex) {
                    raise(TrackService.MoveStageError.AlreadyAtPosition)
                }

                stageIds.add(index, stageIds.removeAt(currentIndex))

                // Positions are rewritten through DSL updates that bypass the DAO cache, so every stage of the
                // track is snapshotted first and refreshed afterwards to record each one that actually moved.
                val stages = DatabaseTrackStageEntity
                    .find { TrackStagesTable.trackId eqId stage.trackId.value }
                    .toList()
                val before = stages.associateWith { it.snapshot() }

                stageIds.forEachIndexed { position, id ->
                    TrackStagesTable.update({ TrackStagesTable.id eq id }) { it[this.position] = -(position + 1) }
                }
                stageIds.forEachIndexed { position, id ->
                    TrackStagesTable.update({ TrackStagesTable.id eq id }) { it[this.position] = position }
                }

                stages.forEach { movedStage ->
                    movedStage.refresh()
                    audit.updated(movedStage, before.getValue(movedStage))
                }

                DatabaseTrackStageEntity.findById(stage.id.value)?.also { it.refresh() }
                    ?: raise(TrackService.MoveStageError.EntityNotFound)
            }
        }) { throwable ->
            raise(TrackService.MoveStageError.Unexpected(throwable))
        }
    }.onRight { moved ->
        gradeway.messaging.publish(
            TrackStageChangedPayload(moved.trackId.value.toString(), moved.id.value.toString(), MessagingAction.UPDATED)
        )
    }

    override fun findTrackById(id: UUID): DatabaseTrackEntity? {
        return transaction(gradeway.database) {
            DatabaseTrackEntity.findById(id)
        }
    }

    override fun findTrackBySlug(slug: String): DatabaseTrackEntity? {
        if (!slug.isNameValid(TableConstants.TRACKS_TABLE_MAX_SLUG_LENGTH)) {
            return null
        }
        return transaction(gradeway.database) {
            DatabaseTrackEntity.find { TracksTable.slug eq slug }.limit(1).firstOrNull()
        }
    }

    override fun findTrackByIdOrSlug(value: String): DatabaseTrackEntity? {
        if (
            value.length <= TableConstants.TRACKS_TABLE_MAX_SLUG_LENGTH &&
            !value.isNameValid(TableConstants.TRACKS_TABLE_MAX_SLUG_LENGTH)
        ) {
            return null
        }
        return transaction(gradeway.database) {
            DatabaseTrackEntity.find {
                (TracksTable.id eqAsStr value) or (TracksTable.slug eq value)
            }.limit(1).firstOrNull()
        }
    }

    override fun findStageById(id: UUID): DatabaseTrackStageEntity? {
        return transaction(gradeway.database) {
            DatabaseTrackStageEntity.findById(id)
        }
    }

    override fun findStageByRole(trackId: UUID, roleId: UUID): DatabaseTrackStageEntity? {
        return transaction(gradeway.database) {
            DatabaseTrackStageEntity.find {
                (TrackStagesTable.trackId eqId trackId) and (TrackStagesTable.roleId eqId roleId)
            }.limit(1).firstOrNull()
        }
    }

    override fun promotePlayer(
        actor: Actor,
        playerId: UUID,
        trackId: UUID,
    ): Either<PlayerTrackService.PromotePlayerError, Unit> = either {
        val player = gradeway.players.findById(playerId)
            ?: raise(PlayerTrackService.PromotePlayerError.EntityNotFound)
        val track = findTrackById(trackId) ?: raise(PlayerTrackService.PromotePlayerError.TargetNotFound)
        return promotePlayer(actor, player, track)
    }

    override fun promotePlayer(
        actor: Actor,
        playerId: UUID,
        track: TrackEntity,
    ): Either<PlayerTrackService.PromotePlayerError, Unit> = either {
        val player = gradeway.players.findById(playerId)
            ?: raise(PlayerTrackService.PromotePlayerError.EntityNotFound)
        return promotePlayer(actor, player, track)
    }

    override fun promotePlayer(
        actor: Actor,
        player: PlayerEntity,
        trackId: UUID,
    ): Either<PlayerTrackService.PromotePlayerError, Unit> = either {
        val track = findTrackById(trackId) ?: raise(PlayerTrackService.PromotePlayerError.TargetNotFound)
        return promotePlayer(actor, player, track)
    }

    override fun promotePlayer(
        actor: Actor,
        player: PlayerEntity,
        track: TrackEntity,
    ): Either<PlayerTrackService.PromotePlayerError, Unit> = either {
        catch({
            transaction(gradeway.database) {
                val currentStage = findPlayersCurrentStage(player, track)
                val targetStage = if (currentStage == null) {
                    findFirstStage(track) ?: raise(PlayerTrackService.PromotePlayerError.NoStages)
                } else {
                    findPlayersNextStage(player, track)
                        ?: raise(PlayerTrackService.PromotePlayerError.MaximumStageReached)
                }
                moveToStage(actor, player, currentStage, targetStage)
            }
        }) { throwable ->
            raise(PlayerTrackService.PromotePlayerError.Unexpected(throwable))
        }
    }

    override fun demotePlayer(
        actor: Actor,
        playerId: UUID,
        trackId: UUID,
    ): Either<PlayerTrackService.DemotePlayerError, Unit> = either {
        val player = gradeway.players.findById(playerId)
            ?: raise(PlayerTrackService.DemotePlayerError.EntityNotFound)
        val track = findTrackById(trackId) ?: raise(PlayerTrackService.DemotePlayerError.TargetNotFound)
        return demotePlayer(actor, player, track)
    }

    override fun demotePlayer(
        actor: Actor,
        playerId: UUID,
        track: TrackEntity,
    ): Either<PlayerTrackService.DemotePlayerError, Unit> = either {
        val player = gradeway.players.findById(playerId)
            ?: raise(PlayerTrackService.DemotePlayerError.EntityNotFound)
        return demotePlayer(actor, player, track)
    }

    override fun demotePlayer(
        actor: Actor,
        player: PlayerEntity,
        trackId: UUID,
    ): Either<PlayerTrackService.DemotePlayerError, Unit> = either {
        val track = findTrackById(trackId) ?: raise(PlayerTrackService.DemotePlayerError.TargetNotFound)
        return demotePlayer(actor, player, track)
    }

    override fun demotePlayer(
        actor: Actor,
        player: PlayerEntity,
        track: TrackEntity,
    ): Either<PlayerTrackService.DemotePlayerError, Unit> = either {
        catch({
            transaction(gradeway.database) {
                val currentStage = findPlayersCurrentStage(player, track)
                    ?: raise(PlayerTrackService.DemotePlayerError.MinimumStageReached)
                val targetStage = findPlayersPreviousStage(player, track)
                    ?: raise(PlayerTrackService.DemotePlayerError.MinimumStageReached)
                moveToStage(actor, player, currentStage, targetStage)
            }
        }) { throwable ->
            raise(PlayerTrackService.DemotePlayerError.Unexpected(throwable))
        }
    }

    override fun findPlayersCurrentStage(
        playerId: UUID,
        trackId: UUID
    ): TrackStageEntity? {
        val player = gradeway.players.findById(playerId) ?: return null
        val track = findTrackById(trackId) ?: return null
        return findPlayersCurrentStage(player, track)
    }

    override fun findPlayersCurrentStage(
        playerId: UUID,
        track: TrackEntity
    ): TrackStageEntity? {
        val player = gradeway.players.findById(playerId) ?: return null
        return findPlayersCurrentStage(player, track)
    }

    override fun findPlayersCurrentStage(
        player: PlayerEntity,
        trackId: UUID
    ): TrackStageEntity? {
        val track = findTrackById(trackId) ?: return null
        return findPlayersCurrentStage(player, track)
    }

    override fun findPlayersCurrentStage(
        player: PlayerEntity,
        track: TrackEntity
    ): DatabaseTrackStageEntity? {
        val primaryRoleId = player.primaryRoleId ?: return null
        return findStageByRole(track.id.value, primaryRoleId.value)
    }

    override fun findPlayersNextStage(
        playerId: UUID,
        trackId: UUID
    ): TrackStageEntity? {
        val player = gradeway.players.findById(playerId) ?: return null
        val track = findTrackById(trackId) ?: return null
        return findPlayersNextStage(player, track)
    }

    override fun findPlayersNextStage(
        playerId: UUID,
        track: TrackEntity
    ): TrackStageEntity? {
        val player = gradeway.players.findById(playerId) ?: return null
        return findPlayersNextStage(player, track)
    }

    override fun findPlayersNextStage(
        player: PlayerEntity,
        trackId: UUID
    ): TrackStageEntity? {
        val track = findTrackById(trackId) ?: return null
        return findPlayersNextStage(player, track)
    }

    override fun findPlayersNextStage(
        player: PlayerEntity,
        track: TrackEntity
    ): DatabaseTrackStageEntity? =
        findAdjacentStage(player, track, forward = true)

    override fun findPlayersPreviousStage(
        playerId: UUID,
        trackId: UUID
    ): TrackStageEntity? {
        val player = gradeway.players.findById(playerId) ?: return null
        val track = findTrackById(trackId) ?: return null
        return findPlayersPreviousStage(player, track)
    }

    override fun findPlayersPreviousStage(
        playerId: UUID,
        track: TrackEntity
    ): TrackStageEntity? {
        val player = gradeway.players.findById(playerId) ?: return null
        return findPlayersPreviousStage(player, track)
    }

    override fun findPlayersPreviousStage(
        player: PlayerEntity,
        trackId: UUID
    ): TrackStageEntity? {
        val track = findTrackById(trackId) ?: return null
        return findPlayersPreviousStage(player, track)
    }

    override fun findPlayersPreviousStage(
        player: PlayerEntity,
        track: TrackEntity
    ): DatabaseTrackStageEntity? =
        findAdjacentStage(player, track, forward = false)

    override fun listPlayerTracks(playerId: UUID): SizedIterable<TrackEntity> {
        val player = gradeway.players.findById(playerId) ?: return emptySized()
        return listPlayerTracks(player)
    }

    override fun listPlayerTracks(player: PlayerEntity): SizedIterable<TrackEntity> {
        val primaryRoleId = player.primaryRoleId ?: return emptySized()
        return transaction(gradeway.database) {
            DatabaseTrackEntity.find {
                TracksTable.id inSubQuery TrackStagesTable
                    .select(TrackStagesTable.trackId)
                    .where { TrackStagesTable.roleId eqId primaryRoleId.value }
            }.orderBy(TracksTable.slug to SortOrder.ASC)
        }
    }

    private fun findFirstStage(track: TrackEntity): DatabaseTrackStageEntity? {
        return transaction(gradeway.database) {
            DatabaseTrackStageEntity.find { TrackStagesTable.trackId eqId track.id.value }
                .orderBy(TrackStagesTable.position to SortOrder.ASC)
                .limit(1)
                .firstOrNull()
        }
    }

    private fun findAdjacentStage(
        player: PlayerEntity,
        track: TrackEntity,
        forward: Boolean
    ): DatabaseTrackStageEntity? {
        val currentStage = findPlayersCurrentStage(player, track) ?: return null
        val condition: Op<Boolean> = if (forward) {
            TrackStagesTable.position greater currentStage.position
        } else {
            TrackStagesTable.position less currentStage.position
        }
        val sortOrder = if (forward) SortOrder.ASC else SortOrder.DESC
        return transaction(gradeway.database) {
            DatabaseTrackStageEntity.find { (TrackStagesTable.trackId eqId track.id.value) and condition }
                .orderBy(TrackStagesTable.position to sortOrder)
                .limit(1)
                .firstOrNull()
        }
    }

    private fun moveToStage(
        actor: Actor,
        player: PlayerEntity,
        currentStage: TrackStageEntity?,
        targetStage: TrackStageEntity,
    ) {
        val targetRole = targetStage.role
        gradeway.players.addRole(actor, player, targetRole).throwUnless(
            message = "Failed to add role ${targetRole.id.value}",
            isTolerated = { it is PlayerService.AddRoleError.AlreadyExists },
            cause = { (it as? PlayerService.AddRoleError.Unexpected)?.throwable }
        )
        gradeway.players.setPrimaryRole(actor, player, targetRole).throwUnless(
            message = "Failed to set primary role ${targetRole.id.value}",
            isTolerated = { it is PlayerService.SetPrimaryRoleError.AlreadyPrimary },
            cause = { (it as? PlayerService.SetPrimaryRoleError.Unexpected)?.throwable }
        )
        if (currentStage == null) {
            return
        }
        val currentRole = currentStage.role
        gradeway.players.removeRole(actor, player, currentRole).throwUnless(
            message = "Failed to remove role ${currentRole.id.value}",
            isTolerated = { it is PlayerService.RemoveRoleError.NotExists },
            cause = { (it as? PlayerService.RemoveRoleError.Unexpected)?.throwable }
        )
    }

    private inline fun <E : Any> Either<E, *>.throwUnless(
        message: String,
        isTolerated: (E) -> Boolean,
        cause: (E) -> Throwable?
    ) {
        onLeft { failure ->
            if (!isTolerated(failure)) {
                throw cause(failure) ?: IllegalStateException("$message: ${failure::class.simpleName}")
            }
        }
    }
}
