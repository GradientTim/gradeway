/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.track

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before a role is added as a stage to a track.
 */
class TrackStageAddEvent(
    val actor: Actor,
    val track: TrackEntity,
    val role: RoleEntity,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
