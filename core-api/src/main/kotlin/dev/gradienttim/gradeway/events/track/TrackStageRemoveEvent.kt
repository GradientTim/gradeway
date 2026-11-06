/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.track

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.track.TrackStageEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before a stage is removed from a track.
 */
class TrackStageRemoveEvent(
    val actor: Actor,
    val stage: TrackStageEntity,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
