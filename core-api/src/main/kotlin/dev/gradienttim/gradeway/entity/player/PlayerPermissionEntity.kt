/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.player

import dev.gradienttim.gradeway.entity.SharedPermissionEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

interface PlayerPermissionEntity: SharedPermissionEntity {
    val playerId: EntityID<UUID>

    val player: PlayerEntity
}
