/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Signals that a permission template itself was created, updated, or deleted.
 *
 * Covers structural changes to the template entity (creation, deletion, name changes, and
 * assigned-to changes); its relationships to permissions, roles, and players are represented by
 * [PermissionTemplatePermissionChangedPayload], [PermissionTemplateRoleLinkChangedPayload],
 * [PermissionTemplateGroupLinkChangedPayload], and [PermissionTemplatePlayerLinkChangedPayload].
 *
 * @property templateId The unique identifier of the affected permission template.
 * @property action The kind of mutation that occurred.
 */
@Serializable
@SerialName("permission_template_changed")
data class PermissionTemplateChangedPayload(
    val templateId: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that a permission was added to or removed from a permission template.
 *
 * @property templateId The unique identifier of the affected permission template.
 * @property permissionId The unique identifier of the affected permission.
 * @property action Whether the permission was added ([MessagingAction.CREATED]) or removed
 * ([MessagingAction.DELETED]) from the template.
 */
@Serializable
@SerialName("permission_template_permission_changed")
data class PermissionTemplatePermissionChangedPayload(
    val templateId: String,
    val permissionId: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that every permission on a permission template was cleared at once.
 *
 * Published instead of one [PermissionTemplatePermissionChangedPayload] per removed permission
 * when a bulk clear removes all of a template's permissions in a single operation, since
 * receivers are expected to treat this as an invalidation signal and re-read current state
 * rather than apply per-entry deltas anyway (see [MessagingPayload]).
 *
 * @property templateId The unique identifier of the affected permission template.
 */
@Serializable
@SerialName("permission_template_permissions_cleared")
data class PermissionTemplatePermissionsClearedPayload(
    val templateId: String
) : MessagingPayload

/**
 * Signals that a permission template was linked to or unlinked from a role.
 *
 * @property templateId The unique identifier of the affected permission template.
 * @property roleId The unique identifier of the affected role.
 * @property action Whether the link was created ([MessagingAction.CREATED]) or removed
 * ([MessagingAction.DELETED]).
 */
@Serializable
@SerialName("permission_template_role_link_changed")
data class PermissionTemplateRoleLinkChangedPayload(
    val templateId: String,
    val roleId: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that a permission template was linked to or unlinked from a group.
 *
 * @property templateId The unique identifier of the affected permission template.
 * @property groupId The unique identifier of the affected group.
 * @property action Whether the link was created ([MessagingAction.CREATED]) or removed
 * ([MessagingAction.DELETED]).
 */
@Serializable
@SerialName("permission_template_group_link_changed")
data class PermissionTemplateGroupLinkChangedPayload(
    val templateId: String,
    val groupId: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that a permission template was linked to or unlinked from a player.
 *
 * @property templateId The unique identifier of the affected permission template.
 * @property playerId The unique identifier of the affected player.
 * @property action Whether the link was created ([MessagingAction.CREATED]) or removed
 * ([MessagingAction.DELETED]).
 */
@Serializable
@SerialName("permission_template_player_link_changed")
data class PermissionTemplatePlayerLinkChangedPayload(
    val templateId: String,
    val playerId: String,
    val action: MessagingAction
) : MessagingPayload
