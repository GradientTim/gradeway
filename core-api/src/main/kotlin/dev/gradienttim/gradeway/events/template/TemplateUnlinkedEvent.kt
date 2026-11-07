/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(UUIDSerializer::class)

package dev.gradienttim.gradeway.events.template

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.EventSubject
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

/**
 * Fired after a permission template was unlinked from a role, group, or player.
 */
@Serializable
class TemplateUnlinkedEvent(
    val actor: Actor,
    val templateId: UUID,
    val subject: EventSubject,
) : PostEvent()
