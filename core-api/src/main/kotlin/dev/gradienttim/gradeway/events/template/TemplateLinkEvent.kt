/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.template

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.event.EventSubject

/**
 * Fired before a permission template is linked to a role, group, or player.
 */
class TemplateLinkEvent(
    val actor: Actor,
    val template: PermissionTemplateEntity,
    val subject: EventSubject,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
