/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.constants

/**
 * The actions recorded in the audit log and matched by the notification `actions` filter.
 *
 * The [dev.gradienttim.gradeway.audit.Target] type of entry tells which entity was affected,
 * so the actions themselves stay generic.
 */
object ActionConstants {
    const val CREATE: String = "create"
    const val UPDATE: String = "update"
    const val DELETE: String = "delete"
}
