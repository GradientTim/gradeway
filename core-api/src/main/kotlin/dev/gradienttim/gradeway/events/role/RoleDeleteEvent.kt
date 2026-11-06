/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.role

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before a role is deleted.
 */
class RoleDeleteEvent(
    val actor: Actor,
    val role: RoleEntity,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
