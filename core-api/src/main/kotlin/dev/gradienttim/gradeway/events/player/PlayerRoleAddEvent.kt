/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.player

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.event.Event
import java.time.Instant

/**
 * Fired before a role is added to a player.
 */
class PlayerRoleAddEvent(
    val actor: Actor,
    val player: PlayerEntity,
    val role: RoleEntity,
    val until: Instant?,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
