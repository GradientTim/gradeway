/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.event

/**
 * Represents an abstract implementation of an event that can be canceled.
 *
 * Classes inheriting from this event can provide functionality to prevent
 * the execution of actions associated with the event by setting the
 * [isCancelled] property to `true`.
 *
 * Cancelling an event allows handling logic to respect modifications to
 * the default behavior of the system in which the event occurs.
 */
abstract class CancellableEvent : Event {
    /**
     * Indicates whether the event has been canceled.
     *
     * When set to `true`, this property prevents the default behavior
     * or further processing of the event in the system. If `false`, the
     * event proceeds as intended without interruption.
     *
     * This property is intended to be used by subclasses of [CancellableEvent],
     * enabling them to modify or halt the event's default execution flow as needed.
     */
    var isCancelled: Boolean = false
}
