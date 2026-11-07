/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.track

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.spec.TrackCreateSpec

/**
 * Fired before a track is created. Listeners may change the [spec].
 */
class TrackCreateEvent(
    val actor: Actor,
    val spec: TrackCreateSpec,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
