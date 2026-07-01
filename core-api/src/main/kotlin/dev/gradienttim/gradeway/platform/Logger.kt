/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

interface Logger {
    /**
     * Logs an informational message.
     *
     * @param message The message to log.
     */
    fun info(message: String)

    /**
     * Logs a warning message.
     *
     * @param message The message to log.
     */
    fun warn(message: String)

    /**
     * Logs an error message.
     *
     * @param message The message to log.
     */
    fun error(message: String)
}
