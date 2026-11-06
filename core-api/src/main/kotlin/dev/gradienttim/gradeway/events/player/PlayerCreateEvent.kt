/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.player

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.spec.PlayerCreateSpec

/**
 * Fired before a player is created. Listeners may change the [spec].
 */
class PlayerCreateEvent(
    val actor: Actor,
    val spec: PlayerCreateSpec,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
