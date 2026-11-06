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
 * Fired before the permissions of a template are copied onto a role, group, or player.
 */
class TemplateApplyEvent(
    val actor: Actor,
    val template: PermissionTemplateEntity,
    val subject: EventSubject,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
