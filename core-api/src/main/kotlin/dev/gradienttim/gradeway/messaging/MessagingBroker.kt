/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging

import dev.gradienttim.gradeway.utilities.lifecycle.Closeable
import dev.gradienttim.gradeway.utilities.lifecycle.Openable

/**
 * Represents a broker interface for managing the publishing and subscribing
 * of messages across different messaging channels.
 *
 * This interface provides functionality to publish messages to specific channels
 * and to subscribe to channels to receive and process messages. It extends
 * the functionality provided by `Openable` and `Closeable`, enabling lifecycle
 * management for initialization and resource cleanup.
 */
interface MessagingBroker : Openable, Closeable {
    /**
     * Indicates whether a warning should be issued if encryption is not enabled for the messaging broker.
     *
     * This flag is used to inform users or developers about scenarios where encryption is not being used
     * for message transmission. Encryption ensures the security and confidentiality of the messages being
     * exchanged between brokers and subscribers. When set to `true`, the system may log a warning or notify
     * about the absence of encryption, encouraging appropriate security measures.
     *
     * This property is particularly useful in environments where encryption is not mandatory but strongly
     * recommended to protect sensitive data from potential interception or unauthorized access.
     */
    val warnNoEncryption: Boolean

    /**
     * Indicates whether a warning should be issued if signing is not enabled for the messaging broker.
     *
     * This flag is used to inform users or developers about scenarios where signing is not being used
     * for message transmission. Signing ensures the authenticity and integrity of the messages being
     * exchanged between brokers and subscribers, without affecting their confidentiality. When set to
     * `true`, the system may log a warning or notify about the absence of signing, encouraging
     * appropriate security measures.
     *
     * This property is particularly useful in environments where the underlying transport itself
     * doesn't already guarantee that a message wasn't forged or tampered with in transit.
     */
    val warnNoSigning: Boolean

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
     * Subscribes to a specific messaging channel and registers a handler to process received messages.
     *
     * This method allows registration of a callback function that will be invoked whenever
     * a message is published to the specified channel. The handler will receive the message's
     * payload as a byte array.
     *
     * @param channel The name of the channel to subscribe to. The channel acts as a logical
     * grouping for messages, and only messages associated with this channel will be forwarded
     * to the handler.
     * @param handler A callback function that will be executed for each message received on
     * the specified channel. The function takes a byte array representing the message payload
     * as its argument. Returns
     * @return A boolean indicating whether the subscription was successfully established.
     */
    fun subscribe(channel: String, handler: (payload: ByteArray) -> Boolean): Boolean
}
