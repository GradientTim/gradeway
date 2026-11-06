/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.lifecycle

import dev.gradienttim.gradeway.event.Event

/**
 * Fired on this server after data was migrated from another plugin.
 */
class MigrationCompletedEvent(
    val strategy: String,
    val fileName: String,
) : Event<Event.EmptyResult>(EmptyResult.None)
