/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.role

import dev.gradienttim.gradeway.entity.group.GroupEntity
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.UUID

interface RoleGroupEntity {
    val id: EntityID<CompositeID>

    val roleId: EntityID<UUID>
    val groupId: EntityID<UUID>

    val role: RoleEntity
    val group: GroupEntity
}
