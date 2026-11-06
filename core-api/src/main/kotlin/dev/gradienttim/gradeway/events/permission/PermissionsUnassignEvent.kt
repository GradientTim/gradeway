/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.permission

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.event.EventSubject

/**
 * Fired before permissions are unset from a role, group, or player.
 */
class PermissionsUnassignEvent(
    val actor: Actor,
    val subject: EventSubject,
    val permissions: List<String>,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
