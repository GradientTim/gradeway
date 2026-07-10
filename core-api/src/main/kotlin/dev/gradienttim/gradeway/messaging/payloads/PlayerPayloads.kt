/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Signals that a player itself was created, updated, or deleted.
 *
 * Covers structural changes to the player entity (creation, deletion, rename, and primary-role
 * changes, since `primaryRoleId` is a plain column on the player entity); its relationships to
 * roles and permissions are represented by [PlayerRoleChangedPayload] and
 * [PlayerPermissionChangedPayload].
 *
 * @property playerId The unique identifier of the affected player.
 * @property action The kind of mutation that occurred.
 */
@Serializable
@SerialName("player_changed")
data class PlayerChangedPayload(
    val playerId: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that a role was added to, removed from, or otherwise had its assignment state changed
 * (pause/resume, expiry) on a player.
 *
 * @property playerId The unique identifier of the affected player.
 * @property roleId The unique identifier of the affected role.
 * @property action Whether the role assignment was created, updated (e.g. paused, resumed, or
 * its expiry changed), or removed.
 */
@Serializable
@SerialName("player_role_changed")
data class PlayerRoleChangedPayload(
    val playerId: String,
    val roleId: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that a permission was enabled or disabled on a player.
 *
 * @property playerId The unique identifier of the affected player.
 * @property permission The permission value that was toggled.
 */
@Serializable
@SerialName("player_permission_changed")
data class PlayerPermissionChangedPayload(
    val playerId: String,
    val permission: String
) : MessagingPayload
