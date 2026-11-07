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
 * Fired before a permission template is unlinked from a role, group, or player.
 */
class TemplateUnlinkEvent(
    val actor: Actor,
    val template: PermissionTemplateEntity,
    val subject: EventSubject,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
