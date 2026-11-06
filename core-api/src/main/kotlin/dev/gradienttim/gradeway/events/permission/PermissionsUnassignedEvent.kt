/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.permission

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.EventSubject
import dev.gradienttim.gradeway.event.PostEvent
import kotlinx.serialization.Serializable

/**
 * Fired after permissions were unset from a role, group, or player.
 */
@Serializable
class PermissionsUnassignedEvent(
    val actor: Actor,
    val subject: EventSubject,
    val permissions: List<String>,
) : PostEvent()
