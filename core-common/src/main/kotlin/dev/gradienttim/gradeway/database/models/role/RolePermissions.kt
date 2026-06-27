/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.role

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.role.RolePermissionEntity
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass

object RolePermissionsTable : CompositeIdTable(name = TableConstants.ROLE_PERMISSIONS_TABLE_NAME) {
    val roleId = reference(
        name = "role_id",
        refColumn = RolesTable.id,
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
}

class DatabaseRolePermissionEntity(id: EntityID<CompositeID>) : CompositeEntity(id), RolePermissionEntity {
    companion object : CompositeEntityClass<DatabaseRolePermissionEntity>(RolePermissionsTable)

    override var roleId by RolePermissionsTable.roleId
    override var permissionId by RolePermissionsTable.permissionId

    override var isEnabled by RolePermissionsTable.isEnabled

    override val role: RoleEntity
        get() = TODO("Not yet implemented")

    override val permission: PermissionEntity
        get() = TODO("Not yet implemented")

    override fun flush() = flush(null)
}
