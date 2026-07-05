/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.role

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupEntity
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.entity.role.RoleGroupEntity
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass

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
    companion object : CompositeEntityClass<DatabaseRoleGroupEntity>(RoleGroupsTable)

    override var roleId by RoleGroupsTable.roleId
    override var groupId by RoleGroupsTable.groupId

    override val role by DatabaseRoleEntity referencedOn RoleGroupsTable.roleId
    override val group by DatabaseGroupEntity referencedOn RoleGroupsTable.groupId
}
