/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(UUIDSerializer::class)

package dev.gradienttim.gradeway.events.group

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

/**
 * Fired after the default weight of a group changed.
 */
@Serializable
class GroupDefaultWeightChangedEvent(
    val actor: Actor,
    val groupId: UUID,
    val from: Int,
    val to: Int,
) : PostEvent()
