/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.event

/**
 * Describes on which server the change behind a [PostEvent] was made.
 */
sealed interface EventOrigin {
    /**
     * The change was made on this server.
     */
    data object Local : EventOrigin

    /**
     * The change was made on another server and relayed through the messaging broker.
     *
     * @property serverId The messaging identifier of the server that made the change.
     */
    data class Remote(val serverId: String) : EventOrigin
}
