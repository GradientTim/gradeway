/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.track

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before the slug of a track changes.
 */
class TrackSlugChangeEvent(
    val actor: Actor,
    val track: TrackEntity,
    val slug: String,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
