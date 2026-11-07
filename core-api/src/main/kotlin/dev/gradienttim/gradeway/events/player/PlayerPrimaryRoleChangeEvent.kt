/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.player

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before the primary role of a player changes. A `null` [role] clears it.
 */
class PlayerPrimaryRoleChangeEvent(
    val actor: Actor,
    val player: PlayerEntity,
    val role: RoleEntity?,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
