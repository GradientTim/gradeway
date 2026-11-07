/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.track

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before a player is demoted on a track.
 */
class PlayerDemoteEvent(
    val actor: Actor,
    val player: PlayerEntity,
    val track: TrackEntity,
    val from: RoleEntity,
    val to: RoleEntity,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
