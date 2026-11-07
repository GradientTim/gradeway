/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.permission

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.event.EventSubject

/**
 * Fired before permissions are set on a role, group, or player.
 */
class PermissionsAssignEvent(
    val actor: Actor,
    val subject: EventSubject,
    val permissions: Map<String, Boolean>,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
