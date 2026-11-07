/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseSerializers(UUIDSerializer::class)

package dev.gradienttim.gradeway.audit

import dev.gradienttim.gradeway.serialization.UUIDSerializer
import dev.gradienttim.gradeway.utilities.Typed
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import java.util.*

/**
 * Represents an actor responsible for initiating actions or events within a system.
 *
 * This sealed interface defines various types of actors that can interact with the system,
 * each implementing the `Typed` interface to provide a `type` property for categorization.
 * Some actors can optionally have an identifier for additional specificity.
 */
@Serializable
sealed interface Actor : Typed {
    /**
     * Retrieves the unique identifier associated with the actor.
     *
     * This method provides an optional string value that serves as an identifier
     * for the specific actor. The identifier can vary based on the actor's type
     * and is used to further specify or distinguish between actors.
     *
     * @return A string representing the actor's unique identifier, or null if the
     *         actor does not have an identifier.
     */
    fun identifier(): String? = null

    @Serializable
    @SerialName("system")
    data object System : Actor {
        @Transient
        override val type: String = "system"
    }

    @Serializable
    @SerialName("console")
    data object Console : Actor {
        @Transient
        override val type: String = "console"
    }

    @Serializable
    @SerialName("player")
    data class Player(
        val uniqueId: UUID,
    ) : Actor {
        @Transient
        override val type: String = "player"
        override fun identifier(): String = uniqueId.toString()
    }

    @Serializable
    @SerialName("api")
    data class Api(
        val name: String,
    ) : Actor {
        @Transient
        override val type: String = "api"
        override fun identifier(): String = name
    }

    @Serializable
    @SerialName("command_block")
    data class CommandBlock(
        val world: String,
        val x: Int,
        val y: Int,
        val z: Int,
    ) : Actor {
        @Transient
        override val type: String = "command_block"
        override fun identifier(): String = "$world:$x:$y:$z"
    }

    @Serializable
    @SerialName("command_minecart")
    data class CommandMinecart(
        val entityId: Int,
        val uniqueId: UUID,
        val world: String,
        val x: Int,
        val y: Int,
        val z: Int,
    ) : Actor {
        @Transient
        override val type: String = "command_minecart"
        override fun identifier(): String = "$entityId:$uniqueId:$world:$x:$y:$z"
    }

    @Serializable
    @SerialName("rcon")
    data class Rcon(
        val address: String,
    ) : Actor {
        @Transient
        override val type: String = "rcon"
        override fun identifier(): String = address
    }

    @Serializable
    @SerialName("entity")
    data class Entity(
        val uniqueId: UUID,
        val entityType: String,
        val world: String,
        val x: Int,
        val y: Int,
        val z: Int,
    ) : Actor {
        @Transient
        override val type: String = "entity"
        override fun identifier(): String = "$uniqueId:$entityType:$world:$x:$y:$z"
    }
}
