/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.role

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before a parent is added to a role.
 */
class RoleParentAddEvent(
    val actor: Actor,
    val role: RoleEntity,
    val parent: RoleEntity,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
