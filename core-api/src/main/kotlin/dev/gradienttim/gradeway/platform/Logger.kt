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

    /**
     * Logs a critical error and halts the application's normal execution flow.
     * This method is intended to handle unrecoverable errors by logging the error
     * and optionally triggering further processes such as application termination
     * or external error reporting.
     *
     * @param throwable The throwable instance representing the critical error
     *                  that caused the application to panic.
     */
    fun panic(throwable: Throwable)
}
