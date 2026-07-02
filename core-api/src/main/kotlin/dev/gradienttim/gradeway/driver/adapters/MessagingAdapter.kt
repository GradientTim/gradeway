/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver.adapters

import dev.gradienttim.gradeway.messaging.MessagingBroker
import dev.gradienttim.gradeway.platform.Environment

/**
 * Represents an adapter responsible for creating and configuring a messaging broker
 * instance based on the provided environment configuration.
 */
interface MessagingAdapter {
    /**
     * Creates and configures a messaging broker instance based on the provided environment configuration.
     *
     * @param environment The environment configuration containing the necessary settings for initializing the messaging broker.
     * @return A MessagingBroker instance configured with the specified environment settings.
     */
    fun createMessagingBroker(environment: Environment): MessagingBroker
}
