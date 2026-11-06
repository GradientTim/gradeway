/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(KeySerializer::class)

package dev.gradienttim.gradeway.events.attribute

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.EventSubject
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.serialization.KeySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.kyori.adventure.key.Key

/**
 * Fired after an attribute of a role or player was updated. [from] and [to] are serialized values.
 */
@Serializable
class AttributeUpdatedEvent(
    val actor: Actor,
    val subject: EventSubject,
    val key: Key,
    val from: String,
    val to: String,
) : PostEvent()
