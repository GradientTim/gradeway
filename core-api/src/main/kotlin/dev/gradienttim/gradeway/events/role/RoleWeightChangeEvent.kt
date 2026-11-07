/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.role

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before the weight of a role changes.
 */
class RoleWeightChangeEvent(
    val actor: Actor,
    val role: RoleEntity,
    val weight: Int,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
