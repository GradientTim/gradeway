/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.role

import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.event.CancellableEvent

class RoleUpdateEvent(
    val to: RoleEntity,
    val from: RoleEntity,
) : CancellableEvent()
