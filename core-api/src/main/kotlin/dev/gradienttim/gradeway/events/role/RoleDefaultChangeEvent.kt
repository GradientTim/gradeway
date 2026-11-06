/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.role

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before the default flag of roles changes. A `null` [role] with [isDefault] `false` clears the flag on every
 * role, [exclusive] means all other roles lose the flag.
 */
class RoleDefaultChangeEvent(
    val actor: Actor,
    val role: RoleEntity?,
    val isDefault: Boolean,
    val exclusive: Boolean,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
