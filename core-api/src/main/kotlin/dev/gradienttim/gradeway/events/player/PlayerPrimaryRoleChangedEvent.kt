/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(UUIDSerializer::class)

package dev.gradienttim.gradeway.events.player

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

/**
 * Fired after the primary role of a player changed.
 */
@Serializable
class PlayerPrimaryRoleChangedEvent(
    val actor: Actor,
    val playerId: UUID,
    val from: UUID?,
    val to: UUID?,
) : PostEvent()
