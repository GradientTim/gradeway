/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.group

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionEntity
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.entity.group.GroupPermissionEntity
import dev.gradienttim.gradeway.utilities.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import java.util.*

object GroupPermissionsTable : CompositeIdTable(name = TableConstants.GROUP_PERMISSIONS_TABLE_NAME) {
    val groupId = reference(
        name = "group_id",
        refColumn = GroupsTable.id,
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
        addIdColumn(groupId)
        addIdColumn(permissionId)
    }
}

class DatabaseGroupPermissionEntity(id: EntityID<CompositeID>) : CompositeEntity(id), GroupPermissionEntity {
    companion object : CompositeEntityClass<DatabaseGroupPermissionEntity>(GroupPermissionsTable),
        Serializable<DatabaseGroupPermissionEntity> {
        override fun serialize(instance: DatabaseGroupPermissionEntity): JsonObject = buildJsonObject {
            put("groupId", instance.groupId.value.toString())
            put("permissionId", instance.permissionId.value.toString())
            put("isEnabled", instance.isEnabled)
        }

        override fun deserialize(json: JsonObject): DatabaseGroupPermissionEntity = new {
            groupId = EntityID(
                id = UUID.fromString(json.getValue("groupId").jsonPrimitive.content),
                table = GroupsTable
            )

            permissionId = EntityID(
                id = UUID.fromString(json.getValue("permissionId").jsonPrimitive.content),
                table = PermissionsTable
            )

            isEnabled = json.getValue("isEnabled").jsonPrimitive.boolean
        }
    }

    override var groupId by GroupPermissionsTable.groupId
    override var permissionId by GroupPermissionsTable.permissionId

    override var isEnabled by GroupPermissionsTable.isEnabled

    override val group by DatabaseGroupEntity referencedOn GroupPermissionsTable.groupId
    override val permission by DatabasePermissionEntity referencedOn GroupPermissionsTable.permissionId
}
