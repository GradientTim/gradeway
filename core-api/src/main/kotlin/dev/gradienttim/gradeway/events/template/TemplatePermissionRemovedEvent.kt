/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(UUIDSerializer::class)

package dev.gradienttim.gradeway.events.template

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

/**
 * Fired after a permission node was removed from a permission template.
 */
@Serializable
class TemplatePermissionRemovedEvent(
    val actor: Actor,
    val templateId: UUID,
    val permissionId: UUID,
    val value: String,
) : PostEvent()
