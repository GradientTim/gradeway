/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.role

import dev.gradienttim.gradeway.entity.SharedPermissionEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

interface RolePermissionEntity : SharedPermissionEntity {
    val roleId: EntityID<UUID>

    val role: RoleEntity
}
