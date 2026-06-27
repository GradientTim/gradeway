/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.role

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatesTable
import dev.gradienttim.gradeway.entity.role.RolePermissionTemplateEntity
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass

object RolePermissionTemplatesTable : CompositeIdTable(name = TableConstants.ROLE_PERMISSION_TEMPLATES_TABLE_NAME) {
    val roleId = reference(
        name = "role_id",
        refColumn = RolesTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val permissionTemplateId = reference(
        name = "permission_template_id",
        refColumn = PermissionTemplatesTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    init {
        addIdColumn(roleId)
        addIdColumn(permissionTemplateId)
        uniqueIndex(roleId, permissionTemplateId)
    }
}

class DatabaseRolePermissionTemplateEntity(id: EntityID<CompositeID>) : CompositeEntity(id),
    RolePermissionTemplateEntity {
    companion object : CompositeEntityClass<DatabaseRolePermissionTemplateEntity>(RolePermissionTemplatesTable)

    override var roleId by RolePermissionTemplatesTable.roleId
    override var permissionTemplateId by RolePermissionTemplatesTable.permissionTemplateId

    override val role by DatabaseRoleEntity referencedOn RolePermissionTemplatesTable.roleId
    override val permissionTemplate by DatabasePermissionTemplateEntity referencedOn
            RolePermissionTemplatesTable.permissionTemplateId
}
