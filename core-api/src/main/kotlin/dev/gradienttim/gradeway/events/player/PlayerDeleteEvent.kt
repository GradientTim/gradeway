/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.player

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before a player is deleted.
 */
class PlayerDeleteEvent(
    val actor: Actor,
    val player: PlayerEntity,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
