/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.group

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.spec.GroupCreateSpec

/**
 * Fired before a group is created. Listeners may change the [spec].
 */
class GroupCreateEvent(
    val actor: Actor,
    val spec: GroupCreateSpec,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
