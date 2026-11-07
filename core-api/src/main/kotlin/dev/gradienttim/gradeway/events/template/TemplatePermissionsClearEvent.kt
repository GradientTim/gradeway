/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.template

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before all permission nodes of a permission template are removed.
 */
class TemplatePermissionsClearEvent(
    val actor: Actor,
    val template: PermissionTemplateEntity,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
