/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Signals that a track itself was created or deleted.
 *
 * @property trackId The unique identifier of the affected track.
 * @property action The kind of mutation that occurred.
 */
@Serializable
@SerialName("track_changed")
data class TrackChangedPayload(
    val trackId: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that a stage was added to or removed from a track.
 *
 * @property trackId The unique identifier of the track the stage belongs to.
 * @property stageId The unique identifier of the affected stage.
 * @property action The kind of mutation that occurred.
 */
@Serializable
@SerialName("track_stage_changed")
data class TrackStageChangedPayload(
    val trackId: String,
    val stageId: String,
    val action: MessagingAction
) : MessagingPayload
