/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.attribute

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.event.EventSubject

/**
 * Fired before all attributes of a role or player are removed.
 */
class AttributesClearEvent(
    val actor: Actor,
    val subject: EventSubject,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
