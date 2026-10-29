/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.audit

import dev.gradienttim.gradeway.utilities.Typed
import java.util.*

/**
 * Represents an actor responsible for initiating actions or events within a system.
 *
 * This sealed interface defines various types of actors that can interact with the system,
 * each implementing the `Typed` interface to provide a `type` property for categorization.
 * Some actors can optionally have an identifier for additional specificity.
 */
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

    data object System : Actor {
        override val type: String = "system"
    }

    data object Console : Actor {
        override val type: String = "console"
    }

    data class Player(
        val uniqueId: UUID,
    ) : Actor {
        override val type: String = "player"
        override fun identifier(): String = uniqueId.toString()
    }

    data class Api(
        val name: String,
    ) : Actor {
        override val type: String = "api"
        override fun identifier(): String = name
    }

    data class CommandBlock(
        val world: String,
        val x: Int,
        val y: Int,
        val z: Int,
    ) : Actor {
        override val type: String = "command_block"
        override fun identifier(): String = "$world;$x:$y:$z"
    }
}
