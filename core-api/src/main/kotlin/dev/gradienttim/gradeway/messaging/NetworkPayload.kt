/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging

import kotlinx.serialization.Serializable

/**
 * Represents a data structure for encapsulating network payloads exchanged
 * between distributed systems or application components.
 *
 * This class is designed to hold a server identifier along with a payload
 * in the form of a byte array. It ensures proper comparison and hashing
 * through overridden equals and hashCode methods that account for
 * content equality of the payload data.
 *
 * @property serverId A unique identifier for the server associated with the payload.
 * @property payload The data payload to be transmitted as a byte array.
 */
@Serializable
data class NetworkPayload(
    val serverId: String,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NetworkPayload) return false

        if (serverId != other.serverId) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serverId.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
