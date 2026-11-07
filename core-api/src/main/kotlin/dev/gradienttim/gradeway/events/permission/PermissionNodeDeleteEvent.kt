/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.permission

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before a permission node is deleted.
 */
class PermissionNodeDeleteEvent(
    val actor: Actor,
    val permission: PermissionEntity,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
