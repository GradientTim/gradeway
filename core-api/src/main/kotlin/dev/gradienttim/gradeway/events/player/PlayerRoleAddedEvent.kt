/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(InstantSerializer::class, UUIDSerializer::class)

package dev.gradienttim.gradeway.events.player

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.serialization.InstantSerializer
import dev.gradienttim.gradeway.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.*

/**
 * Fired after a role was added to a player.
 */
@Serializable
class PlayerRoleAddedEvent(
    val actor: Actor,
    val playerId: UUID,
    val roleId: UUID,
    val until: Instant?,
) : PostEvent()
