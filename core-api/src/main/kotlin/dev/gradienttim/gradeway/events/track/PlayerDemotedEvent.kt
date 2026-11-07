/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(UUIDSerializer::class)

package dev.gradienttim.gradeway.events.track

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

/**
 * Fired after a player was demoted on a track.
 */
@Serializable
class PlayerDemotedEvent(
    val actor: Actor,
    val playerId: UUID,
    val trackId: UUID,
    val fromRoleId: UUID,
    val toRoleId: UUID,
) : PostEvent()
