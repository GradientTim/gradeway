/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.messaging.payloads.MessagingPayload
import dev.gradienttim.gradeway.utilities.lifecycle.*

/**
 * Manages the lifecycle of the configured messaging broker and provides a typed publish/subscribe
 * facade for exchanging [MessagingPayload]s with other servers in real time.
 *
 * Callers are not expected to interact with the underlying messaging broker, channel names, or
 * wire serialization directly; those details are owned entirely by the implementation of this
 * interface.
 */
interface MessagingManager : Loadable, Unloadable, Reloadable, Enableable, Disableable {
    /**
     * Publishes a [MessagingPayload] to every other server connected to the same messaging
     * broker.
     *
     * If messaging is disabled or the broker is not currently connected, this method has no
     * effect and returns `false` rather than throwing, consistent with messaging being an
     * optional feature.
     *
     * @param payload The payload describing the change to synchronize.
     * @return `true` if the payload was successfully handed off to the broker, `false` otherwise.
     */
    fun publish(payload: MessagingPayload): Boolean

    /**
     * Registers a listener that is invoked whenever a [MessagingPayload] originating from another
     * server is received.
     *
     * The listener is invoked in-process; it does not need to know about channels, the broker, or
     * how payloads are serialized on the wire. Payloads published by this server itself are not
     * delivered back to its own listeners.
     *
     * @param listener The callback invoked with each received payload.
     */
    fun subscribe(listener: (MessagingPayload) -> Unit)
}
