/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.spec

/**
 * Describes a track that is about to be created.
 *
 * Configured by the caller of [dev.gradienttim.gradeway.services.TrackService.createTrack] and can be
 * changed by listeners of [dev.gradienttim.gradeway.events.track.TrackCreateEvent] before the insert.
 */
class TrackCreateSpec(var slug: String)
