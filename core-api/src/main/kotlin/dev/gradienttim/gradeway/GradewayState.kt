/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

/**
 * Represents the state of a Gradeway system with respect to its loading and unloading capabilities.
 *
 * GradewayState is an enumeration that defines the three possible states of a system: LOADED, UNLOADED, and PROCESSING.
 * Each state determines whether loading or unloading actions are permissible.
 *
 * @property allowLoad Specifies if loading is allowed in the current state.
 * @property allowUnload Specifies if unloading is allowed in the current state.
 */
enum class GradewayState(
    val allowLoad: Boolean,
    val allowUnload: Boolean,
) {
    /**
     * Indicates the state where the system has been fully loaded.
     *
     * In this state:
     * - Loading is disallowed.
     * - Unloading is allowed.
     *
     * This is typically used to represent a system that has already completed the loading process
     * and is ready for later operations, such as unloading or processing.
     */
    LOADED(false, true),

    /**
     * Indicates the state where the system is completely unloaded.
     *
     * In this state:
     * - Loading is allowed.
     * - Unloading is disallowed.
     *
     * This state typically represents a system that is empty and ready to be loaded with new items or data.
     */
    UNLOADED(true, false),

    /**
     * Indicates the state where the system is in a processing phase.
     *
     * In this state:
     * - Loading is disallowed.
     * - Unloading is disallowed.
     *
     * This state is typically used when the system is undergoing some form of intermediate operation,
     * where neither loading nor unloading actions are permissible until the processing is complete.
     */
    PROCESSING(false, false),
}
