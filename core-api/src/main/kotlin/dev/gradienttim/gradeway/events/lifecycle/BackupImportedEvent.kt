/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.lifecycle

import dev.gradienttim.gradeway.event.Event

/**
 * Fired on this server after the backup [fileName] was imported.
 */
class BackupImportedEvent(
    val fileName: String,
    val wipe: Boolean,
) : Event<Event.EmptyResult>(EmptyResult.None)
