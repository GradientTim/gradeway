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
 * Fired after a role was created.
 */
@Serializable
class RoleCreatedEvent(
    val actor: Actor,
    val roleId: UUID,
    val name: String,
    val weight: Int,
    val isDefault: Boolean,
) : PostEvent()
