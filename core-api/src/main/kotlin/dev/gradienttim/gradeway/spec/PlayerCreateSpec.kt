/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.spec

import java.util.*

/**
 * Describes a player that is about to be created.
 *
 * Configured by the caller of [dev.gradienttim.gradeway.services.PlayerService.create] and can be
 * changed by listeners of [dev.gradienttim.gradeway.events.player.PlayerCreateEvent] before the insert.
 */
class PlayerCreateSpec(val id: UUID, var name: String) {
    var weight: Int = -1
}
