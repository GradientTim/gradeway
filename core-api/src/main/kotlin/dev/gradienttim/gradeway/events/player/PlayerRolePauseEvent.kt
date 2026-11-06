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
 * Fired before a player's role is paused.
 */
class PlayerRolePauseEvent(
    val actor: Actor,
    val player: PlayerEntity,
    val role: RoleEntity,
    val pausedAt: Instant,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
