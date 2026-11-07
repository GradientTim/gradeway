/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.attribute

import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.event.EventSubject

/**
 * Fired before an attribute is added to a role or player.
 */
class AttributeAddEvent(
    val actor: Actor,
    val subject: EventSubject,
    val attribute: Attribute<*>,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
