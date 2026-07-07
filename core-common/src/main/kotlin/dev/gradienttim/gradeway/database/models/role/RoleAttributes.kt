/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.role

import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.columns.adventureKey
import dev.gradienttim.gradeway.entity.role.RoleAttributeEntity
import dev.gradienttim.gradeway.utilities.Serializable
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

object RoleAttributesTable : UUIDTable(name = TableConstants.ROLE_ATTRIBUTES_TABLE_NAME) {
    val roleId = reference(
        name = "role_id",
        refColumn = RolesTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val type = adventureKey("type")
    val key = adventureKey("key")
    val value = text("value")

    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

class DatabaseRoleAttributeEntity(id: EntityID<UUID>) : UUIDEntity(id), RoleAttributeEntity {
    companion object : UUIDEntityClass<DatabaseRoleAttributeEntity>(RoleAttributesTable),
        Serializable<DatabaseRoleAttributeEntity> {
        override fun serialize(instance: DatabaseRoleAttributeEntity): JsonObject = buildJsonObject {
            put("roleId", instance.roleId.value.toString())
            put("type", instance.type.asString())
            put("key", instance.key.asString())
            put("value", instance.value)
            put("createdAt", instance.createdAt.toEpochMilli())
            put("updatedAt", instance.updatedAt.toEpochMilli())
        }

        override fun deserialize(json: JsonObject): DatabaseRoleAttributeEntity = new {
            roleId = EntityID(
                id = UUID.fromString(json.getValue("roleId").jsonPrimitive.content),
                table = RolesTable
            )

            type = Key.key(json.getValue("type").jsonPrimitive.content)
            key = Key.key(json.getValue("key").jsonPrimitive.content)
            value = json.getValue("value").jsonPrimitive.content

            createdAt = Instant.ofEpochMilli(json.getValue("createdAt").jsonPrimitive.long)
            updatedAt = Instant.ofEpochMilli(json.getValue("updatedAt").jsonPrimitive.long)
        }
    }

    override var roleId by RoleAttributesTable.roleId

    override var type by RoleAttributesTable.type
    override var key by RoleAttributesTable.key
    override var value by RoleAttributesTable.value

    override var createdAt by RoleAttributesTable.createdAt
    override var updatedAt by RoleAttributesTable.updatedAt

    override val role by DatabaseRoleEntity referencedOn RoleAttributesTable.roleId

    override val attribute: Attribute<*>
        get() = Attribute.create(type, key, value)
}
