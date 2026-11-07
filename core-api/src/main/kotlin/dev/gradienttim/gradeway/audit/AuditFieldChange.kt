/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.audit

/**
 * A single field that differs between an audit notification's before and after snapshot,
 * with its values already converted to display text.
 *
 * @property before The previous value, or null if the field was added (e.g. on create).
 * @property after The new value, or null if the field was removed (e.g. on delete).
 */
data class AuditFieldChange(
    val field: String,
    val before: String?,
    val after: String?,
)
