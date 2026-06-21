/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.player

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.role.RoleEntity
import dev.gradienttim.gradeway.database.models.role.RolesTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.dao.EntityBatchUpdate
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object PlayerRolesTable : CompositeIdTable(name = TableConstants.PLAYER_ROLES_TABLE_NAME) {
    val playerId = reference(
        name = "player_id",
        refColumn = PlayersTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val roleId = reference(
        name = "role_id",
        refColumn = RolesTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val isPaused = bool("is_paused").default(false)
    val isPrimary = bool("is_primary").default(false)

    val untilAt = timestamp("until_at").nullable()
    val pausedAt = timestamp("paused_at").nullable()

    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    init {
        addIdColumn(roleId)
        addIdColumn(playerId)
        uniqueIndex(roleId, playerId)
    }
}

class PlayerRoleEntity(id: EntityID<CompositeID>) : CompositeEntity(id) {
    companion object : CompositeEntityClass<PlayerRoleEntity>(PlayerRolesTable)

    var playerId by PlayerRolesTable.playerId
    var roleId by PlayerRolesTable.roleId

    var isPaused by PlayerRolesTable.isPaused
    var isPrimary by PlayerRolesTable.isPrimary

    var untilAt by PlayerRolesTable.untilAt
    var pausedAt by PlayerRolesTable.pausedAt

    val createdAt by PlayerRolesTable.createdAt
    var updatedAt by PlayerRolesTable.updatedAt

    val player by PlayerEntity referencedOn PlayerRolesTable.playerId
    val role by RoleEntity referencedOn PlayerRolesTable.roleId

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        updatedAt = Instant.now()
        return super.flush(batch)
    }
}
