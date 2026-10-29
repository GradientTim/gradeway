/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.event

import org.bukkit.event.Cancellable
import org.bukkit.event.Event

abstract class CancellableEvent : Event(), Cancellable {
    private var isCancelled: Boolean = false

    override fun setCancelled(cancel: Boolean) {
        this.isCancelled = cancel
    }

    override fun isCancelled(): Boolean = this.isCancelled
}
