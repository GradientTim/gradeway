/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Signals that a role itself was created, updated, or deleted.
 *
 * Covers structural changes to the role entity (creation, deletion, rename, and weight changes);
 * its relationships to permissions and parent roles are represented by
 * [RolePermissionChangedPayload] and [RoleParentChangedPayload].
 *
 * @property roleId The unique identifier of the affected role.
 * @property action The kind of mutation that occurred.
 */
@Serializable
@SerialName("role_changed")
data class RoleChangedPayload(
    val roleId: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that a permission was enabled or disabled on a role.
 *
 * @property roleId The unique identifier of the affected role.
 * @property permission The permission value that was toggled.
 */
@Serializable
@SerialName("role_permission_changed")
data class RolePermissionChangedPayload(
    val roleId: String,
    val permission: String
) : MessagingPayload

/**
 * Signals that a parent role was added to or removed from a role.
 *
 * @property roleId The unique identifier of the affected (child) role.
 * @property parentRoleId The unique identifier of the parent role.
 * @property action Whether the parent link was added ([MessagingAction.CREATED]) or removed
 * ([MessagingAction.DELETED]).
 */
@Serializable
@SerialName("role_parent_changed")
data class RoleParentChangedPayload(
    val roleId: String,
    val parentRoleId: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that every permission on a role was cleared at once.
 *
 * Published instead of one [RolePermissionChangedPayload] per removed permission when a bulk
 * clear removes all of a role's permissions in a single operation, since receivers are expected
 * to treat this as an invalidation signal and re-read current state rather than apply per-entry
 * deltas anyway (see [MessagingPayload]).
 *
 * @property roleId The unique identifier of the affected role.
 */
@Serializable
@SerialName("role_permissions_cleared")
data class RolePermissionsClearedPayload(
    val roleId: String
) : MessagingPayload
