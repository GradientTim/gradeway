/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.player

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionEntity
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.entity.player.PlayerPermissionEntity
import dev.gradienttim.gradeway.utilities.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import java.util.*

object PlayerPermissionsTable : CompositeIdTable(name = TableConstants.PLAYER_PERMISSIONS_TABLE_NAME) {
    val playerId = reference(
        name = "player_id",
        refColumn = PlayersTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val permissionId = reference(
        name = "permission_id",
        refColumn = PermissionsTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val isEnabled = bool("is_enabled").default(true)

    init {
        addIdColumn(playerId)
        addIdColumn(permissionId)
    }
}

class DatabasePlayerPermissionEntity(id: EntityID<CompositeID>) : CompositeEntity(id), PlayerPermissionEntity {
    companion object : CompositeEntityClass<DatabasePlayerPermissionEntity>(PlayerPermissionsTable),
        Serializable<DatabasePlayerPermissionEntity> {
        override fun serialize(instance: DatabasePlayerPermissionEntity): JsonObject = buildJsonObject {
            put("playerId", instance.playerId.value.toString())
            put("permissionId", instance.permissionId.value.toString())
            put("isEnabled", instance.isEnabled)
        }

        override fun deserialize(json: JsonObject): DatabasePlayerPermissionEntity = new {
            playerId = EntityID(
                id = UUID.fromString(json.getValue("playerId").jsonPrimitive.content),
                table = PlayersTable
            )

            permissionId = EntityID(
                id = UUID.fromString(json.getValue("permissionId").jsonPrimitive.content),
                table = PermissionsTable
            )

            isEnabled = json.getValue("isEnabled").jsonPrimitive.boolean
        }
    }

    override var playerId by PlayerPermissionsTable.playerId
    override var permissionId by PlayerPermissionsTable.permissionId

    override var isEnabled by PlayerPermissionsTable.isEnabled

    override val player by DatabasePlayerEntity referencedOn PlayerPermissionsTable.playerId
    override val permission by DatabasePermissionEntity referencedOn PlayerPermissionsTable.permissionId
}
