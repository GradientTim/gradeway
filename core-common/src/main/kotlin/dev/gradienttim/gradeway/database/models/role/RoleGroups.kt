/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.role

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupEntity
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.entity.role.RoleGroupEntity
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import java.util.*

object RoleGroupsTable : CompositeIdTable(name = TableConstants.ROLE_GROUPS_TABLE_NAME) {
    val roleId = reference("role_id", RolesTable.id, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val groupId = reference("group_id", GroupsTable.id, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    init {
        addIdColumn(roleId)
        addIdColumn(groupId)
        uniqueIndex(roleId, groupId)
    }
}

class DatabaseRoleGroupEntity(id: EntityID<CompositeID>) : CompositeEntity(id), RoleGroupEntity {
    companion object : CompositeEntityClass<DatabaseRoleGroupEntity>(RoleGroupsTable),
        JsonSerializable<DatabaseRoleGroupEntity> {
        override fun serialize(data: DatabaseRoleGroupEntity): JsonObject = buildJsonObject {
            put("roleId", data.roleId.value.toString())
            put("groupId", data.groupId.value.toString())
        }

        override fun deserialize(json: JsonObject): DatabaseRoleGroupEntity = new {
            roleId = EntityID(
                id = UUID.fromString(json.getValue("roleId").jsonPrimitive.content),
                table = RolesTable
            )

            groupId = EntityID(
                id = UUID.fromString(json.getValue("groupId").jsonPrimitive.content),
                table = GroupsTable
            )
        }
    }

    override var roleId by RoleGroupsTable.roleId
    override var groupId by RoleGroupsTable.groupId

    override val role by DatabaseRoleEntity referencedOn RoleGroupsTable.roleId
    override val group by DatabaseGroupEntity referencedOn RoleGroupsTable.groupId
}
