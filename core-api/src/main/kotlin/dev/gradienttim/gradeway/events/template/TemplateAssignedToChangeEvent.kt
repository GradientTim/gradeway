/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.template

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before the entity kinds a permission template can be used for change.
 */
class TemplateAssignedToChangeEvent(
    val actor: Actor,
    val template: PermissionTemplateEntity,
    val assignedTo: PermissionTemplateEntity.AssignedTo,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
