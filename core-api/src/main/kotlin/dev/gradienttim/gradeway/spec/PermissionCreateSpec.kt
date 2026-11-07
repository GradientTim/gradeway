/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.spec

import dev.gradienttim.gradeway.entity.permission.PermissionEntity

/**
 * Describes a permission node that is about to be created.
 *
 * Built from the arguments of [dev.gradienttim.gradeway.services.PermissionService.createPermission]
 * and can be changed by listeners of
 * [dev.gradienttim.gradeway.events.permission.PermissionNodeCreateEvent] before the insert.
 */
class PermissionCreateSpec(var value: String, var type: PermissionEntity.Type)
