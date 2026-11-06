/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.lifecycle

import dev.gradienttim.gradeway.event.Event
import java.nio.file.Path

/**
 * Fired on this server after a backup was exported to [path].
 */
class BackupExportedEvent(
    val path: Path,
) : Event<Event.EmptyResult>(EmptyResult.None)
