/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(UUIDSerializer::class)

package dev.gradienttim.gradeway.events.group

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

/**
 * Fired after a group was renamed.
 */
@Serializable
class GroupRenamedEvent(
    val actor: Actor,
    val groupId: UUID,
    val from: String,
    val to: String,
) : PostEvent()
