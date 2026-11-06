/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.spec

/**
 * Describes a role that is about to be created.
 *
 * Configured by the caller of [dev.gradienttim.gradeway.services.RoleService.create] and can be
 * changed by listeners of [dev.gradienttim.gradeway.events.role.RoleCreateEvent] before the insert.
 */
class RoleCreateSpec(var name: String) {
    var weight: Int = -1
    var isDefault: Boolean = false
}
