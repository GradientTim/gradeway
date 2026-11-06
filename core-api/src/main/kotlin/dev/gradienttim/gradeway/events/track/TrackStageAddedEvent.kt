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
 * Fired after a role was added as a stage to a track.
 */
@Serializable
class TrackStageAddedEvent(
    val actor: Actor,
    val trackId: UUID,
    val stageId: UUID,
    val roleId: UUID,
    val position: Int,
) : PostEvent()
