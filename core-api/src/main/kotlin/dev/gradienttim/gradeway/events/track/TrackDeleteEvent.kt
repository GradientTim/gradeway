/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.track

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before a track is deleted.
 */
class TrackDeleteEvent(
    val actor: Actor,
    val track: TrackEntity,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
