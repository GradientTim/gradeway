/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.permission

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.event.EventSubject

/**
 * Fired before all permissions of a role, group, or player are cleared.
 */
class PermissionsClearEvent(
    val actor: Actor,
    val subject: EventSubject,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
