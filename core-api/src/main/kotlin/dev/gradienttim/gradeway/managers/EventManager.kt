/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.event.Event

/**
 * Interface for managing and dispatching events in the system.
 *
 * EventManager provides a mechanism to handle custom event data by allowing
 * events to be fired and processed through the event system.
 */
interface EventManager {
    /**
     * Fires an event, dispatching it to all registered listeners for processing.
     *
     * @param event The event instance to be fired. The event must implement the Event interface.
     *              Listeners associated with this event type will handle the event.
     */
    fun <TEvent : Event> fire(event: TEvent)
}
