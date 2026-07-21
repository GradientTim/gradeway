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

/**
 * Signals that every attribute on a role was cleared at once.
 *
 * Published instead of one [RoleAttributeChangedPayload] per removed key when a bulk clear
 * removes all of a role's attributes in a single operation, since receivers are expected to
 * treat this as an invalidation signal and re-read current state rather than apply per-entry
 * deltas anyway (see [MessagingPayload]).
 *
 * @property roleId The unique identifier of the affected role.
 */
@Serializable
@SerialName("role_attributes_cleared")
data class RoleAttributesClearedPayload(
    val roleId: String
) : MessagingPayload

/**
 * Signals that every attribute on a player was cleared at once.
 *
 * Published instead of one [PlayerAttributeChangedPayload] per removed key when a bulk clear
 * removes all of a player's attributes in a single operation, since receivers are expected to
 * treat this as an invalidation signal and re-read current state rather than apply per-entry
 * deltas anyway (see [MessagingPayload]).
 *
 * @property playerId The unique identifier of the affected player.
 */
@Serializable
@SerialName("player_attributes_cleared")
data class PlayerAttributesClearedPayload(
    val playerId: String
) : MessagingPayload
