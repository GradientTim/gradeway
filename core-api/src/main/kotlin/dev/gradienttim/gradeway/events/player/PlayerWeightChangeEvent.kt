/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.player

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before the weight of a player changes.
 */
class PlayerWeightChangeEvent(
    val actor: Actor,
    val player: PlayerEntity,
    val weight: Int,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
