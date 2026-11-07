/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.template

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before a permission template is renamed.
 */
class TemplateRenameEvent(
    val actor: Actor,
    val template: PermissionTemplateEntity,
    val name: String,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
