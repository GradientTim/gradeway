/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.event

/**
 * The order in which listeners receive an event, from [LOWEST] first to [MONITOR] last.
 *
 * Listeners with a later priority have the final say over an event's result.
 */
enum class EventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,

    /**
     * Runs last and only observes the final state of an event. Must not change the result.
     */
    MONITOR,
}
