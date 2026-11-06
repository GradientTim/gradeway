/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.role

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.spec.RoleCreateSpec

/**
 * Fired before a role is created. Listeners may change the [spec].
 */
class RoleCreateEvent(
    val actor: Actor,
    val spec: RoleCreateSpec,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
