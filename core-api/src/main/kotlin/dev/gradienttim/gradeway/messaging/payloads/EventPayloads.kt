/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Relays a [dev.gradienttim.gradeway.event.PostEvent] to the other servers of a network, which fire
 * it again with a [dev.gradienttim.gradeway.event.EventOrigin.Remote] origin.
 *
 * Unlike the other payloads, this one is not used for cache invalidation.
 *
 * @property serverId The messaging identifier of the server that made the change.
 * @property event The JSON encoded post event.
 */
@Serializable
@SerialName("event")
data class EventPayload(
    val serverId: String,
    val event: String,
) : MessagingPayload
