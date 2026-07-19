/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging

/**
 * Represents a messaging broker interface for handling messaging operations such as
 * publishing messages to a channel and subscribing to channel events.
 *
 * Implementations of this interface are expected to provide a backend-specific mechanism
 * for communication between distributed systems or application components.
 *
 * Every message [publish]ed is signed with [messagingAuthenticator] before an implementation
 * ever sees it, via [publishAuthenticated], and every message an implementation reports through
 * [dispatch] is verified against that same [messagingAuthenticator] before reaching a subscriber -
 * silently dropped if the signature is missing, was computed with a different shared secret, or
 * no longer matches the received bytes. This applies uniformly to every implementation, so a
 * shared secret is always required to construct a [MessagingBroker], regardless of how trusted
 * its underlying transport is otherwise assumed to be.
 *
 * @property messagingAuthenticator Signs every outgoing message and verifies every incoming one.
 */
abstract class MessagingBroker(private val messagingAuthenticator: MessagingAuthenticator) {
    private var listener: ((payload: ByteArray) -> Unit)? = null

    /**
     * Opens a connection to the underlying messaging broker.
     *
     * This method is responsible for initializing and establishing
     * communication with the messaging broker, allowing the system to
     * perform operations such as messaging and event handling. It should
     * be called before performing any communication-related tasks with
     * the broker.
     */
    abstract fun open()

    /**
     * Closes the connection to the messaging broker.
     *
     * This method releases any resources or connections held by the broker. It is intended
     * to be called when the messaging broker is no longer needed, such as during application
     * shutdown or when the messaging subsystem is being explicitly unloaded. Implementations
     * should ensure that the connection is terminated gracefully to avoid resource leaks
     * or unexpected behavior in dependent systems.
     */
    abstract fun close()

    /**
     * Sends an already-signed message to the specified channel over the underlying transport.
     *
     * Called by [publish] with [payload] already signed by [messagingAuthenticator] - implementations
     * must send it to the wire exactly as received, without signing or otherwise transforming it.
     *
     * @param channel The name of the channel to which the message will be sent.
     * @param payload The already-signed message bytes to send.
     * @return A boolean indicating whether the message was successfully sent.
     */
    protected abstract fun publishAuthenticated(channel: String, payload: ByteArray): Boolean

    /**
     * Registers interest in the given channel with the underlying transport.
     *
     * Implementations must report the raw bytes of every message they subsequently receive on
     * [channel], signature included, to [dispatch] - verification and delivery to the subscribed
     * listener is handled centrally and must not be performed by the implementation itself.
     *
     * @param channel The name of the channel to subscribe to.
     * @return A boolean indicating whether the subscription was successfully established.
     */
    protected abstract fun subscribeChannel(channel: String): Boolean

    /**
     * Publishes a message to the specified channel.
     *
     * This method is used to send a payload of data to a messaging channel.
     * The channel acts as a logical grouping for messages, allowing subscribers
     * to receive messages associated with that channel.
     *
     * @param channel The name of the channel to which the message will be published.
     * @param payload The byte array containing the message data to be published.
     * @return A boolean indicating whether the message was successfully published.
     */
    fun publish(channel: String, payload: ByteArray): Boolean =
        publishAuthenticated(channel, messagingAuthenticator.sign(payload))

    /**
     * Subscribes to a specific channel to receive messages.
     *
     * This method registers a listener invoked whenever a message is published
     * on the specified channel. It allows for real-time response to incoming data
     * by handling the payload received through the listener function.
     *
     * @param channel The name of the channel to subscribe to.
     * @param listener A lambda function that processes the received payload as a byte array.
     * @return A boolean indicating whether the subscription was successfully established.
     */
    fun subscribe(channel: String, listener: (payload: ByteArray) -> Unit): Boolean {
        this.listener = listener
        return subscribeChannel(channel)
    }

    /**
     * Verifies a raw message received off the wire and, if valid, forwards its original payload
     * to the currently subscribed listener. Implementations call this from whatever
     * transport-specific callback they receive messages through, instead of invoking a
     * subscribed listener directly.
     *
     * @param rawPayload The raw bytes received off the wire, signature included.
     * @return The verified, original payload, or `null` if [rawPayload] failed verification and
     * was dropped. Implementations that need to react to a successfully verified message beyond
     * what the subscribed listener already does (e.g. relaying it onward) can use this value;
     * most implementations can ignore it.
     */
    protected fun dispatch(rawPayload: ByteArray): ByteArray? {
        val payload = messagingAuthenticator.verify(rawPayload) ?: return null
        listener?.invoke(payload)
        return payload
    }
}
