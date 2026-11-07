/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(UUIDSerializer::class)

package dev.gradienttim.gradeway.events.role

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

/**
 * Fired after a parent was added to a role.
 */
@Serializable
class RoleParentAddedEvent(
    val actor: Actor,
    val roleId: UUID,
    val parentId: UUID,
) : PostEvent()
