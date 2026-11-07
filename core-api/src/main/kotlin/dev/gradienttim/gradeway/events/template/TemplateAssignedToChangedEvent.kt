/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(UUIDSerializer::class)

package dev.gradienttim.gradeway.events.template

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

/**
 * Fired after the entity kinds a permission template can be used for changed.
 */
@Serializable
class TemplateAssignedToChangedEvent(
    val actor: Actor,
    val templateId: UUID,
    val from: PermissionTemplateEntity.AssignedTo,
    val to: PermissionTemplateEntity.AssignedTo,
) : PostEvent()
