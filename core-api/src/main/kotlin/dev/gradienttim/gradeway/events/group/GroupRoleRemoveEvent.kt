/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.group

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before a role is removed from a group.
 */
class GroupRoleRemoveEvent(
    val actor: Actor,
    val group: GroupEntity,
    val role: RoleEntity,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
