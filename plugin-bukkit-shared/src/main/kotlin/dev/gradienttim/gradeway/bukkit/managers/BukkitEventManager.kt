/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.managers

import dev.gradienttim.gradeway.bukkit.events.role.GradewayRoleCreateEvent
import dev.gradienttim.gradeway.bukkit.events.role.GradewayRoleDeleteEvent
import dev.gradienttim.gradeway.bukkit.events.role.GradewayRoleUpdateEvent
import dev.gradienttim.gradeway.event.CancellableEvent
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.events.role.RoleCreateEvent
import dev.gradienttim.gradeway.events.role.RoleDeleteEvent
import dev.gradienttim.gradeway.events.role.RoleUpdateEvent
import dev.gradienttim.gradeway.managers.EventManager
import org.bukkit.event.Cancellable
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.Event as BukkitEvent

class BukkitEventManager(val plugin: JavaPlugin) : EventManager {
    override fun <TEvent : Event> fire(event: TEvent) {
        val bukkitEvent = convertGradewayToBukkitEvent(event)
        plugin.server.pluginManager.callEvent(bukkitEvent)

        if (event is CancellableEvent && bukkitEvent is Cancellable) {
            event.isCancelled = bukkitEvent.isCancelled
        }
    }

    private fun <TEvent : Event> convertGradewayToBukkitEvent(event: TEvent): BukkitEvent {
        return when (event) {
            is RoleCreateEvent -> GradewayRoleCreateEvent(event.role)
            is RoleDeleteEvent -> GradewayRoleDeleteEvent(event.role)
            is RoleUpdateEvent -> GradewayRoleUpdateEvent(event.to, event.from)
            else -> throw IllegalArgumentException("Unhandled Gradeway event type: ${event::class.simpleName}")
        }
    }
}
