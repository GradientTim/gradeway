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
 */
interface MessagingBroker {
    /**
     * Opens a connection to the underlying messaging broker.
     *
     * This method is responsible for initializing and establishing
     * communication with the messaging broker, allowing the system to
     * perform operations such as messaging and event handling. It should
     * be called before performing any communication-related tasks with
     * the broker.
     */
    fun open()

    /**
     * Closes the connection to the messaging broker.
     *
     * This method releases any resources or connections held by the broker. It is intended
     * to be called when the messaging broker is no longer needed, such as during application
     * shutdown or when the messaging subsystem is being explicitly unloaded. Implementations
     * should ensure that the connection is terminated gracefully to avoid resource leaks
     * or unexpected behavior in dependent systems.
     */
    fun close()

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
    fun publish(channel: String, payload: ByteArray): Boolean

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
    fun subscribe(channel: String, listener: (payload: ByteArray) -> Unit): Boolean
}
