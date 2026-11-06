/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(UUIDSerializer::class)

package dev.gradienttim.gradeway.events.permission

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

/**
 * Fired after the match type of a permission node changed.
 */
@Serializable
class PermissionNodeTypeChangedEvent(
    val actor: Actor,
    val permissionId: UUID,
    val from: PermissionEntity.Type,
    val to: PermissionEntity.Type,
) : PostEvent()
