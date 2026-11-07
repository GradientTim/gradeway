/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.attribute

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.event.EventSubject
import net.kyori.adventure.key.Key

/**
 * Fired before an attribute is removed from a role or player.
 */
class AttributeRemoveEvent(
    val actor: Actor,
    val subject: EventSubject,
    val key: Key,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
