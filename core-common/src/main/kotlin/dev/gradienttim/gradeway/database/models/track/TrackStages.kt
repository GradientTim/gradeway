/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.track

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.entity.track.TrackStageEntity
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.*

object TrackStagesTable : UUIDTable(name = TableConstants.TRACK_STAGES_TABLE_NAME) {
    val trackId = reference(
        name = "track_id",
        refColumn = TracksTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val roleId = reference(
        name = "role_id",
        refColumn = RolesTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val position = integer("position").default(0)

    init {
        uniqueIndex(trackId, position)
        uniqueIndex(trackId, roleId)
    }
}

class DatabaseTrackStageEntity(id: EntityID<UUID>) : UUIDEntity(id), TrackStageEntity {
    companion object : UUIDEntityClass<DatabaseTrackStageEntity>(TrackStagesTable),
        JsonSerializable<DatabaseTrackStageEntity> {
        override fun serialize(data: DatabaseTrackStageEntity): JsonObject = buildJsonObject {
            put("id", data.id.value.toString())
            put("trackId", data.trackId.value.toString())
            put("roleId", data.roleId.value.toString())
            put("position", data.position)
        }

        override fun deserialize(json: JsonObject): DatabaseTrackStageEntity {
            val id = UUID.fromString(json.getValue("id").jsonPrimitive.content)

            return new(id) {
                trackId = EntityID(
                    id = UUID.fromString(json.getValue("trackId").jsonPrimitive.content),
                    table = TracksTable
                )

                roleId = EntityID(
                    id = UUID.fromString(json.getValue("roleId").jsonPrimitive.content),
                    table = RolesTable
                )

                position = json.getValue("position").jsonPrimitive.int
            }
        }
    }

    override var trackId by TrackStagesTable.trackId
    override var roleId by TrackStagesTable.roleId

    override var position by TrackStagesTable.position

    override val track by DatabaseTrackEntity referencedOn TrackStagesTable.trackId
    override val role by DatabaseRoleEntity referencedOn TrackStagesTable.roleId
}
