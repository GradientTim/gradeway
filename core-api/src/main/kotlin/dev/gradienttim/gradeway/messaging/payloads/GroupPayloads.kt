/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Signals that a group itself was created, updated, or deleted.
 *
 * Covers structural changes to the group entity (creation, deletion, rename, and default-weight
 * changes); its relationships to roles and permissions are represented by
 * [GroupRoleChangedPayload] and [GroupPermissionChangedPayload].
 *
 * @property groupId The unique identifier of the affected group.
 * @property action The kind of mutation that occurred.
 */
@Serializable
@SerialName("group_changed")
data class GroupChangedPayload(
    val groupId: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that a role was added to or removed from a group.
 *
 * @property groupId The unique identifier of the affected group.
 * @property roleId The unique identifier of the affected role.
 * @property action Whether the role was added ([MessagingAction.CREATED]) or removed
 * ([MessagingAction.DELETED]) from the group.
 */
@Serializable
@SerialName("group_role_changed")
data class GroupRoleChangedPayload(
    val groupId: String,
    val roleId: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that a permission was enabled or disabled on a group.
 *
 * @property groupId The unique identifier of the affected group.
 * @property permission The permission value that was toggled.
 */
@Serializable
@SerialName("group_permission_changed")
data class GroupPermissionChangedPayload(
    val groupId: String,
    val permission: String
) : MessagingPayload

/**
 * Signals that every permission on a group was cleared at once.
 *
 * Published instead of one [GroupPermissionChangedPayload] per removed permission when a bulk
 * clear removes all of a group's permissions in a single operation, since receivers are expected
 * to treat this as an invalidation signal and re-read current state rather than apply per-entry
 * deltas anyway (see [MessagingPayload]).
 *
 * @property groupId The unique identifier of the affected group.
 */
@Serializable
@SerialName("group_permissions_cleared")
data class GroupPermissionsClearedPayload(
    val groupId: String
) : MessagingPayload
