/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Signals that an attribute was added, updated, or removed on a role.
 *
 * @property roleId The unique identifier of the affected role.
 * @property key The attribute's key, serialized as [net.kyori.adventure.key.Key.asString] since
 * `Key` itself is not `@Serializable`.
 * @property action The kind of mutation that occurred.
 */
@Serializable
@SerialName("role_attribute_changed")
data class RoleAttributeChangedPayload(
    val roleId: String,
    val key: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that an attribute was added, updated, or removed on a player.
 *
 * @property playerId The unique identifier of the affected player.
 * @property key The attribute's key, serialized as [net.kyori.adventure.key.Key.asString] since
 * `Key` itself is not `@Serializable`.
 * @property action The kind of mutation that occurred.
 */
@Serializable
@SerialName("player_attribute_changed")
data class PlayerAttributeChangedPayload(
    val playerId: String,
    val key: String,
    val action: MessagingAction
) : MessagingPayload
