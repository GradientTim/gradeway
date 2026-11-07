/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.template

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before a permission node is added to a permission template.
 */
class TemplatePermissionAddEvent(
    val actor: Actor,
    val template: PermissionTemplateEntity,
    val permission: PermissionEntity,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
