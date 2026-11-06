/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(UUIDSerializer::class)

package dev.gradienttim.gradeway.event

import dev.gradienttim.gradeway.serialization.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

/**
 * Identifies the entity an event that applies to several entity kinds is about.
 */
@Serializable
sealed interface EventSubject {
    val id: UUID

    @Serializable
    @SerialName("role")
    data class Role(override val id: UUID) : EventSubject

    @Serializable
    @SerialName("group")
    data class Group(override val id: UUID) : EventSubject

    @Serializable
    @SerialName("player")
    data class Player(override val id: UUID) : EventSubject
}
