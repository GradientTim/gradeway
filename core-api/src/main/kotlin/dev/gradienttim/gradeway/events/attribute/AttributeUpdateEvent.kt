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
 * Fired before an attribute of a role or player is updated.
 */
class AttributeUpdateEvent(
    val actor: Actor,
    val subject: EventSubject,
    val key: Key,
    val value: Any,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
