/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.permission

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before the match type of a permission node changes.
 */
class PermissionNodeTypeChangeEvent(
    val actor: Actor,
    val permission: PermissionEntity,
    val type: PermissionEntity.Type,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
