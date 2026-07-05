/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.group

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.database.models.role.RoleGroupsTable
import dev.gradienttim.gradeway.entity.group.GroupEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant
import java.util.*

object GroupsTable : UUIDTable(name = TableConstants.GROUPS_TABLE_NAME) {
    val name = varchar("name", TableConstants.GROUPS_TABLE_MAX_NAME_LENGTH)
    val defaultWeight = integer("default_weight").default(-1)

    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

class DatabaseGroupEntity(id: EntityID<UUID>) : UUIDEntity(id), GroupEntity {
    companion object : UUIDEntityClass<DatabaseGroupEntity>(GroupsTable)

    override var name by GroupsTable.name
    override var defaultWeight by GroupsTable.defaultWeight

    override val createdAt by GroupsTable.createdAt
    override val updatedAt by GroupsTable.updatedAt

    override val roles by DatabaseRoleEntity via RoleGroupsTable
    override val permissions by DatabaseGroupPermissionEntity referrersOn GroupPermissionsTable.groupId
    override val permissionTemplates by DatabaseGroupPermissionTemplateEntity referrersOn
            GroupPermissionTemplatesTable.groupId
}
