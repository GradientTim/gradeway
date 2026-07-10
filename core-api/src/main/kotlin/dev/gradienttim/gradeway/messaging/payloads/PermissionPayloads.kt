/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Signals that a permission itself was created, updated, or deleted.
 *
 * Covers structural changes to the permission entity (creation and deletion); property-level
 * changes are represented by [PermissionValueChangedPayload] and [PermissionTypeChangedPayload].
 *
 * @property permissionId The unique identifier of the affected permission.
 * @property action The kind of mutation that occurred.
 */
@Serializable
@SerialName("permission_changed")
data class PermissionChangedPayload(
    val permissionId: String,
    val action: MessagingAction
) : MessagingPayload

/**
 * Signals that a permission's value was changed.
 *
 * @property permissionId The unique identifier of the affected permission.
 */
@Serializable
@SerialName("permission_value_changed")
data class PermissionValueChangedPayload(
    val permissionId: String
) : MessagingPayload

/**
 * Signals that a permission's type was changed.
 *
 * @property permissionId The unique identifier of the affected permission.
 */
@Serializable
@SerialName("permission_type_changed")
data class PermissionTypeChangedPayload(
    val permissionId: String
) : MessagingPayload
