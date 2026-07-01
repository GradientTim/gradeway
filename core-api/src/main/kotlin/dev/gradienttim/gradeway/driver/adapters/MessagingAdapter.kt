/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver.adapters

import dev.gradienttim.gradeway.platform.Environment

interface MessagingAdapter {
    /**
     * Opens the messaging adapter and initializes the connection using the provided environment.
     *
     * @param environment The environment containing configuration values required to establish the connection.
     */
    fun open(environment: Environment)

    /**
     * Closes the messaging adapter and releases any held resources.
     */
    fun close()
}
