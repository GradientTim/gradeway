/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.player

import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

interface PlayerPermissionTemplateEntity {
    val playerId: EntityID<UUID>
    val permissionTemplateId: EntityID<UUID>

    val player: PlayerEntity
    val permissionTemplate: PermissionTemplateEntity
}
