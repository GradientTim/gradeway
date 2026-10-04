/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.track

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.EntityBatchUpdate
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant
import java.util.*

object TracksTable : UUIDTable(name = TableConstants.TRACKS_TABLE_NAME) {
    val slug = varchar("slug", TableConstants.TRACKS_TABLE_MAX_SLUG_LENGTH).uniqueIndex()

    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
}

class DatabaseTrackEntity(id: EntityID<UUID>) : UUIDEntity(id), TrackEntity {
    companion object : UUIDEntityClass<DatabaseTrackEntity>(TracksTable), JsonSerializable<DatabaseTrackEntity> {
        override fun serialize(data: DatabaseTrackEntity): JsonObject = buildJsonObject {
            put("id", data.id.value.toString())
            put("slug", data.slug)
            put("createdAt", data.createdAt.toEpochMilli())
            put("updatedAt", data.updatedAt.toEpochMilli())
        }

        override fun deserialize(json: JsonObject): DatabaseTrackEntity {
            val id = UUID.fromString(json.getValue("id").jsonPrimitive.content)

            return new(id) {
                slug = json.getValue("slug").jsonPrimitive.content
                createdAt = Instant.ofEpochMilli(json.getValue("createdAt").jsonPrimitive.long)
                updatedAt = Instant.ofEpochMilli(json.getValue("updatedAt").jsonPrimitive.long)
            }
        }
    }

    override var slug by TracksTable.slug

    override var createdAt by TracksTable.createdAt
    override var updatedAt by TracksTable.updatedAt

    override val stages by DatabaseTrackStageEntity referrersOn TrackStagesTable.trackId

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        updatedAt = Instant.now()
        return super.flush(batch)
    }
}
