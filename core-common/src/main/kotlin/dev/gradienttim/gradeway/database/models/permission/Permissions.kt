/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.permission

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.utilities.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.*

object PermissionsTable : UUIDTable(name = TableConstants.PERMISSIONS_TABLE_NAME) {
    val value = text("value").uniqueIndex()
    val type = enumeration<PermissionEntity.Type>("type").default(PermissionEntity.Type.EQUALS)
}

class DatabasePermissionEntity(id: EntityID<UUID>) : UUIDEntity(id), PermissionEntity {
    companion object : UUIDEntityClass<DatabasePermissionEntity>(PermissionsTable),
        Serializable<DatabasePermissionEntity> {
        override fun serialize(instance: DatabasePermissionEntity): JsonObject = buildJsonObject {
            put("id", instance.id.value.toString())
            put("value", instance.value)
            put("type", instance.type.name)
        }

        override fun deserialize(json: JsonObject): DatabasePermissionEntity {
            val id = UUID.fromString(json.getValue("id").jsonPrimitive.content)

            return new(id) {
                value = json.getValue("value").jsonPrimitive.content
                type = PermissionEntity.Type.valueOf(json.getValue("type").jsonPrimitive.content)
            }
        }
    }

    override var value by PermissionsTable.value
    override var type by PermissionsTable.type
}
