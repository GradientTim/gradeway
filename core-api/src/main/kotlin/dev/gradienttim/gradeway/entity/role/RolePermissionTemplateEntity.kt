/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.role

import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.UUID

interface RolePermissionTemplateEntity {
    val roleId: EntityID<UUID>
    val permissionTemplateId: EntityID<UUID>

    val role: RoleEntity
    val permissionTemplate: PermissionTemplateEntity
}
