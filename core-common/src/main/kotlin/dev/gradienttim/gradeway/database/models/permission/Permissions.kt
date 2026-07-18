/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.permission

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
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
        JsonSerializable<DatabasePermissionEntity> {
        override fun serialize(data: DatabasePermissionEntity): JsonObject = buildJsonObject {
            put("id", data.id.value.toString())
            put("value", data.value)
            put("type", data.type.name)
        }

        override fun deserialize(json: JsonObject): DatabasePermissionEntity {
            val id = UUID.fromString(json.getValue("id").jsonPrimitive.content)

            return new(id) {
                value = json.getValue("value").jsonPrimitive.content
                type = PermissionEntity.Type.valueOf(json.getValue("type").jsonPrimitive.content)
            }
        }
    }

    override var value: String
        get() = PermissionsTable.value.getValue(this, ::value)
        set(newValue) {
            PermissionsTable.value.setValue(this, ::value, newValue)
            cachedRegex = null
        }

    override var type by PermissionsTable.type

    override val regex: Regex
        get() {
            if (type != PermissionEntity.Type.REGEX) {
                error("This permission is not a Regex type.")
            }
            return cachedRegex ?: Regex(value).also { cachedRegex = it }
        }

    private var cachedRegex: Regex? = null
}
