/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerEntity
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerRoleEntity
import dev.gradienttim.gradeway.database.models.player.PlayerRolesTable
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.player.PlayerRoleEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.extensions.eqAsStr
import dev.gradienttim.gradeway.extensions.isValidName
import dev.gradienttim.gradeway.messaging.payloads.*
import dev.gradienttim.gradeway.platform.CommonCaches
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.Instant
import java.util.*

@Suppress("LargeClass", "TooManyFunctions")
class CommonPlayerService(val gradeway: CommonGradeway) : PlayerService, KoinComponent {
    private val roleService: RoleService by inject()
    private val attributeService: AttributeService by inject()
    private val permissionService: PermissionService by inject()

    init {
        gradeway.messaging.subscribe { payload -> invalidateWeightFor(payload) }
    }

    override fun create(
        id: UUID,
        name: String
    ): Either<PlayerService.CreatePlayerError, DatabasePlayerEntity> = either {
        if (!name.isValidName(TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH)) {
            raise(PlayerService.CreatePlayerError.InvalidName)
        }
        if (existsById(id)) {
            raise(PlayerService.CreatePlayerError.EntityAlreadyExists)
        }
        try {
            transaction(gradeway.database) {
                DatabasePlayerEntity.new(id) {
                    this.name = name
                }
            }
        } catch (throwable: Throwable) {
            raise(PlayerService.CreatePlayerError.Unexpected(throwable))
        }
    }

    override fun delete(id: UUID): Either<PlayerService.DeletePlayerError, Unit> = either {
        val entity = findById(id) ?: raise(PlayerService.DeletePlayerError.EntityNotFound)
        try {
            transaction(gradeway.database) {
                entity.delete()
            }
        } catch (throwable: Throwable) {
            raise(PlayerService.DeletePlayerError.Unexpected(throwable))
        }
    }

    override fun setName(id: UUID, name: String): Either<PlayerService.SetNameError, Boolean> = either {
        val entity = findById(id) ?: raise(PlayerService.SetNameError.EntityNotFound)
        return setName(entity, name)
    }

    override fun setName(
        entity: PlayerEntity,
        name: String
    ): Either<PlayerService.SetNameError, Boolean> = either {
        if (!name.isValidName(TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH)) {
            raise(PlayerService.SetNameError.InvalidName)
        }
        if (entity !is DatabasePlayerEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePlayerEntity")
            raise(PlayerService.SetNameError.Unexpected(throwable))
        }
        try {
            transaction(gradeway.database) {
                entity.name = name
                entity.flush()
            }
        } catch (throwable: Throwable) {
            raise(PlayerService.SetNameError.Unexpected(throwable))
        }
    }

    override fun setWeight(id: UUID, weight: Int): Either<PlayerService.SetWeightError, Boolean> = either {
        val entity = findById(id) ?: raise(PlayerService.SetWeightError.EntityNotFound)
        return setWeight(entity, weight)
    }

    override fun setWeight(
        entity: PlayerEntity,
        weight: Int
    ): Either<PlayerService.SetWeightError, Boolean> = either {
        if (entity !is DatabasePlayerEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePlayerEntity")
            raise(PlayerService.SetWeightError.Unexpected(throwable))
        }
        try {
            transaction(gradeway.database) {
                entity.weight = weight
                entity.flush()
            }
        } catch (throwable: Throwable) {
            raise(PlayerService.SetWeightError.Unexpected(throwable))
        }
    }

    override fun setWeight(idOrName: String, weight: Int): Either<PlayerService.SetWeightError, Boolean> = either {
        val entity = findByIdOrName(idOrName) ?: raise(PlayerService.SetWeightError.EntityNotFound)
        return setWeight(entity, weight)
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

    private fun invalidatePlayerWeight(rawPlayerId: String) {
        val playerId = runCatching { UUID.fromString(rawPlayerId) }.getOrNull() ?: return
        gradeway.caches.playerEffectiveWeights.invalidate(playerId)
    }

    override fun findById(id: UUID): DatabasePlayerEntity? {
        return transaction(gradeway.database) {
            DatabasePlayerEntity.findById(id)
        }
    }

    override fun findByName(name: String): DatabasePlayerEntity? {
        if (!name.isValidName(TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH)) {
            return null
        }
        return transaction(gradeway.database) {
            DatabasePlayerEntity.find { PlayersTable.name eq name }.limit(1).firstOrNull()
        }
    }

    override fun findByIdOrName(value: String): DatabasePlayerEntity? {
        if (
            value.length <= TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH &&
            !value.isValidName(TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH)
        ) {
            return null
        }
        return transaction(gradeway.database) {
            DatabasePlayerEntity.find {
                (PlayersTable.id eqAsStr value) or (PlayersTable.name eq value)
            }.limit(1).firstOrNull()
        }
    }

    override fun existsById(id: UUID): Boolean =
        findById(id) != null

    override fun existsByName(name: String): Boolean =
        findByName(name) != null

    override fun existsByIdOrName(value: String): Boolean =
        findByIdOrName(value) != null

    override fun getPrimaryRole(id: UUID): RoleEntity? {
        val entity = findById(id) ?: return null
        return getPrimaryRole(entity)
    }

    override fun getPrimaryRole(entity: PlayerEntity): RoleEntity? = entity.primaryRole

    override fun addRole(
        playerId: UUID,
        roleId: UUID,
        until: Instant?
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity> = either {
        val player = findById(playerId) ?: raise(PlayerService.AddRoleError.EntityNotFound)
        val role = roleService.findById(roleId) ?: raise(PlayerService.AddRoleError.TargetNotFound)
        return addRole(player, role, until)
    }

    override fun addRole(
        playerId: UUID,
        role: RoleEntity,
        until: Instant?
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity> = either {
        val player = findById(playerId) ?: raise(PlayerService.AddRoleError.EntityNotFound)
        return addRole(player, role, until)
    }

    override fun addRole(
        player: PlayerEntity,
        roleId: UUID,
        until: Instant?
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity> = either {
        val role = roleService.findById(roleId) ?: raise(PlayerService.AddRoleError.TargetNotFound)
        return addRole(player, role, until)
    }

    override fun addRole(
        player: PlayerEntity,
        role: RoleEntity,
        until: Instant?
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity> = either {
        removeExpiredRoles(player)

        if (until != null && until < gradeway.now()) {
            raise(PlayerService.AddRoleError.UntilInPast)
        }

        transaction(gradeway.database) {
            if (player.roles.any { it.roleId == role.id }) {
                raise(PlayerService.AddRoleError.AlreadyExists)
            }

            try {
                DatabasePlayerRoleEntity.new {
                    this.roleId = role.id
                    this.playerId = player.id
                    this.untilAt = until
                }
            } catch (throwable: Throwable) {
                raise(PlayerService.AddRoleError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            PlayerRoleChangedPayload(player.id.value.toString(), role.id.value.toString(), MessagingAction.CREATED)
        )
    }

    override fun addRole(
        playerIdOrName: String,
        roleId: UUID,
        untilAt: Instant?
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.AddRoleError.EntityNotFound)
        return addRole(player, roleId, untilAt)
    }

    override fun addRole(
        playerIdOrName: String,
        role: RoleEntity,
        untilAt: Instant?
    ): Either<PlayerService.AddRoleError, PlayerRoleEntity> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.AddRoleError.EntityNotFound)
        return addRole(player, role, untilAt)
    }

    override fun removeRole(
        playerId: UUID,
        roleId: UUID
    ): Either<PlayerService.RemoveRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.RemoveRoleError.EntityNotFound)
        val role = roleService.findById(roleId) ?: raise(PlayerService.RemoveRoleError.TargetNotFound)
        return removeRole(player, role)
    }

    override fun removeRole(
        playerId: UUID,
        role: RoleEntity
    ): Either<PlayerService.RemoveRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.RemoveRoleError.EntityNotFound)
        return removeRole(player, role)
    }

    override fun removeRole(
        player: PlayerEntity,
        roleId: UUID
    ): Either<PlayerService.RemoveRoleError, Unit> = either {
        val role = roleService.findById(roleId) ?: raise(PlayerService.RemoveRoleError.TargetNotFound)
        return removeRole(player, role)
    }

    override fun removeRole(
        player: PlayerEntity,
        role: RoleEntity
    ): Either<PlayerService.RemoveRoleError, Unit> = either {
        removeExpiredRoles(player)

        transaction(gradeway.database) {
            val playerRoleEntity = player.roles.find { it.roleId == role.id }
            if (playerRoleEntity == null) {
                raise(PlayerService.RemoveRoleError.NotExists)
            }
            if (playerRoleEntity !is DatabasePlayerRoleEntity) {
                val throwable = Throwable("Entity is not a type of DatabasePlayerRoleEntity")
                raise(PlayerService.RemoveRoleError.Unexpected(throwable))
            }
            try {
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
        playerIdOrName: String,
        roleId: UUID
    ): Either<PlayerService.RemoveRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.RemoveRoleError.EntityNotFound)
        return removeRole(player, roleId)
    }

    override fun removeRole(
        playerIdOrName: String,
        role: RoleEntity
    ): Either<PlayerService.RemoveRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.RemoveRoleError.EntityNotFound)
        return removeRole(player, role)
    }

    override fun setRoleUntilAt(
        playerId: UUID,
        roleId: UUID,
        untilAt: Instant
    ): Either<PlayerService.SetRoleUntilAtError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.SetRoleUntilAtError.EntityNotFound)
        val role = roleService.findById(roleId) ?: raise(PlayerService.SetRoleUntilAtError.TargetNotFound)
        return setRoleUntilAt(player, role, untilAt)
    }

    override fun setRoleUntilAt(
        player: PlayerEntity,
        roleId: UUID,
        untilAt: Instant
    ): Either<PlayerService.SetRoleUntilAtError, Unit> = either {
        val role = roleService.findById(roleId) ?: raise(PlayerService.SetRoleUntilAtError.TargetNotFound)
        return setRoleUntilAt(player, role, untilAt)
    }

    override fun setRoleUntilAt(
        playerId: UUID,
        role: RoleEntity,
        untilAt: Instant
    ): Either<PlayerService.SetRoleUntilAtError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.SetRoleUntilAtError.EntityNotFound)
        return setRoleUntilAt(player, role, untilAt)
    }

    override fun setRoleUntilAt(
        player: PlayerEntity,
        role: RoleEntity,
        untilAt: Instant
    ): Either<PlayerService.SetRoleUntilAtError, Unit> = either<PlayerService.SetRoleUntilAtError, Unit> {
        removeExpiredRoles(player)

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
            try {
                playerRoleEntity.untilAt = untilAt
                playerRoleEntity.flush()
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
        playerIdOrName: String,
        roleId: UUID,
        untilAt: Instant
    ): Either<PlayerService.SetRoleUntilAtError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.SetRoleUntilAtError.EntityNotFound)
        return setRoleUntilAt(player, roleId, untilAt)
    }

    override fun setRoleUntilAt(
        playerIdOrName: String,
        role: RoleEntity,
        untilAt: Instant
    ): Either<PlayerService.SetRoleUntilAtError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.SetRoleUntilAtError.EntityNotFound)
        return setRoleUntilAt(player, role, untilAt)
    }

    override fun setRolePausedAt(
        playerId: UUID,
        roleId: UUID,
        pausedAt: Instant
    ): Either<PlayerService.SetRolePausedAtError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.SetRolePausedAtError.EntityNotFound)
        val role = roleService.findById(roleId) ?: raise(PlayerService.SetRolePausedAtError.TargetNotFound)
        return setRolePausedAt(player, role, pausedAt)
    }

    override fun setRolePausedAt(
        playerId: UUID,
        role: RoleEntity,
        pausedAt: Instant
    ): Either<PlayerService.SetRolePausedAtError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.SetRolePausedAtError.EntityNotFound)
        return setRolePausedAt(player, role, pausedAt)
    }

    override fun setRolePausedAt(
        player: PlayerEntity,
        roleId: UUID,
        pausedAt: Instant
    ): Either<PlayerService.SetRolePausedAtError, Unit> = either {
        val role = roleService.findById(roleId) ?: raise(PlayerService.SetRolePausedAtError.TargetNotFound)
        return setRolePausedAt(player, role, pausedAt)
    }

    override fun setRolePausedAt(
        player: PlayerEntity,
        role: RoleEntity,
        pausedAt: Instant
    ): Either<PlayerService.SetRolePausedAtError, Unit> = either<PlayerService.SetRolePausedAtError, Unit> {
        removeExpiredRoles(player)

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
            try {
                playerRoleEntity.pausedAt = pausedAt
                playerRoleEntity.flush()
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
        playerIdOrName: String,
        roleId: UUID,
        pausedAt: Instant
    ): Either<PlayerService.SetRolePausedAtError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.SetRolePausedAtError.EntityNotFound)
        return setRolePausedAt(player, roleId, pausedAt)
    }

    override fun setRolePausedAt(
        playerIdOrName: String,
        role: RoleEntity,
        pausedAt: Instant
    ): Either<PlayerService.SetRolePausedAtError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.SetRolePausedAtError.EntityNotFound)
        return setRolePausedAt(player, role, pausedAt)
    }

    override fun pauseRole(
        playerId: UUID,
        roleId: UUID
    ): Either<PlayerService.PauseRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.PauseRoleError.EntityNotFound)
        val role = roleService.findById(roleId) ?: raise(PlayerService.PauseRoleError.TargetNotFound)
        return pauseRole(player, role)
    }

    override fun pauseRole(
        playerId: UUID,
        role: RoleEntity
    ): Either<PlayerService.PauseRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.PauseRoleError.EntityNotFound)
        return pauseRole(player, role)
    }

    override fun pauseRole(
        player: PlayerEntity,
        roleId: UUID
    ): Either<PlayerService.PauseRoleError, Unit> = either {
        val role = roleService.findById(roleId) ?: raise(PlayerService.PauseRoleError.TargetNotFound)
        return pauseRole(player, role)
    }

    override fun pauseRole(
        player: PlayerEntity,
        role: RoleEntity
    ): Either<PlayerService.PauseRoleError, Unit> = either<PlayerService.PauseRoleError, Unit> {
        removeExpiredRoles(player)

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
            try {
                playerRoleEntity.pausedAt = gradeway.now()
                playerRoleEntity.flush()
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
        playerIdOrName: String,
        roleId: UUID
    ): Either<PlayerService.PauseRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.PauseRoleError.EntityNotFound)
        return pauseRole(player, roleId)
    }

    override fun pauseRole(
        playerIdOrName: String,
        role: RoleEntity
    ): Either<PlayerService.PauseRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.PauseRoleError.EntityNotFound)
        return pauseRole(player, role)
    }

    override fun resumeRole(
        playerId: UUID,
        roleId: UUID
    ): Either<PlayerService.ResumeRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.ResumeRoleError.EntityNotFound)
        val role = roleService.findById(roleId) ?: raise(PlayerService.ResumeRoleError.TargetNotFound)
        return resumeRole(player, role)
    }

    override fun resumeRole(
        playerId: UUID,
        role: RoleEntity
    ): Either<PlayerService.ResumeRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.ResumeRoleError.EntityNotFound)
        return resumeRole(player, role)
    }

    override fun resumeRole(
        player: PlayerEntity,
        roleId: UUID
    ): Either<PlayerService.ResumeRoleError, Unit> = either {
        val role = roleService.findById(roleId) ?: raise(PlayerService.ResumeRoleError.TargetNotFound)
        return resumeRole(player, role)
    }

    override fun resumeRole(
        player: PlayerEntity,
        role: RoleEntity
    ): Either<PlayerService.ResumeRoleError, Unit> = either<PlayerService.ResumeRoleError, Unit> {
        removeExpiredRoles(player)

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

            try {
                val untilAt = playerRoleEntity.untilAt
                if (untilAt != null) {
                    playerRoleEntity.untilAt = untilAt + Duration.between(pausedAt, gradeway.now())
                }
                playerRoleEntity.pausedAt = null
                playerRoleEntity.flush()
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
        playerIdOrName: String,
        roleId: UUID
    ): Either<PlayerService.ResumeRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.ResumeRoleError.TargetNotFound)
        return resumeRole(player, roleId)
    }

    override fun resumeRole(
        playerIdOrName: String,
        role: RoleEntity
    ): Either<PlayerService.ResumeRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.ResumeRoleError.TargetNotFound)
        return resumeRole(player, role)
    }

    override fun setPrimaryRole(
        playerId: UUID,
        roleId: UUID
    ): Either<PlayerService.SetPrimaryRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.SetPrimaryRoleError.EntityNotFound)
        val role = roleService.findById(roleId) ?: raise(PlayerService.SetPrimaryRoleError.TargetNotFound)
        return setPrimaryRole(player, role)
    }

    override fun setPrimaryRole(
        playerId: UUID,
        role: RoleEntity
    ): Either<PlayerService.SetPrimaryRoleError, Unit> = either {
        val player = findById(playerId) ?: raise(PlayerService.SetPrimaryRoleError.EntityNotFound)
        return setPrimaryRole(player, role)
    }

    override fun setPrimaryRole(
        player: PlayerEntity,
        roleId: UUID
    ): Either<PlayerService.SetPrimaryRoleError, Unit> = either {
        val role = roleService.findById(roleId) ?: raise(PlayerService.SetPrimaryRoleError.TargetNotFound)
        return setPrimaryRole(player, role)
    }

    override fun setPrimaryRole(
        player: PlayerEntity,
        role: RoleEntity
    ): Either<PlayerService.SetPrimaryRoleError, Unit> = either {
        removeExpiredRoles(player)

        if (player.primaryRoleId == role.id) {
            raise(PlayerService.SetPrimaryRoleError.AlreadyPrimary)
        }

        if (player !is DatabasePlayerEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePlayerEntity")
            raise(PlayerService.SetPrimaryRoleError.Unexpected(throwable))
        }

        transaction(gradeway.database) {
            try {
                player.primaryRoleId = role.id
                player.flush()
            } catch (throwable: Throwable) {
                raise(PlayerService.SetPrimaryRoleError.Unexpected(throwable))
            }
        }
    }

    override fun setPrimaryRole(
        playerIdOrName: String,
        roleId: UUID
    ): Either<PlayerService.SetPrimaryRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.SetPrimaryRoleError.EntityNotFound)
        return setPrimaryRole(player, roleId)
    }

    override fun setPrimaryRole(
        playerIdOrName: String,
        role: RoleEntity
    ): Either<PlayerService.SetPrimaryRoleError, Unit> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.SetPrimaryRoleError.EntityNotFound)
        return setPrimaryRole(player, role)
    }

    /**
     * Deletes the given already-expired player-role rows and returns the `(playerId, role)` pair for
     * each one removed. Single choke point for physically removing expired role assignments, so a
     * future "player lost role" event dispatch only needs to be wired in here.
     */
    private fun deleteExpiredPlayerRoleRows(
        rows: List<DatabasePlayerRoleEntity>
    ): List<Pair<UUID, RoleEntity>> {
        return rows.map { playerRoleEntity ->
            val playerId = playerRoleEntity.playerId.value
            val role = playerRoleEntity.role
            playerRoleEntity.delete()

            gradeway.messaging.publish(
                PlayerRoleChangedPayload(playerId.toString(), role.id.value.toString(), MessagingAction.DELETED)
            )

            playerId to role
        }
    }

    override fun removeExpiredRoles(
        playerId: UUID
    ): Either<PlayerService.RemoveExpiredRolesError, List<RoleEntity>> = either {
        val player = findById(playerId) ?: raise(PlayerService.RemoveExpiredRolesError.EntityNotFound)
        return removeExpiredRoles(player)
    }

    override fun removeExpiredRoles(
        player: PlayerEntity
    ): Either<PlayerService.RemoveExpiredRolesError, List<RoleEntity>> = either {
        transaction(gradeway.database) {
            val expiredPlayerRoleEntities = player.roles
                .filter { it.pausedAt == null && it.untilAt != null && it.untilAt!! <= gradeway.now() }
                .filterIsInstance<DatabasePlayerRoleEntity>()

            try {
                deleteExpiredPlayerRoleRows(expiredPlayerRoleEntities).map { it.second }
            } catch (throwable: Throwable) {
                raise(PlayerService.RemoveExpiredRolesError.Unexpected(throwable))
            }
        }
    }

    override fun removeExpiredRoles(
        playerIdOrName: String
    ): Either<PlayerService.RemoveExpiredRolesError, List<RoleEntity>> = either {
        val player = findByIdOrName(playerIdOrName) ?: raise(PlayerService.RemoveExpiredRolesError.EntityNotFound)
        return removeExpiredRoles(player)
    }

    override fun removeExpiredRoles(
        playerIds: Collection<UUID>
    ): Either<PlayerService.RemoveExpiredRolesError, List<Pair<UUID, RoleEntity>>> = either {
        if (playerIds.isEmpty()) {
            return@either emptyList()
        }

        transaction(gradeway.database) {
            val expiredPlayerRoleEntities = DatabasePlayerRoleEntity.find {
                (PlayerRolesTable.playerId inList playerIds) and
                    PlayerRolesTable.pausedAt.isNull() and
                    (PlayerRolesTable.untilAt less gradeway.now())
            }.toList()

            try {
                deleteExpiredPlayerRoleRows(expiredPlayerRoleEntities)
            } catch (throwable: Throwable) {
                raise(PlayerService.RemoveExpiredRolesError.Unexpected(throwable))
            }
        }
    }

    override fun <TValue : Any> addAttribute(id: UUID, attribute: Attribute<TValue>) =
        attributeService.addPlayerAttribute(id, attribute)

    override fun <TValue : Any> addAttribute(entity: PlayerEntity, attribute: Attribute<TValue>) =
        attributeService.addPlayerAttribute(entity, attribute)

    override fun <TValue : Any> addAttribute(idOrName: String, attribute: Attribute<TValue>) =
        attributeService.addPlayerAttribute(idOrName, attribute)

    override fun <TValue : Any> updateAttribute(id: UUID, key: Key, value: TValue) =
        attributeService.updatePlayerAttribute(id, key, value)

    override fun <TValue : Any> updateAttribute(entity: PlayerEntity, key: Key, value: TValue) =
        attributeService.updatePlayerAttribute(entity, key, value)

    override fun <TValue : Any> updateAttribute(idOrName: String, key: Key, value: TValue) =
        attributeService.updatePlayerAttribute(idOrName, key, value)

    override fun removeAttribute(id: UUID, key: Key) =
        attributeService.removePlayerAttribute(id, key)

    override fun removeAttribute(entity: PlayerEntity, key: Key) =
        attributeService.removePlayerAttribute(entity, key)

    override fun removeAttribute(idOrName: String, key: Key) =
        attributeService.removePlayerAttribute(idOrName, key)

    override fun clearAttributes(id: UUID) =
        attributeService.clearPlayerAttributes(id)

    override fun clearAttributes(entity: PlayerEntity) =
        attributeService.clearPlayerAttributes(entity)

    override fun clearAttributes(idOrName: String) =
        attributeService.clearPlayerAttributes(idOrName)

    override fun hasAttribute(id: UUID, key: Key) =
        attributeService.hasPlayerAttribute(id, key)

    override fun hasAttribute(entity: PlayerEntity, key: Key) =
        attributeService.hasPlayerAttribute(entity, key)

    override fun hasAttribute(idOrName: String, key: Key) =
        attributeService.hasPlayerAttribute(idOrName, key)

    override fun getAttribute(id: UUID, key: Key) =
        attributeService.getPlayerAttribute(id, key)

    override fun getAttribute(entity: PlayerEntity, key: Key) =
        attributeService.getPlayerAttribute(entity, key)

    override fun getAttribute(idOrName: String, key: Key) =
        attributeService.getPlayerAttribute(idOrName, key)

    override fun setPermission(id: UUID, permission: String, enabled: Boolean) =
        permissionService.setPlayerPermission(id, permission, enabled)

    override fun setPermission(entity: PlayerEntity, permission: String, enabled: Boolean) =
        permissionService.setPlayerPermission(entity, permission, enabled)

    override fun setPermission(idOrName: String, permission: String, enabled: Boolean) =
        permissionService.setPlayerPermission(idOrName, permission, enabled)

    override fun setPermissions(id: UUID, permissions: Map<String, Boolean>) =
        permissionService.setPlayerPermissions(id, permissions)

    override fun setPermissions(entity: PlayerEntity, permissions: Map<String, Boolean>) =
        permissionService.setPlayerPermissions(entity, permissions)

    override fun setPermissions(idOrName: String, permissions: Map<String, Boolean>) =
        permissionService.setPlayerPermissions(idOrName, permissions)

    override fun unsetPermission(id: UUID, permission: String) =
        permissionService.unsetPlayerPermission(id, permission)

    override fun unsetPermission(entity: PlayerEntity, permission: String) =
        permissionService.unsetPlayerPermission(entity, permission)

    override fun unsetPermission(idOrName: String, permission: String) =
        permissionService.unsetPlayerPermission(idOrName, permission)

    override fun unsetPermissions(id: UUID, permissions: List<String>) =
        permissionService.unsetPlayerPermissions(id, permissions)

    override fun unsetPermissions(entity: PlayerEntity, permissions: List<String>) =
        permissionService.unsetPlayerPermissions(entity, permissions)

    override fun unsetPermissions(idOrName: String, permissions: List<String>) =
        permissionService.unsetPlayerPermissions(idOrName, permissions)

    override fun clearPermissions(id: UUID) =
        permissionService.clearPlayerPermissions(id)

    override fun clearPermissions(entity: PlayerEntity) =
        permissionService.clearPlayerPermissions(entity)

    override fun clearPermissions(idOrName: String) =
        permissionService.clearPlayerPermissions(idOrName)

    override fun hasPermission(id: UUID, permission: String) =
        permissionService.hasPlayerPermission(id, permission)

    override fun hasPermission(entity: PlayerEntity, permission: String) =
        permissionService.hasPlayerPermission(entity, permission)

    override fun hasPermission(idOrName: String, permission: String) =
        permissionService.hasPlayerPermission(idOrName, permission)

    override fun hasAnyPermissions(id: UUID, permissions: List<String>) =
        permissionService.hasPlayerAnyPermissions(id, permissions)

    override fun hasAnyPermissions(entity: PlayerEntity, permissions: List<String>) =
        permissionService.hasPlayerAnyPermissions(entity, permissions)

    override fun hasAnyPermissions(idOrName: String, permissions: List<String>) =
        permissionService.hasPlayerAnyPermissions(idOrName, permissions)

    override fun hasAllPermissions(id: UUID, permissions: List<String>) =
        permissionService.hasPlayerAllPermissions(id, permissions)

    override fun hasAllPermissions(entity: PlayerEntity, permissions: List<String>) =
        permissionService.hasPlayerAllPermissions(entity, permissions)

    override fun hasAllPermissions(idOrName: String, permissions: List<String>) =
        permissionService.hasPlayerAllPermissions(idOrName, permissions)

    override fun getPermissions(id: UUID) =
        permissionService.getPlayerPermissions(id)

    override fun getPermissions(entity: PlayerEntity) =
        permissionService.getPlayerPermissions(entity)

    override fun getPermissions(idOrName: String) =
        permissionService.getPlayerPermissions(idOrName)
}
