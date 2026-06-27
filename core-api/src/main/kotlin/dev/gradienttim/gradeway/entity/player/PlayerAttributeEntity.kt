/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.player

import dev.gradienttim.gradeway.entity.SharedAttributeEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

interface PlayerAttributeEntity : SharedAttributeEntity {
    val playerId: EntityID<UUID>

    val player: PlayerEntity
}
