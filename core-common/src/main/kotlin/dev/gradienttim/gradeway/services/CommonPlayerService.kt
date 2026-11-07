/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.AuditRecorder
import dev.gradienttim.gradeway.audit.audited
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerEntity
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerRoleEntity
import dev.gradienttim.gradeway.database.models.player.PlayerRolesTable
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.player.PlayerRoleEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.event.fireCancellable
import dev.gradienttim.gradeway.events.player.*
import dev.gradienttim.gradeway.extensions.eqAsStr
import dev.gradienttim.gradeway.extensions.isIntegrityConstraintViolation
import dev.gradienttim.gradeway.extensions.isNameValid
import dev.gradienttim.gradeway.messaging.payloads.*
import dev.gradienttim.gradeway.platform.CommonCaches
import dev.gradienttim.gradeway.spec.PlayerCreateSpec
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.*

@Suppress("LargeClass", "TooManyFunctions")
class CommonPlayerService(
    val gradeway: CommonGradeway<*>
) : PlayerService {
    init {
        gradeway.messaging.subscribe { payload -> invalidateWeightFor(payload) }
    }

    override fun create(
        actor: Actor,
        id: UUID,
        name: String,
        builder: PlayerCreateSpec.() -> Unit,
    ): Either<PlayerService.CreatePlayerError, DatabasePlayerEntity> = gradeway.audited(actor) { audit ->
        val spec = PlayerCreateSpec(id, name).apply(builder)
        fireCancellable(gradeway, PlayerCreateEvent(actor, spec), PlayerService.CreatePlayerError.Cancelled)
        if (!spec.name.isNameValid(TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH)) {
            raise(PlayerService.CreatePlayerError.InvalidName)
        }
        try {
            transaction(gradeway.database) {
                DatabasePlayerEntity.new(id) {
                    this.name = spec.name
                    this.weight = spec.weight
                }.also {
                    audit.created(it)
                    audit.emit(PlayerCreatedEvent(actor, it.id.value, it.name, it.weight))
                }
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(PlayerService.CreatePlayerError.EntityAlreadyExists)
            }
            raise(PlayerService.CreatePlayerError.Unexpected(throwable))
        }
    }

    override fun delete(
        actor: Actor,
        id: UUID,
    ): Either<PlayerService.DeletePlayerError, Unit> = gradeway.audited(actor) { audit ->
        val entity = findById(id) ?: raise(PlayerService.DeletePlayerError.EntityNotFound)
        fireCancellable(gradeway, PlayerDeleteEvent(actor, entity), PlayerService.DeletePlayerError.Cancelled)
        try {
            transaction(gradeway.database) {
                audit.deleted(entity)
                audit.emit(PlayerDeletedEvent(actor, entity.id.value, entity.name))
                entity.delete()
            }
        } catch (throwable: Throwable) {
            raise(PlayerService.DeletePlayerError.Unexpected(throwable))
        }
    }

    override fun setName(
        actor: Actor,
        id: UUID,
        name: String,
    ): Either<PlayerService.SetNameError, Boolean> = either {
        val entity = findById(id) ?: raise(PlayerService.SetNameError.EntityNotFound)
        return setName(actor, entity, name)
    }

    override fun setName(
        actor: Actor,
        entity: PlayerEntity,
        name: String,
    ): Either<PlayerService.SetNameError, Boolean> = gradeway.audited(actor) { audit ->
        if (!name.isNameValid(TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH)) {
            raise(PlayerService.SetNameError.InvalidName)
        }
        if (entity !is DatabasePlayerEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePlayerEntity")
            raise(PlayerService.SetNameError.Unexpected(throwable))
        }
        fireCancellable(gradeway, PlayerRenameEvent(actor, entity, name), PlayerService.SetNameError.Cancelled)
        try {
            transaction(gradeway.database) {
                val before = entity.snapshot()
                audit.emit(PlayerRenamedEvent(actor, entity.id.value, entity.name, name))
                entity.name = name
                val flushed = entity.flush()
                audit.updated(entity, before)
                flushed
            }
        } catch (throwable: Throwable) {
            raise(PlayerService.SetNameError.Unexpected(throwable))
        }
    }

    override fun setWeight(
        actor: Actor,
        id: UUID,
        weight: Int,
    ): Either<PlayerService.SetWeightError, Boolean> = either {
        val entity = findById(id) ?: raise(PlayerService.SetWeightError.EntityNotFound)
        return setWeight(actor, entity, weight)
    }

    override fun setWeight(
        actor: Actor,
        entity: PlayerEntity,
        weight: Int,
    ): Either<PlayerService.SetWeightError, Boolean> = gradeway.audited(actor) { audit ->
        if (entity !is DatabasePlayerEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePlayerEntity")
            raise(PlayerService.SetWeightError.Unexpected(throwable))
        }
        fireCancellable(
            gradeway,
            PlayerWeightChangeEvent(actor, entity, weight),
            PlayerService.SetWeightError.Cancelled,
        )
        try {
            transaction(gradeway.database) {
                val before = entity.snapshot()
                audit.emit(PlayerWeightChangedEvent(actor, entity.id.value, entity.weight, weight))
                entity.weight = weight
                val flushed = entity.flush()
                audit.updated(entity, before)
                flushed
            }
        } catch (throwable: Throwable) {
            raise(PlayerService.SetWeightError.Unexpected(throwable))
        }
    }

    override fun setWeight(
        actor: Actor,
        idOrName: String,
        weight: Int,
    ): Either<PlayerService.SetWeightError, Boolean> = either {
        val entity = findByIdOrName(idOrName) ?: raise(PlayerService.SetWeightError.EntityNotFound)
        return setWeight(actor, entity, weight)
    }

    override fun getEffectiveWeight(id: UUID): Int {
        return gradeway.caches.playerEffectiveWeights.get(id)
    }

    override fun getEffectiveWeight(entity: PlayerEntity): Int {
        return getEffectiveWeight(entity.id.value)
    }

    override fun getEffectiveWeight(idOrName: String): Int {
        val entity = findByIdOrName(idOrName) ?: return CommonCaches.DEFAULT_WEIGHT
        return getEffectiveWeight(entity)
    }

    private fun invalidateWeightFor(payload: MessagingPayload) {
        when (payload) {
            is PlayerChangedPayload -> invalidatePlayerWeight(payload.playerId)
            is PlayerRoleChangedPayload -> invalidatePlayerWeight(payload.playerId)
            is RoleChangedPayload, is GroupRoleChangedPayload, is GroupChangedPayload, is CacheFlushPayload ->
                gradeway.caches.playerEffectiveWeights.invalidateAll()

            else -> {}
        }
    }

    /**
     * Invalidates both the effective-weight cache and the underlying raw player-entity cache that
     * feeds it: [dev.gradienttim.gradeway.platform.CommonCaches.resolveEffectivePlayerWeight] reads
     * the player's own `weight` field off the cached [PlayerEntity], so leaving that entity cached
     * would let a weight change keep resolving to its stale pre-change value until the entity
     * cache's own TTL separately expires.
     */
    private fun invalidatePlayerWeight(rawPlayerId: String) {
        val playerId = runCatching { UUID.fromString(rawPlayerId) }.getOrNull() ?: return
        gradeway.caches.players.invalidate(playerId)
        gradeway.caches.playerEffectiveWeights.invalidate(playerId)
    }

    override fun findById(id: UUID): DatabasePlayerEntity? {
        return transaction(gradeway.database) {
            DatabasePlayerEntity.findById(id)
        }
    }

    override fun findByName(name: String): DatabasePlayerEntity? {
        if (!name.isNameValid(TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH)) {
            return null
        }
        return transaction(gradeway.database) {
            DatabasePlayerEntity.find { PlayersTable.name eq name }.limit(1).firstOrNull()
        }
    }

    override fun findByIdOrName(value: String): DatabasePlayerEntity? {
        if (
            value.length <= TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH &&
            !value.isNameValid(TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH)
        ) {
            return null
        }
        return transaction(gradeway.database) {
            DatabasePlayerEntity.find {
                (PlayersTable.id eqAsStr value) or (PlayersTable.name eq value)
            }.limit(1).firstOrNull()
        }
    }

    @Deprecated(
        "This function will be removed in near future to avoid \"useless\" function calls.",
        replaceWith = ReplaceWith("findById(id) != null"),
        level = DeprecationLevel.ERROR
    )
    override fun existsById(id: UUID): Boolean =
        findById(id) != null

    @Deprecated(
        "This function will be removed in near future to avoid \"useless\" function calls.",
        replaceWith = ReplaceWith("findByName(name) != null"),
        level = DeprecationLevel.ERROR
    )
    override fun existsByName(name: String): Boolean =
        findByName(name) != null

    @Deprecated(
        "This function will be removed in near future to avoid \"useless\" function calls.",
        replaceWith = ReplaceWith("existsByIdOrName(value) != null"),
        level = DeprecationLevel.ERROR
    )
    override fun existsByIdOrName(value: String): Boolean =
        findByIdOrName(value) != null

    override fun getPrimaryRole(id: UUID): RoleEntity? {
        val entity = findById(id) ?: return null
        return getPrimaryRole(entity)
    }

    override fun getPrimaryRole(entity: PlayerEntity): RoleEntity? =
        transaction(gradeway.database) { entity.primaryRole }

    override fun addRole(
        actor: Actor,
        playerId: UUID,
        roleId: UUID,
        until: Instant?,
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity> = either {
        val player = findById(playerId) ?: raise(PlayerService.AddRoleError.EntityNotFound)
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.AddRoleError.TargetNotFound)
        return addRole(actor, player, role, until)
    }

    override fun addRole(
        actor: Actor,
        playerId: UUID,
        role: RoleEntity,
        until: Instant?,
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity> = either {
        val player = findById(playerId) ?: raise(PlayerService.AddRoleError.EntityNotFound)
        return addRole(actor, player, role, until)
    }

    override fun addRole(
        actor: Actor,
        player: PlayerEntity,
        roleId: UUID,
        until: Instant?,
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity> = either {
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.AddRoleError.TargetNotFound)
        return addRole(actor, player, role, until)
    }

    override fun addRole(
        actor: Actor,
        player: PlayerEntity,
        role: RoleEntity,
        until: Instant?,
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity> = gradeway.audited(actor) { audit ->
        removeExpiredRoles(actor, player)

        if (until != null && until < gradeway.now()) {
            raise(PlayerService.AddRoleError.UntilInPast)
        }
        fireCancellable(gradeway, PlayerRoleAddEvent(actor, player, role, until), PlayerService.AddRoleError.Cancelled)

        try {
            transaction(gradeway.database) {
                DatabasePlayerRoleEntity.new {
                    this.roleId = role.id
                    this.playerId = player.id
                    this.untilAt = until
                }.also {
                    audit.created(it)
                    audit.emit(PlayerRoleAddedEvent(actor, player.id.value, role.id.value, until))
                }
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(PlayerService.AddRoleError.AlreadyExists)
            }
            raise(PlayerService.AddRoleError.Unexpected(throwable))
        }
    }.onRight {
        gradeway.messaging.publish(
            PlayerRoleChangedPayload(player.id.value.toString(), role.id.value.toString(), MessagingAction.CREATED)
        )
    }

    override fun addRole(
        actor: Actor,
        playerIdOrName: String,
        roleId: UUID,
        untilAt: Instant?,
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.AddRoleError.EntityNotFound)
        return addRole(actor, player, roleId, untilAt)
    }

    override fun addRole(
        actor: Actor,
        playerIdOrName: String,
        role: RoleEntity,
        untilAt: Instant?,
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.AddRoleError.EntityNotFound)
        return addRole(actor, player, role, untilAt)
    }

    override fun removeRole(
        actor: Actor,
        playerId: UUID,
        roleId: UUID,
    ): Either<PlayerService.RemoveRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.RemoveRoleError.EntityNotFound)
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.RemoveRoleError.TargetNotFound)
        return removeRole(actor, player, role)
    }

    override fun removeRole(
        actor: Actor,
        playerId: UUID,
        role: RoleEntity,
    ): Either<PlayerService.RemoveRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.RemoveRoleError.EntityNotFound)
        return removeRole(actor, player, role)
    }

    override fun removeRole(
        actor: Actor,
        player: PlayerEntity,
        roleId: UUID,
    ): Either<PlayerService.RemoveRoleError, Unit> = either {
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.RemoveRoleError.TargetNotFound)
        return removeRole(actor, player, role)
    }

    override fun removeRole(
        actor: Actor,
        player: PlayerEntity,
        role: RoleEntity,
    ): Either<PlayerService.RemoveRoleError, Unit> = gradeway.audited(actor) { audit ->
        removeExpiredRoles(actor, player)

        transaction(gradeway.database) {
            val playerRoleEntity = player.roles.find { it.roleId == role.id }
            if (playerRoleEntity == null) {
                raise(PlayerService.RemoveRoleError.NotExists)
            }
            if (playerRoleEntity !is DatabasePlayerRoleEntity) {
                val throwable = Throwable("Entity is not a type of DatabasePlayerRoleEntity")
                raise(PlayerService.RemoveRoleError.Unexpected(throwable))
            }
            fireCancellable(
                gradeway,
                PlayerRoleRemoveEvent(actor, player, role),
                PlayerService.RemoveRoleError.Cancelled,
            )
            try {
                audit.deleted(playerRoleEntity)
                audit.emit(PlayerRoleRemovedEvent(actor, player.id.value, role.id.value))
                playerRoleEntity.delete()
            } catch (throwable: Throwable) {
                raise(PlayerService.RemoveRoleError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            PlayerRoleChangedPayload(player.id.value.toString(), role.id.value.toString(), MessagingAction.DELETED)
        )
    }

    override fun removeRole(
        actor: Actor,
        playerIdOrName: String,
        roleId: UUID,
    ): Either<PlayerService.RemoveRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.RemoveRoleError.EntityNotFound)
        return removeRole(actor, player, roleId)
    }

    override fun removeRole(
        actor: Actor,
        playerIdOrName: String,
        role: RoleEntity,
    ): Either<PlayerService.RemoveRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.RemoveRoleError.EntityNotFound)
        return removeRole(actor, player, role)
    }

    override fun setRoleUntilAt(
        actor: Actor,
        playerId: UUID,
        roleId: UUID,
        untilAt: Instant,
    ): Either<PlayerService.SetRoleUntilAtError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.SetRoleUntilAtError.EntityNotFound)
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.SetRoleUntilAtError.TargetNotFound)
        return setRoleUntilAt(actor, player, role, untilAt)
    }

    override fun setRoleUntilAt(
        actor: Actor,
        player: PlayerEntity,
        roleId: UUID,
        untilAt: Instant,
    ): Either<PlayerService.SetRoleUntilAtError, Unit> = either {
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.SetRoleUntilAtError.TargetNotFound)
        return setRoleUntilAt(actor, player, role, untilAt)
    }

    override fun setRoleUntilAt(
        actor: Actor,
        playerId: UUID,
        role: RoleEntity,
        untilAt: Instant,
    ): Either<PlayerService.SetRoleUntilAtError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.SetRoleUntilAtError.EntityNotFound)
        return setRoleUntilAt(actor, player, role, untilAt)
    }

    override fun setRoleUntilAt(
        actor: Actor,
        player: PlayerEntity,
        role: RoleEntity,
        untilAt: Instant,
    ): Either<PlayerService.SetRoleUntilAtError, Unit> = gradeway.audited(actor) { audit ->
        removeExpiredRoles(actor, player)

        if (untilAt < gradeway.now()) {
            raise(PlayerService.SetRoleUntilAtError.UntilInPast)
        }

        transaction(gradeway.database) {
            val playerRoleEntity = player.roles.find { it.roleId == role.id }
            if (playerRoleEntity == null) {
                raise(PlayerService.SetRoleUntilAtError.RelationNotFound)
            }
            if (playerRoleEntity !is DatabasePlayerRoleEntity) {
                val throwable = Throwable("Entity is not a type of DatabasePlayerRoleEntity")
                raise(PlayerService.SetRoleUntilAtError.Unexpected(throwable))
            }
            fireCancellable(
                gradeway,
                PlayerRoleExpiryChangeEvent(actor, player, role, untilAt),
                PlayerService.SetRoleUntilAtError.Cancelled,
            )
            try {
                val before = playerRoleEntity.snapshot()
                val previousUntilAt = playerRoleEntity.untilAt
                playerRoleEntity.untilAt = untilAt
                audit.emit(
                    PlayerRoleExpiryChangedEvent(actor, player.id.value, role.id.value, previousUntilAt, untilAt)
                )
                playerRoleEntity.flush()
                audit.updated(playerRoleEntity, before)
            } catch (throwable: Throwable) {
                raise(PlayerService.SetRoleUntilAtError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            PlayerRoleChangedPayload(player.id.value.toString(), role.id.value.toString(), MessagingAction.UPDATED)
        )
    }

    override fun setRoleUntilAt(
        actor: Actor,
        playerIdOrName: String,
        roleId: UUID,
        untilAt: Instant,
    ): Either<PlayerService.SetRoleUntilAtError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.SetRoleUntilAtError.EntityNotFound)
        return setRoleUntilAt(actor, player, roleId, untilAt)
    }

    override fun setRoleUntilAt(
        actor: Actor,
        playerIdOrName: String,
        role: RoleEntity,
        untilAt: Instant,
    ): Either<PlayerService.SetRoleUntilAtError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.SetRoleUntilAtError.EntityNotFound)
        return setRoleUntilAt(actor, player, role, untilAt)
    }

    override fun setRolePausedAt(
        actor: Actor,
        playerId: UUID,
        roleId: UUID,
        pausedAt: Instant,
    ): Either<PlayerService.SetRolePausedAtError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.SetRolePausedAtError.EntityNotFound)
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.SetRolePausedAtError.TargetNotFound)
        return setRolePausedAt(actor, player, role, pausedAt)
    }

    override fun setRolePausedAt(
        actor: Actor,
        playerId: UUID,
        role: RoleEntity,
        pausedAt: Instant,
    ): Either<PlayerService.SetRolePausedAtError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.SetRolePausedAtError.EntityNotFound)
        return setRolePausedAt(actor, player, role, pausedAt)
    }

    override fun setRolePausedAt(
        actor: Actor,
        player: PlayerEntity,
        roleId: UUID,
        pausedAt: Instant,
    ): Either<PlayerService.SetRolePausedAtError, Unit> = either {
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.SetRolePausedAtError.TargetNotFound)
        return setRolePausedAt(actor, player, role, pausedAt)
    }

    override fun setRolePausedAt(
        actor: Actor,
        player: PlayerEntity,
        role: RoleEntity,
        pausedAt: Instant,
    ): Either<PlayerService.SetRolePausedAtError, Unit> = gradeway.audited(actor) { audit ->
        removeExpiredRoles(actor, player)

        if (pausedAt < gradeway.now()) {
            raise(PlayerService.SetRolePausedAtError.PauseInPast)
        }

        transaction(gradeway.database) {
            val playerRoleEntity = player.roles.find { it.roleId == role.id }
            if (playerRoleEntity == null) {
                raise(PlayerService.SetRolePausedAtError.RelationNotFound)
            }
            if (playerRoleEntity !is DatabasePlayerRoleEntity) {
                val throwable = Throwable("Entity is not a type of DatabasePlayerRoleEntity")
                raise(PlayerService.SetRolePausedAtError.Unexpected(throwable))
            }
            fireCancellable(
                gradeway,
                PlayerRolePauseEvent(actor, player, role, pausedAt),
                PlayerService.SetRolePausedAtError.Cancelled,
            )
            try {
                val before = playerRoleEntity.snapshot()
                playerRoleEntity.pausedAt = pausedAt
                audit.emit(PlayerRolePausedEvent(actor, player.id.value, role.id.value, pausedAt))
                playerRoleEntity.flush()
                audit.updated(playerRoleEntity, before)
            } catch (throwable: Throwable) {
                raise(PlayerService.SetRolePausedAtError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            PlayerRoleChangedPayload(player.id.value.toString(), role.id.value.toString(), MessagingAction.UPDATED)
        )
    }

    override fun setRolePausedAt(
        actor: Actor,
        playerIdOrName: String,
        roleId: UUID,
        pausedAt: Instant,
    ): Either<PlayerService.SetRolePausedAtError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.SetRolePausedAtError.EntityNotFound)
        return setRolePausedAt(actor, player, roleId, pausedAt)
    }

    override fun setRolePausedAt(
        actor: Actor,
        playerIdOrName: String,
        role: RoleEntity,
        pausedAt: Instant,
    ): Either<PlayerService.SetRolePausedAtError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.SetRolePausedAtError.EntityNotFound)
        return setRolePausedAt(actor, player, role, pausedAt)
    }

    override fun pauseRole(
        actor: Actor,
        playerId: UUID,
        roleId: UUID,
    ): Either<PlayerService.PauseRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.PauseRoleError.EntityNotFound)
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.PauseRoleError.TargetNotFound)
        return pauseRole(actor, player, role)
    }

    override fun pauseRole(
        actor: Actor,
        playerId: UUID,
        role: RoleEntity,
    ): Either<PlayerService.PauseRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.PauseRoleError.EntityNotFound)
        return pauseRole(actor, player, role)
    }

    override fun pauseRole(
        actor: Actor,
        player: PlayerEntity,
        roleId: UUID,
    ): Either<PlayerService.PauseRoleError, Unit> = either {
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.PauseRoleError.TargetNotFound)
        return pauseRole(actor, player, role)
    }

    override fun pauseRole(
        actor: Actor,
        player: PlayerEntity,
        role: RoleEntity,
    ): Either<PlayerService.PauseRoleError, Unit> = gradeway.audited(actor) { audit ->
        removeExpiredRoles(actor, player)

        transaction(gradeway.database) {
            val playerRoleEntity = player.roles.find { it.roleId == role.id }
            if (playerRoleEntity == null) {
                raise(PlayerService.PauseRoleError.RelationNotFound)
            }
            if (playerRoleEntity.pausedAt != null) {
                raise(PlayerService.PauseRoleError.AlreadyPaused)
            }
            if (playerRoleEntity !is DatabasePlayerRoleEntity) {
                val throwable = Throwable("Entity is not a type of DatabasePlayerRoleEntity")
                raise(PlayerService.PauseRoleError.Unexpected(throwable))
            }
            val pausedAt = gradeway.now()
            fireCancellable(
                gradeway,
                PlayerRolePauseEvent(actor, player, role, pausedAt),
                PlayerService.PauseRoleError.Cancelled,
            )
            try {
                val before = playerRoleEntity.snapshot()
                playerRoleEntity.pausedAt = pausedAt
                audit.emit(PlayerRolePausedEvent(actor, player.id.value, role.id.value, pausedAt))
                playerRoleEntity.flush()
                audit.updated(playerRoleEntity, before)
            } catch (throwable: Throwable) {
                raise(PlayerService.PauseRoleError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            PlayerRoleChangedPayload(player.id.value.toString(), role.id.value.toString(), MessagingAction.UPDATED)
        )
    }

    override fun pauseRole(
        actor: Actor,
        playerIdOrName: String,
        roleId: UUID,
    ): Either<PlayerService.PauseRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.PauseRoleError.EntityNotFound)
        return pauseRole(actor, player, roleId)
    }

    override fun pauseRole(
        actor: Actor,
        playerIdOrName: String,
        role: RoleEntity,
    ): Either<PlayerService.PauseRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.PauseRoleError.EntityNotFound)
        return pauseRole(actor, player, role)
    }

    override fun resumeRole(
        actor: Actor,
        playerId: UUID,
        roleId: UUID,
    ): Either<PlayerService.ResumeRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.ResumeRoleError.EntityNotFound)
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.ResumeRoleError.TargetNotFound)
        return resumeRole(actor, player, role)
    }

    override fun resumeRole(
        actor: Actor,
        playerId: UUID,
        role: RoleEntity,
    ): Either<PlayerService.ResumeRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.ResumeRoleError.EntityNotFound)
        return resumeRole(actor, player, role)
    }

    override fun resumeRole(
        actor: Actor,
        player: PlayerEntity,
        roleId: UUID,
    ): Either<PlayerService.ResumeRoleError, Unit> = either {
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.ResumeRoleError.TargetNotFound)
        return resumeRole(actor, player, role)
    }

    override fun resumeRole(
        actor: Actor,
        player: PlayerEntity,
        role: RoleEntity,
    ): Either<PlayerService.ResumeRoleError, Unit> = gradeway.audited(actor) { audit ->
        removeExpiredRoles(actor, player)

        transaction(gradeway.database) {
            val playerRoleEntity = player.roles.find { it.roleId == role.id }
            if (playerRoleEntity == null) {
                raise(PlayerService.ResumeRoleError.RelationNotFound)
            }

            val pausedAt = playerRoleEntity.pausedAt ?: raise(PlayerService.ResumeRoleError.NotPaused)

            if (playerRoleEntity !is DatabasePlayerRoleEntity) {
                val throwable = Throwable("Entity is not a type of DatabasePlayerRoleEntity")
                raise(PlayerService.ResumeRoleError.Unexpected(throwable))
            }

            fireCancellable(
                gradeway,
                PlayerRoleResumeEvent(actor, player, role),
                PlayerService.ResumeRoleError.Cancelled,
            )

            try {
                val before = playerRoleEntity.snapshot()
                val untilAt = playerRoleEntity.untilAt
                if (untilAt != null) {
                    playerRoleEntity.untilAt = untilAt + Duration.between(pausedAt, gradeway.now())
                }
                playerRoleEntity.pausedAt = null
                audit.emit(PlayerRoleResumedEvent(actor, player.id.value, role.id.value, playerRoleEntity.untilAt))
                playerRoleEntity.flush()
                audit.updated(playerRoleEntity, before)
            } catch (throwable: Throwable) {
                raise(PlayerService.ResumeRoleError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            PlayerRoleChangedPayload(player.id.value.toString(), role.id.value.toString(), MessagingAction.UPDATED)
        )
    }

    override fun resumeRole(
        actor: Actor,
        playerIdOrName: String,
        roleId: UUID,
    ): Either<PlayerService.ResumeRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.ResumeRoleError.EntityNotFound)
        return resumeRole(actor, player, roleId)
    }

    override fun resumeRole(
        actor: Actor,
        playerIdOrName: String,
        role: RoleEntity,
    ): Either<PlayerService.ResumeRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.ResumeRoleError.EntityNotFound)
        return resumeRole(actor, player, role)
    }

    override fun setPrimaryRole(
        actor: Actor,
        playerId: UUID,
        roleId: UUID,
    ): Either<PlayerService.SetPrimaryRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.SetPrimaryRoleError.EntityNotFound)
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.SetPrimaryRoleError.TargetNotFound)
        return setPrimaryRole(actor, player, role)
    }

    override fun setPrimaryRole(
        actor: Actor,
        playerId: UUID,
        role: RoleEntity,
    ): Either<PlayerService.SetPrimaryRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.SetPrimaryRoleError.EntityNotFound)
        return setPrimaryRole(actor, player, role)
    }

    override fun setPrimaryRole(
        actor: Actor,
        player: PlayerEntity,
        roleId: UUID,
    ): Either<PlayerService.SetPrimaryRoleError, Unit> = either {
        val role = gradeway.roles.findById(roleId) ?: raise(PlayerService.SetPrimaryRoleError.TargetNotFound)
        return setPrimaryRole(actor, player, role)
    }

    override fun setPrimaryRole(
        actor: Actor,
        player: PlayerEntity,
        role: RoleEntity,
    ): Either<PlayerService.SetPrimaryRoleError, Unit> = gradeway.audited(actor) { audit ->
        removeExpiredRoles(actor, player)

        if (player.primaryRoleId == role.id) {
            raise(PlayerService.SetPrimaryRoleError.AlreadyPrimary)
        }

        if (player !is DatabasePlayerEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePlayerEntity")
            raise(PlayerService.SetPrimaryRoleError.Unexpected(throwable))
        }
        fireCancellable(
            gradeway,
            PlayerPrimaryRoleChangeEvent(actor, player, role),
            PlayerService.SetPrimaryRoleError.Cancelled,
        )

        try {
            transaction(gradeway.database) {
                val before = player.snapshot()
                audit.emit(
                    PlayerPrimaryRoleChangedEvent(actor, player.id.value, player.primaryRoleId?.value, role.id.value)
                )
                player.primaryRoleId = role.id
                player.flush()
                audit.updated(player, before)
            }
        } catch (throwable: Throwable) {
            raise(PlayerService.SetPrimaryRoleError.Unexpected(throwable))
        }
    }

    override fun setPrimaryRole(
        actor: Actor,
        playerIdOrName: String,
        roleId: UUID,
    ): Either<PlayerService.SetPrimaryRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.SetPrimaryRoleError.EntityNotFound)
        return setPrimaryRole(actor, player, roleId)
    }

    override fun setPrimaryRole(
        actor: Actor,
        playerIdOrName: String,
        role: RoleEntity,
    ): Either<PlayerService.SetPrimaryRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.SetPrimaryRoleError.EntityNotFound)
        return setPrimaryRole(actor, player, role)
    }

    override fun clearPrimaryRole(
        actor: Actor,
        playerId: UUID,
    ): Either<PlayerService.ClearPrimaryRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.ClearPrimaryRoleError.EntityNotFound)
        return clearPrimaryRole(actor, player)
    }

    override fun clearPrimaryRole(
        actor: Actor,
        player: PlayerEntity,
    ): Either<PlayerService.ClearPrimaryRoleError, Unit> = gradeway.audited(actor) { audit ->
        if (player.primaryRoleId == null) {
            raise(PlayerService.ClearPrimaryRoleError.NoPrimaryRole)
        }

        if (player !is DatabasePlayerEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePlayerEntity")
            raise(PlayerService.ClearPrimaryRoleError.Unexpected(throwable))
        }
        fireCancellable(
            gradeway,
            PlayerPrimaryRoleChangeEvent(actor, player, null),
            PlayerService.ClearPrimaryRoleError.Cancelled,
        )

        try {
            transaction(gradeway.database) {
                val before = player.snapshot()
                audit.emit(PlayerPrimaryRoleChangedEvent(actor, player.id.value, player.primaryRoleId?.value, null))
                player.primaryRoleId = null
                player.flush()
                audit.updated(player, before)
            }
        } catch (throwable: Throwable) {
            raise(PlayerService.ClearPrimaryRoleError.Unexpected(throwable))
        }
    }

    override fun applyDefaultRole(
        actor: Actor,
        playerId: UUID,
        firstJoin: Boolean,
    ): Either<PlayerService.ApplyDefaultRoleError, RoleEntity?> = either {
        val config = gradeway.configs.gradewayEntry.config.defaultRole
        val player = findById(playerId) ?: raise(PlayerService.ApplyDefaultRoleError.EntityNotFound)

        val shouldAssign = (firstJoin && config.assignOnFirstJoin) ||
                (config.assignWhenNoPrimaryRole && player.primaryRoleId == null)
        if (!shouldAssign) {
            return@either null
        }

        val role = gradeway.roles.getDefaultRole() ?: return@either null

        addRole(actor, player, role).onLeft { error ->
            if (error is PlayerService.AddRoleError.Cancelled) {
                raise(PlayerService.ApplyDefaultRoleError.Cancelled)
            }
            if (error !is PlayerService.AddRoleError.AlreadyExists) {
                raise(PlayerService.ApplyDefaultRoleError.Unexpected(defaultRoleFailure("add", role, error)))
            }
        }
        setPrimaryRole(actor, player, role).onLeft { error ->
            if (error is PlayerService.SetPrimaryRoleError.Cancelled) {
                raise(PlayerService.ApplyDefaultRoleError.Cancelled)
            }
            if (error !is PlayerService.SetPrimaryRoleError.AlreadyPrimary) {
                raise(PlayerService.ApplyDefaultRoleError.Unexpected(defaultRoleFailure("set primary", role, error)))
            }
        }

        role
    }

    private fun defaultRoleFailure(action: String, role: RoleEntity, error: Any): Throwable =
        (error as? PlayerService.AddRoleError.Unexpected)?.throwable
            ?: (error as? PlayerService.SetPrimaryRoleError.Unexpected)?.throwable
            ?: IllegalStateException("Failed to $action default role ${role.id.value}: ${error::class.simpleName}")

    override fun clearPrimaryRole(
        actor: Actor,
        playerIdOrName: String,
    ): Either<PlayerService.ClearPrimaryRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.ClearPrimaryRoleError.EntityNotFound)
        return clearPrimaryRole(actor, player)
    }

    /**
     * Deletes the given already-expired player-role rows and returns the `(playerId, role)` pair for
     * each one removed. Single choke point for physically removing expired role assignments, which
     * emits a [PlayerRoleExpiredEvent] for each of them.
     */
    private fun deleteExpiredPlayerRoleRows(
        actor: Actor,
        audit: AuditRecorder,
        rows: List<DatabasePlayerRoleEntity>
    ): List<Pair<UUID, RoleEntity>> {
        return rows.map { playerRoleEntity ->
            val playerId = playerRoleEntity.playerId.value
            val role = playerRoleEntity.role
            audit.deleted(playerRoleEntity)
            audit.emit(
                PlayerRoleExpiredEvent(actor, playerId, role.id.value, playerRoleEntity.untilAt ?: gradeway.now())
            )
            playerRoleEntity.delete()

            gradeway.messaging.publish(
                PlayerRoleChangedPayload(playerId.toString(), role.id.value.toString(), MessagingAction.DELETED)
            )

            playerId to role
        }
    }

    override fun removeExpiredRoles(
        actor: Actor,
        playerId: UUID,
    ): Either<PlayerService.RemoveExpiredRolesError, List<RoleEntity>> = either {
        val player = findById(playerId) ?: raise(PlayerService.RemoveExpiredRolesError.EntityNotFound)
        return removeExpiredRoles(actor, player)
    }

    override fun removeExpiredRoles(
        actor: Actor,
        player: PlayerEntity,
    ): Either<PlayerService.RemoveExpiredRolesError, List<RoleEntity>> = gradeway.audited(actor) { audit ->
        transaction(gradeway.database) {
            val expiredPlayerRoleEntities = player.roles
                .filter { it.pausedAt == null && it.untilAt != null && it.untilAt!! <= gradeway.now() }
                .filterIsInstance<DatabasePlayerRoleEntity>()

            try {
                deleteExpiredPlayerRoleRows(actor, audit, expiredPlayerRoleEntities).map { it.second }
            } catch (throwable: Throwable) {
                raise(PlayerService.RemoveExpiredRolesError.Unexpected(throwable))
            }
        }
    }

    override fun removeExpiredRoles(
        actor: Actor,
        playerIdOrName: String,
    ): Either<PlayerService.RemoveExpiredRolesError, List<RoleEntity>> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.RemoveExpiredRolesError.EntityNotFound)
        return removeExpiredRoles(actor, player)
    }

    override fun removeExpiredRoles(
        actor: Actor,
        playerIds: Collection<UUID>,
    ): Either<PlayerService.RemoveExpiredRolesError, List<Pair<UUID, RoleEntity>>> = gradeway.audited(actor) { audit ->
        if (playerIds.isEmpty()) {
            return@audited emptyList()
        }

        transaction(gradeway.database) {
            val expiredPlayerRoleEntities = DatabasePlayerRoleEntity.find {
                (PlayerRolesTable.playerId inList playerIds) and
                        PlayerRolesTable.pausedAt.isNull() and
                        (PlayerRolesTable.untilAt less gradeway.now())
            }.toList()

            try {
                deleteExpiredPlayerRoleRows(actor, audit, expiredPlayerRoleEntities)
            } catch (throwable: Throwable) {
                raise(PlayerService.RemoveExpiredRolesError.Unexpected(throwable))
            }
        }
    }

    override fun removeExpiredRoles(
        actor: Actor,
    ): Either<PlayerService.RemoveExpiredRolesError, List<Pair<UUID, RoleEntity>>> =
        gradeway.audited(actor) { audit ->
            transaction(gradeway.database) {
                val expiredPlayerRoleEntities = DatabasePlayerRoleEntity.find {
                    PlayerRolesTable.pausedAt.isNull() and (PlayerRolesTable.untilAt less gradeway.now())
                }.toList()

                try {
                    deleteExpiredPlayerRoleRows(actor, audit, expiredPlayerRoleEntities)
                } catch (throwable: Throwable) {
                    raise(PlayerService.RemoveExpiredRolesError.Unexpected(throwable))
                }
            }
        }

    override fun <TValue : Any> addAttribute(
        actor: Actor,
        id: UUID,
        attribute: Attribute<TValue>,
    ) = gradeway.attributes.addPlayerAttribute(actor, id, attribute)

    override fun <TValue : Any> addAttribute(
        actor: Actor,
        entity: PlayerEntity,
        attribute: Attribute<TValue>,
    ) = gradeway.attributes.addPlayerAttribute(actor, entity, attribute)

    override fun <TValue : Any> addAttribute(
        actor: Actor,
        idOrName: String,
        attribute: Attribute<TValue>,
    ) = gradeway.attributes.addPlayerAttribute(actor, idOrName, attribute)

    override fun <TValue : Any> updateAttribute(
        actor: Actor,
        id: UUID,
        key: Key,
        value: TValue,
    ) = gradeway.attributes.updatePlayerAttribute(actor, id, key, value)

    override fun <TValue : Any> updateAttribute(
        actor: Actor,
        entity: PlayerEntity,
        key: Key,
        value: TValue,
    ) = gradeway.attributes.updatePlayerAttribute(actor, entity, key, value)

    override fun <TValue : Any> updateAttribute(
        actor: Actor,
        idOrName: String,
        key: Key,
        value: TValue,
    ) = gradeway.attributes.updatePlayerAttribute(actor, idOrName, key, value)

    override fun removeAttribute(
        actor: Actor,
        id: UUID,
        key: Key,
    ) = gradeway.attributes.removePlayerAttribute(actor, id, key)

    override fun removeAttribute(
        actor: Actor,
        entity: PlayerEntity,
        key: Key,
    ) = gradeway.attributes.removePlayerAttribute(actor, entity, key)

    override fun removeAttribute(
        actor: Actor,
        idOrName: String,
        key: Key,
    ) = gradeway.attributes.removePlayerAttribute(actor, idOrName, key)

    override fun clearAttributes(
        actor: Actor,
        id: UUID,
    ) = gradeway.attributes.clearPlayerAttributes(actor, id)

    override fun clearAttributes(
        actor: Actor,
        entity: PlayerEntity,
    ) = gradeway.attributes.clearPlayerAttributes(actor, entity)

    override fun clearAttributes(
        actor: Actor,
        idOrName: String,
    ) = gradeway.attributes.clearPlayerAttributes(actor, idOrName)

    override fun hasAttribute(id: UUID, key: Key) =
        gradeway.attributes.hasPlayerAttribute(id, key)

    override fun hasAttribute(entity: PlayerEntity, key: Key) =
        gradeway.attributes.hasPlayerAttribute(entity, key)

    override fun hasAttribute(idOrName: String, key: Key) =
        gradeway.attributes.hasPlayerAttribute(idOrName, key)

    override fun getAttribute(id: UUID, key: Key) =
        gradeway.attributes.getPlayerAttribute(id, key)

    override fun getAttribute(entity: PlayerEntity, key: Key) =
        gradeway.attributes.getPlayerAttribute(entity, key)

    override fun getAttribute(idOrName: String, key: Key) =
        gradeway.attributes.getPlayerAttribute(idOrName, key)

    override fun setPermission(
        actor: Actor,
        id: UUID,
        permission: String,
        enabled: Boolean,
    ) = gradeway.permissions.setPlayerPermission(actor, id, permission, enabled)

    override fun setPermission(
        actor: Actor,
        entity: PlayerEntity,
        permission: String,
        enabled: Boolean,
    ) = gradeway.permissions.setPlayerPermission(actor, entity, permission, enabled)

    override fun setPermission(
        actor: Actor,
        idOrName: String,
        permission: String,
        enabled: Boolean,
    ) = gradeway.permissions.setPlayerPermission(actor, idOrName, permission, enabled)

    override fun setPermissions(
        actor: Actor,
        id: UUID,
        permissions: Map<String, Boolean>,
    ) = gradeway.permissions.setPlayerPermissions(actor, id, permissions)

    override fun setPermissions(
        actor: Actor,
        entity: PlayerEntity,
        permissions: Map<String, Boolean>,
    ) = gradeway.permissions.setPlayerPermissions(actor, entity, permissions)

    override fun setPermissions(
        actor: Actor,
        idOrName: String,
        permissions: Map<String, Boolean>,
    ) = gradeway.permissions.setPlayerPermissions(actor, idOrName, permissions)

    override fun unsetPermission(
        actor: Actor,
        id: UUID,
        permission: String,
    ) = gradeway.permissions.unsetPlayerPermission(actor, id, permission)

    override fun unsetPermission(
        actor: Actor,
        entity: PlayerEntity,
        permission: String,
    ) = gradeway.permissions.unsetPlayerPermission(actor, entity, permission)

    override fun unsetPermission(
        actor: Actor,
        idOrName: String,
        permission: String,
    ) = gradeway.permissions.unsetPlayerPermission(actor, idOrName, permission)

    override fun unsetPermissions(
        actor: Actor,
        id: UUID,
        permissions: List<String>,
    ) = gradeway.permissions.unsetPlayerPermissions(actor, id, permissions)

    override fun unsetPermissions(
        actor: Actor,
        entity: PlayerEntity,
        permissions: List<String>,
    ) = gradeway.permissions.unsetPlayerPermissions(actor, entity, permissions)

    override fun unsetPermissions(
        actor: Actor,
        idOrName: String,
        permissions: List<String>,
    ) = gradeway.permissions.unsetPlayerPermissions(actor, idOrName, permissions)

    override fun clearPermissions(
        actor: Actor,
        id: UUID,
    ) = gradeway.permissions.clearPlayerPermissions(actor, id)

    override fun clearPermissions(
        actor: Actor,
        entity: PlayerEntity,
    ) = gradeway.permissions.clearPlayerPermissions(actor, entity)

    override fun clearPermissions(
        actor: Actor,
        idOrName: String,
    ) = gradeway.permissions.clearPlayerPermissions(actor, idOrName)

    override fun hasPermission(
        id: UUID,
        permission: String,
    ) = gradeway.permissions.hasPlayerPermission(id, permission)

    override fun hasPermission(
        entity: PlayerEntity,
        permission: String,
    ) = gradeway.permissions.hasPlayerPermission(entity, permission)

    override fun hasPermission(
        idOrName: String,
        permission: String,
    ) = gradeway.permissions.hasPlayerPermission(idOrName, permission)

    override fun hasAnyPermissions(
        id: UUID,
        permissions: List<String>,
    ) = gradeway.permissions.hasPlayerAnyPermissions(id, permissions)

    override fun hasAnyPermissions(
        entity: PlayerEntity,
        permissions: List<String>,
    ) = gradeway.permissions.hasPlayerAnyPermissions(entity, permissions)

    override fun hasAnyPermissions(
        idOrName: String,
        permissions: List<String>,
    ) = gradeway.permissions.hasPlayerAnyPermissions(idOrName, permissions)

    override fun hasAllPermissions(
        id: UUID,
        permissions: List<String>,
    ) = gradeway.permissions.hasPlayerAllPermissions(id, permissions)

    override fun hasAllPermissions(
        entity: PlayerEntity,
        permissions: List<String>,
    ) = gradeway.permissions.hasPlayerAllPermissions(entity, permissions)

    override fun hasAllPermissions(
        idOrName: String,
        permissions: List<String>,
    ) = gradeway.permissions.hasPlayerAllPermissions(idOrName, permissions)

    override fun getPermissions(
        id: UUID,
    ) = gradeway.permissions.getPlayerPermissions(id)

    override fun getPermissions(
        entity: PlayerEntity,
    ) = gradeway.permissions.getPlayerPermissions(entity)

    override fun getPermissions(
        idOrName: String,
    ) = gradeway.permissions.getPlayerPermissions(idOrName)
}
