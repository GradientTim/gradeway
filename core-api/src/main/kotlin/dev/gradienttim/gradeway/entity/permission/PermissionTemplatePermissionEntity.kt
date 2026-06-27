/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.permission

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.UUID

interface PermissionTemplatePermissionEntity {
    val templateId: EntityID<UUID>
    val permissionId: EntityID<UUID>

    val template: PermissionTemplateEntity
    val permission: PermissionEntity
}
