/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.event

import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity

/**
 * Maps a role, group, or player entity to the [EventSubject] identifying it.
 */
internal fun eventSubjectOf(entity: Any): EventSubject = when (entity) {
    is RoleEntity -> EventSubject.Role(entity.id.value)
    is GroupEntity -> EventSubject.Group(entity.id.value)
    is PlayerEntity -> EventSubject.Player(entity.id.value)
    else -> throw IllegalArgumentException("${entity::class.simpleName} is not a role, group, or player")
}
