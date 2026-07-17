/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.player

import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.columns.adventureKey
import dev.gradienttim.gradeway.entity.player.PlayerAttributeEntity
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
import kotlinx.serialization.json.*
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant
import java.util.*

object PlayerAttributesTable : UUIDTable(name = TableConstants.PLAYER_ATTRIBUTES_TABLE_NAME) {
    val playerId = reference(
        name = "player_id",
        refColumn = PlayersTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val type = text("type")
    val key = adventureKey("key")
    val value = text("value")

    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

class DatabasePlayerAttributeEntity(id: EntityID<UUID>) : UUIDEntity(id), PlayerAttributeEntity {
    companion object : UUIDEntityClass<DatabasePlayerAttributeEntity>(PlayerAttributesTable),
        JsonSerializable<DatabasePlayerAttributeEntity> {
        override fun serialize(data: DatabasePlayerAttributeEntity): JsonObject = buildJsonObject {
            put("playerId", data.playerId.value.toString())
            put("type", data.type)
            put("key", data.key.asString())
            put("value", data.value)
            put("createdAt", data.createdAt.toEpochMilli())
            put("updatedAt", data.updatedAt.toEpochMilli())
        }

        override fun deserialize(json: JsonObject): DatabasePlayerAttributeEntity = new {
            playerId = EntityID(
                id = UUID.fromString(json.getValue("playerId").jsonPrimitive.content),
                table = PlayersTable
            )

            type = json.getValue("type").jsonPrimitive.content
            key = Key.key(json.getValue("key").jsonPrimitive.content)
            value = json.getValue("value").jsonPrimitive.content

            createdAt = Instant.ofEpochMilli(json.getValue("createdAt").jsonPrimitive.long)
            updatedAt = Instant.ofEpochMilli(json.getValue("updatedAt").jsonPrimitive.long)
        }
    }

    override var playerId by PlayerAttributesTable.playerId

    override var type by PlayerAttributesTable.type
    override var key by PlayerAttributesTable.key
    override var value by PlayerAttributesTable.value

    override var createdAt by PlayerAttributesTable.createdAt
    override var updatedAt by PlayerAttributesTable.updatedAt

    override val player by DatabasePlayerEntity referencedOn PlayerAttributesTable.playerId

    override val attribute: Attribute<*>
        get() = Attribute.createUnsafe(type, key, value)
}
