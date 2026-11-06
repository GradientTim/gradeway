/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.spec

/**
 * Describes a group that is about to be created.
 *
 * Configured by the caller of [dev.gradienttim.gradeway.services.GroupService.create] and can be
 * changed by listeners of [dev.gradienttim.gradeway.events.group.GroupCreateEvent] before the insert.
 */
class GroupCreateSpec(var name: String) {
    var defaultWeight: Int = -1
}
