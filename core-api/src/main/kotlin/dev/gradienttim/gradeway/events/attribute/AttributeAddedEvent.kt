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
 * Fired after an attribute was added to a role or player. [value] is the serialized value.
 */
@Serializable
class AttributeAddedEvent(
    val actor: Actor,
    val subject: EventSubject,
    val key: Key,
    val type: String,
    val value: String,
) : PostEvent()
