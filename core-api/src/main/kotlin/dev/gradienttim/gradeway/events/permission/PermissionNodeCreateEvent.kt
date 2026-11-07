/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.permission

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.spec.PermissionCreateSpec

/**
 * Fired before a permission node is created. Listeners may change the [spec].
 */
class PermissionNodeCreateEvent(
    val actor: Actor,
    val spec: PermissionCreateSpec,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
