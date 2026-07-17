/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.player

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.entity.player.PlayerRoleEntity
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.dao.EntityBatchUpdate
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant
import java.util.*

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

class DatabasePlayerRoleEntity(id: EntityID<CompositeID>) : CompositeEntity(id), PlayerRoleEntity {
    companion object : CompositeEntityClass<DatabasePlayerRoleEntity>(PlayerRolesTable),
        JsonSerializable<DatabasePlayerRoleEntity> {
        override fun serialize(data: DatabasePlayerRoleEntity): JsonObject = buildJsonObject {
            put("playerId", data.playerId.value.toString())
            put("roleId", data.roleId.value.toString())
            put("untilAt", data.untilAt?.toEpochMilli())
            put("pausedAt", data.pausedAt?.toEpochMilli())
            put("createdAt", data.createdAt.toEpochMilli())
            put("updatedAt", data.updatedAt.toEpochMilli())
        }

        override fun deserialize(json: JsonObject): DatabasePlayerRoleEntity = new {
            playerId = EntityID(
                id = UUID.fromString(json.getValue("playerId").jsonPrimitive.content),
                table = PlayersTable
            )
            roleId = EntityID(
                id = UUID.fromString(json.getValue("roleId").jsonPrimitive.content),
                table = RolesTable
            )

            json.getValue("untilAt").jsonPrimitive.longOrNull?.let { rawUntilAt ->
                untilAt = Instant.ofEpochMilli(rawUntilAt)
            }

            json.getValue("pausedAt").jsonPrimitive.longOrNull?.let { rawPausedAt ->
                pausedAt = Instant.ofEpochMilli(rawPausedAt)
            }

            createdAt = Instant.ofEpochMilli(json.getValue("createdAt").jsonPrimitive.long)
            updatedAt = Instant.ofEpochMilli(json.getValue("updatedAt").jsonPrimitive.long)
        }
    }

    override var playerId by PlayerRolesTable.playerId
    override var roleId by PlayerRolesTable.roleId

    override var untilAt by PlayerRolesTable.untilAt
    override var pausedAt by PlayerRolesTable.pausedAt

    override var createdAt by PlayerRolesTable.createdAt
    override var updatedAt by PlayerRolesTable.updatedAt

    override val player by DatabasePlayerEntity referencedOn PlayerRolesTable.playerId
    override val role by DatabaseRoleEntity referencedOn PlayerRolesTable.roleId

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        updatedAt = Instant.now()
        return super.flush(batch)
    }
}
