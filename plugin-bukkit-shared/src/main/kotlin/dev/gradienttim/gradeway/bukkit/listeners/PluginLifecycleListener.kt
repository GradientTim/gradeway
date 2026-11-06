/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.listeners

import dev.gradienttim.gradeway.GradewayLifecycle
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent

/**
 * Removes the Gradeway event subscriptions of plugins that get disabled, so they do not leak.
 */
class PluginLifecycleListener(
    val gradeway: GradewayLifecycle<*>,
) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPluginDisable(event: PluginDisableEvent) {
        gradeway.events.unsubscribeAll(event.plugin)
    }
}
