/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.event

/**
 * A listener subscribed to the [dev.gradienttim.gradeway.managers.EventManager].
 */
interface EventSubscription {
    /**
     * Whether the listener still receives events.
     */
    val isActive: Boolean

    /**
     * Stops the listener from receiving events. Calling it again has no effect.
     */
    fun unsubscribe()
}
