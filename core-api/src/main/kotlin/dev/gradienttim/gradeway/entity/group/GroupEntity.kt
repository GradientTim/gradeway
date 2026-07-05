/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.group

import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.reference.PermissionReference
import dev.gradienttim.gradeway.reference.PermissionTemplateReference
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.time.Instant
import java.util.*

interface GroupEntity : PermissionReference<GroupPermissionEntity>,
    PermissionTemplateReference<GroupPermissionTemplateEntity> {
    val id: EntityID<UUID>

    var name: String
    var defaultWeight: Int

    val createdAt: Instant
    val updatedAt: Instant

    val roles: SizedIterable<RoleEntity>
}
